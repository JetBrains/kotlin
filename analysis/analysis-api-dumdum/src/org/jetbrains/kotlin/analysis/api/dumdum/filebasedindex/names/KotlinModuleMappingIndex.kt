// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names

import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileBasedIndexExtension
import org.jetbrains.kotlin.idea.KotlinModuleFileType
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import java.io.DataInput
import java.io.DataOutput

internal const val MAPPING_FILE_DOT_FILE_EXTENSION = ".${ModuleMapping.MAPPING_FILE_EXT}"

class KotlinModuleMappingIndex internal constructor() : FileBasedIndexExtension<String, PackageParts> {
    companion object {
        val NAME: ID<String, PackageParts> = ID.create(KotlinModuleMappingIndex::class.java.canonicalName)

        internal val STRING_KEY_DESCRIPTOR = object : KeyDescriptor<String> {
            override fun save(output: DataOutput, value: String) = IOUtil.writeUTF(output, value)

            override fun read(input: DataInput) = IOUtil.readUTF(input)

            override fun getHashCode(value: String) = value.hashCode()

            override fun isEqual(val1: String?, val2: String?) = val1 == val2
        }
    }

    private val VALUE_EXTERNALIZER = object : DataExternalizer<PackageParts> {
        override fun read(input: DataInput): PackageParts = PackageParts(IOUtil.readUTF(input)).apply {
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

    override val name
        get() = NAME

    override val keyDescriptor
        get() = STRING_KEY_DESCRIPTOR

    override val valueExternalizer
        get() = VALUE_EXTERNALIZER

    override val inputFilter
        get() = listOf(KotlinModuleFileType.INSTANCE)

    override val version: Int
        get() = 5

    override val indexer: DataIndexer<String, PackageParts, FileContent> =
        DataIndexer { inputData ->
            val content = inputData.content
            val file = inputData.file
            try {
                val moduleMapping = ModuleMapping.loadModuleMapping(
                    bytes = content,
                    debugName = file.toString(),
                    skipMetadataVersionCheck = false,
                    isJvmPackageNameSupported = true,
                    metadataVersionFromLanguageVersion = MetadataVersion.INSTANCE,
                ) {
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
