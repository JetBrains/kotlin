/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.generator

import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DIAGNOSTICS_LIST
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.JVM_DIAGNOSTICS_LIST
import java.nio.file.Paths

fun main() {
    val rootPath = Paths.get("idea/idea-frontend-fir/src").toAbsolutePath()
    val packageName = "org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics"
    DiagnosticClassGenerator.generate(rootPath, DIAGNOSTICS_LIST + JVM_DIAGNOSTICS_LIST, packageName)
}
