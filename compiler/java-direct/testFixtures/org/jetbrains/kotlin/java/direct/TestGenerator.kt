/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    val baseGenPath = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(baseGenPath, "compiler/fir/analysis-tests/") {
            testClass<AbstractJavaUsingAstTest>("JavaUsingAstResolveTestGenerated") {
                model("testData/resolve/j+k")
            }
        }
        testGroup(baseGenPath, "compiler") {
            testClass<AbstractJavaUsingAstTest>("JavaUsingAstLegacyDiagnosticTestGenerated") {
                model("testData/diagnostics/tests/j+k")
            }
        }
    }
}
