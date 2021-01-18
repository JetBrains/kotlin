/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.NoCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability.File
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability.Global
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object CodegenTestDirectives : SimpleDirectivesContainer() {
    val IGNORE_BACKEND by enumDirective<TargetBackend>(
        description = "Ignore failures of test on target backend",
        applicability = Global
    )

    val IGNORE_BACKEND_FIR by enumDirective<TargetBackend>(
        description = "Ignore specific backend if test uses FIR",
        applicability = Global
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
        description = "Dump resulting bytecode to .txt or _ir.txt file",
        applicability = Global
    )

    val RUN_DEX_CHECKER by directive(
        description = "Run DxChecker and D8Checker"
    )

    val IGNORE_DEXING by directive(
        description = "Ignore dex checkers"
    )

    val IGNORE_ERRORS by directive(
        description = """
            Ignore frontend errors in ${NoCompilationErrorsHandler::class}
            If this directive is enabled then ${JvmIrBackendFacade::class} won't produce any binaries for test
              if there are errors in it
        """.trimIndent()
    )

    val IGNORE_FIR_DIAGNOSTICS by directive(
        description = "Run backend even FIR reported some diagnostics with ERROR severity"
    )

    val IR_FILE by stringDirective(
        description = "Specifies file name for IR text dump",
        applicability = File
    )

    val DUMP_EXTERNAL_CLASS by stringDirective(
        description = "Specifies names of external classes which IR should be dumped"
    )

    val EXTERNAL_FILE by directive(
        description = "Indicates that test file is external",
        applicability = File
    )
}
