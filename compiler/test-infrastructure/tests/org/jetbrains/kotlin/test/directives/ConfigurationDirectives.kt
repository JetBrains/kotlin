/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object ConfigurationDirectives : SimpleDirectivesContainer() {
    val KOTLIN_CONFIGURATION_FLAGS by stringDirective(
        "List of kotlin configuration flags"
    )

    val WITH_RUNTIME by directive(
        description = """
            Add Kotlin stdlib to classpath
            This directive is deprecated, use WITH_STDLIB instead
        """.trimIndent()
    )
    val WITH_STDLIB by directive("Add Kotlin runtime to classpath")
}
