/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.inline.CommonInlineCallableReferenceToLambdaPhase

internal class WasmInlineCallableReferenceToLambdaPhase(context: WasmBackendContext) : CommonInlineCallableReferenceToLambdaPhase(
    context, WasmInlineFunctionResolver(context)
) {
    override fun visitFunction(declaration: IrFunction, data: IrDeclarationParent?): IrStatement =
        context.irFactory.stageController.restrictTo(declaration) {
            super.visitFunction(declaration, data)
        }
}