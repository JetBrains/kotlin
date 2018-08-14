/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

class FinallyBlocksLowering(val context: CommonBackendContext, private val throwableType: IrType): FileLoweringPass, IrElementTransformerVoidWithContext() {

    private interface HighLevelJump {
        fun toIr(context: CommonBackendContext, startOffset: Int, endOffset: Int, value: IrExpression): IrExpression
    }

    private data class Return(val target: IrReturnTargetSymbol): HighLevelJump {
        override fun toIr(context: CommonBackendContext, startOffset: Int, endOffset: Int, value: IrExpression)
                =
            IrReturnImpl(startOffset, endOffset, context.irBuiltIns.nothingType, target, value)
    }

    private data class Break(val loop: IrLoop): HighLevelJump {
        override fun toIr(context: CommonBackendContext, startOffset: Int, endOffset: Int, value: IrExpression)
                = IrCompositeImpl(
            startOffset, endOffset, context.irBuiltIns.unitType, null,
            statements = listOf(
                value,
                IrBreakImpl(startOffset, endOffset, context.irBuiltIns.nothingType, loop)
            )
        )
    }

    private data class Continue(val loop: IrLoop): HighLevelJump {
        override fun toIr(context: CommonBackendContext, startOffset: Int, endOffset: Int, value: IrExpression)
                = IrCompositeImpl(
            startOffset, endOffset, context.irBuiltIns.unitType, null,
            statements = listOf(
                value,
                IrContinueImpl(startOffset, endOffset, context.irBuiltIns.nothingType, loop)
            )
        )
    }

    private abstract class Scope

    private class ReturnableScope(val descriptor: CallableDescriptor): Scope()

    private class LoopScope(val loop: IrLoop): Scope()

