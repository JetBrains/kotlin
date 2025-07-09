/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers

import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.library.KOTLINTEST_MODULE_NAME
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.builtInsPlatform
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.uniqueName

object JsTestLibrarySpecialCompatibilityChecker : LibrarySpecialCompatibilityChecker() {
    override fun shouldCheckLibrary(library: KotlinLibrary): Boolean =
        library.uniqueName == KOTLINTEST_MODULE_NAME && library.builtInsPlatform == BuiltInsPlatform.JS

    override fun getMessageToReport(compilerVersion: Version, libraryVersion: Version): String? {
        val rootCause = when {
            libraryVersion < compilerVersion ->
                "The Kotlin/JS test library has an older version ($libraryVersion) than the compiler ($compilerVersion). Such a configuration is not supported."

            !libraryVersion.hasSameLanguageVersion(compilerVersion) ->
                "The Kotlin/JS test library has a more recent version ($libraryVersion) than the compiler supports. The compiler version is $compilerVersion."

            else -> return null
        }

        return "$rootCause\nPlease, make sure that the test library has the version in the range " +
                "[${compilerVersion.toComparableVersionString()} .. ${compilerVersion.toLanguageVersionString()}.${KotlinVersion.MAX_COMPONENT_VALUE}]. " +
                "Adjust your project's settings if necessary."
    }
}