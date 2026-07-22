/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class WasmTailrecCheckerLowering<Context : LoweringContext>(context: Context): TailrecCheckerLowering<Context>(context) {
    // WasmTailrecLowering has a condition `reference.origin == IrStatementOrigin.LAMBDA`, however here it would break the following tests:
    // - recursiveCallInInlineLambda.kt
    // - recursiveCallInInlineLambdaWithCapture
    // TODO: KT-83995: investigate why "lambda origin" is needed in WasmTailrecLowering, when it would break tests
    override fun followRichFunctionReference(reference: IrRichFunctionReference): Boolean =
        false//
}

open class TailrecCheckerLowering<Context : LoweringContext>(val context: Context) : ModuleLoweringPass {
    open fun followRichFunctionReference(reference: IrRichFunctionReference) = false
    open fun followFunctionReference(reference: IrFunctionReference) = false

    override fun lower(irModule: IrModuleFragment) {
        irModule.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                declaration.acceptChildrenVoid(this)

                if (!declaration.isTailrec) return

                val tailCalls = collectTailRecursionCalls(
                    declaration,
                    followFunctionReference = ::followFunctionReference,
                    followRichFunctionReference = ::followRichFunctionReference,
                )

                if (tailCalls.ir.isEmpty()) {
                    context.diagnosticReporter
                        .at(declaration, declaration.file)
                        .report(CommonBackendErrors.NO_TAIL_CALLS_FOUND_IN_IR)
                }
            }
        })
    }
}
