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

package org.jetbrains.jet.outputUtils

import java.io.File
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jet.OutputFileFactory
import org.jetbrains.jet.SingleDirectoryDirector
import org.jetbrains.jet.OutputDirector

public fun OutputFileFactory.writeAll(outputDirector: OutputDirector, report: (sources: List<File>, output: File) -> Unit) {
    for (file in outputFiles) {
        val sources = getSourceFiles(file)
        val output = File(outputDirector.getOutputDirectory(sources), file)
        report(sources, output)
        FileUtil.writeToFile(output, this.asBytes(file))
    }
}

private val REPORT_NOTHING = { (sources: List<File>, output: File) -> }

public inline fun OutputFileFactory.writeAllTo(outputDir: File) {
    writeAll(SingleDirectoryDirector(outputDir), REPORT_NOTHING)
}