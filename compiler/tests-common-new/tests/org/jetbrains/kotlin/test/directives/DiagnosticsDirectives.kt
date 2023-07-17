/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.backend.handlers.JvmBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_JAVAC
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.frontend.classic.handlers.ConstantValuesHandler

object DiagnosticsDirectives : SimpleDirectivesContainer() {
    val WITH_NEW_INFERENCE by directive(
        description = "Enables rendering different diagnostics for old and new inference"
    )

    val DIAGNOSTICS by stringDirective(
        description = """
            Enables or disables rendering of specific diagnostics. 
            Syntax:
              Must be '[+-]DIAGNOSTIC_FACTORY_NAME'
              where '+' means 'include'
                    '-' means 'exclude'
              '+' May be used in case if some diagnostic was disabled by default in test runner
                and it should be enabled in specific test
                
            Also you can enable/disable all diagnostics with specific severity using following syntax:
              [+-]infos
              [+-]warnings
              [+-]errors
        """.trimIndent()
    )

    val SKIP_TXT by directive(
        description = "Disables handler which dumps declarations to testName.txt"
    )

    val NI_EXPECTED_FILE by directive(
        description = "Create separate .ni.txt file for declarations dump with new inference enabled"
    )

    val SKIP_JAVAC by directive(
        description = "Skip this test if $USE_JAVAC enabled"
    )

    val JAVAC_EXPECTED_FILE by directive(
        description = "Dump descriptors to .javac.txt file if $USE_JAVAC enabled"
    )

    val MARK_DYNAMIC_CALLS by directive(
        description = """
            Render debug info about dynamic calls
        """.trimIndent()
    )

    val REPORT_JVM_DIAGNOSTICS_ON_FRONTEND by directive(
        description = """
            Collect additional jvm specific diagnostics on frontend
            Note that this directive is not needed if ${JvmBackendDiagnosticsHandler::class} 
              is enabled in test 
        """.trimIndent()
    )

    val RENDER_PACKAGE by stringDirective(
        description = """
            Dump declaration from packages listed in directive
              (additional to root package)
        """.trimIndent()
    )

    val REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO by directive(
        description = """
            If this directive enabled then `DEBUG_INFO_...` diagnostics will be reported
              only if they are defined in testdata.
        """.trimIndent()
    )

    val CHECK_COMPILE_TIME_VALUES by enumDirective<ConstantValuesHandler.Mode>(
        description = """
            Enables ${ConstantValuesHandler::class} which renders values from constant
              evaluator in <!DEBUG_INFO_CONSTANT_VALUE!> meta infos.
              
            Value determines which context slice should be checked
        """.trimIndent()
    )

    val RENDER_DIAGNOSTICS_FULL_TEXT by directive(
        description = "Render frontend diagnostic texts to .diag.txt"
    )

    val RENDER_ALL_DIAGNOSTICS_FULL_TEXT by directive(
        description = "Render both frontend and backend diagnostic texts to .diag.txt"
    )

    val RENDER_IR_DIAGNOSTICS_FULL_TEXT by directive(
        description = "Render IR diagnostic texts to .ir.diag.txt"
    )
}
