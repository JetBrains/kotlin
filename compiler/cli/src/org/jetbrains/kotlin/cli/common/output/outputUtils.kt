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

package org.jetbrains.kotlin.cli.common.output.outputUtils

import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.io.File
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.backend.common.output.OutputFile

fun OutputFileCollection.writeAll(outputDir: File, report: (file: OutputFile, sources: List<File>, output: File) -> Unit) {
    for (file in asList()) {
        val sources = file.sourceFiles
        val output = File(outputDir, file.relativePath)
        report(file, sources, output)
        FileUtil.writeToFile(output, file.asByteArray())
    }
}

private val REPORT_NOTHING = { file: OutputFile, sources: List<File>, output: File -> }

fun OutputFileCollection.writeAllTo(outputDir: File) {
    writeAll(outputDir, REPORT_NOTHING)
}

fun OutputFileCollection.writeAll(outputDir: File, messageCollector: MessageCollector) {
    writeAll(outputDir) { file, sources, output ->
        messageCollector.report(CompilerMessageSeverity.OUTPUT, OutputMessageUtil.formatOutputMessage(sources, output), CompilerMessageLocation.NO_LOCATION)
    }
}
