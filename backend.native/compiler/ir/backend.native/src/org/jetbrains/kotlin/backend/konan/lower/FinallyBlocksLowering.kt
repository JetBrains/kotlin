package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DeepCopyIrTreeWithDeclarations
import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.IrReturnableBlockImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

internal class FinallyBlocksLowering(val context: Context): FunctionLoweringPass {

    private interface HighLevelJump {
        fun toIr(context: Context, startOffset: Int, endOffset: Int, value: IrExpression): IrExpression
    }

    private data class Return(val functionDescriptor: FunctionDescriptor): HighLevelJump {
        override fun toIr(context: Context, startOffset: Int, endOffset: Int, value: IrExpression)
                = IrReturnImpl(startOffset, endOffset, functionDescriptor, value)
    }

    private data class Break(val loop: IrLoop): HighLevelJump {
        override fun toIr(context: Context, startOffset: Int, endOffset: Int, value: IrExpression)
                = IrBlockImpl(startOffset, endOffset, context.builtIns.nothingType, null,
                statements = listOf(
                        value,
                        IrBreakImpl(startOffset, endOffset, context.builtIns.nothingType, loop)
                ))
    }

    private data class Continue(val loop: IrLoop): HighLevelJump {
        override fun toIr(context: Context, startOffset: Int, endOffset: Int, value: IrExpression)
                = IrBlockImpl(startOffset, endOffset, context.builtIns.nothingType, null,
                statements = listOf(
                        value,
                        IrContinueImpl(startOffset, endOffset, context.builtIns.nothingType, loop)
                ))
    }

    private abstract class Scope

    private class ReturnableScope(val descriptor: CallableDescriptor): Scope()

    private class LoopScope(val loop: IrLoop): Scope()

    private class TryScope(var expression: IrExpression,
                           val finallyExpression: IrExpression,
                           val irBuilder: IrBuilderWithScope): Scope() {
        val jumps = mutableMapOf<HighLevelJump, FunctionDescriptor>()
    }

    private val scopeStack = mutableListOf<Scope>()

    private inline fun <S: Scope, R> using(scope: S, block: (S) -> R): R {
        scopeStack.push(scope)
        try {
            return block(scope)
        } finally {
            scopeStack.pop()
        }
    }

