/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator

import org.jetbrains.kotlin.analysis.api.fir.generator.DiagnosticClassGenerator.generate
import org.jetbrains.kotlin.fir.builder.SYNTAX_DIAGNOSTIC_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.JS_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.JVM_DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.WEB_COMMON_DIAGNOSTICS_LIST
import java.nio.file.Paths

fun main(args: Array<String>) {
    require(args.size == 2) {
        """
        Generator requires the following arguments (in this particular order):
        - generated classes package name
        - path to the directory where generated classes will be placed
        """.trimIndent()
    }
    val packageName = args.first()
    val rootPath = Paths.get(args.last()).toAbsolutePath()
    val diagnostics = DIAGNOSTICS_LIST + JVM_DIAGNOSTICS_LIST + JS_DIAGNOSTICS_LIST + SYNTAX_DIAGNOSTIC_LIST +
            WEB_COMMON_DIAGNOSTICS_LIST
    generate(rootPath, diagnostics, packageName)
}
