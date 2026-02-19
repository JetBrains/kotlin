/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer
import org.jetbrains.kotlin.platform.wasm.WasmTarget

@Suppress("unused")
object WasmConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.wasm.config", "WasmConfigurationKeys") {
    val WASM_ENABLE_ARRAY_RANGE_CHECKS by key<Boolean>()
    val WASM_ENABLE_ASSERTS by key<Boolean>()
    val WASM_GENERATE_WAT by key<Boolean>()
    val WASM_TARGET by key<WasmTarget>(defaultValue = "WasmTarget.JS")
    val WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS by key<Boolean>()
    val WASM_USE_NEW_EXCEPTION_PROPOSAL by key<Boolean>()
    val WASM_NO_JS_TAG by key<Boolean>("Don't use WebAssembly.JSTag for throwing and catching exceptions")
    val WASM_DEBUG by key<Boolean>()
    val DCE_DUMP_DECLARATION_IR_SIZES_TO_FILE by key<String>()
    val WASM_GENERATE_DWARF by key<Boolean>()
    val WASM_FORCE_DEBUG_FRIENDLY_COMPILATION by key<Boolean>()
    val WASM_INCLUDED_MODULE_ONLY by key<Boolean>()
    val WASM_DEPENDENCY_RESOLUTION_MAP by key<String>("Provide alternative paths to imported dependency modules.")
    val WASM_COMMAND_MODULE by key<Boolean>("Use command module initialization (_initialize export).")
    val WASM_DISABLE_CROSS_FILE_OPTIMISATIONS by key<Boolean>("Disables cross-file optimizations. Required to for IC.")
    val WASM_INTERNAL_LOCAL_VARIABLE_PREFIX by key<String>("Prefix for the name of internal/synthetic local variables.")
    val WASM_GENERATE_CLOSED_WORLD_MULTIMODULE by key<Boolean>("Enables multi-module closed-world mode.")
}
