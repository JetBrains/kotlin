/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object ModuleStructureDirectives : SimpleDirectivesContainer() {
    val MODULE by stringDirective(
        """
            Usage: // MODULE: {name}[(dependencies)]
            Describes one module. If no targets are specified then <TODO>
        """.trimIndent()
    )

    val DEPENDENCY by stringDirective(
        """
            Usage: // DEPENDENCY: {name} [Source|Klib|Binary]
            Declares simple dependency on other module 
        """.trimIndent()
    )

    val DEPENDS_ON by stringDirective(
        """
            Usage: // DEPENDS_ON: {name} [Source|Klib|Binary]
            Declares dependency on other module witch may contains `expect`
             declarations which has corresponding `expect` declarations
             in current module
        """.trimIndent()
    )

    val FILE by stringDirective(
        """
            Usage: // FILE: name.{kt|java}
            Declares file with specified name in current module
        """.trimIndent()
    )

    val ALLOW_FILES_WITH_SAME_NAMES by directive(
        """
        Allows specifying test files with the same names using the // FILE directive.
        """.trimIndent()
    )

    val TARGET_FRONTEND by stringDirective(
        """
            Usage: // TARGET_FRONTEND: {Frontend}
            Declares frontend for analyzing current module 
        """.trimIndent()
    )

    val TARGET_BACKEND_KIND by enumDirective<TargetBackend>(
        """
            Usage: // TARGET_BACKEND: {Backend}
            Declares backend for analyzing current module 
        """.trimIndent()
    )

    val TARGET_PLATFORM by enumDirective<TargetPlatformEnum>(
        "Declares target platform for current module"
    )

    val JVM_TARGET by stringDirective(
        "Declares JVM target platform for current module"
    )
}
