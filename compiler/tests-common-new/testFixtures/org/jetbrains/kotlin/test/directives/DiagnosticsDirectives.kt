/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object DiagnosticsDirectives : SimpleDirectivesContainer() {
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

    val MARK_DYNAMIC_CALLS by directive(
        description = """
            Render debug info about dynamic calls
        """.trimIndent()
    )

    val REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO by directive(
        description = """
            If this directive enabled then `DEBUG_INFO_...` diagnostics will be reported
              only if they are defined in testdata.
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
