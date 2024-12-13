/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.util.ExceptionUtil
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.io.IOException
import kotlin.math.min

fun checkKotlinPackageUsageForPsi(configuration: CompilerConfiguration, files: Collection<KtFile>) =
    org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForPsi(configuration, files)


internal fun calculateSourceMapSourceRoot(
    messageCollector: MessageCollector,
    arguments: K2JSCompilerArguments,
): String {
    var commonPath: File? = null
    val pathToRoot = mutableListOf<File>()
    val pathToRootIndexes = hashMapOf<File, Int>()

    try {
        for (path in arguments.freeArgs) {
            var file: File? = File(path).canonicalFile
            if (commonPath == null) {
                commonPath = file

                while (file != null) {
                    pathToRoot.add(file)
                    file = file.parentFile
                }
                pathToRoot.reverse()

                for (i in pathToRoot.indices) {
                    pathToRootIndexes[pathToRoot[i]] = i
                }
            } else {
                while (file != null) {
                    var existingIndex = pathToRootIndexes[file]
                    if (existingIndex != null) {
                        existingIndex = min(existingIndex, pathToRoot.size - 1)
                        pathToRoot.subList(existingIndex + 1, pathToRoot.size).clear()
                        commonPath = pathToRoot[pathToRoot.size - 1]
                        break
                    }
                    file = file.parentFile
                }
                if (file == null) {
                    break
                }
            }
        }
    } catch (e: IOException) {
        val text = ExceptionUtil.getThrowableText(e)
        messageCollector.report(ERROR, "IO error occurred calculating source root:\n$text", location = null)
        return "."
    }

    return commonPath?.path ?: "."
}