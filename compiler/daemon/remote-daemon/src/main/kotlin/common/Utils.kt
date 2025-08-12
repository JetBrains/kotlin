/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import java.io.File
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.io.path.extension

fun calculateCompilationInputHash(sourceFiles: List<File>, compilerArguments: List<String>, compilerVersion: String): String{
    // TODO: at this stage I'm computing input hash with these arguments
    // this needs to be revisited as they might be other important arguments that can produce
    // different compilation results
    println("computing hash for compilation input with args: $compilerArguments, source files: ${sourceFiles.map { it.path }}, compiler version: $compilerVersion")

    // remove the output folder from compiler args because that is not important
    // TODO: consider refactor and create method that just creates arguments for this hashing use case, it will be probably less error prone
    val importantCompilerArgs = compilerArguments.toMutableList()
    val index = importantCompilerArgs.indexOf("-d")
    if (index != -1) {
        // remove the element after "-d" first, then "-d" itself
        importantCompilerArgs.removeAt(index + 1)
        importantCompilerArgs.removeAt(index)
    }


    val digest = MessageDigest.getInstance("SHA-256")
    sourceFiles.sortedBy { it.path }.forEach { file->
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
    }
    importantCompilerArgs.sorted().forEach { arg ->
        digest.update(arg.toByteArray())
    }
    digest.update(compilerVersion.toByteArray())
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun isFileDependency(path: String): Boolean {
    // TODO are there any other extensions ?
    return Paths.get(path).extension in setOf("jar", "klib", "class")
}