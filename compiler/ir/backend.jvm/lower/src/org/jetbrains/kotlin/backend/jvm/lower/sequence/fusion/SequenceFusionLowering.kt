/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion

import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers.*
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.EmptySequenceStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers.TransformerStrategy
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irUnit
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * transformation:
 * ```
 * fun myFun(seq: Sequence<Int>) {
 *     val seq2 = seq.map { it * 2 }.map { it + 1 }
 *     for (x in seq) println(x)
 * }
 * ```
 * becomes
 * ```
 * fun myFun(seq: Sequence<Int>) {
 *     val seq2 = seq.map { it * 2 }.map { it + 1 }
 *     for (x in seq) println({ y -> { x -> x * 2 }(y) + 1 }(x))
 * }
 * ```
 *
 * ```
 * val seq = sequenceOf(1, 2, 3).map { it * 2 }.map { it + 1 }
 * for (x in seq) println(x)
 * ```
 * becomes
 * ```
 * val seq = sequenceOf(1, 2, 3).map { it * 2 }.map { it + 1 }
 * {
 *     println({ y -> { x -> x * 2 }(y) + 1 }(1))
 *     println({ y -> { x -> x * 2 }(y) + 1 }(2))
 *     println({ y -> { x -> x * 2 }(y) + 1 }(3))
 * }
 * ```
 */

internal typealias ConsumerBodyBuilder = (IrValueDeclaration) -> IrContainerExpression

internal data class SequenceReplacement(
    val initialDeclarations: List<IrStatement>,
    val mainBodyBuilder: ConsumerBodyBuilder,
    val finalExpression: IrExpression,
)

class SequenceFusionLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val transformer = SequenceFusionTransformer(context)
        irFile.transformChildrenVoid(transformer)
    }
}

internal typealias IrBuilderWithParent = Pair<IrBuilderWithScope, IrDeclarationParent>

internal fun IrBuilderWithScope.callRichFunctionReference(
    ref: IrRichFunctionReference,
    parent: IrDeclarationParent,
    vararg args: IrExpression,
): IrExpression {
    val freshRef = ref.deepCopyWithSymbols(parent)
    val functionType = freshRef.type as? IrSimpleType
    val returnType = functionType?.arguments?.lastOrNull()?.typeOrNull ?: freshRef.overriddenFunctionSymbol.owner.returnType
    return irCall(freshRef.overriddenFunctionSymbol, returnType).apply {
        arguments.assignFrom(listOf(freshRef) + args)
    }
}

internal fun IrBuilderWithScope.callPredicate(
    predicate: IrExpression,
    parent: IrDeclarationParent,
    vararg args: IrExpression,
): IrExpression {
    return when (predicate) {
        is IrRichFunctionReference -> callRichFunctionReference(predicate, parent, *args)
        else -> {
            val invokeSymbol = predicate.type.classOrNull?.owner?.declarations
                ?.filterIsInstance<IrSimpleFunction>()
                ?.firstOrNull { it.name.asString() == "invoke" }?.symbol
                ?: error("Didn't find invoke for the predicate: ${predicate.dump()}")
            irCall(invokeSymbol).apply {
                dispatchReceiver = predicate.deepCopyWithSymbols(parent)
                args.forEachIndexed { index, arg ->
                    arguments[index + 1] = arg.deepCopyWithSymbols(parent)
                }
            }
        }
    }
}

internal fun IrBuilderWithScope.increment(incrementVariable: IrVariable): IrExpression =
    irSet(incrementVariable, irCall(context.irBuiltIns.intPlusSymbol).apply {
        dispatchReceiver = irGet(incrementVariable)
        arguments[1] = irInt(1)
    })

internal fun isSequenceType(context: JvmBackendContext, element: IrElement): Boolean {
    val sequenceSymbol = context.symbols.sequence ?: return false
    val type = when (element) {
        is IrExpression -> element.type
        is IrVariable -> element.type
        else -> return false
    }
    return type.isSubtypeOfClass(sequenceSymbol)
}

private fun lookupForLoopVariable(loopBody: IrBlock): IrVariable = loopBody.statements.filterIsInstance<IrVariable>()
    .singleOrNull { it.origin == IrDeclarationOrigin.FOR_LOOP_VARIABLE }
    ?: error("No variable with origin FOR_LOOP_VARIABLE found inside a FOR_LOOP origin while")

internal fun getPredicateArgument(expression: IrCall, argument: Int): IrExpression? {
    val predicate = expression.arguments.getOrNull(argument)
    // we don't want to duplicate calls
    if (predicate is IrCall) return null
    return predicate
}

internal data class LoopData(
    val loop: IrLoop?,
    val loopVariable: IrVariable,
    val loopBody: IrBlock,
)

internal fun gatherLoopData(block: IrBlock, parent: IrDeclarationParent): LoopData? {
    if (block.origin != IrStatementOrigin.FOR_LOOP) return null

    // extract loop iterator variable and loop body from IrBlock
    if (block.statements.size != 2) return null
    val blockCopy = block.deepCopyWithSymbols(parent)
    val loop = blockCopy.statements[1] as? IrWhileLoop ?: return null
    val body = loop.body as? IrBlock ?: return null
    val loopVariable = lookupForLoopVariable(body)
    body.statements.remove(loopVariable)
    return LoopData(loop, loopVariable, body)
}

private class SequenceFusionTransformer(val context: JvmBackendContext) : IrElementTransformerVoidWithContext() {
    override fun visitBlock(expression: IrBlock): IrExpression {
        val visitedExpression = super.visitBlock(expression)
        if (visitedExpression !is IrBlock) return visitedExpression

        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        val parent = currentScope?.scope?.scopeOwnerSymbol as? IrDeclarationParent ?: currentDeclarationParent ?: return visitedExpression
        val receiver =
            ((expression.statements.getOrNull(0) as? IrVariable)?.initializer as? IrCall)?.arguments?.getOrNull(0)
                ?: return visitedExpression
        val gatherer = SequenceDataGatherer(context)
        receiver.accept(gatherer, null)
        val sequenceData = receiver.sequenceDataOfExpression ?: return visitedExpression
        val data = ConsumerData(context, builder, parent, sequenceData)
        val consumerStrategy = createConsumerStrategy(visitedExpression, data) ?: return visitedExpression
        val sequenceReplacement = deployTransformerStrategies(consumerStrategy, sequenceData, builder to parent) ?: return visitedExpression
        val producerStrategy = sequenceData.sequenceSource.createProducerStrategy(builder, context)
        if (producerStrategy is EmptySequenceStrategy && consumerStrategy.canBeRemovedOnEmptySequence) return builder.irUnit()
        return producerStrategy.fuseConsumer(builder to parent, sequenceData, sequenceReplacement)
            ?: visitedExpression
    }
}

private fun deployTransformerStrategies(
    consumerStrategy: ConsumerStrategy,
    sequenceData: SequenceData,
    builderWithParent: IrBuilderWithParent,
): SequenceReplacement? {
    var sequenceReplacement = consumerStrategy.createSequenceReplacement() ?: return null
    for (transformer in sequenceData.transformers) {
        val transformerStrategy = TransformerStrategy.create(transformer, builderWithParent)
        sequenceReplacement =
            transformerStrategy.addTransformerToBodyBuilder(sequenceReplacement)
    }
    return sequenceReplacement
}
