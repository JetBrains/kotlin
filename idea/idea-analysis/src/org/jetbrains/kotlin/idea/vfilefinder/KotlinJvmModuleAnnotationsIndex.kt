/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.vfilefinder

import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import java.io.DataInput
import java.io.DataOutput

object KotlinJvmModuleAnnotationsIndex : FileBasedIndexExtension<String, List<ClassId>>() {

    val KEY: ID<String, List<ClassId>> = ID.create(KotlinJvmModuleAnnotationsIndex::class.java.canonicalName)

    private val KEY_DESCRIPTOR = KotlinModuleMappingIndex.STRING_KEY_DESCRIPTOR

    private val VALUE_EXTERNALIZER = object : DataExternalizer<List<ClassId>> {
        override fun read(input: DataInput): List<ClassId>? =
            IOUtil.readStringList(input).map(ClassId::fromString)

        override fun save(out: DataOutput, value: List<ClassId>) =
            IOUtil.writeStringList(out, value.map(ClassId::asString))
    }

    override fun getName() = KEY

    override fun dependsOnFileContent() = true

    override fun getKeyDescriptor() = KEY_DESCRIPTOR

    override fun getValueExternalizer() = VALUE_EXTERNALIZER

    override fun getInputFilter(): FileBasedIndex.InputFilter =
        FileBasedIndex.InputFilter { file -> file.extension == ModuleMapping.MAPPING_FILE_EXT }

    override fun getVersion(): Int = 1

    override fun getIndexer(): DataIndexer<String, List<ClassId>, FileContent> = DataIndexer { inputData ->
        val file = inputData.file
        try {
            val moduleMapping = ModuleMapping.loadModuleMapping(inputData.content, file.toString(), DeserializationConfiguration.Default) {}
            if (moduleMapping !== ModuleMapping.EMPTY) {
                return@DataIndexer mapOf(file.nameWithoutExtension to moduleMapping.moduleData.annotations.map(ClassId::fromString))
            }
        } catch (e: Exception) {
            // Exceptions are already reported in KotlinModuleMappingIndex
        }
        emptyMap()
    }
}
