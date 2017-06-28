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
                checkKotlinOutput = checkKotlinOutput(name)
        )
    }

    private fun checkKotlinOutput(moduleName: String): (String) -> Unit = { actual ->
        KotlinTestUtils.assertEqualsToFile(File(testDataDirectory, "$moduleName.txt"), actual)
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

    fun testNamedReadsTransitive() {
        val a = module("moduleA")
        val b = module("moduleB", listOf(a))
        module("moduleC", listOf(a, b))
    }

    fun testUnnamedReadsTransitive() {
        val a = module("moduleA")
        val b = module("moduleB", listOf(a))
        module("moduleC", listOf(a, b), addModules = listOf("moduleB"))
    }

    fun testNonTransitiveDoesNotAffectExplicitDependency() {
        // In this test, D depends on C (which requires B non-transitively) and on B; also B transitively requires A.
        // We check that if we depend on both C and B, we still transitively depend on A (via B).
        // This is a check against an incorrectly implemented DFS which, upon entering C, would write off B as "visited"
        // and not enter it later even though we explicitly depend on it in D's module-info
        val a = module("moduleA")
        val b = module("moduleB", listOf(a))
        val c = module("moduleC", listOf(a, b))
        module("moduleD", listOf(c, b, a))
    }

    fun testSpecifyPathToModuleInfoInArguments() {
        val a = module("moduleA")

        val jdk9Home = KotlinTestUtils.getJdk9HomeIfPossible() ?: return
        val kotlinOptions = mutableListOf(
                "$testDataDirectory/someOtherDirectoryWithTheActualModuleInfo/module-info.java",
                "-jdk-home", jdk9Home.path,
                "-Xmodule-path=${a.path}"
        )
        compileLibrary(
                "moduleB",
                additionalOptions = kotlinOptions,
                compileJava = { _, _, _ -> error("No .java files in moduleB in this test") },
                checkKotlinOutput = checkKotlinOutput("moduleB")
        )
    }
}
