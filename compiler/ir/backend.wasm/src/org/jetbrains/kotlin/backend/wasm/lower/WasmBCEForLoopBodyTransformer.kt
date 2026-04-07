/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Transformer for loop bodies that replaces get/set operators with getWithoutBoundCheck/setWithoutBoundCheck
 * where the loop bounds prove the index is always in range.
 *
 * The unchecked functions are defined directly in the Wasm stdlib array classes and skip the
 * rangeCheck call, relying on the Wasm trap for out-of-bounds access.
 *
 * Port of Kotlin/Native's `KonanBCEForLoopBodyTransformer`.
 */
class WasmBCEForLoopBodyTransformer : ForLoopBodyTransformer() {
    lateinit var mainLoopVariable: IrVariable
    lateinit var loopHeader: ForLoopHeader
    lateinit var loopVariableComponents: Map<Int, List<IrVariable>>
    lateinit var context: CommonBackendContext

    private var analysisResult: BoundsCheckAnalysisResult = BoundsCheckAnalysisResult(false, null)

    override fun transform(
        context: CommonBackendContext,
        loopBody: IrExpression,
        loopVariable: IrVariable,
        forLoopHeader: ForLoopHeader,
        loopComponents: Map<Int, List<IrVariable>>,
    ) {
        this.context = context
        mainLoopVariable = loopVariable
        loopHeader = forLoopHeader
        loopVariableComponents = loopComponents
        analysisResult = analyzeLoopHeader(loopHeader)
        if (analysisResult.boundsAreSafe && analysisResult.arrayInLoop != null)
            loopBody.transformChildrenVoid(this)
    }

    private inline fun IrGetValue.compareConstValue(compare: (IrExpression) -> Boolean): Boolean {
        val variable = symbol.owner
        return if (variable is IrVariable && !variable.isVar && variable.initializer != null) {
            compare(variable.initializer!!)
        } else false
    }

    private fun IrExpression.compareIntegerNumericConst(compare: (Long) -> Boolean): Boolean {
        @Suppress("UNCHECKED_CAST")
        return when (this) {
            is IrConst -> value is Number && compare((value as Number).toLong())
            is IrGetValue -> compareConstValue { it.compareIntegerNumericConst(compare) }
            else -> false
        }
    }

    private fun IrExpression.compareFloatNumericConst(compare: (Double) -> Boolean): Boolean {
        @Suppress("UNCHECKED_CAST")
        return when (this) {
            is IrConst -> value is Number && compare((value as Number).toDouble())
            is IrGetValue -> compareConstValue { it.compareFloatNumericConst(compare) }
            else -> false
        }
    }

    private fun IrType.isBasicArray() = isPrimitiveArray() || isArray() || isUnsignedArray()

    private fun IrCall.isGetSizeCall() = dispatchReceiver?.type?.isBasicArray() == true &&
            symbol.owner == dispatchReceiver!!.type.getClass()!!.getPropertyGetter("size")!!.owner

    /**
     * Resolve an expression to an [IrCall] by following through immutable variable references.
     * In Wasm, `until` may be inlined before ForLoopsLowering, so `arr.size` may be stored in a
     * temporary variable. This helper traces through such variables to find the underlying call.
     */
    private fun resolveToCall(expression: IrExpression): IrCall? = when (expression) {
        is IrCall -> expression
        is IrGetValue -> {
            val variable = expression.symbol.owner
            if (variable is IrVariable && !variable.isVar && variable.initializer != null) {
                resolveToCall(variable.initializer!!)
            } else null
        }
        else -> null
    }

    private fun IrCall.dispatchReceiverIsGetSizeCall(): Boolean {
        val resolvedCall = dispatchReceiver?.let { resolveToCall(it) } ?: return false
        return resolvedCall.isGetSizeCall()
    }

    private fun lessThanSize(functionCall: IrCall): BoundsCheckAnalysisResult {
        val boundsAreSafe = when (functionCall.symbol.owner.name) {
            OperatorNameConventions.DEC ->
                functionCall.dispatchReceiverIsGetSizeCall()
            OperatorNameConventions.MINUS -> {
                val value = functionCall.arguments[1]
                functionCall.dispatchReceiverIsGetSizeCall() &&
                        value?.compareIntegerNumericConst { it > 0 } == true
            }
            OperatorNameConventions.DIV -> {
                val value = functionCall.arguments[1]
                functionCall.dispatchReceiverIsGetSizeCall() &&
                        value?.compareFloatNumericConst { it > 1 } == true
            }
            else -> false
        }
        return BoundsCheckAnalysisResult(
            boundsAreSafe,
            functionCall.dispatchReceiver?.let { resolveToCall(it) }?.dispatchReceiver?.takeIf { boundsAreSafe }?.let {
                findExpressionValueDescription(it)
            }
        )
    }

    private inline fun checkIrGetValue(
        value: IrGetValue,
        condition: (IrExpression) -> BoundsCheckAnalysisResult,
    ): BoundsCheckAnalysisResult {
        val variable = value.symbol.owner
        return if (variable is IrVariable && !variable.isVar && variable.initializer != null) {
            condition(variable.initializer!!)
        } else {
            BoundsCheckAnalysisResult(false, null)
        }
    }

