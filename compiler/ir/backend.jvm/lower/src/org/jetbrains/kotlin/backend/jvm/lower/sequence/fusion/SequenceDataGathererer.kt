/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
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
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

private const val SEQUENCE_OF = "sequenceOf"
internal const val AS_SEQUENCE = "asSequence"
private const val GENERATE_SEQUENCE = "generateSequence"
private const val MAP = "map"
private const val MAP_INDEXED = "mapIndexed"
private const val MAP_NOT_NULL = "mapNotNull"
private const val MAP_NOT_NULL_INDEXED = "mapIndexedNotNull"
private const val FILTER = "filter"
private const val FILTER_NOT = "filterNot"
private const val FILTER_NOT_NULL = "filterNotNull"
private const val TAKE = "take"

// this is stored for expressions, intended to be passed either to value declarations or to for loops iterated over the expression result
internal var IrExpression.sequenceDataOfExpression: SequenceData? by irAttribute(true)

// this is stored to be one of the future sources of sequence data of expressions
internal var IrValueDeclaration.sequenceDataOfVariable: SequenceData? by irAttribute(true)
// In general, sequence data is gathered from `sequenceOf` or existing sequence variables, modified `by` map calls,
// and consumed by for loops and variable declarations

internal sealed class FilterVersion {
    object Filter : FilterVersion()
    object FilterNot : FilterVersion()
    object FilterNotNull : FilterVersion()
}

