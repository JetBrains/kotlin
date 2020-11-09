/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.tests.generator.generateTestGroupSuite
import org.jetbrains.kotlin.jvm.runtime.AbstractJvm8RuntimeDescriptorLoaderTest
import org.jetbrains.kotlin.jvm.runtime.AbstractJvmRuntimeDescriptorLoaderTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuite(args) {
        testGroup("core/descriptors.runtime/tests", "compiler/testData") {
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
