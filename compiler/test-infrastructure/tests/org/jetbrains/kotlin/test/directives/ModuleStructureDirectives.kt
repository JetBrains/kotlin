/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object ModuleStructureDirectives : SimpleDirectivesContainer() {
    val MODULE by stringDirective(
        """
            Usage: // MODULE: {name}[(dependencies)]
            Describes one module. If no targets are specified then <TODO>
        """.trimIndent()
    )

    val FILE by stringDirective(
        """
            Usage: // FILE: name.{kt|java}
            Declares file with specified name in current module
        """.trimIndent()
    )

    val SNIPPET by directive(
        """
            Usage: // SNIPPET
            Declares (next) snippet with auto-incremented number
        """.trimIndent()
    )
}