    override fun lower(irFunction: IrFunction) {
        val functionDescriptor = irFunction.descriptor
        using(ReturnableScope(functionDescriptor)) {
            irFunction.transformChildrenVoid(object : IrElementTransformerVoid() {

                override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
                    if (expression !is IrReturnableBlockImpl)
                        return super.visitContainerExpression(expression)

                    using(ReturnableScope(expression.descriptor)) {
                        return super.visitContainerExpression(expression)
                    }
                }

                override fun visitLoop(loop: IrLoop): IrExpression {
                    using(LoopScope(loop)) {
                        return super.visitLoop(loop)
                    }
                }

                override fun visitBreak(jump: IrBreak): IrExpression {
                    val startOffset = jump.startOffset
                    val endOffset = jump.endOffset
                    val irBuilder = context.createIrBuilder(functionDescriptor, startOffset, endOffset)
                    return performHighLevelJump(
                            targetScopePredicate = { it is LoopScope && it.loop == jump.loop },
                            jump                 = Break(jump.loop),
                            startOffset          = startOffset,
                            endOffset            = endOffset,
                            value                = irBuilder.irUnit()
                    ) ?: jump
                }

                override fun visitContinue(jump: IrContinue): IrExpression {
                    val startOffset = jump.startOffset
                    val endOffset = jump.endOffset
                    val irBuilder = context.createIrBuilder(functionDescriptor, startOffset, endOffset)
                    return performHighLevelJump(
                            targetScopePredicate = { it is LoopScope && it.loop == jump.loop },
                            jump                 = Continue(jump.loop),
                            startOffset          = startOffset,
                            endOffset            = endOffset,
                            value                = irBuilder.irUnit()
                    ) ?: jump
                }

                override fun visitReturn(expression: IrReturn): IrExpression {
                    expression.transformChildrenVoid(this)

                    return performHighLevelJump(
                            targetScopePredicate = { it is ReturnableScope && it.descriptor == expression.returnTarget },
                            jump                 = Return(expression.returnTarget),
                            startOffset          = expression.startOffset,
                            endOffset            = expression.endOffset,
                            value                = expression.value
                    ) ?: expression
                }

                private fun performHighLevelJump(targetScopePredicate: (Scope) -> Boolean,
                                                 jump: HighLevelJump,
                                                 startOffset: Int,
                                                 endOffset: Int,
                                                 value: IrExpression): IrExpression? {
                    val tryScopes = scopeStack.reversed()
                            .takeWhile { !targetScopePredicate(it) }
                            .filterIsInstance<TryScope>()
                            .toList()
                    if (tryScopes.isEmpty())
                        return null
                    return performHighLevelJump(tryScopes, 0, jump, startOffset, endOffset, value)
                }

                private fun performHighLevelJump(tryScopes: List<TryScope>,
                                                 index: Int,
                                                 jump: HighLevelJump,
                                                 startOffset: Int,
                                                 endOffset: Int,
                                                 value: IrExpression): IrExpression {
                    if (index == tryScopes.size)
                        return jump.toIr(context, startOffset, endOffset, value)

                    val currentTryScope = tryScopes[index]
                    currentTryScope.jumps.getOrPut(jump) {
                        val descriptor = getFakeFunctionDescriptor(jump.toString(), value.type)
                        with(currentTryScope) {
                            irBuilder.run {
                                val inlinedFinally = irInlineFinally(descriptor, expression, finallyExpression)
                                expression = performHighLevelJump(
                                        tryScopes   = tryScopes,
                                        index       = index + 1,
                                        jump        = jump,
                                        startOffset = startOffset,
                                        endOffset   = endOffset,
                                        value       = inlinedFinally)
                            }
                        }
                        descriptor
                    }.let {
                        return IrReturnImpl(
                                startOffset  = startOffset,
                                endOffset    = endOffset,
                                returnTargetDescriptor = it,
                                value        = value)
                    }
                }

                override fun visitTry(aTry: IrTry): IrExpression {
                    val finallyExpression = aTry.finallyExpression
                    if (finallyExpression == null)
                        return super.visitTry(aTry)

                    val startOffset = aTry.startOffset
                    val endOffset = aTry.endOffset
                    val irBuilder = context.createIrBuilder(functionDescriptor, startOffset, endOffset)
                    val transformer = this
                    irBuilder.run {
                        val transformedTry = IrTryImpl(
                                startOffset = startOffset,
                                endOffset   = endOffset,
                                type        = context.builtIns.nothingType
                        )
                        val transformedFinallyExpression = finallyExpression.transform(transformer, null)
                        val parameter = IrTemporaryVariableDescriptorImpl(
                                containingDeclaration = irFunction.descriptor,
                                name                  = Name.identifier("t"),
                                outType               = context.builtIns.throwable.defaultType
                        )
                        val catchParameter = IrVariableImpl(
                                startOffset, endOffset, IrDeclarationOrigin.CATCH_PARAMETER, parameter)

                        val syntheticTry = IrTryImpl(
                                startOffset       = startOffset,
                                endOffset         = endOffset,
                                type              = context.builtIns.nothingType,
                                tryResult         = transformedTry,
                                catches           = listOf(
                                        irCatch(catchParameter).apply {
                                                result = irBlock {
                                                    +finallyExpression.copy()
                                                    +irThrow(irGet(catchParameter.symbol))
                                                }
                                        }),
                                finallyExpression = null
                        )
                        using(TryScope(syntheticTry, transformedFinallyExpression, this)) {
                            val fallThroughDescriptor = getFakeFunctionDescriptor("fallThrough", aTry.type)
                            val transformedResult = aTry.tryResult.transform(transformer, null)
                            transformedTry.tryResult = irReturn(fallThroughDescriptor, transformedResult)
                            for (aCatch in aTry.catches) {
                                val transformedCatch = aCatch.transform(transformer, null)
                                transformedCatch.result = irReturn(fallThroughDescriptor, transformedCatch.result)
                                transformedTry.catches.add(transformedCatch)
                            }
                            return irInlineFinally(fallThroughDescriptor, it.expression, it.finallyExpression)
                        }
                    }
                }

                private fun IrBuilderWithScope.irInlineFinally(descriptor: FunctionDescriptor,
                                                               value: IrExpression,
                                                               finallyExpression: IrExpression): IrExpression {
                    val returnType = descriptor.returnType!!
                    return when {
                        returnType.isUnit() || returnType.isNothing() -> irBlock(value, null, returnType) {
                            +irReturnableBlock(descriptor) {
                                +value
                            }
                            +finallyExpression.copy()
                        }
                        else -> irBlock(value, null, returnType) {
                            val tmp = irTemporary(irReturnableBlock(descriptor) {
                                +irReturn(descriptor, value)
                            })
                            +finallyExpression.copy()
                            +irGet(tmp.symbol)
                        }
                    }
                }

                private fun getFakeFunctionDescriptor(name: String, returnType: KotlinType): FunctionDescriptor {
                    return SimpleFunctionDescriptorImpl.create(
                            functionDescriptor,
                            Annotations.EMPTY,
                            name.synthesizedName,
                            CallableMemberDescriptor.Kind.SYNTHESIZED,
                            SourceElement.NO_SOURCE).apply {
                        initialize(null, null, emptyList(), emptyList(), returnType, Modality.ABSTRACT, Visibilities.PRIVATE)
                    }
                }

                @Suppress("UNCHECKED_CAST")
                private fun <T: IrElement> T.copy() = this.transform(DeepCopyIrTreeWithDeclarations(), data = null) as T
            })
        }
    }

    private object DECLARATION_ORIGIN_FINALLY_BLOCK :
            IrDeclarationOriginImpl("FINALLY_BLOCK")

    private fun IrBuilderWithScope.irVar(descriptor: VariableDescriptor, initializer: IrExpression?) =
            IrVariableImpl(startOffset, endOffset, DECLARATION_ORIGIN_FINALLY_BLOCK, descriptor, initializer)

    fun IrBuilderWithScope.irReturn(target: FunctionDescriptor, value: IrExpression) =
            IrReturnImpl(startOffset, endOffset, target, value)

    inline fun IrBuilderWithScope.irReturnableBlock(descriptor: FunctionDescriptor, body: IrBlockBuilder.() -> Unit) =
            IrReturnableBlockImpl(startOffset, endOffset, descriptor.returnType!!, descriptor, null,
                    IrBlockBuilder(context, scope, startOffset, endOffset, null, descriptor.returnType!!)
                            .block(body).statements)
}