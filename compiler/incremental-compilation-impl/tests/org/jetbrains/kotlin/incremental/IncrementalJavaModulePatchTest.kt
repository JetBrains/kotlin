/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.incremental.utils.TestCompilationResult
import org.jetbrains.kotlin.test.compileJavaFiles
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The sources are members of a module patched with `-Xpatch-module` when one of the sources compiled is in a patch path, so an
 * incremental round that compiles only the Kotlin sources outside the patch path would compile them outside the patched module.
 */
class IncrementalJavaModulePatchTest : AbstractIncrementalJvmCompilerRunnerTest() {

    @Test
    fun testKotlinSourcesInsideAndOutsideThePatchPathAreCompiledNonIncrementally() {
        val patched = writeSource("patch/lib/internal/a.kt", "package lib.internal\n\nfun a(): String = Impl.impl()\n")
        val other = writeSource("other/b.kt", "package other\n\nfun b(): String = lib.internal.Impl.impl()\n")

        build()
        other.writeText("package other\n\nfun b(): String = lib.internal.Impl.impl() + \"b\"\n")
        assertEquals(setOf(patched.name, other.name), build())
    }

    @Test
    fun testRemovingTheLastKotlinSourceInThePatchPathRecompilesTheOthers() {
        val patched = writeSource("patch/lib/internal/a.kt", "package lib.internal\n\nfun a(): String = Impl.impl()\n")
        writeSource("other/b.kt", "package other\n\nfun b(): String = lib.internal.Impl.impl()\n")

        build()
        patched.delete()
        // As in a clean build, b.kt is now compiled in the unnamed module, and lib is not in the module graph.
        val result = make()
        assertCompileError(result, "Unresolved reference 'lib'", "b.kt compiled in the unnamed module has to be rejected")
    }

    @Test
    fun testAddingTheFirstJavaSourceInThePatchPathRecompilesTheKotlinSources() {
        writeSource("other/b.kt", "package other\n\nfun b(): Any = org.jetbrains.annotations.NotNull::class\n")

        build()
        writeSource("patch/lib/internal/Helper.java", "package lib.internal;\n\nclass Helper {}\n")
        // As in a clean build, b.kt is now compiled in lib, which does not read the unnamed module.
        val result = make()
        assertCompileError(result, "unnamed module which is not read", "b.kt compiled in lib has to be rejected")
    }

    @Test
    fun testAddingTheFirstUnlistedJavaSourceInThePatchPathRecompilesTheKotlinSources() {
        writeSource("other/b.kt", "package other\n\nfun b(): Any = org.jetbrains.annotations.NotNull::class\n")
        // A Java source root given as a free argument, so that the Java files in it are not listed as sources.
        val javaRoot = File(workingDir, "java").apply { mkdirs() }
        val patches = listOf("lib=" + javaRoot.path)
        val withJavaRoot: K2JVMCompilerArguments.() -> Unit = { freeArgs = listOf(javaRoot.path) }

        assertEquals(ExitCode.OK, make(patches, configure = withJavaRoot).exitCode)
        File(javaRoot, "lib/internal/Helper.java").apply {
            parentFile.mkdirs()
            writeText("package lib.internal;\n\nclass Helper {}\n")
        }
        // As in a clean build, b.kt is now compiled in lib, which does not read the unnamed module.
        val result = make(patches, configure = withJavaRoot)
        assertTrue(
            result.compileErrors.any { "unnamed module" in it },
            "b.kt compiled in lib has to be rejected: ${result.compileErrors}",
        )
    }

    @Test
    fun testAddingSourcesInThePatchPathOfAnotherModuleIsReported() {
        writeSource("patch/lib/internal/a.kt", "package lib.internal\n\nfun a(): String = Impl.impl()\n")
        val patches = listOf("lib=" + File(sourceRoot, "patch").path, "lib2=" + File(sourceRoot, "patch2").path)

        assertEquals(ExitCode.OK, make(patches).exitCode)
        writeSource("patch2/lib2/Other.java", "package lib2;\n\nclass Other {}\n")
        // As in a clean build, the sources are in the patch paths of two modules.
        val result = make(patches)
        assertCompileError(result, "Too many patched modules", "sources in the patch paths of lib and lib2 have to be rejected")
    }

    @Test
    fun testClassesInAPatchPathCauseARebuild() {
        val patched = writeSource("patch/lib/internal/a.kt", "package lib.internal\n\nfun a(): String = Impl.impl()\n")
        val other = writeSource("patch/other/b.kt", "package other\n\nfun b(): String = lib.internal.Impl.impl()\n")
        val patches = listOf("lib=" + File(sourceRoot, "patch").path + File.pathSeparator + libModule.path)

        assertEquals(ExitCode.OK, make(patches).exitCode)
        other.writeText("package other\n\nfun b(): String = lib.internal.Impl.impl() + \"b\"\n")
        assertEquals(setOf(patched.name, other.name), make(patches).compiledSources.mapTo(mutableSetOf(), File::getName))
    }

    @Test
    fun testDeletingTheLastClassInAPatchPathRecompilesTheKotlinSources() {
        val patchClasses = File(workingDir, "patch-classes")
        val extra = File(workingDir, "extra-src/lib/internal/Extra.java").apply {
            parentFile.mkdirs()
            writeText("package lib.internal; public class Extra { public static String extra() { return \"extra\"; } }")
        }
        compileJavaFiles(listOf(extra), listOf("-d", patchClasses.path)).assertSuccessful()
        writeSource("patch/lib/internal/a.kt", "package lib.internal\n\nfun a(): String = Impl.impl() + Extra.extra()\n")
        val patches = listOf("lib=" + File(sourceRoot, "patch").path + File.pathSeparator + patchClasses.path)

        assertEquals(ExitCode.OK, make(patches).exitCode)
        File(patchClasses, "lib/internal/Extra.class").delete()
        // As in a clean build, Extra is not found.
        val result = make(patches)
        assertCompileError(result, "Unresolved reference 'Extra'", "a.kt has to be compiled again without Extra")
    }

