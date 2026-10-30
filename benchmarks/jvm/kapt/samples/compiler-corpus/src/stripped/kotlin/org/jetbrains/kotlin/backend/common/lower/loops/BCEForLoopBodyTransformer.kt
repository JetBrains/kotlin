/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
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

// Base class describing value of expression.
sealed class ValueDescription

// Contains information about base variable symbol.
data class LocalValueDescription(val variableSymbol: IrValueSymbol) : ValueDescription()

// Contains information about property symbol and receiver's value description.
data class PropertyValueDescription(val receiver: ValueDescription?, val propertySymbol: IrPropertySymbol) : ValueDescription()

data class ObjectValueDescription(val classSymbol: IrClassSymbol) : ValueDescription()

// Class contains information about analyzed loop.
internal class BoundsCheckAnalysisResult(val boundsAreSafe: Boolean, val arrayInLoop: ValueDescription?)

// TODO: support `forEachIndexed`. Function is inlined and index is separate variable which isn't connected with loop induction variable.
/**
 * Transformer for loop bodies that replaces get/set operators with unchecked variants
 * (e.g. `getWithoutBoundCheck`/`setWithoutBoundCheck`) where the loop bounds prove
 * the index is always in range.
 *
 * The actual function names are backend-specific and read from
 * [CommonBackendContext.symbols].[getWithoutBoundCheckName][org.jetbrains.kotlin.backend.common.ir.BackendSymbols.getWithoutBoundCheckName] /
 * [setWithoutBoundCheckName][org.jetbrains.kotlin.backend.common.ir.BackendSymbols.setWithoutBoundCheckName].
 */
class BCEForLoopBodyTransformer : ForLoopBodyTransformer() {
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
    ) { TODO() }

    private inline fun IrGetValue.compareConstValue(compare: (IrExpression) -> Boolean): Boolean {
        val variable = symbol.owner
        return if (variable is IrVariable && !variable.isVar && variable.initializer != null) {
            compare(variable.initializer!!)
        } else false
    }

    private fun IrExpression.compareIntegerNumericConst(compare: (Long) -> Boolean): Boolean { TODO() }

    private fun IrExpression.compareFloatNumericConst(compare: (Double) -> Boolean): Boolean { TODO() }

    private fun IrType.isBasicArray() = isPrimitiveArray() || isArray() || isUnsignedArray()

    private fun IrCall.isGetSizeCall() = dispatchReceiver?.type?.isBasicArray() == true &&
            symbol.owner == dispatchReceiver!!.type.getClass()!!.getPropertyGetter("size")!!.owner

    /**
     * Resolve an expression to an [IrCall] by following through immutable variable references.
     * Some backends inline `until` before ForLoopsLowering, so `arr.size` may be stored in a
     * temporary variable. This helper traces through such variables to find the underlying call.
     */
    private fun resolveToCall(expression: IrExpression): IrCall? = TODO()

    private fun IrCall.dispatchReceiverIsGetSizeCall(): Boolean { TODO() }

    private fun lessThanSize(functionCall: IrCall): BoundsCheckAnalysisResult { TODO() }

    private inline fun checkIrGetValue(value: IrGetValue, condition: (IrExpression) -> BoundsCheckAnalysisResult): BoundsCheckAnalysisResult {
        val variable = value.symbol.owner
        return if (variable is IrVariable && !variable.isVar && variable.initializer != null) {
            condition(variable.initializer!!)
        } else {
            BoundsCheckAnalysisResult(false, null)
        }
    }

    private fun checkIrCallCondition(expression: IrExpression, condition: (IrCall) -> BoundsCheckAnalysisResult): BoundsCheckAnalysisResult = TODO()

    private val IrProperty.canChangeValue: Boolean
        get() {
            if (isVar || isDelegated)
                return true

            val overrideBackingField = backingField?.let {
                getter != null && getter?.origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            } ?:
                // Analyze inheritance.
                if (isFakeOverride)
                    resolveFakeOverride()?.canChangeValue
                else true

            return overrideBackingField ?: true
        }

    // Find base symbol with value or property and the main(first) dispatch receiver in the chain.
    // Top-level properties accessors and local variables/parameters have null receivers.
    private fun findExpressionValueDescription(expression: IrExpression): ValueDescription? { TODO() }

    private fun checkLastElement(last: IrExpression, loopHeader: ProgressionLoopHeader): BoundsCheckAnalysisResult = TODO()

    private fun IrExpression.isProgressionPropertyGetter(propertyName: String) =
        this is IrCall && symbol.owner.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR &&
                (symbol.signature as? IdSignature.AccessorSignature)?.propertySignature?.asPublic()?.shortName == propertyName &&
                dispatchReceiver?.type?.getClass()?.symbol in context.symbols.progressionClasses

    private val untilFqName = FqName("kotlin.ranges.until")

    private fun analyzeLoopHeader(loopHeader: ForLoopHeader): BoundsCheckAnalysisResult { TODO() }

    private fun replaceOperators(expression: IrCall, index: IrExpression, safeIndexVariables: List<IrVariable>): IrExpression { TODO() }

    override fun visitCall(expression: IrCall): IrExpression { TODO() }
}
