/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability.Global
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object FirDiagnosticsDirectives : SimpleDirectivesContainer() {
    val DUMP_CFG by directive(
        description = """
            Dumps control flow graphs of all declarations to `testName.dot` file
            This directive may be applied only to all modules
        """.trimIndent(),
        applicability = Global
    )

    val FIR_DUMP by directive(
        description = """
            Dumps resulting fir to `testName.fir` file
        """.trimIndent(),
        applicability = Global
    )

    val FIR_IDENTICAL by directive(
        description = "Contents of fir test data file and FE 1.0 are identical",
        applicability = Global
    )

    val USE_LIGHT_TREE by directive(
        description = "Enables light tree parser instead of PSI"
    )

    val COMPARE_WITH_LIGHT_TREE by directive(
        description = """
            Enable comparing diagnostics between PSI and light tree modes
            For enabling light tree mode use $USE_LIGHT_TREE directive
        """.trimIndent(),
        applicability = Global
    )

    val WITH_EXTENDED_CHECKERS by directive(
        description = "Enable extended checkers"
    )
}
