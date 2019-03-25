/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.matchers.IrCallMatcher
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

// TODO: Handle withIndex()
// TODO: Handle reversed()
// TODO: Handle direct iteration on Ranges/Progressions (e.g., NOT using rangeTo or step)
// TODO: Handle Strings/CharSequences

enum class ProgressionType(val numberCastFunctionName: Name) {
    INT_PROGRESSION(Name.identifier("toInt")),
    LONG_PROGRESSION(Name.identifier("toLong")),
    CHAR_PROGRESSION(Name.identifier("toChar"));

    /** Returns the [IrType] of the `first`/`last` properties and elements in the progression. */
    fun elementType(builtIns: IrBuiltIns): IrType = when (this) {
        ProgressionType.INT_PROGRESSION -> builtIns.intType
        ProgressionType.LONG_PROGRESSION -> builtIns.longType
        ProgressionType.CHAR_PROGRESSION -> builtIns.charType
    }

    companion object {
        fun fromIrType(irType: IrType, symbols: Symbols<CommonBackendContext>): ProgressionType? = when {
            irType.isSubtypeOfClass(symbols.charProgression) -> ProgressionType.CHAR_PROGRESSION
            irType.isSubtypeOfClass(symbols.intProgression) -> ProgressionType.INT_PROGRESSION
            irType.isSubtypeOfClass(symbols.longProgression) -> ProgressionType.LONG_PROGRESSION
            else -> null
        }
    }
}

// Information about loop that is required by HeaderProcessor to build ForLoopHeader
internal sealed class HeaderInfo(
    val progressionType: ProgressionType,
    val lowerBound: IrExpression,
    val upperBound: IrExpression,
    val step: IrExpression,
    val increasing: Boolean,
    val closed: Boolean
)

internal class ProgressionHeaderInfo(
    progressionType: ProgressionType,
    lowerBound: IrExpression,
    upperBound: IrExpression,
    step: IrExpression,
    increasing: Boolean = true,
    closed: Boolean = true
) : HeaderInfo(progressionType, lowerBound, upperBound, step, increasing, closed)

internal class ArrayHeaderInfo(
    lowerBound: IrExpression,
    upperBound: IrExpression,
    step: IrExpression,
    val arrayVariable: IrVariable
) : HeaderInfo(
    ProgressionType.INT_PROGRESSION,
    lowerBound,
    upperBound,
    step,
    increasing = true,
    closed = false
)

internal interface HeaderInfoHandler<T> {
    val matcher: IrCallMatcher

    fun build(call: IrCall, data: T): HeaderInfo?

    fun handle(irCall: IrCall, data: T) = if (matcher(irCall)) {
        build(irCall, data)
    } else {
        null
    }
}
internal typealias ProgressionHandler = HeaderInfoHandler<ProgressionType>

// We need to wrap builder into visitor because of `StepHandler` which has to visit its subtree
// to get information about underlying progression.
private class ProgressionHeaderInfoBuilder(val context: CommonBackendContext) : IrElementVisitor<HeaderInfo?, Nothing?> {

    private val symbols = context.ir.symbols

    private val progressionElementTypes = symbols.integerClassesTypes + symbols.char.descriptor.defaultType

    private val progressionHandlers = listOf(
        IndicesHandler(context),
        UntilHandler(context, progressionElementTypes),
        DownToHandler(context, progressionElementTypes),
        RangeToHandler(context, progressionElementTypes)
    )

    override fun visitElement(element: IrElement, data: Nothing?): HeaderInfo? = null

    override fun visitCall(expression: IrCall, data: Nothing?): HeaderInfo? {
        val progressionType = ProgressionType.fromIrType(expression.type, symbols)
            ?: return null
        return progressionHandlers.firstNotNullResult { it.handle(expression, progressionType) }
    }
}

internal class HeaderInfoBuilder(context: CommonBackendContext) {
    private val progressionInfoBuilder = ProgressionHeaderInfoBuilder(context)
    private val arrayIterationHandler = ArrayIterationHandler(context)

    fun build(variable: IrVariable): HeaderInfo? {
        val initializer = variable.initializer!! as IrCall
        return arrayIterationHandler.handle(initializer, null)
            ?: initializer.dispatchReceiver?.accept(progressionInfoBuilder, null)
    }
}