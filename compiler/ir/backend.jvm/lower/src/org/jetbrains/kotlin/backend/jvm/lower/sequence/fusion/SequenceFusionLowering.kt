/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion

import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * transformation:
 * ```
 * fun myFun(seq: Sequence<Int>) {
 *     val seq2 = seq.map { it * 2 }.map { it + 1 }
 *     for (x in seq2) println(x)
 * }
 * ```
 * becomes
 * ```
 * fun myFun(seq: Sequence<Int>) {
 *     val seq2 = seq.map { it * 2 }.map { it + 1 }
 *     val iterator = seq.iterator()
 *     while (iterator.hasNext()) println({ y -> { x -> x * 2 }(y) + 1 }(iterator.next()))
 * }
 * ```
 *
 * ```
 * val seq = sequenceOf(1, 2, 3).map { it * 2 }.map { it + 1 }
 * for (x in seq) println(x)
 * ```
 * becomes (see `SequenceOfStrategy`)
 * ```
 * val seq = sequenceOf(1, 2, 3).map { it * 2 }.map { it + 1 }
 * val e0 = 1; val e1 = 2; val e2 = 3
 * var n = 0
 * while (n < 3) {
 *     val x = when (n) { 0 -> e0; 1 -> e1; 2 -> e2; else -> throw NoWhenBranchMatchedException() }
 *     println({ y -> { x -> x * 2 }(y) + 1 }(x))
 *     n++
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

internal fun IrBuilderWithScope.increment(incrementVariable: IrVariable, context: JvmBackendContext): IrExpression {
    val exception = irCallConstructor(context.symbols.arithmeticExceptionCtorString, emptyList()).apply {
        arguments[0] = irString("Index overflow has happened.")
    }

    val throwExpression = irThrow(exception)

    val intLessThanFunction = context.irBuiltIns.lessFunByOperandType[context.irBuiltIns.intClass] ?: error("No less function for int")
    val isNegative = irCall(intLessThanFunction, context.irBuiltIns.booleanType).apply {
        arguments[0] = irGet(incrementVariable)
        arguments[1] = irInt(0)
    }
    val checkOverflow = irIfThen(
        isNegative,
        throwExpression
    )
    return irBlock {
        +irSet(incrementVariable, irCall(context.irBuiltIns.intPlusSymbol).apply {
            dispatchReceiver = irGet(incrementVariable)
            arguments[1] = irInt(1)
        })
        +checkOverflow
    }
}

internal fun IrBuilderWithScope.irReturnTo(target: IrReturnTargetSymbol, value: IrExpression): IrExpression =
    IrReturnImpl(startOffset, endOffset, context.irBuiltIns.nothingType, target, value)

internal fun isSequenceType(context: JvmBackendContext, element: IrElement): Boolean {
    val sequenceSymbol = context.symbols.sequence ?: return false
    val type = when (element) {
        is IrExpression -> element.type
        is IrVariable -> element.type
        else -> return false
    }
    return type.isSubtypeOfClass(sequenceSymbol)
}

// The `next()` call can be wrapped in an IMPLICIT_NOTNULL type-cast when the element type has enhanced nullability from Java
private fun IrExpression.findForLoopNextCall(): IrCall? = when (this) {
    is IrCall -> takeIf { origin == IrStatementOrigin.FOR_LOOP_NEXT }
    is IrTypeOperatorCall -> argument.findForLoopNextCall()
    else -> null
}

internal fun IrExpression.replaceForLoopNextCall(replacement: IrExpression): IrExpression = when (this) {
    is IrCall -> replacement
    is IrTypeOperatorCall -> apply { argument = argument.replaceForLoopNextCall(replacement) }
    else -> error("Unexpected for loop variable initializer: ${dump()}")
}

private fun lookupForLoopVariable(loopBody: IrBlock): IrVariable? = loopBody.statements.filterIsInstance<IrVariable>()
    .singleOrNull { it.initializer?.findForLoopNextCall() != null }

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
    val loopVariable = lookupForLoopVariable(body) ?: return null
    body.statements.remove(loopVariable)
    return LoopData(loop, loopVariable, body)
}

private class SequenceFusionTransformer(val context: JvmBackendContext) : IrElementTransformerVoidWithContext() {
    override fun visitBlock(expression: IrBlock): IrExpression {
        val visitedExpression = super.visitBlock(expression)
        if (visitedExpression !is IrBlock) return visitedExpression
        if (visitedExpression.origin != IrStatementOrigin.FOR_LOOP) return visitedExpression

        val parent = currentDeclarationParent ?: return visitedExpression
        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        val receiver =
            ((expression.statements.getOrNull(0) as? IrVariable)?.initializer as? IrCall)?.arguments?.getOrNull(0)
                ?: return visitedExpression
        val gatherer = SequenceDataGatherer(context)
        receiver.accept(gatherer, null)
        val sequenceData = receiver.sequenceDataOfExpression ?: return visitedExpression
        val data = ConsumerData(context, builder, parent, sequenceData)
        val consumerStrategy = createConsumerStrategy(visitedExpression, data) ?: return visitedExpression
        return fuse(consumerStrategy, sequenceData, builder to parent) ?: visitedExpression
    }

    private fun fuse(
        consumerStrategy: ConsumerStrategy,
        sequenceData: SequenceData,
        builderWithParent: IrBuilderWithParent,
    ): IrExpression? {
        // null if the consumer cannot be fused with its arguments
        val sequenceReplacement = consumerStrategy.createSequenceReplacement() ?: return null
        val producerStrategy = sequenceData.sequenceSource.createProducerStrategy(builderWithParent.first, context)
        return producerStrategy.fuseConsumer(builderWithParent, sequenceData, sequenceReplacement)
    }
}
