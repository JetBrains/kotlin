/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers.SequenceTransformer
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal const val KOTLIN_SEQUENCES_PREFIX = "kotlin.sequences.SequencesKt."
private const val SEQUENCE_OF = KOTLIN_SEQUENCES_PREFIX + "sequenceOf"
internal const val MAP = KOTLIN_SEQUENCES_PREFIX + "map"
internal const val MAP_INDEXED = KOTLIN_SEQUENCES_PREFIX + "mapIndexed"
internal const val MAP_NOT_NULL = KOTLIN_SEQUENCES_PREFIX + "mapNotNull"
internal const val MAP_NOT_NULL_INDEXED = KOTLIN_SEQUENCES_PREFIX + "mapIndexedNotNull"

internal var IrExpression.sequenceDataOfExpression: SequenceData? by irAttribute(true)

internal var IrValueDeclaration.sequenceDataOfVariable: SequenceData? by irAttribute(true)
// sequence data is gathered from sequence producers or existing sequence variables,
// extended by transformer calls, and consumed by for loops and consumer calls


private fun isSafeToLowerFromSequenceOf(expression: IrExpression): Boolean = expression.isSafeToReevaluate()


internal fun gatherVarargArgumentFromSequenceOf(argument: IrExpression): List<IrExpression>? {
    return if (argument is IrVararg) {
        // sequenceOf(vararg arguments)
        if (argument.elements.any { it is IrSpreadElement }) return null // skip lowering sequenceOf with spread arguments
        if (argument.elements.any { !isSafeToLowerFromSequenceOf(it as IrExpression) }) return null
        argument.elements.map { it as IrExpression }
    } else {
        // sequenceOf(argument)
        if (!isSafeToLowerFromSequenceOf(argument)) return null
        listOf(argument)
    }
}

internal fun IrExpression.isSafeToReevaluate(): Boolean {
    var safe = true
    this.acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            if (safe) element.acceptChildrenVoid(this)
        }

        override fun visitCall(expression: IrCall) {
            if (!expression.isPrimitiveIntrinsic()) {
                safe = false
            } else {
                expression.acceptChildrenVoid(this)
            }
        }

        // could be constructor calls
        override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
            safe = false
        }

        // `toString()` of the arguments can have side effects
        override fun visitStringConcatenation(expression: IrStringConcatenation) {
            if (expression.arguments.all { it.type.isPrimitiveOrString() }) {
                super.visitStringConcatenation(expression)
            } else {
                safe = false
            }
        }

        override fun visitGetField(expression: IrGetField) {
            if (expression.symbol.owner.isFinal) {
                super.visitGetField(expression)
            } else {
                safe = false
            }
        }

        override fun visitSetValue(expression: IrSetValue) {
            safe = false
        }

        override fun visitSetField(expression: IrSetField) {
            safe = false
        }

        override fun visitGetValue(expression: IrGetValue) {
            val owner = expression.symbol.owner
            if (owner is IrVariable && owner.isVar) safe = false
        }
    })
    return safe
}

private fun IrCall.isPrimitiveIntrinsic(): Boolean {
    val owner = symbol.owner

    val parentClass = owner.parent as? IrClass ?: return false
    return parentClass.defaultType.isPrimitiveOrString() && arguments.all { it == null || it.type.isPrimitiveOrString() }
}

private fun IrType.isPrimitiveOrString(): Boolean = isPrimitiveType() || isString()

