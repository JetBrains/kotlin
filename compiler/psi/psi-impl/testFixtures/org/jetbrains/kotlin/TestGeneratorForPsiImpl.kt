/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.lexer.kdoc.AbstractKDocLexerTest
import org.jetbrains.kotlin.lexer.kotlin.AbstractKotlinLexerTest
import org.jetbrains.kotlin.psi.AbstractKDocTagContentTest
import org.jetbrains.kotlin.psi.parsing.AbstractBlockCodeFragmentParsingTest
import org.jetbrains.kotlin.psi.parsing.AbstractExpressionCodeFragmentParsingTest
import org.jetbrains.kotlin.psi.parsing.AbstractPsiParsingTest

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("compiler/psi/psi-impl/tests-gen", "compiler/psi/psi-impl/testData") {
            testClass<AbstractPsiParsingTest> {
                model("psi", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractKDocTagContentTest> {
                model("psi/kdoc", pattern = TestGeneratorUtil.KT_OR_KTS)
            }

            testClass<AbstractExpressionCodeFragmentParsingTest> {
                model("expressionCodeFragment", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractBlockCodeFragmentParsingTest> {
                model("blockCodeFragment", pattern = TestGeneratorUtil.KT)
            }

            testClass<AbstractKDocLexerTest> {
                model("lexer/kdoc")
            }

            testClass<AbstractKotlinLexerTest> {
                model("lexer/kotlin")
            }
        }
    }
}
