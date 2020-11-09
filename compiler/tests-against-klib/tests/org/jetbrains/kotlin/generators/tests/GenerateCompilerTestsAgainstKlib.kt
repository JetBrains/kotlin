/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.codegen.ir.AbstractCompileKotlinAgainstKlibTest
import org.jetbrains.kotlin.generators.tests.generator.generateTestGroupSuite
import org.jetbrains.kotlin.test.TargetBackend

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuite(args) {
        testGroup("compiler/tests-against-klib/tests", "compiler/testData") {
            testClass<AbstractCompileKotlinAgainstKlibTest> {
                model("codegen/boxKlib", targetBackend = TargetBackend.JVM_IR)
            }
        }
    }
}
