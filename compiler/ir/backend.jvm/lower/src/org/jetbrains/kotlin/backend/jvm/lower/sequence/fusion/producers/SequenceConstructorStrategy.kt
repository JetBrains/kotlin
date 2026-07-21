/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers

import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.ConsumerBodyBuilder
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceDataGatherer
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceSource
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.gatherVarargArgument
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.getBaseTypeFromSequenceScopeFunction
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.getGenericTypeFromExpression
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.isElementSequence
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.sequenceDataOfExpression
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irReturnUnit
import org.jetbrains.kotlin.ir.builders.irReturnableBlock
import org.jetbrains.kotlin.ir.builders.irUnit
import org.jetbrains.kotlin.ir.builders.irWhile
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name

private class YieldReplacer(
    val builderWithParent: IrBuilderWithParent,
    val consumeFunction: IrFunction,
    val returnableBlock: IrReturnableBlock,
    val oldLambdaSymbol: IrSimpleFunctionSymbol,
    val context: JvmBackendContext,
) : IrElementTransformerVoid() {
    var shouldTerminate = false
    override fun visitCall(expression: IrCall): IrExpression {
        if (shouldTerminate) return super.visitCall(expression)
        val functionName = expression.symbol.owner.name.asString()
        return when (functionName) {
            "yield" -> {
                val yieldArgument = expression.arguments.getOrNull(1)
                if (yieldArgument == null) {
                    shouldTerminate = true
                    return super.visitCall(expression)
                }
                yieldReplacement(builderWithParent, yieldArgument, consumeFunction)
            }
            "yieldAll" -> {
                val yieldAllArgument = expression.arguments.getOrNull(1)
                if (yieldAllArgument == null) {
                    shouldTerminate = true
                    return super.visitCall(expression)
                }

                val yieldReplacement = when {
                    yieldAllArgument.type.isSubtypeOfClass(context.irBuiltIns.iterableClass) -> yieldAllIterableReplacement(
                        builderWithParent,
                        yieldAllArgument,
                        consumeFunction
                    )
                    yieldAllArgument.type.isSubtypeOfClass(context.irBuiltIns.iteratorClass) -> yieldAllIteratorReplacement(
                        builderWithParent,
                        yieldAllArgument,
                        consumeFunction
                    )
                    isElementSequence(context, yieldAllArgument) -> yieldAllSequenceReplacement(
                        builderWithParent,
                        yieldAllArgument,
                        consumeFunction
                    )
                    else -> {
                        shouldTerminate = true
                        return super.visitCall(expression)
                    }
                }
                if (yieldReplacement != null) yieldReplacement else {
                    shouldTerminate = true
                    super.visitCall(expression)
                }
            }
            else -> {
                if (expression.symbol.owner.isSuspend) shouldTerminate = true
                super.visitCall(expression)
            }
        }
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        if (expression.returnTargetSymbol == oldLambdaSymbol) {
            expression.returnTargetSymbol = returnableBlock.symbol
        }
        return expression
    }

    private fun yieldReplacement(
        builderWithParent: IrBuilderWithParent,
        argument: IrExpression,
        consumeFunction: IrFunction,
    ): IrExpression {
        return with(builderWithParent.first) {
            val consumeCall = irCall(consumeFunction.symbol, type = context.irBuiltIns.booleanType).apply {
                arguments[0] = argument.deepCopyWithSymbols(builderWithParent.second)
            }
            val notConsumed = irNot(consumeCall)
            val returnStatement = irReturnUnit().apply {
                returnTargetSymbol = returnableBlock.symbol
            }
            irIfThen(context.irBuiltIns.unitType, notConsumed, returnStatement)
        }
    }

    private fun yieldAllSequenceReplacement(
        builderWithParent: IrBuilderWithParent,
        sequenceProducer: IrExpression,
        consumeFunction: IrFunction,
    ): IrExpression? {
        // argument is either Iterable, Iterator, or Sequence
        return with(builderWithParent.first) {
            // lowered argument.forEach { consume(it) }
            // it is enough to provide a consumer body builder, there doesn't need to be a for each constructed
            val consumerBodyBuilder: ConsumerBodyBuilder = createForEachConsumer(this, this@YieldReplacer.context, consumeFunction)
            val initialDeclarations = emptyList<IrVariable>()
            val finalExpression = irUnit()

            val gatherer = SequenceDataGatherer(this@YieldReplacer.context)
            sequenceProducer.accept(gatherer, null)
            val sequenceData =
                sequenceProducer.sequenceDataOfExpression ?: return null
            val producerStrategy = sequenceData.sequenceSource.createProducerStrategy(this, this@YieldReplacer.context)
            val sequenceReplacement = SequenceReplacement(initialDeclarations, consumerBodyBuilder, finalExpression)
            producerStrategy.fuseConsumer(builderWithParent, sequenceData, sequenceReplacement)
                ?: return null
        }
    }

    private fun yieldAllIteratorReplacement(
        builderWithParent: IrBuilderWithParent,
        iterator: IrExpression,
        consumeFunction: IrFunction,
    ): IrExpression? {
        // yieldAll(iterator)
        // should be changed into
        // while (iterator.hasNext()) {
        //     val shouldContinue = consume(iterator.next())
        //     if (!shouldContinue) return Unit
        // }
        val builder = builderWithParent.first
        val parent = builderWithParent.second
        return buildLoopFromIterator(builder, iterator, consumeFunction, parent)
    }

    private fun yieldAllIterableReplacement(
        builderWithParent: IrBuilderWithParent,
        iterable: IrExpression,
        consumeFunction: IrFunction,
    ): IrExpression? {
        val builder = builderWithParent.first
        val parent = builderWithParent.second
        // listOf, mutableListOf, setOf, mutableSetOf, arrayListOf,
        if (iterable is IrCall) {
            when (iterable.symbol.owner.name.asString()) {
                "listOf", "mutableListOf", "setOf", "mutableSetOf", "arrayListOf" -> {
                    val initialDeclarations = emptyList<IrVariable>()
                    val finalExpression = builder.irUnit()
                    val consumerBodyBuilder: ConsumerBodyBuilder = createForEachConsumer(builder, context, consumeFunction)
                    val argument = iterable.arguments.singleOrNull()
                    val elements: List<IrExpression> = if (argument != null) gatherVarargArgument(argument)
                        ?: return null else if (iterable.arguments.isEmpty()) emptyList() else return null
                    val baseType = getGenericTypeFromExpression(iterable) ?: return null
                    val source = SequenceSource.SequenceOf(elements, baseType)
                    val producerStrategy = SequenceOfStrategy(source)
                    val sequenceReplacement = SequenceReplacement(initialDeclarations, consumerBodyBuilder, finalExpression)
                    val sequenceData = SequenceData(source, emptyList())
                    return producerStrategy.fuseConsumer(builderWithParent, sequenceData, sequenceReplacement)
                }
            }
        }
        val iteratorCall = builder.buildCallWithReceiver(iterable, iterable.type, "iterator", parent) ?: return null
        return buildLoopFromIterator(builder, iteratorCall, consumeFunction, parent)
    }

    private fun buildLoopFromIterator(
        builder: IrBuilderWithScope,
        iterator: IrExpression,
        consumeFunction: IrFunction,
        parent: IrDeclarationParent
    ): IrExpression? {
        val iteratorDeclaration = builder.scope.createTemporaryVariable(iterator, isMutable = false, nameHint = "yieldAllIterator")
        val loopCondition =
            builder.buildCallWithReceiver(builder.irGet(iteratorDeclaration), iterator.type, "hasNext", parent) ?: return null
        val nextCall = builder.buildCallWithReceiver(builder.irGet(iteratorDeclaration), iterator.type, "next", parent) ?: return null
        val consumeCall = builder.irCall(consumeFunction.symbol, type = context.irBuiltIns.booleanType).apply {
            arguments[0] = nextCall
        }
        return builder.irReturnableBlock(builder.context.irBuiltIns.unitType) {
            +iteratorDeclaration
            +irWhile().apply {
                condition = loopCondition
                body = irBlock {
                    +irIfThen(
                        context.irBuiltIns.unitType,
                        irNot(consumeCall),
                        irReturnUnit().apply { this.returnTargetSymbol = returnableBlockSymbol })
                }
            }
        }
    }
}

