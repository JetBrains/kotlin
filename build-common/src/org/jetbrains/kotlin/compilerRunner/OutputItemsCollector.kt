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
package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import java.io.File

interface OutputItemsCollector {
    fun add(sourceFiles: Collection<File>, outputFile: File)
    fun addSourceReferencedByCompilerPlugin(sourceFile: File)
    fun addOutputFileGeneratedForPlugin(outputFile: File)
}

class OutputItemsCollectorImpl : OutputItemsCollector {
    val outputs: List<SimpleOutputItem>
        get() = _outputs

    private val _outputs: MutableList<SimpleOutputItem> = mutableListOf()

    val sourcesReferencedByCompilerPlugin: List<File>
        get() = _sourcesReferencedByCompilerPlugin

    private val _sourcesReferencedByCompilerPlugin: MutableList<File> = mutableListOf()

    val outputsFileGeneratedForPlugin: List<File>
        get() = _outputsFileGeneratedForPlugin

    private val _outputsFileGeneratedForPlugin: MutableList<File> = mutableListOf()

    override fun add(sourceFiles: Collection<File>, outputFile: File) {
        _outputs.add(SimpleOutputItem(sourceFiles, outputFile))
    }

    override fun addSourceReferencedByCompilerPlugin(sourceFile: File) {
        _sourcesReferencedByCompilerPlugin.add(sourceFile)
    }

    override fun addOutputFileGeneratedForPlugin(outputFile: File) {
        _outputsFileGeneratedForPlugin.add(outputFile)
    }
}

data class SimpleOutputItem(val sourceFiles: Collection<File>, val outputFile: File) {
    override fun toString(): String =
        "$sourceFiles->$outputFile"
}

fun SimpleOutputItem.toGeneratedFile(metadataVersionFromLanguageVersion: MetadataVersion): GeneratedFile =
    when {
        outputFile.name.endsWith(".class") -> GeneratedJvmClass(sourceFiles, outputFile, metadataVersionFromLanguageVersion)
        else -> GeneratedFile(sourceFiles, outputFile)
    }
