/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm

enum class WasmServiceImportExportKind(val prefix: String) {
    VTABLE("__vt$"),
    ITABLE("__it$"),
    RTTI("__rt$"),
    FUNC("__fn$")
}

/**
 * Names of exports that the Kotlin/Wasm compiler generates on its own, not from user declarations.
 */
object WasmSpecialExportNames {
    const val MEMORY = "memory"
    const val INITIALIZE = "_initialize"
    const val START = "_start"
    const val START_UNIT_TESTS = "startUnitTests"
    const val CALL_FUNCTION_PREFIX = "__callFunction_"

    /**
     * Whether the export is used only by the compiler itself.
     * Such exports are not exposed from the generated JS module, so user declarations must not be exported to JS under these names.
     */
    fun isInternal(name: String): Boolean =
        name == MEMORY ||
                name.startsWith(CALL_FUNCTION_PREFIX) ||
                WasmServiceImportExportKind.entries.any { name.startsWith(it.prefix) }
}
