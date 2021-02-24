/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.findUsages

import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.uitls.IgnoreTests
import java.nio.file.Path
import java.nio.file.Paths


fun doTestWithFIRFlagsByPath(path: String, body: () -> Unit) =
    doTestWithFIRFlags(Paths.get(path), body)

fun doTestWithFIRFlags(testFile: Path, body: () -> Unit) {
    if (InTextDirectivesUtils.isDirectiveDefined(testFile.toFile().readText(), "FIR_IGNORE")) return
    IgnoreTests.runTestIfEnabledByFileDirective(testFile, IgnoreTests.DIRECTIVES.FIR_COMPARISON) {
        body()
    }
}