internal class SequenceDataGatherer(val context: JvmBackendContext) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitVariable(declaration: IrVariable) {
        super.visitVariable(declaration)
        if (declaration.isVar) return
        if (!isElementSequence(context, declaration)) return
        val expressionSequenceData = declaration.initializer?.sequenceDataOfExpression
        declaration.symbol.owner.sequenceDataOfVariable = if (expressionSequenceData?.sequenceSource is SequenceSource.GenerateSequence &&
            expressionSequenceData.sequenceSource.initialValue is GenerateSequenceInitialValue.NoInitialValue &&
            (declaration.usageCounter ?: 0) > 1
        ) {
            SequenceData(
                SequenceData.defaultMapReplacement,
                SequenceSource.Variable(declaration.symbol),
                SequenceData.defaultLoopPrologue,
                SequenceData.defaultTakeVariableDeclarations,
                emptyList(),
            )
        } else {
            expressionSequenceData?.let {
                SequenceData(
                    it.mapReplacement,
                    it.sequenceSource,
                    it.newLoopPrologue,
                    it.declarationsBeforeLoop,
                    emptyList(),
                )
            }
        }
    }

    // assigns sequence data of the variable to the corresponding expression
    override fun visitGetValue(expression: IrGetValue) {
        super.visitGetValue(expression)
        // now the children have assigned appropriate sequence data
        if (!isElementSequence(context, expression)) return
        expression.sequenceDataOfExpression = expression.symbol.owner.sequenceDataOfVariable?.let {
            SequenceData(
                it.mapReplacement,
                it.sequenceSource,
                it.newLoopPrologue,
                it.declarationsBeforeLoop,
                listOf(expression.startOffset to expression.endOffset)
            )
        } ?: SequenceData(
            SequenceData.defaultMapReplacement,
            SequenceSource.Variable(expression.symbol),
            SequenceData.defaultLoopPrologue,
            SequenceData.defaultTakeVariableDeclarations,
            listOf(expression.startOffset to expression.endOffset)
        )
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
                }
                super.visitCall(expression)
            }

            override fun visitSetValue(expression: IrSetValue) {
                safe = false
                super.visitSetValue(expression)
            }

            override fun visitSetField(expression: IrSetField) {
                safe = false
                super.visitSetField(expression)
            }

            override fun visitGetValue(expression: IrGetValue) {
                val owner = expression.symbol.owner
                if (owner is IrVariable && owner.isVar) safe = false
            }

            override fun visitConst(expression: IrConst) {}
        })
        return safe
    }

    private fun IrCall.isPrimitiveIntrinsic(): Boolean {
        val owner = symbol.owner

        val parentClass = owner.parent as? IrClass ?: return false
        return parentClass.defaultType.isPrimitiveType() || parentClass.defaultType.isString()
    }

    private fun isSafeToLower(reference: IrRichFunctionReference): Boolean {
        if (reference.boundValues.isNotEmpty()) return false
        if (reference.invokeFunction.dispatchReceiverParameter != null) return false
        return true
    }

    private fun isSafeToLower(expression: IrExpression): Boolean {
        if (containsMutable(expression)) return false
        when (expression) {
            is IrRichFunctionReference -> {
                return isSafeToLower(expression)
            }
        }
        return true
    }

    private fun isSafeToLowerFromSequenceOf(expression: IrExpression): Boolean {
        if (containsMutable(expression)) return false
        if (!expression.isSafeToMove()) return false // skip lowering if an expression contains something that has to be evaluated only once
        return true
    }

    private fun matchWithMap(
        expression: IrCall,
        isIndexed: Boolean,
        isNotNull: Boolean,
    ) {
        val receiver = expression.arguments.getOrNull(0) ?: return
        val receiverData = receiver.sequenceDataOfExpression ?: return
        val fnArg = expression.arguments.getOrNull(1) ?: return
        val fnRef = fnArg as? IrRichFunctionReference ?: return
        if (!isSafeToLower(fnRef)) return
        val results = if (isIndexed) {
            val indexVariableCell = object {
                var value: IrVariable? = null
            }
            val indexVariableLambda =
                { builder: IrBuilderWithScope ->
                    builder.scope.createTemporaryVariable(
                        builder.irInt(0),
                        isMutable = true,
                        nameHint = "mapIndexedVariable"
                    )
                }
            val getOrCreateIndexVariable = { builder: IrBuilderWithScope ->
                indexVariableCell.value ?: indexVariableLambda(builder).also {
                    indexVariableCell.value = it
                }
            }

            val updatedSequenceData = receiverData.addDeclarationExpectingBuilder(getOrCreateIndexVariable)
            SequenceData.MapReplacement { builderWithParent: IrBuilderWithParent, value: IrExpression ->
                val builder = builderWithParent.first
                val indexVariable = getOrCreateIndexVariable(builder)
                val mapReplacement = updatedSequenceData.createMapReplacement(fnRef, builder.irGet(indexVariable))
                builder.irBlock {
                    val mapResult = irTemporary(mapReplacement(builderWithParent, value), nameHint = "mapIndexedResult")
                    +irSet(indexVariable, irCall(context.irBuiltIns.intPlusSymbol).apply {
                        dispatchReceiver = irGet(indexVariable)
                        arguments[1] = irInt(1)
                    })
                    +irGet(mapResult)
                }
            } to updatedSequenceData
        } else receiverData.createMapReplacement(fnRef) to receiverData
        val newMapReplacement = results.first
        val sequenceDataWithDeclarations = results.second

        val mappedSequenceData = sequenceDataWithDeclarations.applyMap(newMapReplacement, expression.startOffset to expression.endOffset)
        val filteredSequenceData = if (isNotNull) {
            val filterNotNullSegment = mappedSequenceData.createNewFilterNotNullSegment()
            mappedSequenceData.applyFilter(expression.startOffset to expression.endOffset, filterNotNullSegment)
        } else {
            mappedSequenceData
        }
        expression.sequenceDataOfExpression = filteredSequenceData
    }

    private inline fun updateSequenceDataUsingExpression(
        call: IrCall,
        applyFunction: (SequenceData, IrExpression, Pair<Int, Int>) -> SequenceData
    ) {
        val receiver = call.arguments.getOrNull(0) ?: return
        val argumentExpression = call.arguments.getOrNull(1) ?: return
        val receiverData = receiver.sequenceDataOfExpression ?: return
        if (containsMutable(argumentExpression)) return
        call.sequenceDataOfExpression = applyFunction(receiverData, argumentExpression, call.startOffset to call.endOffset)
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

    private fun matchWithGenerateSequence(expression: IrCall) {
        val results = when (expression.arguments.size) {
            1 -> {
                // generateSequence(() -> T?)
                val func = expression.arguments.getOrNull(0) as? IrRichFunctionReference ?: return
                GenerateSequenceInitialValue.NoInitialValue to func
            }
            2 -> {
                val initialValueOrFunction = expression.arguments.getOrNull(0)
                val func = expression.arguments.getOrNull(1) as? IrRichFunctionReference ?: return
                when (initialValueOrFunction) {
                    is IrRichFunctionReference -> {
                        // generateSequence(() -> T?, (T) -> T?)
                        GenerateSequenceInitialValue.InitialFunction(initialValueOrFunction) to func
                    }
                    else -> {
                        // generateSequence(T?, (T) -> T?)
                        if (initialValueOrFunction == null) return
                        if (!isSafeToLower(initialValueOrFunction)) return
                        GenerateSequenceInitialValue.InitialValue(initialValueOrFunction) to func
                    }
                }
            }
            else -> {
                return
            }
        }
        val initialValue = results.first
        val func = results.second
        val elementType = extractSequenceArgumentType(expression.type) ?: return
        expression.sequenceDataOfExpression = SequenceData(
            SequenceData.defaultMapReplacement,
            SequenceSource.GenerateSequence(initialValue, func, elementType),
            SequenceData.defaultLoopPrologue,
            SequenceData.defaultTakeVariableDeclarations,
            listOf(expression.startOffset to expression.endOffset)
        )
    }

    private fun extractSequenceArgumentType(sequenceType: IrType): IrType? =
        (sequenceType as? IrSimpleType)?.arguments?.singleOrNull()?.let { return it.typeOrNull }

    private fun matchWithFilter(call: IrCall, version: FilterVersion) {
        val receiver = call.arguments.getOrNull(0) ?: return
        val receiverData = receiver.sequenceDataOfExpression ?: return
        val newSegment = when (version) {
            FilterVersion.Filter -> {
                val filterFunction = call.arguments.getOrNull(1) as? IrRichFunctionReference ?: return
                receiverData.createNewFilterSegment(filterFunction)
            }
            FilterVersion.FilterNot -> {
                val filterFunction = call.arguments.getOrNull(1) as? IrRichFunctionReference ?: return
                receiverData.createNewFilterNotSegment(filterFunction)
            }
            FilterVersion.FilterNotNull -> receiverData.createNewFilterNotNullSegment()
        }
        call.sequenceDataOfExpression = receiverData.applyFilter(
            call.startOffset to call.endOffset,
            newSegment
        )
    }

    private fun matchWithSequenceOf(expression: IrCall) {
        // store the sequence of arguments inside the sequence source
        if (expression.arguments.size > 1) return
        val elementType = extractSequenceArgumentType(expression.type) ?: return
        if (expression.arguments.isEmpty()) {
            expression.sequenceDataOfExpression = SequenceData(
                SequenceData.defaultMapReplacement,
                SequenceSource.SequenceOf(listOf(), elementType),
                SequenceData.defaultLoopPrologue,
                SequenceData.defaultTakeVariableDeclarations,
                listOf(expression.startOffset to expression.endOffset)
            )
            return
        }
        val argument = expression.arguments.getOrNull(0) ?: return
        val sequenceOfArguments = if (argument is IrVararg) {
            // sequenceOf(vararg arguments)
            if (argument.elements.any { it is IrSpreadElement }) return // skip lowering sequenceOf with spread arguments
            if (argument.elements.any { !isSafeToLowerFromSequenceOf(it as IrExpression) }) return
            argument.elements.map { it as IrExpression }
        } else {
            // sequenceOf(argument)
            if (!isSafeToLowerFromSequenceOf(argument)) return
            listOf(argument)
        }
        expression.sequenceDataOfExpression = SequenceData(
            SequenceData.defaultMapReplacement,
            SequenceSource.SequenceOf(sequenceOfArguments, elementType),
            SequenceData.defaultLoopPrologue,
            SequenceData.defaultTakeVariableDeclarations,
            listOf(expression.startOffset to expression.endOffset)
        )
    }

    private fun matchWithAsSequence(expression: IrCall) {
        val receiver = expression.arguments.getOrNull(0) ?: return
        if (receiver is IrGetValue) {
            if (!isSafeToLower(receiver)) return
            if (!receiver.type.isSubtypeOfClass(context.irBuiltIns.iterableClass)) return
        }
        expression.sequenceDataOfExpression = SequenceData(
            SequenceData.defaultMapReplacement,
            SequenceSource.AsSequence(receiver),
            SequenceData.defaultLoopPrologue,
            SequenceData.defaultTakeVariableDeclarations,
            listOf(expression.startOffset to expression.endOffset)
        )
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)
        if (!isElementSequence(context, expression)) return
        val functionName = expression.symbol.owner.name.asString()
        when (functionName) {
            MAP -> matchWithMap(expression, isIndexed = false, isNotNull = false)
            MAP_INDEXED -> matchWithMap(expression, isIndexed = true, isNotNull = false)
            MAP_NOT_NULL -> matchWithMap(expression, isIndexed = false, isNotNull = true)
            MAP_NOT_NULL_INDEXED -> matchWithMap(expression, isIndexed = true, isNotNull = true)
            FILTER -> matchWithFilter(expression, FilterVersion.Filter)
            FILTER_NOT -> matchWithFilter(expression, FilterVersion.FilterNot)
            FILTER_NOT_NULL -> matchWithFilter(expression, FilterVersion.FilterNotNull)
            TAKE -> updateSequenceDataUsingExpression(expression, SequenceData::applyTake)
            GENERATE_SEQUENCE -> matchWithGenerateSequence(expression)
            SEQUENCE_OF -> matchWithSequenceOf(expression)
            AS_SEQUENCE -> matchWithAsSequence(expression)
        }
    }
}

