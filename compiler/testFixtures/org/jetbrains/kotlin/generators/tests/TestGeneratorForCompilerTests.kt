/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.codegen.AbstractCheckLocalVariablesTableTest
import org.jetbrains.kotlin.codegen.flags.AbstractWriteFlagsTest
import org.jetbrains.kotlin.generators.dsl.junit4.generateTestGroupSuiteWithJUnit4
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.jvm.compiler.AbstractCompileJavaAgainstKotlinTest
import org.jetbrains.kotlin.jvm.compiler.AbstractWriteSignatureTest
import org.jetbrains.kotlin.lexer.kdoc.AbstractKDocLexerTest
import org.jetbrains.kotlin.lexer.kotlin.AbstractKotlinLexerTest
import org.jetbrains.kotlin.modules.xml.AbstractModuleXmlParserTest
import org.jetbrains.kotlin.test.TargetBackend

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()
    generateTestGroupSuiteWithJUnit4(args, mainClassName) {
        testGroup("compiler/tests-gen", "compiler/testData") {
            testClass<AbstractModuleXmlParserTest> {
                model("modules.xml", extension = "xml")
            }

            testClass<AbstractKDocLexerTest> {
                model("lexer/kdoc")
            }

            testClass<AbstractKotlinLexerTest> {
                model("lexer/kotlin")
            }

            testClass<AbstractCompileJavaAgainstKotlinTest> {
                model(
                    "compileJavaAgainstKotlin",
                    testClassName = "WithoutJavac",
                    testMethod = "doTestWithoutJavac",
                    targetBackend = TargetBackend.JVM_IR
                )
                model(
                    "compileJavaAgainstKotlin",
                    testClassName = "WithJavac",
                    testMethod = "doTestWithJavac",
                    targetBackend = TargetBackend.JVM_IR
                )
            }

            testClass<AbstractWriteFlagsTest> {
                model("writeFlags", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractCheckLocalVariablesTableTest> {
                model("checkLocalVariablesTable", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractWriteSignatureTest> {
                model("writeSignature", targetBackend = TargetBackend.JVM_IR)
            }
        }
    }
}
