/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.cli

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object CliDirectives : SimpleDirectivesContainer() {
    val KOTLINC_ARGS by stringDirective("Additional arguments to pass to kotlinc")

    val CHECK_COMPILER_OUTPUT by directive("Check compiler output (except diagnostics with location) against a golden file")

    val FORCE_COMPILE_AS_JAVA_MODULE by directive(
        "Force using -Xmodule-path instead of -classpath, as if a Java module is compiled (even if there's no module-info.java)"
    )
}
