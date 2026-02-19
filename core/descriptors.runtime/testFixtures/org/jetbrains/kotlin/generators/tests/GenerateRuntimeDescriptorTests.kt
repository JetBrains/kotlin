/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit4.generateTestGroupSuiteWithJUnit4
import org.jetbrains.kotlin.jvm.runtime.AbstractJvm8RuntimeDescriptorLoaderTest
import org.jetbrains.kotlin.jvm.runtime.AbstractJvmRuntimeDescriptorLoaderTest

fun main(args: Array<String>) {
    val testsRoot = args[0]

    generateTestGroupSuiteWithJUnit4(args) {
        testGroup(testsRoot, "compiler/testData") {
            testClass<AbstractJvmRuntimeDescriptorLoaderTest> {
                model("loadJava/compiledKotlin")
                model("loadJava/compiledJava", extension = "java", excludeDirs = listOf("sam", "kotlinSignature/propagation"))
            }

            testClass<AbstractJvm8RuntimeDescriptorLoaderTest> {
                model("loadJava8/compiledJava", extension = "java")
            }
        }
    }
}
