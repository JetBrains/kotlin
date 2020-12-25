/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object CodegenTestDirectives : SimpleDirectivesContainer() {
    val IGNORE_BACKEND by enumDirective<TargetBackend>(
        description = "Ignore failures of test on target backend"
    )

    val JAVAC_OPTIONS by stringDirective(
        description = "Specify javac options to compile java files"
    )

    val WITH_HELPERS by directive(
        """
            Adds util functions for checking coroutines
            See files in ./compiler/testData/codegen/helpers/
        """.trimIndent()
    )

    val CHECK_BYTECODE_LISTING by directive(
        description = "Dump resulting bytecode to .txt or _ir.txt file"
    )

    val RUN_DEX_CHECKER by directive(
        description = "Run DxChecker and D8Checker"
    )

    val IGNORE_DEXING by directive(
        description = "Ignore dex checkers"
    )
}
