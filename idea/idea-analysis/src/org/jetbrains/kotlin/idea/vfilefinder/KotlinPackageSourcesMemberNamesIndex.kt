/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.vfilefinder

import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import java.io.DataInput
import java.io.DataOutput

object KotlinPackageSourcesMemberNamesIndex : FileBasedIndexExtension<String, Collection<String>>() {
    val KEY: ID<String, Collection<String>> = ID.create(KotlinPackageSourcesMemberNamesIndex::class.java.canonicalName)

    private val KEY_DESCRIPTOR = EnumeratorStringDescriptor()

    override fun getName() = KEY

    override fun dependsOnFileContent() = true

    override fun getKeyDescriptor() = KEY_DESCRIPTOR

    override fun getValueExternalizer() = StringSetExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter =
        FileBasedIndex.InputFilter { file -> file.extension == KotlinFileType.EXTENSION || file.extension == KotlinParserDefinition.STD_SCRIPT_SUFFIX }

    override fun getVersion(): Int = 2

    override fun getIndexer(): DataIndexer<String, Collection<String>, FileContent> =
        DataIndexer { inputData ->
            // Check if ".kt" file is marked as plain text
            if (inputData.fileType !is KotlinFileType) return@DataIndexer emptyMap()

            val ktFile = inputData.psiFile as? KtFile ?: return@DataIndexer emptyMap()
            val packageName = ktFile.packageDirective?.fqName?.asString() ?: ""

            if (!ktFile.isScript()) {
                mapOf(packageName to ktFile.declarations.mapNotNullTo(hashSetOf(), KtDeclaration::getName))
            } else {
                mapOf(packageName to listOfNotNull(ktFile.script?.name).toHashSet())
            }
        }
}

object StringSetExternalizer : DataExternalizer<Collection<String>> {
    private val ELEMENTS_SERIALIZER = EnumeratorStringDescriptor()

    override fun read(input: DataInput): Set<String> {
        val size = input.readInt()
        return hashSetOf<String>().apply {
            repeat(size) {
                add(ELEMENTS_SERIALIZER.read(input))
            }
        }
    }

    override fun save(output: DataOutput, value: Collection<String>) {
        output.writeInt(value.size)
        value.toSet().forEach { ELEMENTS_SERIALIZER.save(output, it) }
    }
}
