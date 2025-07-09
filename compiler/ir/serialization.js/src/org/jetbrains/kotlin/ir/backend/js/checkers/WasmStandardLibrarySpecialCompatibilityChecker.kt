/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers

import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isWasmStdlib
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

object WasmStandardLibrarySpecialCompatibilityChecker : LibrarySpecialCompatibilityChecker() {
    override fun shouldCheckLibrary(library: KotlinLibrary) = library.isWasmStdlib

    override fun getMessageToReport(compilerVersion: Version, libraryVersion: Version): String? {
        return runUnless(libraryVersion == compilerVersion) {
            "The version of the Kotlin/Wasm standard library ($libraryVersion) differs from the version of the compiler ($compilerVersion). " +
                    "Please, note that while Kotlin/Wasm is in active development phase only matching versions are supported.\n" +
                    "Please, make sure that the standard library has the same version as the compiler. " +
                    "Adjust your project's settings if necessary."
        }
    }
}