internal class SequenceDataGatherer(val context: JvmBackendContext) : IrVisitorVoid() {
    private val visitedVariables = mutableSetOf<IrVariable>()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitVariable(declaration: IrVariable) {
        if (!visitedVariables.add(declaration)) return
        super.visitVariable(declaration)
        if (declaration.isVar) return
        if (!isSequenceType(context, declaration)) return
        val expressionSequenceData = declaration.initializer?.sequenceDataOfExpression
        val readCounter = declaration.readCounter ?: 0
        declaration.symbol.owner.sequenceDataOfVariable = if (expressionSequenceData?.isMovable == false ||
            // the elements and the lambdas are copied to every place of use, which grows exponentially when they contain copies
            // e.g. `val b = sequence { yieldAll(a); yieldAll(a) }; val c = sequence { yieldAll(b); yieldAll(b) }; ...`;
            // we cut the chain at copying variables which already contain copies
            readCounter > 1 && expressionSequenceData?.containsVariableCopies == true
        ) {
            null
        } else {
            expressionSequenceData
        }
    }

    // assigns sequence data of the variable to the corresponding expression
    override fun visitGetValue(expression: IrGetValue) {
        super.visitGetValue(expression)
        // now the children have assigned appropriate sequence data
        if (!isSequenceType(context, expression)) return
        val variableDeclaration = expression.symbol.owner
        variableDeclaration.accept(this, null)
        expression.sequenceDataOfExpression = variableDeclaration.sequenceDataOfVariable?.asVariableCopy()
    }

    private fun IrCall.receiverSequenceData(): SequenceData? =
        arguments.firstOrNull()?.sequenceDataOfExpression

    private fun IrCall.appendTransformer(
        receiverData: SequenceData,
        transformer: SequenceTransformer,
        copiedExpression: IrExpression?,
        isMovable: Boolean = true,
    ) {
        sequenceDataOfExpression = SequenceData(
            receiverData.sequenceSource,
            receiverData.transformers + transformer,
            receiverData.isMovable && isMovable,
            receiverData.containsVariableCopies || copiedExpression?.containsFusedVariableCopy() == true,
            receiverData.isVariableCopy,
        )
    }

    private fun matchWithMap(
        expression: IrCall,
        isIndexed: Boolean,
        isNotNull: Boolean,
    ) {
        val receiverData = expression.receiverSequenceData() ?: return
        val fnArg = getPredicateArgument(expression, 1, isEvaluatedOnce = true) ?: return
        expression.appendTransformer(
            receiverData,
            SequenceTransformer.Map(fnArg, isIndexed, isNotNull),
            copiedExpression = fnArg,
            isMovable = fnArg.isFunctionExpressionSafeToReevaluate(),
        )
    }


    private fun extractSequenceArgumentType(sequenceType: IrType): IrType? =
        (sequenceType as? IrSimpleType)?.arguments?.singleOrNull()?.typeOrNull



    private fun matchWithSequenceOf(expression: IrCall) {
        // store the sequence of arguments inside the sequence source
        if (expression.arguments.size > 1) return
        val elementType = extractSequenceArgumentType(expression.type) ?: return
        if (expression.arguments.isEmpty()) {
            expression.sequenceDataOfExpression = SequenceData(
                SequenceSource.Empty,
            )
            return
        }
        val argument = expression.arguments.getOrNull(0) ?: return
        require(expression.arguments.size == 1) { "SequenceOf should have exactly one argument in the IR: ${expression.dump()}" }
        val sequenceOfArguments = gatherVarargArgumentFromSequenceOf(argument) ?: return
        val sequenceSource = SequenceSource.SequenceOf(sequenceOfArguments, elementType)
        expression.sequenceDataOfExpression = SequenceData(sequenceSource)
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)
        if (!isSequenceType(context, expression)) return
        val functionName = expression.symbol.owner.fqNameWhenAvailable?.asString()
        when (functionName) {
            MAP -> matchWithMap(expression, isIndexed = false, isNotNull = false)
            MAP_INDEXED -> matchWithMap(expression, isIndexed = true, isNotNull = false)
            MAP_NOT_NULL -> matchWithMap(expression, isIndexed = false, isNotNull = true)
            MAP_NOT_NULL_INDEXED -> matchWithMap(expression, isIndexed = true, isNotNull = true)
            SEQUENCE_OF -> matchWithSequenceOf(expression)
        }
    }
}
