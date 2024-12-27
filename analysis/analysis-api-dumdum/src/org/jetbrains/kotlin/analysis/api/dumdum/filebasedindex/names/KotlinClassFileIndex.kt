package org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.name.FqName

class KotlinClassFileIndex : KotlinFileIndexBase() {
    companion object {
        val NAME: ID<FqName, Unit> = ID.create("org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names.KotlinClassFileIndex")
    }

    override val name: ID<FqName, Unit>
        get() = NAME

    override val version: Int
        get() = 3

    override val inputFilter: List<FileType>
        get() = listOf(JavaClassFileType.INSTANCE)

    override val indexer: DataIndexer<FqName, Unit, FileContent> = indexer { fileContent ->
        val headerInfo = ClsKotlinBinaryClassCache.getInstance().getKotlinBinaryClassHeaderData(fileContent.file, fileContent.content)
        if (headerInfo != null && headerInfo.metadataVersion.isCompatibleWithCurrentCompilerVersion()) headerInfo.classId.asSingleFqName() else null
    }
}