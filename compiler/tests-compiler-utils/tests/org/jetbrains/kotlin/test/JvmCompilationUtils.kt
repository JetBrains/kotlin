/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("JvmCompilationUtils")

package org.jetbrains.kotlin.test

import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.stream.Collectors
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider

sealed class JavaCompilationResult {
    object Success : JavaCompilationResult()

    sealed class Failure : JavaCompilationResult() {
        abstract val diagnostics: String
        abstract val presentableDiagnostics: String

        class InProcess(diagnosticCollector: DiagnosticCollector<JavaFileObject>) : Failure() {
            override val diagnostics: String = errorsToString(diagnosticCollector, humanReadable = false)
            override val presentableDiagnostics: String = errorsToString(diagnosticCollector, humanReadable = true)
        }

        class External(override val diagnostics: String) : Failure() {
            override val presentableDiagnostics: String = diagnostics
        }
    }

    fun assertSuccessful() {
        if (this is Failure) throw JavaCompilationError(presentableDiagnostics)
    }
}

class JavaCompilationError(errors: String) : AssertionError("Java files are not compiled successfully\n$errors")

@JvmOverloads
fun compileJavaFiles(files: Collection<File>, options: List<String?>, jdkHome: File? = null): JavaCompilationResult {
    if (jdkHome != null) {
        return compileJavaFilesExternally(files, options, jdkHome)
    }

    val javaCompiler = ToolProvider.getSystemJavaCompiler()
    val diagnosticCollector = DiagnosticCollector<JavaFileObject>()
    javaCompiler.getStandardFileManager(diagnosticCollector, Locale.ENGLISH, Charset.forName("utf-8")).use { fileManager ->
        val javaFileObjectsFromFiles = fileManager.getJavaFileObjectsFromFiles(files)
        val task = javaCompiler.getTask(
            StringWriter(),  // do not write to System.err
            fileManager,
            diagnosticCollector,
            options,
            null,
            javaFileObjectsFromFiles
        )
        return when (task.call()) {
            true -> JavaCompilationResult.Success
            false -> JavaCompilationResult.Failure.InProcess(diagnosticCollector)
            null -> error("JavaCompiler call() returned null")
        }
    }
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
            builder.append(diagnostic).append("\n")
        } else {
            builder.append(File(diagnostic.source.toUri()).name).append(":")
                .append(diagnostic.lineNumber).append(":")
                .append(diagnostic.columnNumber).append(":")
                .append(diagnostic.code).append("\n")
        }
    }
    return builder.toString()
}
