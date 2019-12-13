/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.vfilefinder

import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import java.io.DataInput
import java.io.DataOutput

object KotlinModuleMappingIndex : FileBasedIndexExtension<String, PackageParts>() {

    val KEY: ID<String, PackageParts> = ID.create(KotlinModuleMappingIndex::class.java.canonicalName)

    internal val STRING_KEY_DESCRIPTOR = object : KeyDescriptor<String> {
        override fun save(output: DataOutput, value: String) = IOUtil.writeUTF(output, value)

        override fun read(input: DataInput) = IOUtil.readUTF(input)

        override fun getHashCode(value: String) = value.hashCode()

        override fun isEqual(val1: String?, val2: String?) = val1 == val2
    }

    private val VALUE_EXTERNALIZER = object : DataExternalizer<PackageParts> {
        override fun read(input: DataInput): PackageParts? = PackageParts(IOUtil.readUTF(input)).apply {
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

    override fun getKeyDescriptor() = STRING_KEY_DESCRIPTOR

    override fun getValueExternalizer() = VALUE_EXTERNALIZER

    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { file ->
        file.extension == ModuleMapping.MAPPING_FILE_EXT
    }

    override fun getVersion(): Int = 5

    override fun getIndexer(): DataIndexer<String, PackageParts, FileContent> = DataIndexer { inputData ->
        val content = inputData.content
        val file = inputData.file
        try {
            val moduleMapping = ModuleMapping.loadModuleMapping(content, file.toString(), DeserializationConfiguration.Default) {
                // Do nothing; it's OK for an IDE index to just ignore incompatible module files
            }
            if (moduleMapping === ModuleMapping.CORRUPTED) {
                file.refresh(true, false)
            }
            return@DataIndexer moduleMapping.packageFqName2Parts
        } catch (e: Exception) {
            throw RuntimeException("Error on indexing $file", e)
        }
    }
}
