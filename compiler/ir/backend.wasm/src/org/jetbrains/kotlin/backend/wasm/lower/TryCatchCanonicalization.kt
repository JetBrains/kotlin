/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.isCanonical
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name

// This pass transforms try/catch statements into a simple canonical form which is easily mapped into wasm instruction set.
// From this:
//    try {
//        ...exprs
//    } catch (e: Foo) {
//        ...exprs
//    } catch (e: Bar) {
//        ...exprs
//    } finally {
//        ...exprs
//    }
// We get this:
//    try {
//        ...exprs
//    } catch (e: Throwable) {
//        when (e) {
//            is Foo -> ...exprs
//            is Bar -> ...exprs
//        }
//    }
// With finally we transform this:
//    try {
//        ...exprs
//    } catch (e: Throwable) {
//        ...exprs
//    } finally {
//        ...<finally exprs>
//    }
// Into something like this (tmp variable is used only if we return some result):
//    val tmp = block { // this is where we return if we return from original try/catch with the result
//      try {
//        try {
//            return@block ...exprs
//        } catch (e: Throwable) {
//            return@block ...exprs
//        }
//     }
//     catch (e: Throwable) {
//       ...<finally exprs>
//       throw e // rethrow exception if it happened inside of the catch statement
//     }
//   }
//   ...<finally exprs>
//   tmp // result


internal class TryCatchCanonicalization(private val ctx: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(CatchMerger(ctx))

        irFile.transformChildrenVoid(FinallyBlocksLowering(ctx, ctx.irBuiltIns.throwableType))

        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitTry(aTry: IrTry) {
                check(aTry.isCanonical(ctx.irBuiltIns)) { "Found non canonical try/catch $aTry" }
            }
        })
    }
}

internal class CatchMerger(private val ctx: CommonBackendContext) : IrElementTransformerVoidWithContext() {
    override fun visitTry(aTry: IrTry): IrExpression {
        // First, handle all nested constructs
        aTry.transformChildrenVoid(this)

        // Nothing to do
        if (aTry.catches.isEmpty() ||
            aTry.catches.singleOrNull()?.catchParameter?.symbol?.owner?.type == ctx.irBuiltIns.throwableType)
            return aTry

        ctx.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).run {
            val newCatchParameter = buildVariable(
                currentScope!!.scope.getLocalDeclarationParent(),
                startOffset,
                endOffset,
                IrDeclarationOrigin.CATCH_PARAMETER,
                Name.identifier("merged_catch_param"),
                ctx.irBuiltIns.throwableType
            )

            val newCatchBody = irBlock(aTry) {
                +irWhen(
                    aTry.type,
                    aTry.catches.map {
                        irBranch(
                            irIs(irGet(newCatchParameter), it.catchParameter.type),
                            irBlock(it.result) {
                                it.catchParameter.initializer = irImplicitCast(irGet(newCatchParameter), it.catchParameter.type)
                                it.catchParameter.origin = IrDeclarationOrigin.DEFINED
                                +it.catchParameter
                                +it.result
                            }
                        )
                    } + irElseBranch(irThrow(irGet(newCatchParameter)))
                )
            }

            val newCatch = irCatch(newCatchParameter, newCatchBody)

            return irTry(aTry.type, aTry.tryResult, listOf(newCatch), aTry.finallyExpression)
        }
    }
}

