// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names

import com.intellij.openapi.fileTypes.FileType
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltInDefinitionFile
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

class KotlinBuiltInsMetadataIndex : KotlinFileIndexBase() {
    companion object {
        val NAME: ID<FqName, Unit> = ID.create("org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names.KotlinBuiltInsMetadataIndex")
    }

    override val name: ID<FqName, Unit>
        get() = NAME

    override val version: Int
        get() = 4

    override val inputFilter: List<FileType> = listOf(KotlinBuiltInFileType)

    override val indexer: DataIndexer<FqName, Unit, FileContent> =
        indexer { fileContent ->
            val packageFqName =
                if (fileContent.fileType == KotlinBuiltInFileType &&
                    fileContent.fileName.endsWith(BuiltInSerializerProtocol.DOT_DEFAULT_EXTENSION)
                ) {
                    val builtins = readKotlinMetadataDefinition(fileContent) as? BuiltInDefinitionFile
                    builtins?.packageFqName
                } else null
            packageFqName
        }
}