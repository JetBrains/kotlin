/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.common.output

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import java.io.File
import java.io.FileNotFoundException

fun OutputFileCollection.writeAll(outputDir: File, report: ((sources: List<File>, output: File) -> Unit)?) {
    for (file in asList()) {
        val sources = file.sourceFiles
        val output = File(outputDir, file.relativePath)
        report?.invoke(sources, output)
        try {
            FileUtil.writeToFile(output, file.asByteArray())
        } catch (e: FileNotFoundException) {
            if (outputDir.isDirectory) {
                // output directory exists
                throw NoPermissionException("error while writing $output (Permission denied)", e)
            }
            // Failed to create directory, possibly due to lack of write permission or clash with existing file.
            // In both cases, the directory will not exist, so for the sake of simplicity, we treat them same way.
            throw e
        }
    }
}

fun OutputFileCollection.writeAllTo(outputDir: File) {
    writeAll(outputDir, null)
}

fun OutputFileCollection.writeAll(outputDir: File, messageCollector: MessageCollector, reportOutputFiles: Boolean) {
    try {
        if (!reportOutputFiles) writeAllTo(outputDir)
        else writeAll(outputDir) { sources, output ->
            messageCollector.report(CompilerMessageSeverity.OUTPUT, OutputMessageUtil.formatOutputMessage(sources, output))
        }
    } catch (e: NoPermissionException) {
        messageCollector.report(CompilerMessageSeverity.ERROR, e.message!!)
    } catch (e: FileNotFoundException) {
        messageCollector.report(CompilerMessageSeverity.ERROR, "directory not found: $outputDir")
    }
}

private class NoPermissionException(message: String, cause: Throwable?) : IllegalStateException(message, cause)
