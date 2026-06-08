/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers

import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.*
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.LoweringStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.irAsNotNull
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBreak
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

abstract class ConsumerBodyReplacementCreator {
    abstract fun create(expression: IrCall): IrExpression?
}

internal fun createConsumerBodyReplacementCreator(
    functionName: String,
    context: JvmBackendContext,
    builder: IrBuilderWithScope,
    parent: IrDeclarationParent,
    sequenceData: SequenceData,
): ConsumerBodyReplacementCreator? {
    return when (functionName) {
        FOR_EACH -> ForEachBodyReplacementCreator(context, builder, parent, sequenceData)
        FIND -> FindBodyReplacementCreator(true, context, builder, parent, sequenceData)
        FIND_LAST -> FindBodyReplacementCreator(false, context, builder, parent, sequenceData)
        FIRST -> FirstLastBodyReplacementCreator(isOrNull = false, isFirst = true, context, builder, parent, sequenceData)
        FIRST_OR_NULL -> FirstLastBodyReplacementCreator(isOrNull = true, isFirst = true, context, builder, parent, sequenceData)
        FIRST_NOT_NULL_OF -> FirstNotNullOfBodyReplacementCreator(false, context, builder, parent, sequenceData)
        FIRST_NOT_NULL_OF_OR_NULL -> FirstNotNullOfBodyReplacementCreator(true, context, builder, parent, sequenceData)
        LAST -> FirstLastBodyReplacementCreator(isOrNull = false, isFirst = false, context, builder, parent, sequenceData)
        LAST_OR_NULL -> FirstLastBodyReplacementCreator(isOrNull = true, isFirst = false, context, builder, parent, sequenceData)
        FILTER_TO -> FilterToBodyReplacementCreator(FilterVersion.Filter, context, builder, parent, sequenceData)
        FILTER_NOT_TO -> FilterToBodyReplacementCreator(FilterVersion.FilterNot, context, builder, parent, sequenceData)
        FILTER_NOT_NULL_TO -> FilterToBodyReplacementCreator(FilterVersion.FilterNotNull, context, builder, parent, sequenceData)
        else -> null
    }
}

internal fun createFirstBody(builder: IrBuilderWithScope, loop: IrLoop): (IrExpression, IrVariable) -> IrExpression {
    return { argument: IrExpression, resultVariable: IrVariable ->
        builder.irBlock {
            +irSet(resultVariable, argument)
            +irBreak(loop)
        }
    }
}

internal fun createLastBody(builder: IrBuilderWithScope): (IrExpression, IrVariable) -> IrExpression {
    return { argument: IrExpression, resultVariable: IrVariable ->
        builder.irBlock {
            +irSet(resultVariable, argument)
        }
    }
}

internal fun lowerAndReturnVariable(
    builder: IrBuilderWithScope,
    parent: IrDeclarationParent,
    sequenceData: SequenceData,
    loop: IrLoop,
    resultVariable: IrVariable,
    loopBody: (IrVariable) -> IrContainerExpression,
    returnType: IrType
): IrExpression? {
    val strategy = sequenceData.sequenceSource.createStrategy(builder)
    val newBody = strategy.lowerLoop(builder to parent, loopBody, sequenceData, loop, null) ?: return null
    newBody.statements.add(builder.irGet(resultVariable))
    newBody.type = returnType
    return newBody
}

internal fun createFirstLastBody(
    strategy: LoweringStrategy,
    builderWithParent: IrBuilderWithParent,
    oldBody: (IrVariable) -> IrContainerExpression,
    sequenceData: SequenceData,
    loop: IrLoop,
    skippedIterationVariable: IrVariable,
    isOrNull: Boolean,
    context: JvmBackendContext,
): IrContainerExpression? {
    val builder = builderWithParent.first
    val newBody =
        strategy.lowerLoop(
            builderWithParent,
            oldBody,
            sequenceData,
            loop,
            null
        )
            ?: return null

    val notFoundStatement = if (isOrNull) null else builder.irIfThen(
        context.irBuiltIns.unitType,
        builder.irGet(skippedIterationVariable),
        builder.irThrow(
            builder.irCallConstructor(context.symbols.noSuchElementExceptionCtorString, emptyList()).apply {
                arguments[0] = builder.irString("Sequence is empty.")
            }
        )
    )

    if (notFoundStatement != null)
        newBody.statements.add(notFoundStatement)
    return newBody
}

internal inline fun firstLastDeclarations(
    expression: IrCall,
    sequenceData: SequenceData,
    builderWithParent: IrBuilderWithParent,
    context: JvmBackendContext,
    block: (
        builder: IrBuilderWithScope,
        parent: IrDeclarationParent,
        updatedData: SequenceData,
        strategy: LoweringStrategy,
        predicate: IrRichFunctionReference?,
        resultVariable: IrVariable,
        skippedIterationVariable: IrVariable,
    ) -> IrExpression
): IrExpression? {
    val builder = builderWithParent.first
    val resultVariable = builder.scope.createTemporaryVariable(
        builder.irNull(),
        isMutable = true,
        irType = expression.type.makeNullable(),
        nameHint = "FirstLastResult"
    )
    val skippedIterationVariable = builder.scope.createTemporaryVariable(
        builder.irTrue(),
        isMutable = true,
        irType = context.irBuiltIns.booleanType,
        nameHint = "skippedIteration"
    )
    val updatedSequenceData = sequenceData.addDeclaration(resultVariable).addDeclaration(skippedIterationVariable)
    val strategy = updatedSequenceData.sequenceSource.createStrategy(builder)
    val lambda = expression.arguments.getOrNull(1)
    if (lambda != null) {
        if (lambda !is IrRichFunctionReference) return null
    }
    return block(builder, builderWithParent.second, updatedSequenceData, strategy, lambda, resultVariable, skippedIterationVariable)
}

internal fun addResultGetValueToBody(
    expression: IrExpression,
    resultVariable: IrVariable,
    builder: IrBuilderWithScope,
    newBody: IrContainerExpression
) {
    if (expression.type == resultVariable.type) {
        newBody.statements.add(builder.irGet(resultVariable))
    } else if (expression.type.makeNullable() == resultVariable.type) {
        newBody.statements.add(builder.irAsNotNull(builder.irGet(resultVariable)))
    } else {
        error("resultVariable type mismatch: ${expression.type.dumpKotlinLike()}, ${resultVariable.type.dumpKotlinLike()}")
    }
}
