/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object FirDiagnosticsDirectives : SimpleDirectivesContainer() {
    val DUMP_CFG by directive(
        description = """
            Dumps control flow graphs of all declarations to `testName.dot` file
            This directive may be applied only to all modules
        """.trimIndent()
    )

    val FIR_DUMP by directive(
        description = """
            Dumps resulting fir to `testName.fir` file
        """.trimIndent()
    )

    val FIR_IDENTICAL by directive(
        description = "Contents of fir test data file and FE 1.0 are identical"
    )
}
