/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object MultiplatformDiagnosticsDirectives : SimpleDirectivesContainer() {
    val MULTIPLATFORM_COMPOSITE_ANALYSIS_MODE by directive(
        description = """
            Will setup the classical frontend similar to 'CLI metadata compilation' or 'IDE analysis' where dependsOn sources
            are located in separate module descriptors.
        """.trimIndent(),
        applicability = DirectiveApplicability.Global
    )
}
