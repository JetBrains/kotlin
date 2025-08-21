/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers

import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isWasmKotlinTest
import org.jetbrains.kotlin.library.isWasmStdlib
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

object WasmLibrarySpecialCompatibilityChecker : LibrarySpecialCompatibilityChecker() {
    override fun shouldCheckLibrary(library: KotlinLibrary) = library.isWasmStdlib || library.isWasmKotlinTest

    override fun getMessageToReport(compilerVersion: Version, libraryVersion: Version, library: KotlinLibrary): String? {
        val libraryDisplayName = when {
            library.isWasmStdlib -> "standard"
            library.isWasmKotlinTest -> "kotlin-test"
            else -> null
        }
        return runUnless(libraryVersion == compilerVersion) {
            "The version of the Kotlin/Wasm $libraryDisplayName library ($libraryVersion) differs from the version of the compiler ($compilerVersion).\n" +
                    "Please, make sure that the $libraryDisplayName library has the same version as the compiler. " +
                    "Adjust your project's settings if necessary."
        }
    }
}
