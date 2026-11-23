/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.tools.ToolProvider
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.io.path.writeText

/** The `-Xuse-nio-jvm-dependencies-index` switch: compilation results are identical with it on and off. */
class NioRootEntriesCompilationTest : JvmDependenciesIndexContractTestBase() {
    private data class CompilationResult(val outDir: Path, val exitCode: ExitCode, val messages: String, val classFiles: Set<String>)

    /** Compiles [sources] with the NIO index on and off, each into its own output directory, and asserts the runs are identical. */
    private fun assertCompilesTheSameInBothModes(
        name: String,
        sources: List<Path>,
        expectedExitCode: ExitCode = ExitCode.OK,
        classpath: String? = null,
        extraArgs: List<String> = emptyList(),
    ): Pair<CompilationResult, CompilationResult> {
        val nio = compile("$name-nio", "-Xuse-nio-jvm-dependencies-index", sources, classpath, extraArgs)
        val vfs = compile("$name-vfs", "-Xuse-nio-jvm-dependencies-index=false", sources, classpath, extraArgs)
        assertEquals(vfs.exitCode, nio.exitCode, "exit codes differ between NIO and VFS modes")
        assertEquals(expectedExitCode, nio.exitCode) { "unexpected exit code in NIO mode:\n${nio.messages}" }
        assertEquals(vfs.messages, nio.messages, "compiler messages differ between NIO and VFS modes")
        assertEquals(vfs.classFiles, nio.classFiles, "produced class files differ between NIO and VFS modes")
        return nio to vfs
    }

    private fun compile(
        outDirName: String,
        nioFlag: String,
        sources: List<Path>,
        classpath: String?,
        extraArgs: List<String>,
    ): CompilationResult {
        val outDir = tempDir.resolve(outDirName)
        val args = buildList {
            sources.forEach { add(it.toString()) }
            if (classpath != null) {
                add("-cp")
                add(classpath)
            }
            add("-d")
            add(outDir.toString())
            addAll(extraArgs)
            add(nioFlag)
        }
        val [output, exitCode] = CompilerTestUtil.executeCompiler(K2JVMCompiler(), args)
        val messages = output.replace(outDir.toString(), "\$OUT").replace(tempDir.toString(), "\$TMP")
        val classFiles =
            if (Files.isDirectory(outDir)) {
                Files.walk(outDir).use { paths ->
                    paths.filter { it.toString().endsWith(".class") }
                        .map { outDir.relativize(it).toString() }
                        .collect(Collectors.toSet())
                }
            } else {
                emptySet()
            }
        return CompilationResult(outDir, exitCode, messages, classFiles)
    }

    @Test
    fun compilesAgainstJarLibrary() {
        val libOut = tempDir.resolve("libOut").createDirectories()
        val libKt = tempDir.resolve("lib.kt")
        libKt.writeText(
            """
            package lib

            class Box(val v: Int)

            fun topLevel() = 1

            inline fun twice(f: () -> Int) = f() + f()
            """.trimIndent()
        )
        CompilerTestUtil.executeCompilerAssertSuccessful(K2JVMCompiler(), listOf(libKt.toString(), "-d", libOut.toString()))

        val javaSource = tempDir.resolve("J.java")
        javaSource.writeText("package lib; public class J { public static int answer() { return 42; } }")
        javac("-d", libOut.toString(), javaSource.toString())

        val libJar = zipDir(libOut, tempDir.resolve("lib.jar"))

        val main = tempDir.resolve("main.kt")
        main.writeText(
            """
            import lib.Box
            import lib.J
            import lib.topLevel
            import lib.twice

            fun compute(): Int = Box(2).v + topLevel() + twice { 3 } + J.answer()
            """.trimIndent()
        )
        val nio = assertCompilesTheSameInBothModes("user", listOf(main), classpath = libJar.toString()).first

        val urls = arrayOf(nio.outDir.toUri().toURL(), libJar.toUri().toURL())
        URLClassLoader(urls, Thread.currentThread().contextClassLoader).use { loader ->
            assertEquals(51, Class.forName("MainKt", true, loader).getDeclaredMethod("compute").invoke(null))
        }
    }

