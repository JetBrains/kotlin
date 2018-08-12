/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.checkers.*
import org.jetbrains.kotlin.generators.tests.generator.testGroup

fun main(args: Array<String>) {
    testGroup("compiler/tests-spec/tests", "compiler/tests-spec/testData") {
        testClass<AbstractDiagnosticsTestSpec> {
            model("diagnostics", excludeDirs = listOf("_helpers"))
        }
    }
}