    @Test
    fun testAddingModuleInfoRecompilesTheKotlinSourcesInThePatchPath() {
        writeSource("patch/lib/internal/a.kt", "package lib.internal\n\nfun a(): String = Impl.impl()\n")
        writeSource("patch/other/b.kt", "package other\n\nfun b(): String = lib.internal.Impl.impl()\n")

        build()
        writeSource("module-info.java", "module tests { requires lib; requires kotlin.stdlib; }")
        // As in a clean build, the sources are now members of tests, which lib does not export lib.internal to. The exit code is not
        // checked, since the javac of the test fixture also fails on module-info.java.
        val result = make()
        assertTrue(
            result.compileErrors.any { "does not export package 'lib.internal'" in it },
            "a.kt and b.kt compiled in tests have to be rejected: ${result.compileErrors}",
        )
    }

    @Test
    fun testChangingOnlyAScriptInThePatchPathCompilesItInThePatchedModule() {
        writeSource("patch/lib/internal/a.kt", "package lib.internal\n\nfun a(): String = Impl.impl()\n")
        val script = writeSource("patch/lib/internal/b.kts", "val x = 1\n")

        assertEquals(ExitCode.OK, make(configure = { allowAnyScriptsInSourceRoots = true }).exitCode)
        script.writeText("val x = lib.internal.Impl.impl()\n")
        val result = make(configure = { allowAnyScriptsInSourceRoots = true })
        assertEquals(ExitCode.OK, result.exitCode, result.compileErrors.joinToString("\n"))
    }

    @Test
    fun testDeclaringAModuleInAnExistingModuleInfoRecompilesTheKotlinSourcesInThePatchPath() {
        writeSource("patch/lib/internal/a.kt", "package lib.internal\n\nfun a(): String = Impl.impl()\n")
        writeSource("patch/other/b.kt", "package other\n\nfun b(): String = lib.internal.Impl.impl()\n")
        val moduleInfo = writeSource("module-info.java", "// No module declaration\n")

        build()
        moduleInfo.writeText("module tests { requires lib; requires kotlin.stdlib; }")
        // As in a clean build, the sources are now members of tests, which lib does not export lib.internal to. The exit code is not
        // checked, since the javac of the test fixture also fails on module-info.java.
        val result = make()
        assertTrue(
            result.compileErrors.any { "does not export package 'lib.internal'" in it },
            "a.kt and b.kt compiled in tests have to be rejected: ${result.compileErrors}",
        )
    }

    @Test
    fun testKotlinSourcesInThePatchPathAreCompiledIncrementally() {
        writeSource("patch/lib/internal/a.kt", "package lib.internal\n\nfun a(): String = Impl.impl()\n")
        val other = writeSource("patch/other/b.kt", "package other\n\nfun b(): String = lib.internal.Impl.impl()\n")

        build()
        other.writeText("package other\n\nfun b(): String = lib.internal.Impl.impl() + \"b\"\n")
        assertEquals(setOf(other.name), build())
    }

    private fun assertCompileError(result: TestCompilationResult, expected: String, message: String) {
        assertTrue(result.compileErrors.any { expected in it }, "$message: ${result.compileErrors}")
    }

    private val sourceRoot: File
        get() = File(workingDir, "src")

    private fun writeSource(path: String, text: String): File =
        File(sourceRoot, path).apply {
            parentFile.mkdirs()
            writeText(text)
        }

    /** Compiles [sourceRoot] with `src/patch` as a patch path of `lib` and returns the names of the files compiled. */
    private fun build(): Set<String> {
        val result = make()
        assertEquals(ExitCode.OK, result.exitCode, result.compileErrors.joinToString("\n"))
        return result.compiledSources.mapTo(mutableSetOf(), File::getName)
    }

    private fun make(
        patches: List<String> = listOf("lib=" + File(sourceRoot, "patch").path),
        configure: K2JVMCompilerArguments.() -> Unit = {},
    ): TestCompilationResult {
        val outputDirectory = File(workingDir, "out").apply { mkdirs() }
        val arguments = createCompilerArguments(outputDirectory, workingDir).apply {
            classpath = KtTestUtil.getAnnotationsJar().path
            javaModulePath = listOf(libModule, ForTestCompileRuntime.runtimeJarForTests()).joinToString(File.pathSeparator)
            javaModulePatches = patches.toTypedArray()
            configure()
        }
        // The source roots are given with -Xjava-source-roots too.
        return make(File(workingDir, "caches"), outputDirectory, listOf(sourceRoot), arguments)
    }

    private val libModule: File by lazy {
        val sources = File(workingDir, "lib-src")
        val files = listOf(
            "module-info.java" to "module lib { requires kotlin.stdlib; }",
            "lib/internal/Impl.java" to "package lib.internal; public class Impl { public static String impl() { return \"impl\"; } }",
        ).map { source -> File(sources, source.first).apply { parentFile.mkdirs(); writeText(source.second) } }
        val destination = File(workingDir, "lib").apply { mkdirs() }
        val options = listOf("-d", destination.path, "--module-path", ForTestCompileRuntime.runtimeJarForTests().path)
        compileJavaFiles(files, options).assertSuccessful()
        destination
    }
}
