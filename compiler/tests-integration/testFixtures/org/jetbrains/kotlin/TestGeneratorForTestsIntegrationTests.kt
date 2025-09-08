/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.codegen.AbstractIrCustomScriptCodegenTest
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.multiplatform.AbstractMultiPlatformIntegrationTest
import org.jetbrains.kotlin.repl.AbstractReplInterpreterTest
import org.jetbrains.kotlin.test.TargetBackend

fun main(args: Array<String>) {
    val mainClassName = TestGeneratorUtil.getMainClassName()

    generateTestGroupSuite(args, mainClassName) {
        testGroup("compiler/tests-integration/tests-gen", "compiler/testData") {
            testClass<AbstractMultiPlatformIntegrationTest> {
                model("multiplatform", extension = null, recursive = true, excludeParentDirs = true)
            }

            testClass<AbstractIrCustomScriptCodegenTest> {
                model("codegen/customScript", pattern = "^(.*)$", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractReplInterpreterTest> {
                model("repl", extension = "repl")
            }

            testClass<AbstractCliTest> {
                model("cli/jvm/readingConfigFromEnvironment", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/plugins", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/hmpp", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/apiVersion", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/argFileCommonChecks", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/diagnosticTests", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/diagnosticTests/crv", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/extraArgCommonChecks", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/internalArgCommonChecks", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/jdkHome", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/languageVersion", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/optIn", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/sourcesCommonChecks", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/XexplicitApi", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/XjdkRelease", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/XjspecifyAnnotation", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/Xjsr305", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/XnewInference", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/XsupressWarnings", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm/XXmultiPlatformProject", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/jvm", extension = "args", testMethod = "doJvmTest", recursive = false)
                model("cli/js", extension = "args", testMethod = "doJsTest", recursive = false)
                model("cli/wasm", extension = "args", testMethod = "doJsTest", recursive = false)
                model("cli/metadata", extension = "args", testMethod = "doMetadataTest", recursive = false)
            }
        }
    }
}