    private fun checkIrCallCondition(
        expression: IrExpression,
        condition: (IrCall) -> BoundsCheckAnalysisResult,
    ): BoundsCheckAnalysisResult =
        when (expression) {
            is IrCall -> condition(expression)
            is IrGetValue -> checkIrGetValue(expression) { valueInitializer -> checkIrCallCondition(valueInitializer, condition) }
            else -> BoundsCheckAnalysisResult(false, null)
        }

    private val IrProperty.canChangeValue: Boolean
        get() {
            if (isVar || isDelegated)
                return true

            val overrideBackingField = backingField?.let {
                getter != null && getter?.origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            } ?:
                if (isFakeOverride)
                    resolveFakeOverride()?.canChangeValue
                else true

            return overrideBackingField ?: true
        }

    private fun findExpressionValueDescription(expression: IrExpression): ValueDescription? {
        return when (expression) {
            is IrGetValue -> {
                when (val declaration = expression.symbol.owner) {
                    is IrVariable -> {
                        if (declaration.isVar) return null
                        val initializerDescription = declaration.initializer?.let { findExpressionValueDescription(it) }
                        initializerDescription ?: LocalValueDescription(expression.symbol)
                    }
                    is IrValueParameter -> LocalValueDescription(expression.symbol)
                    else -> null
                }
            }
            is IrCall -> {
                val propertySymbol = expression.symbol.owner.correspondingPropertySymbol

                if (propertySymbol == null || propertySymbol.owner.canChangeValue)
                    return null

                val valueDescriptionFromDispatchReceiver = expression.dispatchReceiver?.let {
                    findExpressionValueDescription(it) ?: return null
                }

                PropertyValueDescription(valueDescriptionFromDispatchReceiver, propertySymbol)
            }
            is IrGetObjectValue -> {
                ObjectValueDescription(expression.symbol)
            }
            else -> null
        }
    }

    private fun checkLastElement(last: IrExpression, loopHeader: ProgressionLoopHeader): BoundsCheckAnalysisResult =
        checkIrCallCondition(last) { call ->
            if (call.isGetSizeCall() && !loopHeader.headerInfo.isLastInclusive) {
                BoundsCheckAnalysisResult(true, call.dispatchReceiver?.let { findExpressionValueDescription(it) })
            } else {
                lessThanSize(call)
            }
        }

    private fun IrExpression.isProgressionPropertyGetter(propertyName: String) =
        this is IrCall && symbol.owner.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR &&
                (symbol.signature as? IdSignature.AccessorSignature)?.propertySignature?.asPublic()?.shortName == propertyName &&
                dispatchReceiver?.type?.getClass()?.symbol in context.symbols.progressionClasses

    private val untilFqName = FqName("kotlin.ranges.until")

    private fun analyzeLoopHeader(loopHeader: ForLoopHeader): BoundsCheckAnalysisResult {
        var analysisResult = BoundsCheckAnalysisResult(false, null)
        when (loopHeader) {
            is ProgressionLoopHeader ->
                when (loopHeader.headerInfo.direction) {
                    ProgressionDirection.INCREASING -> {
                        if (!loopHeader.headerInfo.first.compareIntegerNumericConst { it >= 0 }) {
                            return analysisResult
                        }
                        if (loopHeader.headerInfo.last is IrCall) {
                            val functionCall = (loopHeader.headerInfo.last as IrCall)
                            if (loopHeader.headerInfo.progressionType.getProgressionLastElementFunction == functionCall.symbol) {
                                val nestedLastVariable = functionCall.arguments[1]
                                if (nestedLastVariable is IrGetValue && nestedLastVariable.symbol.owner is IrVariable) {
                                    val nestedLast = (nestedLastVariable.symbol.owner as IrVariable).initializer
                                    analysisResult = checkLastElement(nestedLast!!, loopHeader)
                                }
                            } else {
                                analysisResult = checkLastElement(functionCall, loopHeader)
                            }
                        } else {
                            analysisResult = checkLastElement(loopHeader.headerInfo.last, loopHeader)
                        }
                    }
                    ProgressionDirection.DECREASING -> {
                        val valueToCompare = if (loopHeader.headerInfo.isLastInclusive) 0 else -1
                        var boundsAreSafe = false
                        if (loopHeader.headerInfo.last is IrCall) {
                            val functionCall = (loopHeader.headerInfo.last as IrCall)
                            if (loopHeader.headerInfo.progressionType.getProgressionLastElementFunction == functionCall.symbol) {
                                if (functionCall.arguments[1]?.compareIntegerNumericConst { it >= valueToCompare } == true) {
                                    boundsAreSafe = true
                                }
                            }
                        } else if (loopHeader.headerInfo.last.compareIntegerNumericConst { it >= valueToCompare }) {
                            boundsAreSafe = true
                        }
                        if (!boundsAreSafe)
                            return analysisResult

                        analysisResult = checkIrCallCondition(loopHeader.headerInfo.first, ::lessThanSize)
                    }
                    ProgressionDirection.UNKNOWN ->
                        if (loopHeader.headerInfo.first.isProgressionPropertyGetter("first") &&
                            loopHeader.headerInfo.last.isProgressionPropertyGetter("last")
                        ) {
                            val firstReceiver = ((loopHeader.headerInfo.first as IrCall).dispatchReceiver as? IrGetValue)?.symbol?.owner
                            val lastReceiver = ((loopHeader.headerInfo.last as IrCall).dispatchReceiver as? IrGetValue)?.symbol?.owner
                            if (firstReceiver == lastReceiver) {
                                val createRangeCall = (firstReceiver as? IrVariable)?.initializer as? IrCall
                                val createRange =
                                    if (createRangeCall?.symbol?.owner?.parameters?.firstOrNull()?.kind == IrParameterKind.ExtensionReceiver) {
                                        createRangeCall.arguments[0] as? IrCall
                                    } else null
                                val first = createRange?.symbol?.owner?.let {
                                    if (it.fqNameWhenAvailable == untilFqName || createRange.origin == IrStatementOrigin.RANGE_UNTIL) {
                                        createRange.arguments[0]
                                    } else null
                                }
                                if (first?.compareIntegerNumericConst { it >= 0 } == true) {
                                    val last = createRange.arguments[1]!!
                                    analysisResult = checkIrCallCondition(last) { call ->
                                        if (call.isGetSizeCall())
                                            BoundsCheckAnalysisResult(
                                                true,
                                                call.dispatchReceiver?.let { findExpressionValueDescription(it) })
                                        else
                                            lessThanSize(call)
                                    }
                                }
                            }
                        }
                }

            is WithIndexLoopHeader ->
                when (loopHeader.nestedLoopHeader) {
                    is IndexedGetLoopHeader -> {
                        analysisResult = BoundsCheckAnalysisResult(
                            true,
                            (loopHeader.loopInitStatements[0] as? IrVariable)?.initializer?.let {
                                findExpressionValueDescription(it)
                            })
                    }
                    is ProgressionLoopHeader -> analysisResult = analyzeLoopHeader(loopHeader.nestedLoopHeader)
                }
        }
        return analysisResult
    }

