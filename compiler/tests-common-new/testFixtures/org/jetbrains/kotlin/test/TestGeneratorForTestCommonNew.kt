/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.test.runners.AbstractDirectivesValidatorTest
import org.jetbrains.kotlin.test.runners.codegen.inlineScopes.*

fun main(args: Array<String>) {
    val testsRoot = args[0]
    val mainClassName = TestGeneratorUtil.getMainClassName()
    val k1BoxTestDir = listOf("multiplatform/k1")
    val k2BoxTestDir = listOf("multiplatform/k2")
    val excludedScriptDirs = listOf("script")

    // We exclude the 'inlineScopes/newFormatToOld' directory from tests that have inline scopes enabled
    // by default, since we only want to test the scenario where code with inline scopes is inlined by the
    // old inliner with $iv suffixes.
    val inlineScopesNewFormatToOld = listOf("inlineScopes/newFormatToOld")

    generateTestGroupSuiteWithJUnit5(args, mainClassName) {
        testGroup(testsRoot, testDataRoot = "compiler/testData/codegen") {
            testClass<AbstractDirectivesValidatorTest> {
                model("box")
                model("boxJvm")
            }

            testClass<AbstractFirBlackBoxCodegenTestWithInlineScopes> {
                model("box", excludeDirs = k1BoxTestDir + excludedScriptDirs)
                model("boxJvm", excludeDirs = k1BoxTestDir + excludedScriptDirs)
            }
        }

        testGroup(testsRoot, testDataRoot = "compiler/testData") {
            // ------------- Inline scopes tests duplication -------------

            testClass<AbstractFirBytecodeTextTestWithInlineScopes> {
                model("codegen/bytecodeText")
            }

            testClass<AbstractFirSteppingTestWithInlineScopes> {
                model("debug/stepping")
            }

            testClass<AbstractFirLocalVariableTestWithInlineScopes> {
                model("debug/localVariables", excludeDirs = inlineScopesNewFormatToOld)
            }

            testClass<AbstractFirBlackBoxInlineCodegenTestWithInlineScopes> {
                model("codegen/boxInline", excludeDirs = k2BoxTestDir)
            }

            testClass<AbstractFirBlackBoxCodegenTestWithInlineScopes>("FirBlackBoxModernJdkCodegenTestGeneratedWithInlineScopes") {
                model("codegen/boxModernJdk")
            }
        }
    }
}
