// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names

import com.intellij.openapi.fileTypes.FileType
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltInDefinitionFile
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.DOT_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.getClassId

abstract class KotlinMetadataFileIndexBase(indexFunction: (ClassId) -> FqName) : KotlinFileIndexBase() {
    override val inputFilter: List<FileType> = listOf(KotlinBuiltInFileType)

    override val version = 2

    override val indexer = indexer { fileContent ->
        val classId = fileContent.classIdFromKotlinMetadata() ?: return@indexer null
        indexFunction(classId)
    }
}

internal fun FileContent.classIdFromKotlinMetadata(): ClassId? {
    val builtIns = readKotlinMetadataDefinition(this) as? BuiltInDefinitionFile ?: return null

    val singleClass = builtIns.proto.class_List.singleOrNull()
    if (singleClass != null) {
        return builtIns.nameResolver.getClassId(singleClass.fqName)
    }

    val facadeName = this.fileName.substringBeforeLast(DOT_METADATA_FILE_EXTENSION)
    return ClassId(builtIns.packageFqName, Name.identifier(facadeName))
}
