/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.setDeclarationsParent
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal class FinallyBlocksLowering(val context: Context): FileLoweringPass, IrElementTransformerVoidWithContext() {
    private val symbols = context.ir.symbols

    private interface HighLevelJump {
        fun toIr(context: Context, startOffset: Int, endOffset: Int, value: IrExpression): IrExpression
    }

    private data class Return(val target: IrReturnTargetSymbol): HighLevelJump {
        override fun toIr(context: Context, startOffset: Int, endOffset: Int, value: IrExpression)
                = IrReturnImpl(startOffset, endOffset, context.irBuiltIns.nothingType, target, value)
    }

    private data class Break(val loop: IrLoop): HighLevelJump {
        override fun toIr(context: Context, startOffset: Int, endOffset: Int, value: IrExpression)
                = IrBlockImpl(startOffset, endOffset, context.irBuiltIns.nothingType, null,
                statements = listOf(
                        value,
                        IrBreakImpl(startOffset, endOffset, context.irBuiltIns.nothingType, loop)
                ))
    }

    private data class Continue(val loop: IrLoop): HighLevelJump {
        override fun toIr(context: Context, startOffset: Int, endOffset: Int, value: IrExpression)
                = IrBlockImpl(startOffset, endOffset, context.irBuiltIns.nothingType, null,
                statements = listOf(
                        value,
                        IrContinueImpl(startOffset, endOffset, context.irBuiltIns.nothingType, loop)
                ))
    }

    private abstract class Scope

    private class ReturnableScope(val symbol: IrReturnTargetSymbol): Scope()

    private class LoopScope(val loop: IrLoop): Scope()

    private class TryScope(var expression: IrExpression,
                           val finallyExpression: IrExpression,
                           val irBuilder: IrBuilderWithScope): Scope() {
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
        using(ReturnableScope(declaration.symbol)) {
            return super.visitFunctionNew(declaration)
        }
    }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        if (expression !is IrReturnableBlock)
            return super.visitContainerExpression(expression)

        using(ReturnableScope(expression.symbol)) {
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
                targetScopePredicate = { it is ReturnableScope && it.symbol == expression.returnTargetSymbol },
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
                                     value: IrExpression): IrExpression? {
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

    private fun createSyntheticFunctionDescriptor(name: String): SimpleFunctionDescriptor {
        val descriptor = WrappedSimpleFunctionDescriptor()
        descriptor.bind(IrFunctionImpl(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                IrDeclarationOrigin.DEFINED,
                IrSimpleFunctionSymbolImpl(descriptor),
                Name.identifier(name),
                Visibilities.PUBLIC,
                Modality.FINAL,
                context.irBuiltIns.unitType,
                false,
                false,
                false,
                false,
                false,
                false,
                false)
        )
        return descriptor
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
            val type = (jump as? Return)?.target?.owner?.returnType ?: value.type
            val symbol = IrReturnableBlockSymbolImpl(createSyntheticFunctionDescriptor("\$Finally$index"))
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
                    startOffset        = startOffset,
                    endOffset          = endOffset,
                    type               = context.irBuiltIns.nothingType,
                    returnTargetSymbol = it,
                    value              = value)
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
                    endOffset   = endOffset,
                    type        = context.irBuiltIns.nothingType
            )
            val transformedFinallyExpression = finallyExpression.transform(transformer, null)
            val catchParameter = WrappedVariableDescriptor().let {
                IrVariableImpl(
                        startOffset, endOffset,
                        IrDeclarationOrigin.CATCH_PARAMETER,
                        IrVariableSymbolImpl(it),
                        Name.identifier("t"),
                        symbols.throwable.owner.defaultType,
                        isVar = false,
                        isConst = false,
                        isLateinit = false
                ).apply {
                    it.bind(this)
                    parent = this@run.parent
                }
            }

            val syntheticTry = IrTryImpl(
                    startOffset       = startOffset,
                    endOffset         = endOffset,
                    type              = context.irBuiltIns.nothingType,
                    tryResult         = transformedTry,
                    catches           = listOf(
                            irCatch(catchParameter, irBlock {
                                +copy(finallyExpression)
                                +irThrow(irGet(catchParameter))
                            })
                    ),
                    finallyExpression = null
            )
            using(TryScope(syntheticTry, transformedFinallyExpression, this)) {
                val fallThroughType = aTry.type
                val fallThroughSymbol = IrReturnableBlockSymbolImpl(createSyntheticFunctionDescriptor("\$Fallthrough"))
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
                                                   finallyExpression: IrExpression): IrExpression {
        val returnTypeClassifier = (type as? IrSimpleType)?.classifier
        return when (returnTypeClassifier) {
            symbols.unit, symbols.nothing -> irBlock(value, null, type) {
                +irReturnableBlock(finallyExpression.startOffset, finallyExpression.endOffset, symbol, type) {
                    +value
                }
                +copy(finallyExpression)
            }
            else -> irBlock(value, null, type) {
                val tmp = irTemporary(irReturnableBlock(finallyExpression.startOffset, finallyExpression.endOffset, symbol, type) {
                    +irReturn(symbol, value)
                })
                +copy(finallyExpression)
                +irGet(tmp)
            }
        }
    }

    private inline fun <reified T : IrElement> IrBuilderWithScope.copy(element: T) =
            element.deepCopyWithVariables().setDeclarationsParent(parent)

    fun IrBuilderWithScope.irReturn(target: IrReturnTargetSymbol, value: IrExpression) =
            IrReturnImpl(startOffset, endOffset, context.irBuiltIns.nothingType, target, value)

    private inline fun IrBuilderWithScope.irReturnableBlock(startOffset: Int, endOffset: Int, symbol: IrReturnableBlockSymbol,
                                                            type: IrType, body: IrBlockBuilder.() -> Unit) =
            IrReturnableBlockImpl(startOffset, endOffset, type, symbol, null,
                    IrBlockBuilder(context, scope, startOffset, endOffset, null, type)
                            .block(body).statements)
}
