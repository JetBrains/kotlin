/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.toCatchThrowableOrJsException
import org.jetbrains.kotlin.backend.wasm.utils.isCanonical
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.name.Name

/**
 * Transforms try/catch statements into a simple canonical form which is easily mapped into Wasm instruction set.
 *
 * From this:
 *
 *    try {
 *        ...exprs
 *    } catch (e: Foo) {
 *        ...exprs
 *    } catch (e: Bar) {
 *        ...exprs
 *    } finally {
 *        ...exprs
 *    }
 *
 * We get this:
 *
 *    try {
 *        ...exprs
 *    } catch (e: Throwable) {
 *        when (e) {
 *            is Foo -> ...exprs
 *            is Bar -> ...exprs
 *        }
 *    }
 *
 * With `finally` we transform this:
 *
 *    try {
 *        ...exprs
 *    } catch (e: Throwable) {
 *        ...exprs
 *    } finally {
 *        ...<finally exprs>
 *    }
 *
 * Into something like this (tmp variable is used only if we return some result):
 *
 *    val tmp = block { // this is where we return if we return from original try/catch with the result
 *        try {
 *            try {
 *                return@block ...exprs
 *            } catch (e: Throwable) {
 *                return@block ...exprs
 *            }
 *        }
 *        catch (e: Throwable) {
 *            ...<finally exprs>
 *            throw e // rethrow exception if it happened inside of the catch statement
 *        }
 *    }
 *    ...<finally exprs>
 *    tmp // result
 */
@PhasePrerequisites(FunctionInlining::class)
internal class TryCatchCanonicalization(private val ctx: WasmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(CatchMerger(ctx))

        irFile.transformChildrenVoid(FinallyBlocksLowering(ctx, ctx.irBuiltIns.throwableType))

        irFile.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitTry(aTry: IrTry) {
                check(aTry.isCanonical(ctx)) { "Found non canonical try/catch $aTry" }
            }
        })
    }
}

internal class CatchMerger(private val ctx: WasmBackendContext) : IrElementTransformerVoidWithContext() {

    override fun visitTry(aTry: IrTry): IrExpression {
        // First, handle all nested constructs
        aTry.transformChildrenVoid(this)

        val throwableHandlerIndex = aTry.catches.indexOfFirst { it.catchParameter.type == ctx.irBuiltIns.throwableType }
        val activeCatches = aTry.catches.apply {
            if (throwableHandlerIndex != -1 && throwableHandlerIndex != lastIndex) {
                subList(throwableHandlerIndex + 1, size).clear()
            }
        }

        // Nothing to do
        if (activeCatches.isEmpty())
            return super.visitTry(aTry)

        activeCatches.singleOrNull()?.let { irCatch ->
            // Nothing to do
            if (irCatch == ctx.irBuiltIns.throwableType) {
                irCatch.toCatchThrowableOrJsException = true
                return super.visitTry(aTry)
            }
        }

        ctx.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).run {
            val newCatchParameter = buildVariable(
                currentScope!!.scope.getLocalDeclarationParent(),
                startOffset,
                endOffset,
                IrDeclarationOrigin.CATCH_PARAMETER,
                Name.identifier("merged_catch_param"),
                ctx.irBuiltIns.throwableType
            )

            val newCatchBody = irBlock(aTry, startOffset = UNDEFINED_OFFSET, endOffset = UNDEFINED_OFFSET) {
                +irWhen(
                    aTry.type,
                    activeCatches.map {
                        irBranch(
                            irIs(irGet(newCatchParameter), it.catchParameter.type),
                            irBlock(it.result, startOffset = UNDEFINED_OFFSET, endOffset = UNDEFINED_OFFSET) {

                                it.catchParameter.initializer = irImplicitCast(irGet(newCatchParameter), it.catchParameter.type)
                                it.catchParameter.origin = IrDeclarationOrigin.DEFINED
                                +it.catchParameter
                                +it.result
                            }
                        )
                    } + irElseBranch(irThrow(irGet(newCatchParameter)))
                )
            }

            val newCatch = irCatch(newCatchParameter, newCatchBody).apply {
                val hasThrowableOrJsException =
                    throwableHandlerIndex >= 0 ||
                            (ctx.isWasmJsTarget &&
                                    activeCatches.any { it.catchParameter.type == ctx.wasmSymbols.jsRelatedSymbols.jsException.defaultType })

                toCatchThrowableOrJsException = hasThrowableOrJsException
            }

            return irTry(
                aTry.type,
                aTry.tryResult,
                listOf(newCatch),
                aTry.finallyExpression
            )
        }
    }
}

