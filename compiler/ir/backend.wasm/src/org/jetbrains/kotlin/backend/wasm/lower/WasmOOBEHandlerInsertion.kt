/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrRichFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.implicitCastTo
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.selectSAMOverriddenFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

/**
 * Insert calls to withJsOutOfBoundsExceptionToKotlinAdapter around try blocks
 * whose catch clauses could catch an IndexOutOfBoundsException.
 *
 * This is done because Wasm runtimes trap on out of bounds exceptions, which we would like to
 * convert to Kotlin exceptions. Instead of inserting a software bounds check at every array
 * reference, this moves the associated overhead to just when try/catch blocks are established.
 *
 * Logically, the following source-to-source transformation is performed:
 *
 * try {
 *     // ...
 * } catch (e: Whatever) {
 *     // ...
 * }
 *
 * =>
 *
 * try {
 *     withJsOutOfBoundsExceptionToKotlinAdapter {
 *         // ...unchecked array reads
 *     }
 * } catch (e: Whatever) {
 *     // ...handling
 * }
 *
 * Note that because neither Kotlin nor Wasm supports non-local returns for non-inline
 * functions or non-local exits more generally, we have to fake non-local returns and other exits by
 * scanning the body of the try block and rewriting them into a state machine-like contraption.
 *
 * One limitation of this pass is that try bodies with suspend calls can't easily be lowered.
 Perhaps the stack-switching mechanism will help remove this limitation.
 *
 */
internal class WasmOOBEHandlerInsertionLowering(private val ctx: WasmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (!ctx.isWasmJsTarget) return
        if (ctx.configuration.getBoolean(WasmConfigurationKeys.WASM_DISABLE_OOBE_HANDLER_INSERTION)) return
        irFile.transformChildrenVoid(WasmOOBEHandlerInsertionTransformer(ctx))
    }
}

private fun collectNonLocalReturnTargets(expression: IrExpression): List<IrReturnTargetSymbol> {
    val localTargets = mutableSetOf<IrReturnTargetSymbol>()
    val nonLocalTargets = mutableSetOf<IrReturnTargetSymbol>()
    expression.acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitFunction(declaration: IrFunction) {
            localTargets.add(declaration.symbol)
            super.visitFunction(declaration)
        }

        override fun visitReturnableBlock(expression: IrReturnableBlock) {
            localTargets.add(expression.symbol)
            super.visitReturnableBlock(expression)
        }

        override fun visitReturn(expression: IrReturn) {
            if (expression.returnTargetSymbol !in localTargets) {
                nonLocalTargets.add(expression.returnTargetSymbol)
            }
            super.visitReturn(expression)
        }
    })
    return nonLocalTargets.toList()
}

private fun collectNonLocalJumps(expression: IrExpression): List<Pair<IrLoop, Boolean>> {
    val localLoops = mutableSetOf<IrLoop>()
    val jumps = mutableSetOf<Pair<IrLoop, Boolean>>()
    expression.acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitLoop(loop: IrLoop) {
            localLoops.add(loop)
            super.visitLoop(loop)
        }

        override fun visitBreak(jump: IrBreak) {
            if (jump.loop !in localLoops) jumps.add(jump.loop to true)
            super.visitBreak(jump)
        }

        override fun visitContinue(jump: IrContinue) {
            if (jump.loop !in localLoops) jumps.add(jump.loop to false)
            super.visitContinue(jump)
        }
    })
    return jumps.toList()
}

private fun containsSuspendCalls(expression: IrExpression): Boolean {
    var found = false
    expression.acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            if (!found) element.acceptChildrenVoid(this)
        }

        override fun visitCall(expression: IrCall) {
            if (expression.symbol.owner.isSuspend) found = true
            else super.visitCall(expression)
        }
    })
    return found
}

private class WasmOOBEHandlerInsertionTransformer(private val ctx: WasmBackendContext) : IrElementTransformerVoidWithContext() {
    private val adapterSymbol = ctx.wasmSymbols.jsRelatedSymbols.jsInteropAdapters.withJsOutOfBoundsExceptionToKotlinAdapter
    private val ioobeClass = ctx.wasmSymbols.indexOutOfBoundsException.owner

