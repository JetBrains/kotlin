/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
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

    val IGNORE_BACKEND_MULTI_MODULE by enumDirective<TargetBackend>(
        description = "Ignore failures of multimodule test on target backend",
        applicability = Global
    )

    val USE_JAVAC_BASED_ON_JVM_TARGET by directive(
        description = """
            Determine version of javac for compilation of java files based
              on JvmTarget of module. If not enabled then javac from
              current runtime will be used
        """.trimIndent()
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

    val IGNORE_JAVA_ERRORS by directive(
        description = "Ignore compilation errors from java"
    )

    val IGNORE_FIR_DIAGNOSTICS by directive(
        description = "Run backend even FIR reported some diagnostics with ERROR severity"
    )

    val DUMP_IR by directive(
        description = "Dumps generated backend IR (enables ${IrTextDumpHandler::class})"
    )

    val DUMP_EXTERNAL_CLASS by stringDirective(
        description = "Specifies names of external classes which IR should be dumped"
    )

    val EXTERNAL_FILE by directive(
        description = "Indicates that test file is external and should be skipped in ${IrTextDumpHandler::class}",
        applicability = File
    )

    val DUMP_KT_IR by directive(
        description = "Dumps generated backend IR in pretty kotlin dump (enables ${IrPrettyKotlinDumpHandler::class})"
    )

    val SKIP_KT_DUMP by directive(
        description = "Skips check pretty kt IR dump (disables ${IrPrettyKotlinDumpHandler::class})"
    )

    val TREAT_AS_ONE_FILE by directive(
        description = "Treat bytecode from all files as one in ${BytecodeTextHandler::class}"
    )

    val NO_CHECK_LAMBDA_INLINING by directive(
        description = "Skip checking of lambda inlining in ${BytecodeInliningHandler::class.java}"
    )

    val SKIP_INLINE_CHECK_IN by stringDirective(
        description = "Skip checking of specific methods in ${BytecodeInliningHandler::class.java}"
    )

    val DUMP_SMAP by directive(
        description = """Enables ${SMAPDumpHandler::class}"""
    )

    val NO_SMAP_DUMP by directive(
        description = "Don't dump smap for marked file",
        applicability = File
    )

    val SEPARATE_SMAP_DUMPS by directive(
        description = """
            If enabled then ${SMAPDumpHandler::class} will dump smap dumps
              into ${SMAPDumpHandler.SMAP_SEP_EXT} and ${SMAPDumpHandler.SMAP_EXT}
              files instead of ${SMAPDumpHandler.SMAP_EXT} depending of module
              structure of test
        """.trimIndent()
    )
}
