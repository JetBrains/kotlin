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

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.jar.Manifest

class Java9ModulesIntegrationTest : AbstractKotlinCompilerIntegrationTest() {
    override val testDataPath: String
        get() = "compiler/testData/javaModules/"

    private fun module(
            name: String,
            modulePath: List<File> = emptyList(),
            addModules: List<String> = emptyList(),
            manifest: Manifest? = null
    ): File {
        val jdk9Home = KotlinTestUtils.getJdk9HomeIfPossible() ?: return File("<test-skipped>")

        val paths = (modulePath + ForTestCompileRuntime.runtimeJarForTests()).joinToString(separator = File.pathSeparator) { it.path }

        val kotlinOptions = mutableListOf(
                "-jdk-home", jdk9Home.path,
                "-jvm-target", "1.8",
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
                checkKotlinOutput = checkKotlinOutput(name),
                manifest = manifest
        )
    }

    private fun checkKotlinOutput(moduleName: String): (String) -> Unit = { actual ->
        KotlinTestUtils.assertEqualsToFile(File(testDataDirectory, "$moduleName.txt"), actual)
    }

    private fun createMultiReleaseJar(jdk9Home: File, destination: File, mainRoot: File, java9Root: File): File {
        val command = listOf<String>(
                File(jdk9Home, "bin/jar").path,
                "--create", "--file=$destination",
                "-C", mainRoot.path, ".",
                "--release", "9",
                "-C", java9Root.path, "."
        )

        val process = ProcessBuilder().command(command).inheritIO().start()
        process.waitFor()
        assertEquals("'jar' did not finish successfully", 0, process.exitValue())

        return destination
    }

    private fun java9BuildVersion(): Int? {
        val jdk9Home = KotlinTestUtils.getJdk9HomeIfPossible() ?: return null
        val process = ProcessBuilder().command(File(jdk9Home, "bin/java").path, "--version").start()
        val lines = process.inputStream.use {
            it.reader().readLines().also {
                process.waitFor()
            }
        }
        if (process.exitValue() != 0) return null
        val line = lines.getOrNull(1) ?: return null

        val result = ".*\\(build 9(-ea)?\\+(\\d+)\\)".toRegex().matchEntire(line)?.groupValues ?: return null
        return result[2].toIntOrNull()
    }

    // -------------------------------------------------------

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

    fun testMultiReleaseLibrary() {
        val jdk9Home = KotlinTestUtils.getJdk9HomeIfPossible() ?: return

        val librarySrc = FileUtil.findFilesByMask(JAVA_FILES, File(testDataDirectory, "library"))
        val libraryOut = File(tmpdir, "out")
        KotlinTestUtils.compileJavaFilesExternallyWithJava9(librarySrc, listOf("-d", libraryOut.path))

        val libraryOut9 = File(tmpdir, "out9")
        libraryOut9.mkdirs()
        File(libraryOut, "module-info.class").renameTo(File(libraryOut9, "module-info.class"))

        // Use the name other from 'library' to prevent it from being loaded as an automatic module if module-info.class is not found
        val libraryJar = createMultiReleaseJar(jdk9Home, File(tmpdir, "multi-release-library.jar"), libraryOut, libraryOut9)

        module("main", listOf(libraryJar))
    }

    fun testAutomaticModuleNames() {
        // Automatic module names are computed differently starting from some build of 9-ea
        // TODO: remove this as soon as Java 9 is released and installed on all TeamCity agents
        val version = java9BuildVersion()
        if (version == null || version < 176) {
            System.err.println("Java 9 build is not recognized or is too old (build $version), skipping the test")
            return
        }

        // This name should be sanitized to just "auto.mat1c.m0d.ule"
        val m1 = File(tmpdir, ".auto--mat1c-_-!@#\$%^&*()m0d_ule--1.0..0-release..jar")
        module("automatic-module1").renameTo(m1)

        val m2 = module("automatic-module2", manifest = Manifest().apply {
            mainAttributes.putValue("Manifest-Version", "1.0")
            mainAttributes.putValue("Automatic-Module-Name", "automodule2")
        })

        module("main", listOf(m1, m2))
    }

    fun testUnnamedAgainstSeveralAutomatic() {
        val a = module("autoA")
        val b = module("autoB")
        // Even though we only add autoA to the module graph, autoB should be added as well because autoA, being automatic,
        // transitively requires every other automatic module, and in particular, autoB.
        // Furthermore, because autoB is automatic, main should read autoB
        module("main", listOf(a, b), addModules = listOf("autoA"))
    }

    fun testNamedAgainstSeveralAutomatic() {
        val a = module("autoA")
        val b = module("autoB")
        module("main", listOf(a, b))
    }

    fun testSeveralModulesWithTheSameName() {
        val d1 = module("dependency1")
        val d2 = module("dependency2")
        module("main", listOf(d1, d2))
    }

    fun testDependencyOnStdlib() {
        module("unnamed")
        module("namedWithExplicitDependency")
        module("namedWithoutExplicitDependency")
    }

    fun testDependencyOnStdlibJdk78() {
        module("usage", listOf(File("dist/kotlinc/lib/kotlin-stdlib-jdk7.jar"), File("dist/kotlinc/lib/kotlin-stdlib-jdk8.jar")))
    }
}
