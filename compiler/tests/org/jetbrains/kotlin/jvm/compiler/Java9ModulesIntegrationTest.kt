/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.jar.Manifest

class Java9ModulesIntegrationTest : AbstractKotlinCompilerIntegrationTest() {
    override val testDataPath: String
        get() = "compiler/testData/javaModules/"

    private fun module(
            name: String,
            modulePath: List<File> = emptyList(),
            addModules: List<String> = emptyList(),
            additionalKotlinArguments: List<String> = emptyList(),
            manifest: Manifest? = null
    ): File {
        val paths = (modulePath + ForTestCompileRuntime.runtimeJarForTests()).joinToString(separator = File.pathSeparator) { it.path }

        val kotlinOptions = mutableListOf(
            "-jdk-home", KotlinTestUtils.getJdk9Home().path,
            "-jvm-target", "1.8",
            "-Xmodule-path=$paths"
        )
        if (addModules.isNotEmpty()) {
            kotlinOptions += "-Xadd-modules=${addModules.joinToString()}"
        }
        kotlinOptions += additionalKotlinArguments

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

        val kotlinOptions = mutableListOf(
            "$testDataDirectory/someOtherDirectoryWithTheActualModuleInfo/module-info.java",
            "-jdk-home", KotlinTestUtils.getJdk9Home().path,
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
        val librarySrc = FileUtil.findFilesByMask(JAVA_FILES, File(testDataDirectory, "library"))
        val libraryOut = File(tmpdir, "out")
        KotlinTestUtils.compileJavaFilesExternallyWithJava9(librarySrc, listOf("-d", libraryOut.path))

        val libraryOut9 = File(tmpdir, "out9")
        libraryOut9.mkdirs()
        File(libraryOut, "module-info.class").renameTo(File(libraryOut9, "module-info.class"))

        // Use the name other from 'library' to prevent it from being loaded as an automatic module if module-info.class is not found
        val libraryJar = createMultiReleaseJar(
            KotlinTestUtils.getJdk9Home(), File(tmpdir, "multi-release-library.jar"), libraryOut, libraryOut9
        )

        module("main", listOf(libraryJar))
    }

    fun testAutomaticModuleNames() {
        // This name should be sanitized to just "auto.mat1c.m0d.ule"
        val m1 = File(tmpdir, ".auto--mat1c-_-!@#\$%^&()m0d_ule--1.0..0-release..jar")
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
        val namedWithExplicitDependency = module("namedWithExplicitDependency")
        module("namedWithoutExplicitDependency")
        module("namedWithIndirectDependencyViaOtherModule", listOf(namedWithExplicitDependency))
        module("namedWithIndirectDependencyViaReflect", listOf(ForTestCompileRuntime.reflectJarForTests()))
    }

    fun testDependencyOnStdlibJdk78() {
        module("usage", listOf(File("dist/kotlinc/lib/kotlin-stdlib-jdk7.jar"), File("dist/kotlinc/lib/kotlin-stdlib-jdk8.jar")))
    }

    fun testDependencyOnReflect() {
        module("usage", listOf(ForTestCompileRuntime.reflectJarForTests()))
    }

    fun testWithBuildFile() {
        // This test checks that module path is configured correctly when the compiler is invoked in the '-Xbuild-file' mode. Note that
        // the "'-d' option is ignored" warning in this test is an artifact of the test infrastructure and is not a part of the test.
        val buildFile = AbstractCliTest.replacePathsInBuildXml(
            "-Xbuild-file=${File(testDataDirectory, "build.xml").path}",
            testDataDirectory.absolutePath,
            tmpdir.absolutePath
        )
        module("usage", additionalKotlinArguments = listOf("-no-stdlib", buildFile))
    }

    fun testCoroutinesDebugMetadata() {
        val jar = module("usage", listOf(ForTestCompileRuntime.runtimeJarForTests()))

        val command = listOf<String>(
            File(KotlinTestUtils.getJdk9Home(), "bin/java").path,
            "-p",
            "${ForTestCompileRuntime.runtimeJarForTests().path}${File.pathSeparator}${jar.path}",
            "-m",
            "usage/some.module.withsome.packages.UsageKt"
        )

        val process = ProcessBuilder().command(command).start()
        process.waitFor(1, TimeUnit.MINUTES)
        val got = process.inputStream.reader().readText()
        KotlinTestUtils.assertEqualsToFile(File("$testDataDirectory/stdout.txt"), got)
    }
}