    @Test
    fun firstJarWinsOnClasspath() {
        val dirA = tempDir.resolve("dirA").createDirectories()
        val dirB = tempDir.resolve("dirB").createDirectories()
        val cWithA = tempDir.resolve("CWithA.kt")
        cWithA.writeText("package p\n\nclass C { fun a() = 1 }\n")
        val cWithB = tempDir.resolve("CWithB.kt")
        cWithB.writeText("package p\n\nclass C { fun b() = 2 }\n")
        CompilerTestUtil.executeCompilerAssertSuccessful(K2JVMCompiler(), listOf(cWithA.toString(), "-d", dirA.toString()))
        CompilerTestUtil.executeCompilerAssertSuccessful(K2JVMCompiler(), listOf(cWithB.toString(), "-d", dirB.toString()))
        val firstJar = zipDir(dirA, tempDir.resolve("first.jar"))
        val secondJar = zipDir(dirB, tempDir.resolve("second.jar"))

        val usesA = tempDir.resolve("usesA.kt")
        usesA.writeText("package p\n\nfun call(): Int = C().a()\n")
        val usesB = tempDir.resolve("usesB.kt")
        usesB.writeText("package p\n\nfun call(): Int = C().b()\n")

        val jarThenJar = listOf(firstJar, secondJar).joinToString(File.pathSeparator) { it.toString() }
        val dirThenJar = listOf(dirA, secondJar).joinToString(File.pathSeparator) { it.toString() }
        assertCompilesTheSameInBothModes("jar-jar-ok", listOf(usesA), classpath = jarThenJar)
        assertCompilesTheSameInBothModes(
            "jar-jar-bad", listOf(usesB), expectedExitCode = ExitCode.COMPILATION_ERROR, classpath = jarThenJar
        )
        assertCompilesTheSameInBothModes("dir-jar-ok", listOf(usesA), classpath = dirThenJar)
        assertCompilesTheSameInBothModes(
            "dir-jar-bad", listOf(usesB), expectedExitCode = ExitCode.COMPILATION_ERROR, classpath = dirThenJar
        )
    }

    @Test
    fun jdkRoots() {
        assumeJdk11OrNewer()

        val httpClient = tempDir.resolve("httpClient.kt")
        httpClient.writeText("package jdk\n\nimport java.net.http.HttpClient\n\nfun client(): HttpClient = HttpClient.newHttpClient()\n")
        val optional = tempDir.resolve("optional.kt")
        optional.writeText(
            "package jdk\n\nimport java.util.Base64\nimport java.util.Optional\n\nfun opt(): Optional<Int> = Optional.of(1)\n\n" +
                    "fun encode(bytes: ByteArray): ByteArray = Base64.getEncoder().encode(bytes)\n"
        )

        assertCompilesTheSameInBothModes("jrt", listOf(httpClient))
        assertCompilesTheSameInBothModes(
            "ct-sym-8-http", listOf(httpClient), expectedExitCode = ExitCode.COMPILATION_ERROR, extraArgs = listOf("-Xjdk-release=8")
        )
        assertCompilesTheSameInBothModes("ct-sym-8-optional", listOf(optional), extraArgs = listOf("-Xjdk-release=8"))
        assertCompilesTheSameInBothModes("ct-sym-11", listOf(httpClient), extraArgs = listOf("-Xjdk-release=11"))
    }

