/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
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

private const val KOTLIN_SEQUENCES_PREFIX = "kotlin.sequences.SequencesKt."
private const val SEQUENCE_OF = KOTLIN_SEQUENCES_PREFIX + "sequenceOf"

// this is stored for expressions, intended to be passed either to value declarations or to for loops iterated over the expression result
internal var IrExpression.sequenceDataOfExpression: SequenceData? by irAttribute(true)

// this is stored to be one of the future sources of sequence data of expressions
internal var IrValueDeclaration.sequenceDataOfVariable: SequenceData? by irAttribute(true)
// In general, sequence data is gathered from `sequenceOf` or existing sequence variables, modified `by` map calls,
// and consumed by for loops and variable declarations

private fun isSafeToLowerFromSequenceOf(expression: IrExpression): Boolean {
    if (containsMutable(expression)) return false
    if (!expression.isSafeToMove()) return false // skip lowering if an expression contains something that has to be evaluated only once
    return true
}

internal fun gatherVarargArgument(argument: IrExpression): List<IrExpression>? {
    return if (argument is IrVararg) {
        // argument is vararg arguments
        if (argument.elements.any { it is IrSpreadElement }) return null // skip lowering sequenceOf with spread arguments
        if (argument.elements.any { !isSafeToLowerFromSequenceOf(it as IrExpression) }) return null
        argument.elements.map { it as IrExpression }
    } else {
        // single argument
        if (!isSafeToLowerFromSequenceOf(argument)) return null
        listOf(argument)
    }
}

private fun IrExpression.isSafeToMove(): Boolean {
    var safe = true
    this.acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            if (safe) element.acceptChildrenVoid(this)
        }

        override fun visitCall(expression: IrCall) {
            if (!expression.isPrimitiveIntrinsic()) {
                safe = false
            } else {
                super.visitCall(expression)
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
    return parentClass.defaultType.isPrimitiveType() || parentClass.defaultType.isString()
}

private fun containsMutable(expression: IrExpression): Boolean {
    var found = false
    expression.acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            if (!found) {
                element.acceptChildrenVoid(this)
            }
        }

        override fun visitGetValue(expression: IrGetValue) {
            val variable = expression.symbol.owner as? IrVariable ?: return
            if (variable.isVar) {
                found = true
            }
        }
    })
    return found
}

internal class SequenceDataGatherer(val context: JvmBackendContext) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitVariable(declaration: IrVariable) {
        super.visitVariable(declaration)
        if (declaration.isVar) return
        if (!isSequenceType(context, declaration)) return
        val expressionSequenceData = declaration.initializer?.sequenceDataOfExpression
        declaration.symbol.owner.sequenceDataOfVariable = expressionSequenceData
    }

    // assigns sequence data of the variable to the corresponding expression
    override fun visitGetValue(expression: IrGetValue) {
        super.visitGetValue(expression)
        // now the children have assigned appropriate sequence data
        if (!isSequenceType(context, expression)) return
        val variableDeclaration = expression.symbol.owner
        variableDeclaration.accept(this, null)
        expression.sequenceDataOfExpression = variableDeclaration.sequenceDataOfVariable
    }

    private fun extractSequenceArgumentType(sequenceType: IrType): IrType? =
        (sequenceType as? IrSimpleType)?.arguments?.singleOrNull()?.let { return it.typeOrNull }

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
        val sequenceOfArguments = gatherVarargArgument(argument) ?: return
        expression.sequenceDataOfExpression = SequenceData(
            SequenceSource.SequenceOf(sequenceOfArguments, elementType),
        )
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)
        if (!isSequenceType(context, expression)) return
        val functionName = expression.symbol.owner.fqNameWhenAvailable?.asString()
        when (functionName) {
            SEQUENCE_OF -> matchWithSequenceOf(expression)
        }
    }
}

