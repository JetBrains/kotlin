/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.codegen.AbstractCheckLocalVariablesTableTest
import org.jetbrains.kotlin.codegen.flags.AbstractWriteFlagsTest
import org.jetbrains.kotlin.generators.dsl.junit4.generateTestGroupSuiteWithJUnit4
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.jvm.compiler.AbstractCompileJavaAgainstKotlinTest
import org.jetbrains.kotlin.jvm.compiler.AbstractWriteSignatureTest
import org.jetbrains.kotlin.test.TargetBackend

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()
    generateTestGroupSuiteWithJUnit5(args, mainClassName) {
        testGroup("compiler/tests-gen", "compiler/testData") {
            testClass<AbstractCompileJavaAgainstKotlinTest> {
                model(
                    "compileJavaAgainstKotlin",
                    testClassName = "WithoutJavac",
                    testMethod = "doTestWithoutJavac",
                )
                model(
                    "compileJavaAgainstKotlin",
                    testClassName = "WithJavac",
                    testMethod = "doTestWithJavac",
                )
            }
        }
    }

    generateTestGroupSuiteWithJUnit4(args, mainClassName) {
        testGroup("compiler/tests-gen", "compiler/testData") {
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
