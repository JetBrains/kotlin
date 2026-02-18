/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.InlineObjectsWithPureInitializationLowering
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys

class WasmInlineObjectsWithPureInitializationLowering(context: WasmBackendContext) : InlineObjectsWithPureInitializationLowering(context) {
    override fun lower(irModule: IrModuleFragment) {
        val disableCrossFileOptimisations = context.configuration.getBoolean(WasmConfigurationKeys.WASM_DISABLE_CROSS_FILE_OPTIMISATIONS)
        val isDebugFriendlyCompilation = context.configuration.getBoolean(WasmConfigurationKeys.WASM_FORCE_DEBUG_FRIENDLY_COMPILATION)
        if (disableCrossFileOptimisations || isDebugFriendlyCompilation) return
        super.lower(irModule)
    }
}