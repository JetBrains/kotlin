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

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

class Java9ModulesIntegrationTest : AbstractKotlinCompilerIntegrationTest() {
    override val testDataPath: String
        get() = "compiler/testData/javaModules/"

    private fun module(
            name: String,
            modulePath: List<File> = emptyList(),
            addModules: List<String> = emptyList()
    ): File {
        val jdk9Home = KotlinTestUtils.getJdk9HomeIfPossible() ?: return File("<test-skipped>")

        val paths = modulePath.joinToString(separator = File.pathSeparator) { it.path }

        val kotlinOptions = mutableListOf(
                "-jdk-home", jdk9Home.path,
                "-Xmodule-path=$paths"
        )
        if (addModules.isNotEmpty()) {
            kotlinOptions += "-Xadd-modules=${addModules.joinToString()}"
        }

        return compileLibrary(
                name,
                additionalOptions = kotlinOptions,
                compileJava = { _, javaFiles, outputDir ->
                    val javaOptions = mutableListOf(
                            "-d", outputDir.path,
                            "--module-path", paths
                    )
                    if (addModules.isNotEmpty()) {
                        javaOptions += "--add-modules"
                        javaOptions += addModules.joinToString()
                    }
                    KotlinTestUtils.compileJavaFilesExternallyWithJava9(javaFiles, javaOptions)
                },
                checkKotlinOutput = { actual ->
                    KotlinTestUtils.assertEqualsToFile(File(testDataDirectory, "$name.txt"), actual)
                }
        )
    }

    fun testSimple() {
        val a = module("moduleA")
        module("moduleB", listOf(a))
    }

    fun testSimpleUseNonExportedPackage() {
        val a = module("moduleA")
        module("moduleB", listOf(a))
    }

    fun testDependOnManyModules() {
        val a = module("moduleA")
        val b = module("moduleB")
        val c = module("moduleC")
        module("moduleD", listOf(a, b, c))
    }

    fun testUnnamedDependsOnNamed() {
        val a = module("moduleA")
        module("moduleB", listOf(a), listOf("moduleA"))

        // Also check that -Xadd-modules=ALL-MODULE-PATH has the same effect as -Xadd-module=moduleA, i.e. adds moduleA to the roots
        module("moduleB", listOf(a), listOf("ALL-MODULE-PATH"))
    }

    fun testAllModulePathAndNamedModule() {
        try {
            module("main", addModules = listOf("ALL-MODULE-PATH"))
        }
        catch (e: JavaCompilationError) {
            // Java compilation should fail, it's expected
        }
    }

    fun testJdkModulesFromNamed() {
        module("main")
    }

    fun testJdkModulesFromUnnamed() {
        module("main")
    }

    fun testUnnamedDoesNotReadNotAdded() {
        // Test that although we have moduleA in the module path, it's not in the module graph
        // because we did not provide -Xadd-modules=moduleA
        module("moduleB", listOf(module("moduleA")), addModules = emptyList())
    }
}
