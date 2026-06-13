/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.coroutines.transformSuspendFunction
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

internal class WasmAddContinuationToNonLocalSuspendFunctionsLowering(override val context: WasmBackendContext) :
    AddContinuationToNonLocalSuspendFunctionsLowering(context) {

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrSimpleFunction || !declaration.isSuspend) return null
        return if (context.wasmUseStackSwitching) {
            listOf(transformSuspendFunction(context, declaration, declaration.returnType))
        } else {
            listOf(transformSuspendFunction(context, declaration))
        }
    }
}
