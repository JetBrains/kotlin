/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers

import org.jetbrains.kotlin.backend.common.diagnostics.StandardLibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isJsStdlib
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

class JsStandardLibrarySpecialCompatibilityChecker : StandardLibrarySpecialCompatibilityChecker() {
    override fun isStdlib(library: KotlinLibrary) = library.isJsStdlib

    override fun getMessageToReport(compilerVersion: Version, stdlibVersion: Version): String? {
        return runUnless(stdlibVersion >= compilerVersion) {
            "The Kotlin/JS standard library has an older version ($stdlibVersion) than the compiler ($compilerVersion). " +
                    "Such a configuration is not supported.\n" +
                    "Please, make sure that the standard library has at least the same version as the compiler. " +
                    "Adjust your project's settings if necessary."
        }
    }
}
