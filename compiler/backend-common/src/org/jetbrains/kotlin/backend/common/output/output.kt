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

package org.jetbrains.kotlin.backend.common.output

import java.io.File

interface OutputFileCollection {
    fun get(relativePath: String): OutputFile?
    fun asList(): List<OutputFile>
}

class SimpleOutputFileCollection(private val outputFiles: List<OutputFile>) : OutputFileCollection {
    override fun get(relativePath: String): OutputFile? = outputFiles.firstOrNull { it.relativePath == relativePath }
    override fun asList(): List<OutputFile> = outputFiles
}

interface OutputFile {
    val relativePath: String
    val sourceFiles: List<File>
    fun asByteArray(): ByteArray
    fun asText(): String
}

class SimpleOutputFile(
        override val sourceFiles: List<File>,
        override val relativePath: String,
        private val content: String
) : OutputFile {
    override fun asByteArray(): ByteArray = content.toByteArray()
    override fun asText(): String = content

    override fun toString() = "$relativePath (compiled from $sourceFiles)"
}

class SimpleOutputBinaryFile(
        override val sourceFiles: List<File>,
        override val relativePath: String,
        private val content: ByteArray
) : OutputFile {
    override fun asByteArray(): ByteArray = content
    override fun asText(): String = String(content)

    override fun toString() = "$relativePath (compiled from $sourceFiles)"
}