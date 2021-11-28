/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("JvmCompilationUtils")

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.rethrow
import java.io.*
import java.nio.charset.Charset
import java.util.*
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import java.util.stream.Collectors


@JvmOverloads
@Throws(IOException::class)
fun compileJavaFiles(
    files: Collection<File>,
    options: List<String?>?,
    javaErrorFile: File? = null,
    assertions: Assertions,
    ignoreJavaErrors: Boolean = false
): Boolean {
    val javaCompiler = ToolProvider.getSystemJavaCompiler()
    val diagnosticCollector = DiagnosticCollector<JavaFileObject>()
    javaCompiler.getStandardFileManager(diagnosticCollector, Locale.ENGLISH, Charset.forName("utf-8")).use { fileManager ->
        val javaFileObjectsFromFiles = fileManager.getJavaFileObjectsFromFiles(files)
        val task = try {
            javaCompiler.getTask(
                StringWriter(),  // do not write to System.err
                fileManager,
                diagnosticCollector,
                options,
                null,
                javaFileObjectsFromFiles
            )
        } catch (e: Throwable) {
            if (ignoreJavaErrors) return false
            else throw e
        }
        val success = task.call() // do NOT inline this variable, call() should complete before errorsToString()
        if (javaErrorFile == null || !javaErrorFile.exists()) {
            assertions.assertTrue(success || ignoreJavaErrors) { errorsToString(diagnosticCollector, true) }
        } else {
            assertions.assertEqualsToFile(javaErrorFile, errorsToString(diagnosticCollector, false))
        }
        return success
    }
}

fun compileJavaFilesExternallyWithJava11(files: Collection<File>, options: List<String?>): Boolean {
    return compileJavaFilesExternally(files, options, KtTestUtil.getJdk11Home())
}

fun compileJavaFilesExternally(files: Collection<File>, options: List<String?>, jdkHome: File): Boolean {
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

    val isSuccess = process.exitValue() == 0

    if (!isSuccess) {
        System.err.println(errors)
    }

    return isSuccess
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