    private fun replaceOperators(expression: IrCall, index: IrExpression, safeIndexVariables: List<IrVariable>): IrExpression {
        if (index is IrGetValue && index.symbol.owner in safeIndexVariables) {
            val arrayClass = expression.arguments[0]!!.type.getClass() ?: return expression
            val targetName = if (expression.symbol.owner.name == OperatorNameConventions.SET)
                setWithoutBoundCheckName
            else
                getWithoutBoundCheckName
            val operatorWithoutBoundCheck = arrayClass.functions.singleOrNull { it.name == targetName }
                ?: return expression
            return IrCallImpl(
                expression.startOffset, expression.endOffset, expression.type, operatorWithoutBoundCheck.symbol,
                typeArgumentsCount = expression.typeArguments.size
            ).apply {
                expression.arguments.forEachIndexed { argIndex, arg ->
                    arguments[argIndex] = arg
                }
            }
        }
        return expression
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val newExpression = super.visitCall(expression)
        require(newExpression is IrCall)
        if (expression.symbol.owner.name != OperatorNameConventions.SET && expression.symbol.owner.name != OperatorNameConventions.GET)
            return newExpression
        if (expression.dispatchReceiver == null || expression.dispatchReceiver?.type?.isBasicArray() != true ||
            findExpressionValueDescription(expression.dispatchReceiver!!)?.equals(analysisResult.arrayInLoop!!) != true
        )
            return newExpression
        val index = newExpression.arguments[1]!!
        return when (loopHeader) {
            is ProgressionLoopHeader -> with(loopHeader as ProgressionLoopHeader) {
                replaceOperators(newExpression, index, listOf(mainLoopVariable, inductionVariable))
            }

            is WithIndexLoopHeader -> with(loopHeader as WithIndexLoopHeader) {
                when (nestedLoopHeader) {
                    is IndexedGetLoopHeader ->
                        replaceOperators(newExpression, index, listOfNotNull(indexVariable) + loopVariableComponents[1].orEmpty())
                    is ProgressionLoopHeader ->
                        replaceOperators(
                            newExpression, index,
                            listOfNotNull(indexVariable) + loopVariableComponents[1].orEmpty() + loopVariableComponents[2].orEmpty()
                        )
                    else -> newExpression
                }
            }

            else -> newExpression
        }
    }

    companion object {
        val getWithoutBoundCheckName: Name = Name.identifier("getWithoutBoundCheck")
        val setWithoutBoundCheckName: Name = Name.identifier("setWithoutBoundCheck")
    }
}

// Value description classes, mirroring Native's implementation
sealed class ValueDescription

data class LocalValueDescription(val variableSymbol: IrValueSymbol) : ValueDescription()

data class PropertyValueDescription(val receiver: ValueDescription?, val propertySymbol: IrPropertySymbol) : ValueDescription()

data class ObjectValueDescription(val classSymbol: IrClassSymbol) : ValueDescription()

internal class BoundsCheckAnalysisResult(val boundsAreSafe: Boolean, val arrayInLoop: ValueDescription?)
