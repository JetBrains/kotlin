/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object MultiplatformDiagnosticsDirectives : SimpleDirectivesContainer() {
    val ENABLE_MULTIPLATFORM_COMPOSITE_ANALYSIS_MODE by directive(
        description = """
            If present, sets up the classical frontend in a way, similar to how analysis works in IDE, when dependsOn sources are located 
            in separate module descriptors. If absent, setups the frontend in a "CLI-mode", when there's only two modules: 
            one with currently compiled sources (including dependsOn-sources), and the second one with all dependencies. 
        """.trimIndent(),
        applicability = DirectiveApplicability.Global
    )
}
