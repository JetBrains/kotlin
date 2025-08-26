/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object CustomKlibCompilerTestDirectives : SimpleDirectivesContainer() {
    val IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE by stringDirective(
        description = """
            Ignore a KLIB backward-compatibility test (i.e. the test with the custom KLIB compiler version
            used on the first phase of the test pipeline).
            
            The value of this directive is the specific compiler version prefixes where the test is expected
            to be ignored.
        """.trimIndent(),
    )
}
