/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers

import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isJsKotlinTest
import org.jetbrains.kotlin.library.isJsStdlib

object JsLibrarySpecialCompatibilityChecker : LibrarySpecialCompatibilityChecker() {
    override fun KotlinLibrary.toCheckedLibrary(): CheckedLibrary? = when {
        isJsStdlib -> CheckedLibrary(libraryDisplayName = "standard", platformDisplayName = "Kotlin/JS")
        isJsKotlinTest -> CheckedLibrary(libraryDisplayName = "kotlin-test", platformDisplayName = "Kotlin/JS")
        else -> null
    }
}