    override fun visitTry(aTry: IrTry): IrExpression {
        aTry.transformChildrenVoid(this)

        if (aTry.catches.isEmpty()) return aTry

        // Only insert the adapter if it's possible that IOOBE could be handled here somehow.
        if (aTry.catches.none { catchClause ->
                val catchClass = catchClause.catchParameter.type.classOrNull?.owner ?: return@none false
                ioobeClass.isSubclassOf(catchClass)
            }) return aTry

        // Make sure we don't insert the adapter in the adapter definition itself.
        if (allScopes.any { (it.irElement as? IrFunction)?.symbol == adapterSymbol }) return aTry

        // The adapter lambda is non-suspend, so we can't wrap try bodies with suspend calls.
        if (containsSuspendCalls(aTry.tryResult)) return aTry

        val anyNType = ctx.irBuiltIns.anyNType

        ctx.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).run {

            val lambdaFunction = ctx.irFactory.buildFun {
                startOffset = aTry.startOffset
                endOffset = aTry.endOffset
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                name = Name.special("<OOBEWrapper>")
                visibility = DescriptorVisibilities.LOCAL
                returnType = anyNType
            }.apply {
                this.parent = scope.getLocalDeclarationParent()
            }

            val nonLocalReturnTargets = collectNonLocalReturnTargets(aTry.tryResult)
            val nonLocalJumps = collectNonLocalJumps(aTry.tryResult)
            val hasReturns = nonLocalReturnTargets.isNotEmpty()
            val hasJumps = nonLocalJumps.isNotEmpty()

            // Use an int state variable to track which non-local exit to perform.
            // 0 = normal, positive values index into the combined return/jump lists.
            var exitStateVar: IrVariable? = null
            var returnValueVar: IrVariable? = null

            val returnIndexMap: Map<IrReturnTargetSymbol, Int>
            val jumpIndexMap: Map<Pair<IrLoop, Boolean>, Int>

            if (hasReturns || hasJumps) {
                exitStateVar = buildVariable(
                    parent, aTry.startOffset, aTry.endOffset,
                    IrDeclarationOrigin.IR_TEMPORARY_VARIABLE, Name.identifier("exitState"), ctx.irBuiltIns.intType,
                    isVar = true
                ).apply {
                    initializer = irInt(0)
                }

                var nextIndex = 1
                returnIndexMap = nonLocalReturnTargets.associateWith { nextIndex++ }
                jumpIndexMap = nonLocalJumps.associateWith { nextIndex++ }

                if (hasReturns) {
                    returnValueVar = buildVariable(
                        parent, aTry.startOffset, aTry.endOffset,
                        IrDeclarationOrigin.IR_TEMPORARY_VARIABLE, Name.identifier("earlyReturnValue"), anyNType,
                        isVar = true
                    ).apply {
                        initializer = irNull()
                    }
                }
            } else {
                returnIndexMap = emptyMap()
                jumpIndexMap = emptyMap()
            }

            // Rewrite returns and break/continue that can't cross the lambda boundary.
            if (hasReturns || hasJumps) {
                aTry.tryResult.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        val index = returnIndexMap[expression.returnTargetSymbol]
                        if (index != null) {
                            return IrReturnImpl(
                                expression.startOffset, expression.endOffset,
                                ctx.irBuiltIns.nothingType,
                                lambdaFunction.symbol,
                                irBlock {
                                    +irSet(exitStateVar!!.symbol, irInt(index))
                                    +irSet(returnValueVar!!.symbol, expression.value.implicitCastTo(anyNType))
                                    +irNull()
                                }
                            )
                        }
                        return super.visitReturn(expression)
                    }

                    override fun visitBreak(jump: IrBreak): IrExpression {
                        val index = jumpIndexMap[jump.loop to true]
                        if (index != null) {
                            return IrReturnImpl(
                                jump.startOffset, jump.endOffset,
                                ctx.irBuiltIns.nothingType,
                                lambdaFunction.symbol,
                                irBlock {
                                    +irSet(exitStateVar!!.symbol, irInt(index))
                                    +irNull()
                                }
                            )
                        }
                        return super.visitBreak(jump)
                    }

                    override fun visitContinue(jump: IrContinue): IrExpression {
                        val index = jumpIndexMap[jump.loop to false]
                        if (index != null) {
                            return IrReturnImpl(
                                jump.startOffset, jump.endOffset,
                                ctx.irBuiltIns.nothingType,
                                lambdaFunction.symbol,
                                irBlock {
                                    +irSet(exitStateVar!!.symbol, irInt(index))
                                    +irNull()
                                }
                            )
                        }
                        return super.visitContinue(jump)
                    }
                })
            }

            lambdaFunction.body = irBlockBody {
                +IrReturnImpl(
                    aTry.startOffset, aTry.endOffset,
                    ctx.irBuiltIns.nothingType,
                    lambdaFunction.symbol,
                    aTry.tryResult.implicitCastTo(anyNType)
                )
            }
            lambdaFunction.patchDeclarationParents(lambdaFunction.parent)

            val funType = ctx.irBuiltIns.functionN(0)
            val lambdaExpression = IrRichFunctionReferenceImpl(
                startOffset = aTry.startOffset,
                endOffset = aTry.endOffset,
                type = funType.typeWith(anyNType),
                reflectionTargetSymbol = null,
                overriddenFunctionSymbol = funType.selectSAMOverriddenFunction().symbol,
                invokeFunction = lambdaFunction,
                origin = IrStatementOrigin.LAMBDA
            )

            val hasNonLocalExits = hasReturns || hasJumps
            val tryResultType = if (hasNonLocalExits) anyNType else aTry.type

            val newTryBody = irBlock(startOffset = aTry.startOffset, endOffset = aTry.endOffset) {
                val call = irCall(adapterSymbol, anyNType).apply {
                    arguments[0] = lambdaExpression
                }
                if (hasNonLocalExits) +call else +irImplicitCast(call, aTry.type)
            }

            val newTry = irTry(
                tryResultType,
                newTryBody,
                aTry.catches,
                aTry.finallyExpression
            )

            if (hasReturns || hasJumps) {
                val esv = exitStateVar!!
                return irBlock(startOffset = UNDEFINED_OFFSET, endOffset = UNDEFINED_OFFSET) {
                    +esv
                    if (hasReturns) {
                        +returnValueVar!!
                    }

                    val tryExprResult = irTemporary(newTry, nameHint = "try_execution_value")

                    for ((target, index) in returnIndexMap) {
                        val returnType = when (val owner = target.owner) {
                            is IrFunction -> owner.returnType
                            is IrReturnableBlock -> owner.type
                            else -> anyNType
                        }
                        +irIfThen(
                            irEquals(irGet(esv), irInt(index)),
                            IrReturnImpl(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                ctx.irBuiltIns.nothingType,
                                target,
                                irImplicitCast(irGet(returnValueVar!!), returnType)
                            )
                        )
                    }
                    for ((jump, index) in jumpIndexMap) {
                        val (loop, isBreak) = jump
                        +irIfThen(
                            irEquals(irGet(esv), irInt(index)),
                            if (isBreak) irBreak(loop) else irContinue(loop)
                        )
                    }

                    +irGet(tryExprResult)
                }
            } else {
                return newTry
            }
        }
    }
}