internal class SequenceConstructorStrategy(
    val sequenceScope: IrRichFunctionReference,
    val context: JvmBackendContext,
) : ProducerStrategy() {

    override fun fuseConsumer(
        builderWithParent: IrBuilderWithParent,
        sequenceData: SequenceData,
        sequenceReplacement: SequenceReplacement,
    ): IrContainerExpression? {
        val builder = builderWithParent.first
        val parent = builderWithParent.second
        if (containsOtherSuspendCalls(sequenceScope.invokeFunction.body!!)) return null

        val localConsumerFunction = buildLocalConsumerFunction(builder, parent, sequenceReplacement.mainBodyBuilder)

        val baseType = getBaseTypeFromSequenceScopeFunction(sequenceScope) ?: return null

        val innerBlock = builder.irReturnableBlock(context.irBuiltIns.unitType) {
            +sequenceScope.invokeFunction.body!!.deepCopyWithSymbols(parent).statements
        }
        innerBlock.statements.add(builder.irReturnUnit().apply { this.returnTargetSymbol = innerBlock.symbol })
        val outerBlock = builder.irBlock(resultType = baseType) {
            +sequenceReplacement.initialDeclarations
            +localConsumerFunction
            +innerBlock
            +sequenceReplacement.finalExpression
        }
        val oldLambdaSymbol = sequenceScope.invokeFunction.symbol
        val yieldReplacer = YieldReplacer(
            builderWithParent,
            localConsumerFunction,
            innerBlock,
            oldLambdaSymbol,
            context,
        )

        outerBlock.transformChildren(yieldReplacer, null)
        if (yieldReplacer.shouldTerminate) return null
        return outerBlock
    }

    private fun buildLocalConsumerFunction(
        builder: IrBuilderWithScope,
        parent: IrDeclarationParent,
        consumerBodyBuilder: ConsumerBodyBuilder,
    ): IrFunction {
        val consumerTarget = context.irFactory.buildFun {
            name = Name.identifier("sequenceYieldConsumer")
            returnType = context.irBuiltIns.booleanType
            visibility = DescriptorVisibilities.LOCAL
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        }.apply {
            this.parent = parent
        }

        val baseType = getBaseTypeFromSequenceScopeFunction(sequenceScope) ?: return consumerTarget
        val elementParam = consumerTarget.addValueParameter("element", baseType)

        consumerTarget.body = builder.irBlockBody {
            +irReturn(
                consumerBodyBuilder(elementParam)
            ).apply {
                this.returnTargetSymbol = consumerTarget.symbol
            }
        }

        return consumerTarget
    }

    fun containsOtherSuspendCalls(sequenceBlock: IrBody): Boolean {
        var result = false

        sequenceBlock.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                if (expression.symbol.owner.isSuspend) {
                    val functionName = expression.symbol.owner.name.asString()
                    if (functionName != "yield" && functionName != "yieldAll") {
                        result = true
                    }
                }
                super.visitCall(expression)
            }
        })
        return result
    }
}

private fun createForEachConsumer(
    builder: IrBuilderWithScope,
    context: JvmBackendContext,
    consumeFunction: IrFunction
): ConsumerBodyBuilder = { sequenceElement ->
    builder.irReturnableBlock(context.irBuiltIns.booleanType) {
        val consumeCall = irCall(consumeFunction.symbol, type = context.irBuiltIns.booleanType).apply {
            arguments[0] = irGet(sequenceElement)
        }
        +irReturn(consumeCall).apply { returnTargetSymbol = returnableBlockSymbol }
    }
}
