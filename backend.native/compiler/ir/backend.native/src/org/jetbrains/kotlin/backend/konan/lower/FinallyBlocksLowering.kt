/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

internal class FinallyBlocksLowering(val context: Context): FileLoweringPass, IrElementTransformerVoidWithContext() {

    private interface HighLevelJump {
        fun toIr(context: Context, startOffset: Int, endOffset: Int, value: IrExpression): IrExpression
    }

    private data class Return(val target: IrReturnTargetSymbol): HighLevelJump {
        override fun toIr(context: Context, startOffset: Int, endOffset: Int, value: IrExpression)
                = IrReturnImpl(startOffset, endOffset, context.builtIns.nothingType, target, value)
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
            val symbol = getIrReturnableBlockSymbol(jump.toString(), value.type)
            with(currentTryScope) {
                irBuilder.run {
                    val inlinedFinally = irInlineFinally(symbol, expression, finallyExpression)
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
                    type               = context.builtIns.nothingType,
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
                    type        = context.builtIns.nothingType
            )
            val transformedFinallyExpression = finallyExpression.transform(transformer, null)
            val parameter = IrTemporaryVariableDescriptorImpl(
                    containingDeclaration = currentScope!!.scope.scopeOwner,
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
                val fallThroughSymbol = getIrReturnableBlockSymbol("fallThrough", aTry.type)
                val transformedResult = aTry.tryResult.transform(transformer, null)
                transformedTry.tryResult = irReturn(fallThroughSymbol, transformedResult)
                for (aCatch in aTry.catches) {
                    val transformedCatch = aCatch.transform(transformer, null)
                    transformedCatch.result = irReturn(fallThroughSymbol, transformedCatch.result)
                    transformedTry.catches.add(transformedCatch)
                }
                return irInlineFinally(fallThroughSymbol, it.expression, it.finallyExpression)
            }
        }
    }

    private fun IrBuilderWithScope.irInlineFinally(symbol: IrReturnableBlockSymbol,
                                                   value: IrExpression,
                                                   finallyExpression: IrExpression): IrExpression {
        val returnType = symbol.descriptor.returnType!!
        return when {
            returnType.isUnit() || returnType.isNothing() -> irBlock(value, null, returnType) {
                +irReturnableBlock(symbol) {
                    +value
                }
                +finallyExpression.copy()
            }
            else -> irBlock(value, null, returnType) {
                val tmp = irTemporary(irReturnableBlock(symbol) {
                    +irReturn(symbol, value)
                })
                +finallyExpression.copy()
                +irGet(tmp.symbol)
            }
        }
    }

    private fun getFakeFunctionDescriptor(name: String, returnType: KotlinType) =
            SimpleFunctionDescriptorImpl.create(currentScope!!.scope.scopeOwner, Annotations.EMPTY, name.synthesizedName,
                    CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE).apply {
                initialize(null, null, emptyList(), emptyList(), returnType, Modality.ABSTRACT, Visibilities.PRIVATE)
            }

    private fun getIrReturnableBlockSymbol(name: String, returnType: KotlinType): IrReturnableBlockSymbol =
            IrReturnableBlockSymbolImpl(getFakeFunctionDescriptor(name, returnType))

    private inline fun <reified T : IrElement> T.copy() = this.deepCopyWithVariables()

    fun IrBuilderWithScope.irReturn(target: IrReturnTargetSymbol, value: IrExpression) =
            IrReturnImpl(startOffset, endOffset, context.builtIns.nothingType, target, value)

    inline fun IrBuilderWithScope.irReturnableBlock(symbol: IrReturnableBlockSymbol, body: IrBlockBuilder.() -> Unit) =
            IrReturnableBlockImpl(startOffset, endOffset, symbol.descriptor.returnType!!, symbol, null,
                    IrBlockBuilder(context, scope, startOffset, endOffset, null, symbol.descriptor.returnType!!)
                            .block(body).statements)
}