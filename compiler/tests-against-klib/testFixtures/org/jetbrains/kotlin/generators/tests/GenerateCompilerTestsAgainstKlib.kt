/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.codegen.ir.AbstractCompileKotlinAgainstKlibTest
import org.jetbrains.kotlin.codegen.ir.AbstractJKlibTest
import org.jetbrains.kotlin.generators.dsl.junit4.generateTestGroupSuiteWithJUnit4
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.jklib.test.runners.AbstractFirLightTreeJKlibIrTextTest

fun main(args: Array<String>) {
    val testsRoot = args[0]
    val mainClassName = TestGeneratorUtil.getMainClassName()

    generateTestGroupSuiteWithJUnit4(args) {
        testGroup(testsRoot, "compiler/testData") {
            testClass<AbstractCompileKotlinAgainstKlibTest> {
                model("codegen/boxKlib", targetBackend = TargetBackend.JVM_IR)
            }
            testClass<AbstractJKlibTest> {
                model("codegen/jklib", targetBackend = TargetBackend.JVM_IR)
            }
        }
    }

    generateTestGroupSuiteWithJUnit5(args, mainClassName) {
        testGroup(testsRoot, "compiler/testData") {
            testClass<AbstractFirLightTreeJKlibIrTextTest> {
                model("ir/irText")
            }
        }
    }
}
