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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.implicitCastTo
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
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
 * functions, we have to fake non-local returns by scanning the body of the try block and rewriting
 * early returns with a state machine.
 *
 */
internal class WasmOOBEHandlerInsertionLowering(private val ctx: WasmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (ctx.configuration.getBoolean(WasmConfigurationKeys.WASM_DISABLE_OOBE_HANDLER_INSERTION)) return
        irFile.transformChildrenVoid(WasmOOBEHandlerInsertionTransformer(ctx))
    }
}

private fun containsReturnTargeting(expression: IrExpression, targetSymbol: IrReturnTargetSymbol): Boolean {
    var found = false
    expression.acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            if (!found) element.acceptChildrenVoid(this)
        }

        override fun visitReturn(expression: IrReturn) {
            if (expression.returnTargetSymbol == targetSymbol) found = true
            else super.visitReturn(expression)
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
        val anyNType = ctx.irBuiltIns.anyNType

        val outerFunctionSymbol = (currentFunction!!.irElement as IrFunction).symbol
        val outerReturnType = outerFunctionSymbol.owner.returnType

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

            val hasReturns = containsReturnTargeting(aTry.tryResult, outerFunctionSymbol)

            var hasReturnedVar: IrVariable? = null
            var returnValueVar: IrVariable? = null

            // If there are early returns, rewrite the body to set some state variables with the
            // returns properly retargeted.
            if (hasReturns) {
                hasReturnedVar = buildVariable(
                    parent, aTry.startOffset, aTry.endOffset,
                    IrDeclarationOrigin.IR_TEMPORARY_VARIABLE, Name.identifier("hasEarlyReturn"), ctx.irBuiltIns.booleanType,
                    isVar = true
                ).apply {
                    initializer = irBoolean(false)
                }

                returnValueVar = buildVariable(
                    parent, aTry.startOffset, aTry.endOffset,
                    IrDeclarationOrigin.IR_TEMPORARY_VARIABLE, Name.identifier("earlyReturnValue"), anyNType,
                    isVar = true
                ).apply {
                    initializer = irNull()
                }

                aTry.tryResult.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        if (expression.returnTargetSymbol == outerFunctionSymbol) {
                            return IrReturnImpl(
                                expression.startOffset, expression.endOffset,
                                ctx.irBuiltIns.nothingType,
                                lambdaFunction.symbol,
                                irBlock {
                                    +irSet(hasReturnedVar.symbol, irBoolean(true))
                                    +irSet(returnValueVar.symbol, expression.value.implicitCastTo(anyNType))
                                    +irNull()
                                }
                            )
                        }
                        return super.visitReturn(expression)
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

            val lambdaExpression = IrFunctionExpressionImpl(
                aTry.startOffset,
                aTry.endOffset,
                ctx.irBuiltIns.functionN(0).typeWith(anyNType),
                lambdaFunction,
                IrStatementOrigin.LAMBDA
            )

            val newTryBody = irBlock(startOffset = aTry.startOffset, endOffset = aTry.endOffset) {
                val call = irCall(adapterSymbol, anyNType).apply {
                    arguments[0] = lambdaExpression
                }
                +irImplicitCast(call, aTry.type)
            }

            val newTry = irTry(
                aTry.type,
                newTryBody,
                aTry.catches,
                aTry.finallyExpression
            )

            // Make sure we actually early return if there are any.
            if (hasReturns) {
                val hrv = hasReturnedVar!!
                val rvv = returnValueVar!!
                return irBlock(startOffset = UNDEFINED_OFFSET, endOffset = UNDEFINED_OFFSET) {
                    +hrv
                    +rvv

                    val tryExprResult = irTemporary(newTry, nameHint = "try_execution_value")

                    +irIfThen(irGet(hrv), irReturn(irImplicitCast(irGet(rvv), outerReturnType)))
                    +irGet(tryExprResult)
                }
            } else {
                return newTry
            }
        }
    }
}
