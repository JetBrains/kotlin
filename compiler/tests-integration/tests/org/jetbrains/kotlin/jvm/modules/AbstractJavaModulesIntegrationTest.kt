/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.modules

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.AbstractCliTest.getNormalizedCompilerOutput
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.jvm.compiler.AbstractKotlinCompilerIntegrationTest
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.jar.Manifest
import kotlin.test.fail

abstract class AbstractJavaModulesIntegrationTest(
    private val languageVersion: LanguageVersion,
) : AbstractKotlinCompilerIntegrationTest() {
    private val jdkHome: File
        get() = KtTestUtil.getJdk11Home()

    override val testDataPath: String
        get() = "compiler/testData/javaModules/"

    private fun module(
        name: String,
        modulePath: List<File> = emptyList(),
        addModules: List<String> = emptyList(),
        additionalKotlinArguments: List<String> = emptyList(),
        manifest: Manifest? = null,
        destination: File? = null,
        checkKotlinOutput: (String) -> Unit = this.checkKotlinOutput(name),
    ): File {
        val paths = (modulePath + ForTestCompileRuntime.runtimeJarForTests()).joinToString(separator = File.pathSeparator) { it.path }

        val kotlinOptions = mutableListOf(
            "-jdk-home", jdkHome.path,
            "-Xmodule-path=$paths",
            "-language-version", languageVersion.versionString,
        )
        if (addModules.isNotEmpty()) {
            kotlinOptions += "-Xadd-modules=${addModules.joinToString()}"
        }
        kotlinOptions += additionalKotlinArguments

        return compileLibrary(
            name,
            destination ?: File(tmpdir, "$name.jar"),
            kotlinOptions,
            compileJava = { _, javaFiles, outputDir ->
                val javaOptions = mutableListOf(
                    "-d", outputDir.path,
                    "--module-path", paths
                )
                if (addModules.isNotEmpty()) {
                    javaOptions += "--add-modules"
                    javaOptions += addModules.joinToString()
                }
                KotlinTestUtils.compileJavaFilesExternallyWithJava11(javaFiles, javaOptions)
            },
            checkKotlinOutput,
            manifest
        )
    }

    private fun checkKotlinOutput(moduleName: String): (String) -> Unit {
        val expectedFirFile = File(testDataDirectory, "$moduleName.fir.txt")
        return { actual ->
            KotlinTestUtils.assertEqualsToFile(
                if (languageVersion.usesK2 && expectedFirFile.exists()) expectedFirFile else File(testDataDirectory, "$moduleName.txt"),
                getNormalizedCompilerOutput(actual, null, testDataPath, tmpdir.absolutePath)
            )
        }
    }

    private data class ModuleRunResult(val stdout: String, val stderr: String)

    private fun runModule(className: String, modulePath: List<File>): ModuleRunResult {
        val command = listOf(
            File(jdkHome, "bin/java").path,
            "-p", (modulePath + ForTestCompileRuntime.runtimeJarForTests()).joinToString(File.pathSeparator, transform = File::getPath),
            "-m", className
        )

        val process = ProcessBuilder().command(command).start()
        process.waitFor(1, TimeUnit.MINUTES)
        return ModuleRunResult(
            process.inputStream.reader().readText().trimEnd(),
            process.errorStream.reader().readText().trimEnd()
        )
    }

    private fun createMultiReleaseJar(destination: File, mainRoot: File, version: Int, versionSpecificRoot: File): File {
        val command = listOf<String>(
            File(jdkHome, "bin/jar").path,
            "--create", "--file=$destination",
            "-C", mainRoot.path, ".",
            "--release", version.toString(),
            "-C", versionSpecificRoot.path, "."
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

    fun testAllModulePathAndNamedModule() {
        try {
            module("main", addModules = listOf("ALL-MODULE-PATH"))
        } catch (e: JavaCompilationError) {
            // Java compilation should fail, it's expected
        }
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

    fun testInheritedDeclarationFromTwiceTransitiveDependency() {
        // module A <-t- module B <-t- module C <--- module D

        // Java: class A { String ok() { /* ... */ } }
        val a = module("moduleA")

        // Java: class B extends A
        val b = module("moduleB", listOf(a))

        val c = module("moduleC", listOf(a, b))

        // Java: new B().ok()
        // Kotlin: B().ok()
        val d = module("moduleD", listOf(a, b, c))

        // validate the run-time behavior of Java-compiled code for the sake of comparison
        val (javaStdout, javaStderr) = runModule("moduleD/d.JavaMain", listOf(d, c, b, a))
        assertEquals("", javaStderr)
        assertEquals("OK", javaStdout)

        // test the run-time behavior of Kotlin-compiled code
        val (kotlinStdout, kotlinStderr) = runModule("moduleD/d.KotlinMainKt", listOf(d, c, b, a))
        assertEquals("", kotlinStderr)
        assertEquals("OK", kotlinStdout)
    }

    fun testSpecifyPathToModuleInfoInArguments() {
        val a = module("moduleA")

        val kotlinOptions = mutableListOf(
            "$testDataDirectory/someOtherDirectoryWithTheActualModuleInfo/module-info.java",
            "-jdk-home", jdkHome.path,
            "-Xmodule-path=${a.path}",
            "-language-version", languageVersion.versionString,
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
        KotlinTestUtils.compileJavaFilesExternallyWithJava11(librarySrc, listOf("-d", libraryOut.path))

        val libraryOut11 = File(tmpdir, "out11")
        libraryOut11.mkdirs()
        File(libraryOut, "module-info.class").renameTo(File(libraryOut11, "module-info.class"))

        for (version in listOf(9, 10, 11)) {
            try {
                // Use the name other from 'library' to prevent it from being loaded as an automatic module if module-info.class is not found.
                val libraryJar = createMultiReleaseJar(
                    File(tmpdir, "multi-release-library-jdk$version.jar"), libraryOut, version, libraryOut11
                )
                module("main", listOf(libraryJar))
            } catch (e: Throwable) {
                fail("Fail on testing that module-info.class is loaded from META-INF/versions/$version", e)
            }
        }
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
        val usage = module("usage")
        val (stdout, stderr) = runModule("usage/some.module.withsome.packages.UsageKt", listOf(usage))
        assertEquals("", stderr)
        assertEquals("usage/some.module.withsome.packages.Test", stdout)
    }

    fun testReflection() {
        val reflect = ForTestCompileRuntime.reflectJarForTests()
        val usage = module("usage", listOf(reflect))
        val (stdout, stderr) = runModule("usage/usage.test.UsageKt", listOf(usage, reflect))
        assertEquals("", stderr)
        assertEquals("OK", stdout)
    }

    fun testDoNotLoadIrrelevantJarsFromUnnamed() {
        // This test checks that we don't load irrelevant .jar files from the JDK distribution when resolving JDK dependencies.
        // Here we're testing that references to symbols from lib/ant-javafx are unresolved, if that file is present.
        // The test succeeds though even if the file is absent, because it's not guaranteed to be present in JDK.
        module("main", checkKotlinOutput = {
            assertTrue(it, it.trimEnd().endsWith("COMPILATION_ERROR"))
        })
    }

    fun testDoNotLoadIrrelevantJarsFromNamed() {
        // See the comment in testDoNotLoadIrrelevantJarsFromUnnamed.
        module("main", checkKotlinOutput = {
            assertTrue(it, it.trimEnd().endsWith("COMPILATION_ERROR"))
        })
    }

    fun testNoDependencyOnNamed() {
        // This is a test on the JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE diagnostic.
        val lib = module("lib")
        module("main", listOf(lib), listOf("lib"))
    }

    fun testNoDependencyOnUnnamed() {
        // This is a test on the JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE diagnostic.
        // Most of the other tests in this class are compiling modules to jars, however here we need to compile the module to a directory.
        // The reason is twofold:
        // 1) Currently the compiler adds all classpath entries as module path entries as long as we're compiling a named module (i.e. have
        //    `module-info.java` in the sources), see `configureSourceRoots` in `KotlinToJVMBytecodeCompiler.kt`. This is most likely
        //    incorrect.
        // 2) All jars found in the module path which are NOT named modules are loaded as automatic modules, see
        //    `ClasspathRootsResolver.modularBinaryRoot`. Combined with p.1, it means that _any_ jar in the dependencies will be loaded
        //    as either a named or an automatic module; there's no way to observe a jar that is a part of the unnamed module.
        // In this test we're checking the diagnostic about using symbols from unnamed modules, so we need to compile 'lib' to a directory.
        val lib = module("lib", destination = File(tmpdir, name))
        module("main", additionalKotlinArguments = listOf("-classpath", lib.path))
    }

    fun testNamedDoesNotReadAutomaticWithUnrelatedNamed() {
        // This test should result in an error because 'main' does not depend on 'lib' or any other automatic module.
        // But currently it's OK for compatibility, see KT-66622.
        val lib = module("lib")
        val unrelated = module("unrelated")
        module("main", listOf(lib, unrelated))
    }

    fun testNamedDoesNotReadAutomaticWithTransitiveStdlib() {
        // This test should result in an error because 'main' does not depend on 'lib' or any other automatic module.
        // But currently it's OK for compatibility, see KT-66622.
        val lib = module("lib")
        module("main", listOf(lib))
    }

    fun testNamedReadsAutomaticWithUnrelatedAutomatic() {
        // Similarly to how it works in javac, if we depend on one automatic module, we depend on all of them. So even though 'main' does
        // not have explicit "requires lib", in fact it depends on 'lib' because it has "requires unrelated". So "OK" is expected.
        val lib = module("lib")
        val unrelated = module("unrelated")
        module("main", listOf(lib, unrelated))
    }
}
