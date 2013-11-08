/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.cli.common.output.outputUtils

import org.jetbrains.jet.OutputFileCollection
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation
import org.jetbrains.jet.cli.common.messages.OutputMessageUtil
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.jet.cli.common.messages.MessageCollector
import org.jetbrains.jet.cli.common.output.OutputDirector
import java.io.File
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jet.cli.common.output.SingleDirectoryDirector

public fun OutputFileCollection.writeAll(outputDirector: OutputDirector, report: (sources: List<File>, output: File) -> Unit) {
    for (file in asList()) {
        val sources = file.sourceFiles
        val output = File(outputDirector.getOutputDirectory(sources), file.relativePath)
        report(sources, output)
        FileUtil.writeToFile(output, file.asByteArray())
    }
}

private val REPORT_NOTHING = { (sources: List<File>, output: File) -> }

public inline fun OutputFileCollection.writeAllTo(outputDir: File) {
    writeAll(SingleDirectoryDirector(outputDir), REPORT_NOTHING)
}

public inline fun OutputFileCollection.writeAll(outputDirector: OutputDirector, messageCollector: MessageCollector) {
    writeAll(outputDirector) { sources, output ->
        messageCollector.report(CompilerMessageSeverity.OUTPUT, OutputMessageUtil.formatOutputMessage(sources, output), CompilerMessageLocation.NO_LOCATION)
    }
}