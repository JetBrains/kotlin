/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers

import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isWasmKotlinTest
import org.jetbrains.kotlin.library.isWasmStdlib

object WasmLibrarySpecialCompatibilityChecker : LibrarySpecialCompatibilityChecker() {
    override fun KotlinLibrary.toCheckedLibrary(): CheckedLibrary? = when {
        isWasmStdlib -> CheckedLibrary(libraryDisplayName = "standard", platformDisplayName = "Kotlin/Wasm")
        isWasmKotlinTest -> CheckedLibrary(libraryDisplayName = "kotlin-test", platformDisplayName = "Kotlin/Wasm")
        else -> null
    }
}
