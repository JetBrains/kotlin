/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer
import org.jetbrains.kotlin.platform.wasm.WasmTarget

@Suppress("unused")
object WasmConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.wasm.config", "WasmConfigurationKeys") {
    val WASM_ENABLE_ARRAY_RANGE_CHECKS by key<Boolean>("enable array range checks")
    val WASM_ENABLE_ASSERTS by key<Boolean>("enable asserts")
    val WASM_GENERATE_WAT by key<Boolean>("generate wat file")
    val WASM_TARGET by key<WasmTarget>("wasm target", defaultValue = "WasmTarget.JS")
    val WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS by key<Boolean>("use wasm traps instead of throwing exceptions")
    val WASM_USE_NEW_EXCEPTION_PROPOSAL by key<Boolean>("use wasm new exception proposal")
    val WASM_NO_JS_TAG by key<Boolean>("Don't use WebAssembly.JSTag for throwing and catching exceptions")
    val WASM_DEBUG by key<Boolean>("Generate debug information")
    val DCE_DUMP_DECLARATION_IR_SIZES_TO_FILE by key<String>("Path for dumping declaration IR sizes to file")
    val WASM_GENERATE_DWARF by key<Boolean>("generate DWARF debug information")
    val WASM_FORCE_DEBUG_FRIENDLY_COMPILATION by key<Boolean>("avoid optimizations that can break debugging.")
    val WASM_INCLUDED_MODULE_ONLY by key<Boolean>("compile single module.")
    val WASM_DEPENDENCY_RESOLUTION_MAP by key<String>("provide alternative paths to imported dependency modules.")
    val WASM_COMMAND_MODULE by key<Boolean>("use command module initialization (_initialize export).")
}
