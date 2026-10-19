/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.modules

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.AbstractCliTest.getNormalizedCompilerOutput
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.additionalJavaModuleReads
import org.jetbrains.kotlin.config.jdkHome
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithoutSource
import org.jetbrains.kotlin.jvm.compiler.AbstractKotlinCompilerIntegrationTest
import org.jetbrains.kotlin.test.JavaCompilationError
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestDataAssertions
import org.jetbrains.kotlin.test.compileJavaFiles
import org.jetbrains.kotlin.test.testFramework.runWithDisposable
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.jar.Manifest
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.test.fail

class ModulesIntegrationTest : AbstractKotlinCompilerIntegrationTest() {
    private val languageVersion: LanguageVersion = LanguageVersion.LATEST_STABLE

    private val jdkHome: File
        get() = KtTestUtil.getJdk11Home()

    override val testDataPath: String
        get() = ForTestCompileRuntime.transformTestDataPath("compiler/tests-integration/testData/javaModules/").absolutePath

    private fun module(
        name: String,
        modulePath: List<File> = emptyList(),
        addModules: List<String> = emptyList(),
        additionalKotlinArguments: List<String> = emptyList(),
        additionalJavaArguments: List<String> = emptyList(),
        manifest: Manifest? = null,
        destination: File? = null,
        checkKotlinOutput: (String) -> Unit = this.checkKotlinOutput(name),
    ): File {
        @Suppress("DEPRECATION")
        val paths = (modulePath + ForTestCompileRuntime.runtimeJarFromDistForTests())
            .joinToString(separator = File.pathSeparator) { it.path }

        val kotlinOptions = mutableListOf(
            K2JVMCompilerArguments::jdkHome.cliArgument, jdkHome.path,
            K2JVMCompilerArguments::javaModulePath.cliArgument(paths),
            K2JVMCompilerArguments::languageVersion.cliArgument, languageVersion.versionString,
            K2JVMCompilerArguments::suppressVersionWarnings.cliArgument,
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
                javaOptions += additionalJavaArguments
                compileJavaFiles(javaFiles, javaOptions, KtTestUtil.getJdk11Home())
            },
            checkKotlinOutput,
            manifest
        )
    }

    private fun checkKotlinOutput(moduleName: String): (String) -> Unit {
        val expectedFirFile = File(testDataDirectory, "$moduleName.fir.txt")
        return { actual ->
            TestDataAssertions.assertEqualsToFile(
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
        assertEquals(0, process.exitValue(), "'jar' did not finish successfully")

        return destination
    }

    // -------------------------------------------------------

    @Test
    fun testSimple() {
        val a = module("moduleA")
        module("moduleB", listOf(a))
    }

    @Test
    fun testAllModulePathAndNamedModule() {
        try {
            module("main", addModules = listOf("ALL-MODULE-PATH"))
        } catch (_: JavaCompilationError) {
            // Java compilation should fail, it's expected
        }
    }

    @Test
    fun testNamedReadsTransitive() {
        val a = module("moduleA")
        val b = module("moduleB", listOf(a))
        module("moduleC", listOf(a, b))
    }

    @Test
    fun testUnnamedReadsTransitive() {
        val a = module("moduleA")
        val b = module("moduleB", listOf(a))
        module("moduleC", listOf(a, b), addModules = listOf("moduleB"))
    }

    @Test
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
        (val javaStdout = stdout, val javaStderr = stderr) = runModule("moduleD/d.JavaMain", listOf(d, c, b, a))
        assertEquals("", javaStderr)
        assertEquals("OK", javaStdout)

        // test the run-time behavior of Kotlin-compiled code
        (val kotlinStdout = stdout, val kotlinStderr = stderr) = runModule("moduleD/d.KotlinMainKt", listOf(d, c, b, a))
        assertEquals("", kotlinStderr)
        assertEquals("OK", kotlinStdout)
    }

    @Test
    fun testSpecifyPathToModuleInfoInArguments() {
        val a = module("moduleA")

        val kotlinOptions = mutableListOf(
            "$testDataDirectory/someOtherDirectoryWithTheActualModuleInfo/module-info.java",
            K2JVMCompilerArguments::jdkHome.cliArgument, jdkHome.path,
            K2JVMCompilerArguments::javaModulePath.cliArgument(a.path),
            K2JVMCompilerArguments::languageVersion.cliArgument, languageVersion.versionString,
            K2JVMCompilerArguments::suppressVersionWarnings.cliArgument,
        )
        compileLibrary(
            "moduleB",
            additionalOptions = kotlinOptions,
            compileJava = { _, _, _ -> error("No .java files in moduleB in this test") },
            checkKotlinOutput = checkKotlinOutput("moduleB")
        )
    }

    @Test
    fun testMultiReleaseLibrary() {
        val librarySrc = FileUtil.findFilesByMask(JAVA_FILES, File(testDataDirectory, "library"))
        val libraryOut = File(tmpdir, "out")
        compileJavaFiles(librarySrc, listOf("-d", libraryOut.path), KtTestUtil.getJdk11Home()).assertSuccessful()

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

    @Test
    fun testAutomaticModuleNames() {
        // This name should be sanitized to just "auto.mat1c.m0d.ule"
        val m1 = File(tmpdir, ".auto--mat1c-_-!@#$%^&()m0d_ule--1.0..0-release..jar")
        module("automatic-module1").renameTo(m1)

        val m2 = module("automatic-module2", manifest = Manifest().apply {
            mainAttributes.putValue("Manifest-Version", "1.0")
            mainAttributes.putValue("Automatic-Module-Name", "automodule2")
        })

        module("main", listOf(m1, m2))
    }

    @Test
    fun testUnnamedAgainstSeveralAutomatic() {
        val a = module("autoA")
        val b = module("autoB")
        // Even though we only add autoA to the module graph, autoB should be added as well because autoA, being automatic,
        // transitively requires every other automatic module, and in particular, autoB.
        // Furthermore, because autoB is automatic, the main should read autoB
        module("main", listOf(a, b), addModules = listOf("autoA"))
    }

    @Test
    fun testNamedAgainstSeveralAutomatic() {
        val a = module("autoA")
        val b = module("autoB")
        module("main", listOf(a, b))
    }

    @Test
    fun testSeveralModulesWithTheSameName() {
        val d1 = module("dependency1")
        val d2 = module("dependency2")
        module("main", listOf(d1, d2))
    }

    @Suppress("DEPRECATION")
    @Test
    fun testDependencyOnStdlib() {
        module("unnamed")
        val namedWithExplicitDependency = module("namedWithExplicitDependency")
        module("namedWithoutExplicitDependency")
        module("namedWithIndirectDependencyViaOtherModule", listOf(namedWithExplicitDependency))
        module("namedWithIndirectDependencyViaReflect", listOf(ForTestCompileRuntime.reflectJarFromDistForTests()))
    }

    @Test
    fun testDependencyOnStdlibJdk78() {
        module("usage", listOf(File("dist/kotlinc/lib/kotlin-stdlib-jdk7.jar"), File("dist/kotlinc/lib/kotlin-stdlib-jdk8.jar")))
    }

    @Suppress("DEPRECATION")
    @Test
    fun testDependencyOnReflect() {
        module("usage", listOf(ForTestCompileRuntime.reflectJarFromDistForTests()))
    }

    @Test
    fun testWithBuildFile() {
        // This test checks that the module path is configured correctly when the compiler is invoked in the '-Xbuild-file' mode. Note that
        // the "'-d' option is ignored" warning in this test is an artifact of the test infrastructure and is not a part of the test.
        val buildFile = AbstractCliTest.replacePathsInBuildXml(
            "-Xbuild-file=${File(testDataDirectory, "build.xml").path}",
            testDataDirectory,
            tmpdir.absolutePath
        )
        module("usage", additionalKotlinArguments = listOf("-no-stdlib", buildFile))
    }

    @Test
    fun testCoroutinesDebugMetadata() {
        val usage = module("usage")
        val (stdout, stderr) = runModule("usage/some.module.withsome.packages.UsageKt", listOf(usage))
        assertEquals("", stderr)
        assertEquals("usage/some.module.withsome.packages.Test", stdout)
    }

    @Suppress("DEPRECATION")
    @Test
    fun testReflection() {
        val reflect = ForTestCompileRuntime.reflectJarFromDistForTests()
        val usage = module("usage", listOf(reflect))
        val (stdout, stderr) = runModule("usage/usage.test.UsageKt", listOf(usage, reflect))
        assertEquals("", stderr)
        assertEquals("OK", stdout)
    }

    @Test
    fun testDoNotLoadIrrelevantJarsFromUnnamed() {
        // This test checks that we don't load irrelevant .jar files from the JDK distribution when resolving JDK dependencies.
        // Here we're testing that references to symbols from lib/ant-javafx are unresolved if that file is present.
        // The test succeeds, though, even if the file is absent, because it's not guaranteed to be present in JDK.
        module("main", checkKotlinOutput = {
            assertTrue(it.trimEnd().endsWith("COMPILATION_ERROR"), it)
        })
    }

    @Test
    fun testDoNotLoadIrrelevantJarsFromNamed() {
        // See the comment in testDoNotLoadIrrelevantJarsFromUnnamed.
        module("main", checkKotlinOutput = {
            assertTrue(it.trimEnd().endsWith("COMPILATION_ERROR"), it)
        })
    }

    @Test
    fun testNoDependencyOnNamed() {
        // This is a test on the JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE diagnostic.
        val lib = module("lib")
        module("main", listOf(lib), listOf("lib"))
    }

    @Test
    fun testNoDependencyOnUnnamed() {
        // This is a test on the JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE diagnostic.
        // Most of the other tests in this class are compiling modules to jars; however, here we need to compile the module to a directory.
        // The reason is twofold:
        // 1) Currently, the compiler adds all classpath entries as module path entries as long as we're compiling a named module (i.e. have
        //    `module-info.java` in the sources), see `configureSourceRoots` in `KotlinToJVMBytecodeCompiler.kt`. This is most likely
        //    incorrect.
        // 2) All jars found in the module path which are NOT named modules are loaded as automatic modules, see
        //    `ClasspathRootsResolver.modularBinaryRoot`. Combined with p.1, it means that _any_ jar in the dependencies will be loaded
        //    as either a named or an automatic module; there's no way to observe a jar that is a part of the unnamed module.
        // In this test we're checking the diagnostic about using symbols from unnamed modules, so we need to compile 'lib' to a directory.
        val lib = module("lib", destination = File(tmpdir, testInfo.testMethod.get().name))
        module("main", additionalKotlinArguments = listOf("-classpath", lib.path))
    }

    @Test
    fun testNamedDoesNotReadAutomaticWithUnrelatedNamed() {
        // This test should result in an error because 'main' does not depend on 'lib' or any other automatic module.
        // But currently it's OK for compatibility, see KT-66622.
        val lib = module("lib")
        val unrelated = module("unrelated")
        module("main", listOf(lib, unrelated))
    }

    @Test
    fun testNamedDoesNotReadAutomaticWithTransitiveStdlib() {
        // This test should result in an error because 'main' does not depend on 'lib' or any other automatic module.
        // But currently it's OK for compatibility, see KT-66622.
        val lib = module("lib")
        module("main", listOf(lib))
    }

    @Test
    fun testNamedReadsAutomaticWithUnrelatedAutomatic() {
        // Similarly to how it works in javac, if we depend on one automatic module, we depend on all of them. So even though 'main' does
        // not have explicit "requires lib", in fact it depends on 'lib' because it has "requires unrelated". So "OK" is expected.
        val lib = module("lib")
        val unrelated = module("unrelated")
        module("main", listOf(lib, unrelated))
    }

    @Test
    fun testAddReadsUnnamed() {
        // The same sources as in testNoDependencyOnUnnamed, where 'lib' is compiled to a directory for the same reason.
        val lib = module("lib", destination = File(tmpdir, "lib"))
        module("main", additionalKotlinArguments = listOf("-classpath", lib.path, "-Xadd-reads=main=ALL-UNNAMED"))
    }

    @Test
    fun testAddReadsUnnamedJar() {
        // Like javac, a jar given with -classpath is in the unnamed module, although the compiler also reads it as an automatic module
        // (see testNoDependencyOnUnnamed). With -no-stdlib, that automatic module is not in the module graph.
        val lib = module("lib")
        val arguments = listOf("-classpath", lib.path, "-Xadd-reads=main=ALL-UNNAMED")
        module("main", additionalKotlinArguments = arguments)
        module("main", additionalKotlinArguments = arguments + "-no-stdlib")
        // Like javac, a jar given only with -classpath is not a module that -Xadd-reads can read, directly or through an automatic module.
        module("main", additionalKotlinArguments = listOf("-classpath", lib.path, "-Xadd-reads=main=lib")) { output ->
            assertTrue("module name in '-Xadd-reads' not found: 'lib'" in output && "does not depend on" in output, output)
        }
        val autoA = module("autoA")
        module("main", listOf(autoA), listOf("autoA"), listOf("-classpath", lib.path, "-Xadd-reads=main=autoA")) { output ->
            assertTrue("symbol is declared in module 'lib', which the current module does not depend on" in output, output)
        }
    }

    @Test
    fun testAddReadsUnnamedResolved() {
        // Like javac, a module made to read the unnamed module also reads every module in the module graph, as the unnamed module does.
        val lib = module("lib")
        module("main", listOf(lib), listOf("lib"), listOf("-Xadd-reads=main=ALL-UNNAMED"))
        // Like javac, 'lib' is a module when given with -Xmodule-path, also with -classpath, or with a jar of the same module name on the
        // classpath, so it does not export 'lib.internal' to the unnamed module.
        val other = module("other", destination = File(tmpdir, "classpath").apply { mkdirs() }.resolve("lib.jar"))
        for (classpath in listOf(lib, other)) {
            module("main2", listOf(lib), listOf("lib"), listOf("-classpath", classpath.path, "-Xadd-reads=main2=ALL-UNNAMED")) { output ->
                assertTrue("does not export package 'lib.internal'" in output, output)
            }
        }
        // Like javac, kotlin.stdlib is read through ALL-UNNAMED only when it is in the module graph.
        module("main3", additionalKotlinArguments = listOf("-Xadd-reads=main3=ALL-UNNAMED"))
        module("main3", additionalKotlinArguments = listOf("-Xadd-reads=main3=ALL-UNNAMED", "-no-stdlib")) { output ->
            assertTrue("standard library is not found in the module graph" in output, output)
        }
    }

    @Test
    fun testAddReadsNamed() {
        // The same sources as in testNoDependencyOnNamed.
        val lib = module("lib")
        module("main", listOf(lib), listOf("lib"), additionalKotlinArguments = listOf("-Xadd-reads=main=lib"))
    }

    @Test
    fun testAddReadsNamedTransitive() {
        // Like javac, reading 'lib' also reads 'base', which 'lib' requires transitively.
        val base = module("base")
        val lib = module("lib", listOf(base))
        module("main", listOf(lib, base), listOf("lib"), additionalKotlinArguments = listOf("-Xadd-reads=main=lib"))
    }

    @Test
    fun testAddExportsNamed() {
        val lib = module("lib")
        val doesNotExport: (String) -> Unit = {
            assertTrue(it.contains("does not export") && it.trimEnd().endsWith("COMPILATION_ERROR"), it)
        }
        module("main", listOf(lib), checkKotlinOutput = doesNotExport)
        module(
            "main", listOf(lib),
            additionalKotlinArguments = listOf("-Xadd-exports=lib/foo.internal=ALL-UNNAMED"),
            checkKotlinOutput = doesNotExport,
        )
        // With the PSI-based frontend, the module of the file being checked is unknown, so ALL-UNNAMED must not be applied either.
        module(
            "main", listOf(lib),
            additionalKotlinArguments = listOf("-Xuse-fir-lt=false", "-Xadd-exports=lib/foo.internal=ALL-UNNAMED"),
            checkKotlinOutput = doesNotExport,
        )
        module("main", listOf(lib), additionalKotlinArguments = listOf("-Xadd-exports=lib/foo.internal=main"))
        // Like javac, targets are applied split on spaces too, even though 'other main' is reported as a bad name.
        module(
            "main", listOf(lib),
            additionalKotlinArguments = listOf("-Xadd-exports=lib/foo.internal=other main"),
            checkKotlinOutput = { assertTrue(it.contains("'other main'") && !it.contains("error:"), it) },
        )
    }

    @Test
    fun testAddExportsUnnamed() {
        val lib = module("lib")
        module("main", listOf(lib), listOf("lib"), additionalKotlinArguments = listOf("-Xadd-exports=lib/foo.internal=ALL-UNNAMED"))
    }

    @Test
    fun testAddReadsAndExportsInvalidValues() {
        // Values that javac rejects with "bad value for --add-reads option" or "bad value for --add-exports option".
        module(
            "main",
            additionalKotlinArguments = listOf(
                "-Xadd-reads=main", "-Xadd-reads==lib", "-Xadd-reads=main=",
                "-Xadd-exports=lib=main", "-Xadd-exports=lib/foo=", "-Xadd-exports=lib/=main",
            ),
        )
    }

    @Test
    fun testAddReadsAndExportsBadNames() {
        // Like javac, invalid names are skipped with a warning, empty ones silently, and modules not in the module graph are reported.
        module(
            "main",
            additionalKotlinArguments = listOf(
                "-Xadd-reads=main=lib=other", "-Xadd-reads=1bad=lib", "-Xadd-reads=ALL-UNNAMED=lib", "-Xadd-reads=main=int,,lib",
                "-Xadd-reads=main=lib other", "-Xadd-exports=lib/foo=main/bar", "-Xadd-exports=lib/foo/bar=main",
                "-Xadd-exports=lib/foo=,int", "-Xadd-exports=1bad/foo=2bad",
            ),
        )
    }

    @Test
    fun testAddExportsFromJdkModule() {
        val doesNotExport: (String) -> Unit = {
            assertTrue(it.contains("does not export") && it.trimEnd().endsWith("COMPILATION_ERROR"), it)
        }
        module("main", checkKotlinOutput = doesNotExport)
        module("main", additionalKotlinArguments = listOf("-Xadd-exports=java.base/sun.nio.ch=ALL-UNNAMED"))
    }

    @Test
    fun testAddReadsStdlib() {
        // The same check as in testDependencyOnStdlib for 'namedWithoutExplicitDependency', where the read is added on the command line.
        module("main", additionalKotlinArguments = listOf("-Xadd-reads=main=kotlin.stdlib"))
    }

    @Test
    fun testAddReadsUnresolved() {
        // Like javac, a module that is not in the module graph is reported and ignored, so 'main' does not read 'visible' through it.
        val visible = module("visible")
        val hidden = module("hidden", listOf(visible))
        module("main", listOf(visible, hidden), listOf("visible"), additionalKotlinArguments = listOf("-Xadd-reads=main=hidden"))
    }

    @Test
    fun testAddReadsAutomatic() {
        // Like javac, reading one automatic module also reads the others, as in testNamedReadsAutomaticWithUnrelatedAutomatic.
        val autoA = module("autoA")
        val autoB = module("autoB")
        module("main", listOf(autoA, autoB), listOf("autoA"), additionalKotlinArguments = listOf("-Xadd-reads=main=autoA"))
    }

    @Test
    fun testAddReadsAndExportsWithJdkRelease() {
        module(
            "main",
            additionalKotlinArguments = listOf(
                "-Xjdk-release=11", "-Xadd-reads=java.base=ALL-UNNAMED", "-Xadd-exports=java.base/sun.nio.ch=ALL-UNNAMED",
            ),
        )
    }

    @Test
    fun testAddReadsOnClasspathUpdate() = runWithDisposable { disposable ->
        // Roots added later, as for a script with dependencies, are resolved without the source module, which must not be reported
        // as missing from the module graph.
        val configuration = KotlinTestUtils.newConfiguration().apply {
            jdkHome = this@ModulesIntegrationTest.jdkHome
            addJavaSourceRoot(File(testDataDirectory, "main"))
            additionalJavaModuleReads = listOf("main=ALL-UNNAMED")
        }

        @OptIn(CoreEnvironmentDeprecation::class)
        val environment = KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        environment.updateClasspath(listOf(JvmClasspathRoot(ForTestCompileRuntime.runtimeJarForTests())))

        val warnings = configuration.diagnosticsCollector.diagnostics
            .filter { it.factory == CliDiagnostics.CLASSPATH_RESOLUTION_WARNING }
            .map { (it as KtDiagnosticWithoutSource).message }
        assertEquals(emptyList<String>(), warnings)
    }

    private fun patchModule(moduleName: String, vararg paths: String): String =
        "-Xpatch-module=$moduleName=" + paths.joinToString(File.pathSeparator) { File(testDataDirectory, it).path }

    private fun assertCompilationError(vararg expected: String): (String) -> Unit = { output ->
        assertTrue(expected.all(output::contains) && output.trimEnd().endsWith("COMPILATION_ERROR"), output)
    }

    @Test
    fun testPatchModuleSources() {
        // Like javac, sources in a patch path are members of the patched module, which is then the root module.
        val lib = module("lib")
        val javacPatch = listOf("--patch-module", "lib=" + File(testDataDirectory, "test").path)
        module(
            "test", listOf(lib), listOf("lib"), additionalJavaArguments = javacPatch,
            checkKotlinOutput = assertCompilationError("does not export package 'lib.internal'"),
        )
        for (javaDirect in listOf(true, false)) {
            module(
                "test", listOf(lib), additionalKotlinArguments = listOf(patchModule("lib", "test"), "-Xjava-direct=$javaDirect"),
                additionalJavaArguments = javacPatch,
            )
        }
    }

    @Test
    fun testPatchModuleReadsUnnamed() {
        // Like javac, the patched module reads only the modules it requires, so -Xadd-reads is needed for sources that use the classpath.
        val lib = module("lib")
        val assertions = module("assertions", destination = File(tmpdir, "assertions"))
        val arguments = listOf("-classpath", assertions.path, patchModule("lib", "test"))
        module("test", listOf(lib), additionalKotlinArguments = arguments, checkKotlinOutput = assertCompilationError("unnamed module"))
        module("test", listOf(lib), additionalKotlinArguments = arguments + "-Xadd-reads=lib=ALL-UNNAMED")
    }

    @Test
    fun testPatchModuleBinary() {
        // Like javac, classes in a patch path are members of the patched module, so a package added by the patch is not exported.
        val lib = module("lib")
        val extra = module("extra", destination = File(tmpdir, "extra"))
        val patch = "-Xpatch-module=lib=${extra.path}"
        module(
            "main", listOf(lib), listOf("lib"), listOf(patch),
            checkKotlinOutput = assertCompilationError("does not export package 'lib.extra'"),
        )
        module("main", listOf(lib), listOf("lib"), listOf(patch, "-Xadd-exports=lib/lib.extra=ALL-UNNAMED"))
    }

    @Test
    fun testPatchModuleOutsideGraph() {
        // Like javac, a patch of a module outside the module graph has no effect, so the classes in the patch path stay in the unnamed
        // module.
        val lib = module("lib")
        val extra = module("extra", destination = File(tmpdir, "extra"))
        module("main", listOf(lib), additionalKotlinArguments = listOf("-classpath", extra.path, "-Xpatch-module=lib=${extra.path}"))
    }

    @Test
    fun testPatchModuleAutomatic() {
        // Like javac, an automatic module can be patched, and its sources read the unnamed module without -Xadd-reads.
        val auto = module("auto")
        val assertions = module("assertions", destination = File(tmpdir, "assertions"))
        module("test", listOf(auto), additionalKotlinArguments = listOf("-classpath", assertions.path, patchModule("auto", "test")))
        // Unlike for an explicit module, ALL-MODULE-PATH is allowed, so 'lib' is in the module graph.
        val lib = module("lib")
        module(
            "test2", listOf(auto, lib), additionalKotlinArguments = listOf(patchModule("auto", "test2")),
            checkKotlinOutput = assertCompilationError(),
        )
        module("test2", listOf(auto, lib), listOf("ALL-MODULE-PATH"), listOf(patchModule("auto", "test2")))
    }

    @Test
    fun testPatchModuleSourceDirectory() {
        // A directory source root is compiled recursively, so a patch path inside it contains sources. Like javac, sources outside the
        // patch path are then members of the patched module too, and sources in the patch paths of two modules are an error.
        val lib = module("lib")
        val other = module("other")
        module("test", listOf(lib, other), additionalKotlinArguments = listOf(patchModule("lib", "test/lib")))
        module(
            "test", listOf(lib, other),
            additionalKotlinArguments = listOf(patchModule("lib", "test/lib"), patchModule("other", "test/other")),
            checkKotlinOutput = assertCompilationError("too many patched modules: 'lib', 'other'"),
        )
    }

    @Test
    fun testPatchModuleNotFound() {
        // Like javac, sources in a patch path of a module that is not found, or is only on the classpath, are an error.
        val lib = module("lib")
        module("test", additionalKotlinArguments = listOf(patchModule("nosuch", "test")))
        module("test2", additionalKotlinArguments = listOf("-classpath", lib.path, patchModule("lib", "test2")))
        // Module handling is skipped with -Xjdk-release below 9, the patches included.
        module("test", additionalKotlinArguments = listOf("-Xjdk-release=8", patchModule("nosuch", "test"), patchModule("other", "test"))) {
            assertEquals("OK", it.trim())
        }
    }

    @Test
    fun testPatchModuleInvalidValues() {
        // Values that javac rejects with "bad value for --patch-module option", and a module patched twice. An empty source root in the
        // patch path of a module that is not found contains no sources, so it is not reported.
        val empty = File(tmpdir, "empty").apply { mkdirs() }
        module(
            "main",
            additionalKotlinArguments = listOf(
                "-Xpatch-module=lib", "-Xpatch-module==lib", "-Xpatch-module=lib=", "-Xpatch-module=lib/foo=dir",
                "-Xpatch-module=lib=a", "-Xpatch-module=lib=b",
                "-Xjava-source-roots=${empty.path}", "-Xpatch-module=nosuch=${empty.path}",
            ),
        )
    }

    @Test
    fun testPatchModuleIncremental() {
        // As in incremental compilation, the output directory is on the classpath, and the classes compiled into it earlier are members
        // of the patched module too.
        val lib = module("lib")
        val output = File(tmpdir, "output")
        module("test", listOf(lib), additionalKotlinArguments = listOf(patchModule("lib", "test")), destination = output)
        module(
            "test2", listOf(lib), additionalKotlinArguments = listOf("-classpath", output.path, patchModule("lib", "test2")),
            destination = output,
        )
    }

    private fun compileJdkPatch(): File {
        // Compiled with javac as a member of java.base.
        val sources = File(testDataDirectory, "jdkPatch")
        val output = File(tmpdir, "jdkPatch")
        compileJavaFiles(
            listOf(File(sources, "java/lang/extra/Extra.java")),
            listOf("-d", output.path, "--patch-module", "java.base=${sources.path}"),
            jdkHome,
        ).assertSuccessful()
        return output
    }

    @Test
    fun testPatchModuleJdkBinary() {
        // Like javac, a class added to a JDK module by a binary patch is a member of that module, with and without -Xjdk-release.
        val patch = "-Xpatch-module=java.base=${compileJdkPatch().path}"
        val doesNotExport = assertCompilationError("does not export package 'java.lang.extra'")
        module("main", additionalKotlinArguments = listOf(patch), checkKotlinOutput = doesNotExport)
        module("main", additionalKotlinArguments = listOf(patch, "-Xadd-exports=java.base/java.lang.extra=ALL-UNNAMED"))
        module("main", additionalKotlinArguments = listOf(patch, "-Xjdk-release=10"), checkKotlinOutput = doesNotExport)
    }

    @Test
    fun testPatchModuleJdkSources() {
        // Like javac, sources in a patch path of a JDK module are members of it and use its packages that are not exported.
        module("test", checkKotlinOutput = assertCompilationError("does not export package 'jdk.internal.misc'"))
        module(
            "test", additionalKotlinArguments = listOf(patchModule("java.base", "test")),
            checkKotlinOutput = assertCompilationError("add '-Xadd-reads=java.base=kotlin.stdlib'"),
        )
        module("test", additionalKotlinArguments = listOf(patchModule("java.base", "test"), "-Xadd-reads=java.base=kotlin.stdlib"))
    }

    @Test
    fun testPatchModuleSymlink() {
        assumeFalse(isWindows, "Symlinks do not work on Windows")
        // Like javac, a source file counts as being in a patch path when its real path is, whatever path it is compiled through.
        val lib = module("lib")
        val linked = File(testDataDirectory, "linked")
        val sources = File(tmpdir, "sources").apply { mkdirs() }
        File(sources, "link").toPath().createSymbolicLinkPointingTo(linked.toPath())
        module("main", listOf(lib), additionalKotlinArguments = listOf(sources.path, patchModule("lib", "linked")))
        val patch = File(tmpdir, "patch").apply { mkdirs() }
        File(patch, "link").toPath().createSymbolicLinkPointingTo(linked.toPath())
        module(
            "main", listOf(lib), additionalKotlinArguments = listOf(patch.path, "-Xpatch-module=lib=${patch.path}"),
            checkKotlinOutput = assertCompilationError("unresolved reference 'Impl'"),
        )
    }

    @Test
    fun testPatchModuleClasspathOnly() {
        // Like javac, which puts a jar given with -classpath in the unnamed module, a patch of the module read from it has no effect.
        val foo = module("foo")
        val extra = module("extra", destination = File(tmpdir, "extra"))
        val arguments = listOf("-classpath", foo.path, "-Xpatch-module=foo=${extra.path}", "-Xadd-reads=main=ALL-UNNAMED")
        module("main", additionalKotlinArguments = arguments, checkKotlinOutput = assertCompilationError("unresolved reference 'foo2'"))
    }

    @Test
    fun testPatchModuleReplacedClass() {
        // Like javac, a class in a patch path replaces the class of the module, also when the module is given with -classpath too.
        val lib = module("lib")
        val sources = File(testDataDirectory, "patch")
        val patch = File(tmpdir, "patch")
        compileJavaFiles(
            listOf(File(sources, "p/C.java")),
            listOf("-d", patch.path, "--patch-module", "lib=${sources.path}", "--module-path", lib.path),
            jdkHome,
        ).assertSuccessful()
        for (arguments in listOf(emptyList(), listOf("-classpath", lib.path))) {
            module("main", listOf(lib), additionalKotlinArguments = arguments + "-Xpatch-module=lib=${patch.path}")
        }
    }

    @Test
    fun testPatchModuleSourceOverPatchClass() {
        // Like javac, a Java source being compiled takes precedence over a class of the same name in a patch path.
        val lib = module("lib")
        val sources = File(testDataDirectory, "patch")
        val patch = File(tmpdir, "patch")

        @Suppress("DEPRECATION")
        val modulePath = lib.path + File.pathSeparator + ForTestCompileRuntime.runtimeJarFromDistForTests().path
        compileJavaFiles(
            listOf(File(sources, "p/C.java")),
            listOf("-d", patch.path, "--patch-module", "lib=${sources.path}", "--module-path", modulePath),
            jdkHome,
        ).assertSuccessful()
        val paths = File(testDataDirectory, "test").path + File.pathSeparator + patch.path
        module(
            "test", listOf(lib), additionalKotlinArguments = listOf("-Xpatch-module=lib=$paths"),
            additionalJavaArguments = listOf("--patch-module", "lib=$paths"),
        )
    }

    @Test
    fun testPatchModuleSourceFile() {
        // Like javac, a source file given as the patch path is not in it, since a patch path is a directory or a jar.
        val lib = module("lib")
        module(
            "test", listOf(lib), additionalKotlinArguments = listOf(patchModule("lib", "test/lib/internal/implTest.kt")),
            checkKotlinOutput = assertCompilationError("unresolved reference 'Impl'"),
        )
    }
}
