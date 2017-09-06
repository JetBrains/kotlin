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

package org.jetbrains.kotlin.idea.vfilefinder

import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.jetbrains.kotlin.load.kotlin.PackageParts
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import java.io.DataInput
import java.io.DataOutput

object KotlinModuleMappingIndex : FileBasedIndexExtension<String, PackageParts>() {

    val KEY: ID<String, PackageParts> = ID.create(KotlinModuleMappingIndex::class.java.canonicalName)

    private val KEY_DESCRIPTOR = object : KeyDescriptor<String> {
        override fun save(output: DataOutput, value: String) = IOUtil.writeUTF(output, value)

        override fun read(input: DataInput) = IOUtil.readUTF(input)

        override fun getHashCode(value: String) = value.hashCode()

        override fun isEqual(val1: String?, val2: String?) = val1 == val2
    }

    private val VALUE_EXTERNALIZER = object : DataExternalizer<PackageParts> {
        override fun read(input: DataInput): PackageParts? =
                PackageParts(IOUtil.readUTF(input)).apply {
                    val partInternalNames = IOUtil.readStringList(input)
                    val facadeInternalNames = IOUtil.readStringList(input)
                    for ((partName, facadeName) in partInternalNames zip facadeInternalNames) {
                        addPart(partName, if (facadeName.isNotEmpty()) facadeName else null)
                    }
                    IOUtil.readStringList(input).forEach(this::addMetadataPart)
                }

        override fun save(out: DataOutput, value: PackageParts) {
            IOUtil.writeUTF(out, value.packageFqName)
            IOUtil.writeStringList(out, value.parts)
            IOUtil.writeStringList(out, value.parts.map { value.getMultifileFacadeName(it).orEmpty() })
            IOUtil.writeStringList(out, value.metadataParts)
        }
    }

    override fun getName() = KEY

    override fun dependsOnFileContent() = true

    override fun getKeyDescriptor() = KEY_DESCRIPTOR

    override fun getValueExternalizer() = VALUE_EXTERNALIZER

    override fun getInputFilter(): FileBasedIndex.InputFilter =
            FileBasedIndex.InputFilter { file -> file.extension == ModuleMapping.MAPPING_FILE_EXT }

    override fun getVersion(): Int = 5

    override fun getIndexer(): DataIndexer<String, PackageParts, FileContent> {
        return DataIndexer<String, PackageParts, FileContent> { inputData ->
            val content = inputData.content
            val file = inputData.file
            try {
                val moduleMapping = ModuleMapping.create(content, file.toString(), DeserializationConfiguration.Default)
                if (moduleMapping === ModuleMapping.CORRUPTED) {
                    file.refresh(true, false)
                }
                return@DataIndexer moduleMapping.packageFqName2Parts
            }
            catch(e: Exception) {
                throw RuntimeException("Error on indexing $file", e)
            }
        }
    }
}
