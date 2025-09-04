/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyOf
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import java.security.MessageDigest

fun calculateCompilationInputHash(
    args: K2JVMCompilerArguments
): String {
    // TODO: at this stage I'm computing input hash with these arguments
    // this needs to be revisited as they might be other important arguments that can produce
    // different compilation results

    // TODO: remove the output folder from compiler args because that is not important
    // TODO: consider refactor and create method that just creates arguments for this hashing use case, it will be probably less error prone
    val importantCompilerArgs = args.copyOf()
    importantCompilerArgs.destination = null
    importantCompilerArgs.classpath = null
    importantCompilerArgs.pluginConfigurations = null
    importantCompilerArgs.freeArgs = importantCompilerArgs.freeArgs.filter { !it.startsWith("/") }

    val digest = MessageDigest.getInstance("SHA-256")
    val files = CompilerUtils.getSourceFiles(args) + CompilerUtils.getDependencyFiles(args)
    files.sortedBy { it.path }.forEach { file ->
        digest.update(computeSha256(file).toByteArray(Charsets.UTF_8))// TODO: double check this approach
    }
    ArgumentUtils.convertArgumentsToStringList(importantCompilerArgs).sorted().forEach { arg ->
        digest.update(arg.toByteArray())
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}