    private class TryScope(var expression: IrExpression,
                           val finallyExpression: IrExpression,
                           val irBuilder: IrBuilderWithScope
    ): Scope() {
        val jumps = mutableMapOf<HighLevelJump, IrReturnTargetSymbol>()
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

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        using(ReturnableScope(declaration.descriptor)) {
            return super.visitFunctionNew(declaration)
        }
    }

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
        val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
        return performHighLevelJump(
            targetScopePredicate = { it is LoopScope && it.loop == jump.loop },
            jump                 = Break(jump.loop),
            startOffset          = startOffset,
            endOffset            = endOffset,
            value                = irBuilder.irGetObject(context.ir.symbols.unit)
        ) ?: jump
    }

    override fun visitContinue(jump: IrContinue): IrExpression {
        val startOffset = jump.startOffset
        val endOffset = jump.endOffset
        val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
        return performHighLevelJump(
            targetScopePredicate = { it is LoopScope && it.loop == jump.loop },
            jump                 = Continue(jump.loop),
            startOffset          = startOffset,
            endOffset            = endOffset,
            value                = irBuilder.irGetObject(context.ir.symbols.unit)
        ) ?: jump
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        expression.transformChildrenVoid(this)

        return performHighLevelJump(
            targetScopePredicate = { it is ReturnableScope && it.descriptor == expression.returnTarget },
            jump                 = Return(expression.returnTargetSymbol),
            startOffset          = expression.startOffset,
            endOffset            = expression.endOffset,
            value                = expression.value
        ) ?: expression
    }

    private fun performHighLevelJump(targetScopePredicate: (Scope) -> Boolean,
                                     jump: HighLevelJump,
                                     startOffset: Int,
                                     endOffset: Int,
                                     value: IrExpression
    ): IrExpression? {
        val tryScopes = scopeStack.reversed()
                .takeWhile { !targetScopePredicate(it) }
                .filterIsInstance<TryScope>()
                .toList()
        if (tryScopes.isEmpty())
            return null
        return performHighLevelJump(tryScopes, 0, jump, startOffset, endOffset, value)
    }

    private val IrReturnTarget.returnType: IrType
        get() = when (this) {
            is IrConstructor -> context.irBuiltIns.unitType
            is IrFunction -> returnType
            is IrReturnableBlock -> type
            else -> error("Unknown ReturnTarget: $this")
        }

    private fun performHighLevelJump(tryScopes: List<TryScope>,
                                     index: Int,
                                     jump: HighLevelJump,
                                     startOffset: Int,
                                     endOffset: Int,
                                     value: IrExpression
    ): IrExpression {
        if (index == tryScopes.size)
            return jump.toIr(context, startOffset, endOffset, value)

        val currentTryScope = tryScopes[index]
        currentTryScope.jumps.getOrPut(jump) {
            val type = (jump as? Return)?.target?.owner?.returnType ?: value.type
            val symbol = getIrReturnableBlockSymbol(jump.toString(), type)
            with(currentTryScope) {
                irBuilder.run {
                    val inlinedFinally = irInlineFinally(symbol, type, expression, finallyExpression)
                    expression = performHighLevelJump(
                            tryScopes   = tryScopes,
                            index       = index + 1,
                            jump        = jump,
                            startOffset = startOffset,
                            endOffset   = endOffset,
                            value       = inlinedFinally)
                }
            }
            symbol
        }.let {
            return IrReturnImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                type = context.irBuiltIns.nothingType,
                returnTargetSymbol = it,
                value = value
            )
        }
    }

    override fun visitTry(aTry: IrTry): IrExpression {
        val finallyExpression = aTry.finallyExpression
                ?: return super.visitTry(aTry)

        val startOffset = aTry.startOffset
        val endOffset = aTry.endOffset
        val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
        val transformer = this
        irBuilder.run {
            val transformedTry = IrTryImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                type = context.irBuiltIns.unitType
            )
            val transformedFinallyExpression = finallyExpression.transform(transformer, null)
            val parameter = IrTemporaryVariableDescriptorImpl(
                containingDeclaration = currentScope!!.scope.scopeOwner,
                name = Name.identifier("t"),
                outType = throwableType.toKotlinType()
            )
            val catchParameter = IrVariableImpl(
                startOffset, endOffset, IrDeclarationOrigin.CATCH_PARAMETER, parameter,
                throwableType
            )

            val syntheticTry = IrTryImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                type = context.irBuiltIns.unitType,
                tryResult = transformedTry,
                catches = listOf(
                    irCatch(catchParameter).apply {
                        result = irComposite {
                            +finallyExpression.copy()
                            +irThrow(irGet(catchParameter))
                        }
                    }),
                finallyExpression = null
            )
            using(TryScope(syntheticTry, transformedFinallyExpression, this)) {
                val fallThroughType = aTry.type
                val fallThroughSymbol = getIrReturnableBlockSymbol("fallThrough", fallThroughType)
                val transformedResult = aTry.tryResult.transform(transformer, null)
                transformedTry.tryResult = irReturn(fallThroughSymbol, transformedResult)
                for (aCatch in aTry.catches) {
                    val transformedCatch = aCatch.transform(transformer, null)
                    transformedCatch.result = irReturn(fallThroughSymbol, transformedCatch.result)
                    transformedTry.catches.add(transformedCatch)
                }
                return irInlineFinally(fallThroughSymbol, fallThroughType, it.expression, it.finallyExpression)
            }
        }
    }

    private fun IrBuilderWithScope.irInlineFinally(symbol: IrReturnableBlockSymbol, type: IrType,
                                                                                    value: IrExpression,
                                                                                    finallyExpression: IrExpression
    ): IrExpression {
        val returnType = symbol.descriptor.returnType!!
        return when {
            returnType.isUnit() || returnType.isNothing() -> irBlock(value, null, type) {
                +irReturnableBlock(symbol, type) {
                    +value
                }
                +finallyExpression.copy()
            }
            else -> irComposite(value, null, type) {
                val tmp = irTemporary(irReturnableBlock(symbol, type) {
                    +irReturn(symbol, value)
                })
                +finallyExpression.copy()
                +irGet(tmp)
            }
        }
    }

    private fun getFakeFunctionDescriptor(name: String, returnType: KotlinType) =
            SimpleFunctionDescriptorImpl.create(
                currentScope!!.scope.scopeOwner,
                Annotations.EMPTY,
                name.synthesizedName,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                SourceElement.NO_SOURCE
            ).apply {
                initialize(null, null, emptyList(), emptyList(), returnType,
                           Modality.ABSTRACT,
                           Visibilities.PRIVATE
                )
            }

    private fun getIrReturnableBlockSymbol(name: String, returnType: IrType): IrReturnableBlockSymbol =
        IrReturnableBlockSymbolImpl(getFakeFunctionDescriptor(name, returnType.toKotlinType()))

    private inline fun <reified T : IrElement> T.copy() = this.deepCopyWithVariables()

    fun IrBuilderWithScope.irReturn(target: IrReturnTargetSymbol, value: IrExpression) =
        IrReturnImpl(startOffset, endOffset, context.irBuiltIns.nothingType, target, value)

    inline fun IrBuilderWithScope.irReturnableBlock(symbol: IrReturnableBlockSymbol, type: IrType, body: IrBlockBuilder.() -> Unit) =
        IrReturnableBlockImpl(
            startOffset, endOffset, type, symbol, null,
            IrBlockBuilder(context, scope, startOffset, endOffset, null, type, true)
                .block(body).statements
        )
}