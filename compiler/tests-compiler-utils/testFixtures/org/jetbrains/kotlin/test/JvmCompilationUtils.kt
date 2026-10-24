/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("JvmCompilationUtils")

package org.jetbrains.kotlin.test

import com.intellij.util.currentJavaVersion
import com.intellij.util.lang.JavaVersion
import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.stream.Collectors
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider

sealed class JavaCompilationResult {
    object Success : JavaCompilationResult() {
        override fun assertSuccessful() {}
    }

    sealed class Failure : JavaCompilationResult() {
        abstract val diagnostics: String
        abstract val presentableDiagnostics: String

        class InProcess(diagnosticCollector: DiagnosticCollector<JavaFileObject>, val output: String) : Failure() {
            override val diagnostics: String = errorsToString(diagnosticCollector, humanReadable = false)
            override val presentableDiagnostics: String = errorsToString(diagnosticCollector, humanReadable = true)

            override fun assertSuccessful() {
                throw JavaCompilationError(presentableDiagnostics.ifBlank { output })
            }
        }

        class External(override val diagnostics: String) : Failure() {
            override val presentableDiagnostics: String = diagnostics
        }

        override fun assertSuccessful() {
            throw JavaCompilationError(presentableDiagnostics)
        }
    }

    abstract fun assertSuccessful()
}

class JavaCompilationError(errors: String) :
    AssertionError("Java files are not compiled successfully\n$errors")

/**
 * Compiles [files] like the `javac` of the JDK at [jdkHome] would, or like the `javac` of the test JVM if [jdkHome] is `null`.
 *
 * The `javac` of the test JVM compiles in-process whenever it can stand in for the requested one: with the given [options] for a JDK
 * of the same feature version, and with `--release` added for an older one. Otherwise, the `javac` of [jdkHome] runs in a separate
 * process, which takes a JVM start per call, usually longer than the compilation of a test's few sources.
 */
@JvmOverloads
fun compileJavaFiles(files: Collection<File>, options: List<String?>, jdkHome: File? = null): JavaCompilationResult {
    val patchModuleOptions = extractPatchModuleOptions(files)
    val releaseOptions = if (jdkHome != null) {
        releaseOptionsToCompileInProcess(jdkHome, options + patchModuleOptions)
            ?: return compileJavaFilesExternally(files, options, jdkHome)
    } else {
        emptyList()
    }

    val javaCompiler = ToolProvider.getSystemJavaCompiler()
    val diagnosticCollector = DiagnosticCollector<JavaFileObject>()

    return javaCompiler.getStandardFileManager(diagnosticCollector, Locale.ENGLISH, Charset.forName("utf-8")).use { fileManager ->
        val javaFileObjectsFromFiles = fileManager.getJavaFileObjectsFromFiles(files)
        StringWriter().use { compilerOutputWriter ->
            val task = javaCompiler.getTask(
                compilerOutputWriter,
                fileManager,
                diagnosticCollector,
                options + releaseOptions + listOf("-Djava.ext.dirs=") + patchModuleOptions,
                null,
                javaFileObjectsFromFiles
            )

            when (task.call()) {
                true -> JavaCompilationResult.Success
                false -> JavaCompilationResult.Failure.InProcess(diagnosticCollector, compilerOutputWriter.toString())
                null -> error("JavaCompiler call() returned null")
            }
        }
    }
}

private val JAVA_SOURCE_SET_PATH_PATTERN = """(.*/java-sources/[^/]+/java/).+""".toRegex()

/**
 * With JDK 17, it's no longer possible to "just overload" classes from `java.base` module
 * by redeclaring custom ones in the same package: to do so, we now need to pass
 * `--patch-module java.base=<path-to-java-sources>` to javac.
 */
private fun extractPatchModuleOptions(files: Collection<File>): List<String> =
    files.mapNotNullTo(mutableSetOf()) {
        val normalizedPath = it.path.replace(File.separator, "/")
        JAVA_SOURCE_SET_PATH_PATTERN.matchEntire(normalizedPath)?.groupValues?.getOrNull(1)
    }.flatMap {
        listOf("--patch-module", "java.base=$it")
    }

/**
 * Returns the options with which the `javac` of the test JVM compiles like the `javac` of the JDK at [jdkHome]: none for the same
 * feature version, and `--release` for an older JDK. Returns `null` if it can't: the JDK is newer, or [options] contain an option
 * that `javac` doesn't allow together with `--release`.
 */
private fun releaseOptionsToCompileInProcess(jdkHome: File, options: List<String?>): List<String>? {
    val jdkVersion = readJdkFeatureVersion(jdkHome) ?: return null
    val currentVersion = currentJavaVersion().feature
    return when {
        jdkVersion == currentVersion -> emptyList()
        jdkVersion > currentVersion -> null
        options.any { it != null && it.conflictsWithRelease() } -> null
        ToolProvider.getSystemJavaCompiler().sourceVersions.none { it.name == "RELEASE_$jdkVersion" } -> null
        else -> listOf("--release", jdkVersion.toString())
    }
}

private val OPTIONS_CONFLICTING_WITH_RELEASE = setOf(
    "-source", "--source", "-target", "--target", "--release", "-bootclasspath", "--boot-class-path", "-extdirs",
    "--extension-directories", "-endorseddirs", "--endorsed-directories", "--system", "--upgrade-module-path", "--patch-module",
    "--add-exports", "--add-reads", "--limit-modules",
)

private fun String.conflictsWithRelease(): Boolean =
    substringBefore('=') in OPTIONS_CONFLICTING_WITH_RELEASE || startsWith("-Xbootclasspath")

/**
 * Returns the feature version of the JDK at [jdkHome] (e.g., 17), as recorded in its `release` file, or `null` if it can't be read.
 */
fun readJdkFeatureVersion(jdkHome: File): Int? {
    val releaseFile = File(jdkHome, "release").takeIf { it.isFile } ?: return null
    val version = releaseFile.readLines().firstNotNullOfOrNull { it.substringAfter("JAVA_VERSION=", "").ifEmpty { null } }
        ?: return null
    return JavaVersion.tryParse(version.trim('"'))?.feature
}

private fun compileJavaFilesExternally(files: Collection<File>, options: List<String?>, jdkHome: File): JavaCompilationResult {
    val command: MutableList<String?> = ArrayList()
    command.add(File(jdkHome, "bin/javac").path)
    command.addAll(options)
    for (file in files) {
        command.add(file.path)
    }
    val process = ProcessBuilder().command(command).start()
    val errorsReader = BufferedReader(InputStreamReader(process.errorStream))
    val errors = errorsReader.lines().collect(Collectors.joining(System.lineSeparator()))

    process.waitFor()

    return if (process.exitValue() == 0)
        JavaCompilationResult.Success
    else
        JavaCompilationResult.Failure.External(errors)
}

private fun errorsToString(diagnosticCollector: DiagnosticCollector<JavaFileObject>, humanReadable: Boolean): String {
    val builder = StringBuilder()
    for (diagnostic in diagnosticCollector.diagnostics) {
        if (diagnostic.kind != Diagnostic.Kind.ERROR) continue
        if (humanReadable) {
            builder.append(diagnostic).append('\n')
        } else {
            if (diagnostic.source != null) {
                builder.append(File(diagnostic.source.toUri()).name).append(':')
                    .append(diagnostic.lineNumber).append(':')
                    .append(diagnostic.columnNumber).append(':')
            }
            builder.append(diagnostic.code).append('\n')
        }
    }
    return builder.toString()
}