    @Test
    fun javaModules() {
        assumeJdk11OrNewer()

        val libOut = tempDir.resolve("libModuleOut").createDirectories()
        val libModuleInfo = tempDir.resolve("libModule/module-info.java").apply { parent.createDirectories() }
        libModuleInfo.writeText("module lib {\n    exports lib.api;\n}\n")
        val api = tempDir.resolve("libModule/lib/api/Api.java").apply { parent.createDirectories() }
        api.writeText("package lib.api;\n\npublic class Api {\n    public static int answer() { return 42; }\n}\n")
        val hidden = tempDir.resolve("libModule/lib/internal/Hidden.java").apply { parent.createDirectories() }
        hidden.writeText("package lib.internal;\n\npublic class Hidden {\n    public static int secret() { return 1; }\n}\n")
        javac("--release", "11", "-d", libOut.toString(), libModuleInfo.toString(), api.toString(), hidden.toString())
        val libJar = zipDir(libOut, tempDir.resolve("libModule.jar"))

        val appModuleInfo = tempDir.resolve("app/module-info.java").apply { parent.createDirectories() }
        appModuleInfo.writeText("module app {\n    requires kotlin.stdlib;\n    requires lib;\n}\n")
        val usesApi = tempDir.resolve("app/usesApi.kt")
        usesApi.writeText("package app\n\nimport lib.api.Api\n\nfun call(): Int = Api.answer()\n")
        val usesHidden = tempDir.resolve("app/usesHidden.kt")
        usesHidden.writeText("package app\n\nimport lib.internal.Hidden\n\nfun call(): Int = Hidden.secret()\n")

        assertCompilesTheSameInBothModes("module-ok", listOf(appModuleInfo, usesApi), extraArgs = listOf("-Xmodule-path=$libJar"))
        assertCompilesTheSameInBothModes(
            "module-bad",
            listOf(appModuleInfo, usesHidden),
            expectedExitCode = ExitCode.COMPILATION_ERROR,
            extraArgs = listOf("-Xmodule-path=$libJar"),
        )

        val autoOut = tempDir.resolve("autoModuleOut").createDirectories()
        val util = tempDir.resolve("autoModule/auto/Util.java").apply { parent.createDirectories() }
        util.writeText("package auto;\n\npublic class Util {\n    public static int answer() { return 1; }\n}\n")
        javac("-d", autoOut.toString(), util.toString())
        val autoJar = zipDir(
            autoOut,
            tempDir.resolve("autoModule.jar"),
            manifest = Manifest().apply {
                mainAttributes.putValue("Manifest-Version", "1.0")
                mainAttributes.putValue("Automatic-Module-Name", "auto.lib")
            },
        )

        val autoAppModuleInfo = tempDir.resolve("autoApp/module-info.java").apply { parent.createDirectories() }
        autoAppModuleInfo.writeText("module app {\n    requires kotlin.stdlib;\n    requires auto.lib;\n}\n")
        val usesAuto = tempDir.resolve("autoApp/usesAuto.kt")
        usesAuto.writeText("package app\n\nimport auto.Util\n\nfun call(): Int = Util.answer()\n")
        assertCompilesTheSameInBothModes(
            "automatic-module", listOf(autoAppModuleInfo, usesAuto), extraArgs = listOf("-Xmodule-path=$autoJar")
        )
    }

    /** `Runtime.version()` is JDK 9+ API, which this module does not compile against. */
    private fun assumeJdk11OrNewer() {
        val specificationVersion = System.getProperty("java.specification.version") // "1.8" on JDK 8, "11"+ on JDK 9+
        val featureVersion = specificationVersion.removePrefix("1.").substringBefore('.').toInt()
        assumeTrue(featureVersion >= 11, "the test JVM must be JDK 11 or newer: $specificationVersion")
    }

    private fun javac(vararg args: String) {
        val exitCode = ToolProvider.getSystemJavaCompiler().run(null, null, null, *args)
        assertEquals(0, exitCode, "javac failed: ${args.joinToString(" ")}")
    }

    /** Zips the regular files under [dir] into [target], optionally writing [manifest] as META-INF/MANIFEST.MF. */
    private fun zipDir(dir: Path, target: Path, manifest: Manifest? = null): Path {
        target.parent.createDirectories()
        val stream = if (manifest != null) JarOutputStream(target.outputStream(), manifest) else ZipOutputStream(target.outputStream())
        stream.use { out ->
            Files.walk(dir).use { paths ->
                paths.filter { Files.isRegularFile(it) }.sorted().forEach { file ->
                    out.putNextEntry(ZipEntry(dir.relativize(file).toString().replace(File.separatorChar, '/')))
                    Files.newInputStream(file).use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }
        return target
    }
}
