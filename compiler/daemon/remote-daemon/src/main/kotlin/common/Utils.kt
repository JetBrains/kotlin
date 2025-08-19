/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import java.security.MessageDigest

fun calculateCompilationInputHash(
    compilerArguments: Map<String, String>
): String {
    // TODO: at this stage I'm computing input hash with these arguments
    // this needs to be revisited as they might be other important arguments that can produce
    // different compilation results

    // TODO: remove the output folder from compiler args because that is not important
    // TODO: consider refactor and create method that just creates arguments for this hashing use case, it will be probably less error prone
    val importantCompilerArgs = compilerArguments.toMutableMap()
    importantCompilerArgs.remove("-d")
    importantCompilerArgs.remove("-classpath")
    importantCompilerArgs.remove("-Xplugin=")

    val digest = MessageDigest.getInstance("SHA-256")
    val files = CompilerUtils.getSourceFiles(importantCompilerArgs) + CompilerUtils.getDependencyFiles(importantCompilerArgs)
    files.sortedBy { it.path }.forEach { file ->
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
    }
    importantCompilerArgs.values.sorted().forEach { arg ->
        digest.update(arg.toByteArray())
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}