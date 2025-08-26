/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.jvm.runtime.AbstractJvm8RuntimeDescriptorLoaderTest
import org.jetbrains.kotlin.jvm.runtime.AbstractJvmRuntimeDescriptorLoaderTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuite(args) {
        testGroup("core/descriptors.runtime/tests-gen", "compiler/testData") {
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
