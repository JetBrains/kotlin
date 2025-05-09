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

package org.jetbrains.kotlin.build

import org.jetbrains.kotlin.incremental.LocalFileKotlinClass
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.utils.sure
import java.io.File

open class GeneratedFile(
    sourceFiles: Collection<File>,
    val outputFile: File,
    data: ByteArray? = null,
    relativePath: String? = null,
) {
    val sourceFiles = sourceFiles.sortedBy { it.path }
    val data: ByteArray by lazy { data ?: outputFile.readBytes() }
    val relativePath: String by lazy { relativePath ?: outputFile.canonicalPath }

    override fun toString(): String = "${this::class.java.simpleName}: $outputFile"
}

class GeneratedJvmClass(
    sourceFiles: Collection<File>,
    outputFile: File,
    metadataVersionFromLanguageVersion: MetadataVersion,
    data: ByteArray? = null,
    relativePath: String? = null,
) : GeneratedFile(sourceFiles, outputFile, data, relativePath) {
    val outputClass: LocalFileKotlinClass = when(data) {
        null -> LocalFileKotlinClass.create(outputFile, metadataVersionFromLanguageVersion)
        else -> LocalFileKotlinClass.create(
            outputFile, data, metadataVersionFromLanguageVersion
        )
    }.sure { "Couldn't load KotlinClass from $outputFile; it may happen because class doesn't have valid Kotlin annotations" }
}

private val META_INF_SUFFIX = File.separatorChar + "META-INF"

@Suppress("unused")
fun File.isModuleMappingFile(): Boolean {
    if (!path.endsWith(".kotlin_module")) {
        return false
    }
    val parentPath = parent
    return parentPath == "META-INF" || parentPath.endsWith(META_INF_SUFFIX)
}