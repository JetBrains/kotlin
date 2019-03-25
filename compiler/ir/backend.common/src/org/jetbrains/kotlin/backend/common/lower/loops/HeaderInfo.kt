/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.matchers.IrCallMatcher
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
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
}

// Information about loop that is required by HeaderProcessor to build ForLoopHeader
internal sealed class HeaderInfo(
    val progressionType: ProgressionType,
    val lowerBound: IrExpression,
    val upperBound: IrExpression,
    val step: IrExpression?, // null value denotes default step (1)
    val increasing: Boolean,
    val needLastCalculation: Boolean,
    val closed: Boolean
)

internal class ProgressionHeaderInfo(
    progressionType: ProgressionType,
    lowerBound: IrExpression,
    upperBound: IrExpression,
    step: IrExpression? = null,
    increasing: Boolean = true,
    needLastCalculation: Boolean = false,
    closed: Boolean = true
) : HeaderInfo(progressionType, lowerBound, upperBound, step, increasing, needLastCalculation, closed)

internal class ArrayHeaderInfo(
    lowerBound: IrExpression,
    upperBound: IrExpression,
    val arrayDeclaration: IrValueDeclaration
) : HeaderInfo(
    ProgressionType.INT_PROGRESSION,
    lowerBound,
    upperBound,
    step = null,
    increasing = true,
    needLastCalculation = false,
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
        UntilHandler(progressionElementTypes),
        DownToHandler(progressionElementTypes),
        StepHandler(context, this),
        RangeToHandler(progressionElementTypes)
    )

    private fun IrType.getProgressionType(): ProgressionType? = when {
        isSubtypeOfClass(symbols.charProgression) -> ProgressionType.CHAR_PROGRESSION
        isSubtypeOfClass(symbols.intProgression) -> ProgressionType.INT_PROGRESSION
        isSubtypeOfClass(symbols.longProgression) -> ProgressionType.LONG_PROGRESSION
        else -> null
    }

    override fun visitElement(element: IrElement, data: Nothing?): HeaderInfo? = null

    override fun visitCall(expression: IrCall, data: Nothing?): HeaderInfo? {
        val progressionType = expression.type.getProgressionType()
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