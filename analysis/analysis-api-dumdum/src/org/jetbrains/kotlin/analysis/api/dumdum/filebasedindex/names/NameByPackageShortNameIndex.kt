// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.analysis.api.dumdum.index.*
import org.jetbrains.kotlin.analysis.decompiler.konan.FileWithMetadata
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibMetaFileType
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import java.io.DataInput
import java.io.DataOutput

fun FileBasedIndex.getNamesInPackage(indexId: ID<FqName, List<Name>>, packageFqName: FqName, scope: GlobalSearchScope): Set<Name> {
    return buildSet {
        processValues(indexId, packageFqName, scope) {
            ProgressManager.checkCanceled()
            addAll(it)
            true
        }
    }
}

object FqNameKeyDescriptor : KeyDescriptor<FqName> {
    override fun save(output: DataOutput, value: FqName) {
        IOUtil.writeUTF(output, value.asString())
    }

    override fun read(input: DataInput): FqName = FqName(IOUtil.readUTF(input))
    override fun getHashCode(value: FqName): Int = value.asString().hashCode()
    override fun isEqual(val1: FqName?, val2: FqName?): Boolean = val1 == val2
}


abstract class NameByPackageShortNameIndex : FileBasedIndexExtension<FqName, List<Name>> {
    private val LOG = logger<NameByPackageShortNameIndex>()

    protected abstract fun getDeclarationNamesByKtFile(ktFile: KtFile): List<Name>
    protected abstract fun getDeclarationNamesByMetadata(kotlinJvmBinaryClass: KotlinJvmBinaryClass): List<Name>
    protected abstract fun getPackageAndNamesFromBuiltIns(fileContent: FileContent): Map<FqName, List<Name>>
    protected abstract fun getDeclarationNamesByKnm(kotlinNativeMetadata: FileWithMetadata.Compatible): List<Name>

    fun dependsOnFileContent() = true
    override val version get() = 2
    override val keyDescriptor: FqNameKeyDescriptor
        get() = FqNameKeyDescriptor
    override val valueExternalizer: DataExternalizer<List<Name>>
        get() = ListOfNamesDataExternalizer
    fun traceKeyHashToVirtualFileMapping(): Boolean = true

    override val inputFilter: List<FileType>
        get() = listOf(
            KotlinFileType.INSTANCE,
            JavaClassFileType.INSTANCE,
            KotlinBuiltInFileType,
            KlibMetaFileType,
        )

    override val indexer
        get() = DataIndexer<FqName, List<Name>, FileContent> { fileContent ->
            try {
                when (fileContent.fileType) {
                    JavaClassFileType.INSTANCE -> getPackageAndNamesFromMetadata(fileContent)
                    KotlinBuiltInFileType -> getPackageAndNamesFromBuiltIns(fileContent)
                    KotlinFileType.INSTANCE -> {
                        val ktFile = fileContent.psiFile as? KtFile ?: return@DataIndexer emptyMap()
                        mapOf(ktFile.packageFqName to getDeclarationNamesByKtFile(ktFile).distinct())
                    }
                    KlibMetaFileType -> getPackageAndNamesFromKnm(fileContent)
                    else -> emptyMap()
                }
            } catch (e: Throwable) {
                if (e is ControlFlowException) throw e
                LOG.warn("Error `(${e.javaClass.simpleName}: ${e.message})` while indexing file ${fileContent.fileName} using ${name} index. Probably the file is broken.")
                emptyMap()
            }
        }

    private fun getPackageAndNamesFromMetadata(fileContent: FileContent): Map<FqName, List<Name>> {
        val binaryClass = fileContent.toKotlinJvmBinaryClass() ?: return emptyMap()
        if (binaryClass.classHeader.kind == KotlinClassHeader.Kind.SYNTHETIC_CLASS) return emptyMap()
        if (binaryClass.classId.isLocal) return emptyMap()

        val packageName = binaryClass.packageName
        return mapOf(packageName to getDeclarationNamesByMetadata(binaryClass).distinct())
    }

    private fun getPackageAndNamesFromKnm(fileContent: FileContent): Map<FqName, List<Name>> {
        val fileWithMetadata = fileContent.toCompatibleFileWithMetadata() ?: return emptyMap()
        return mapOf(fileWithMetadata.packageFqName to getDeclarationNamesByKnm(fileWithMetadata))
    }
}

private object ListOfNamesDataExternalizer : DataExternalizer<List<Name>> {
    override fun read(input: DataInput): List<Name> =
        IOUtil.readStringList(input).map(Name::identifier)

    override fun save(out: DataOutput, value: List<Name>) =
        IOUtil.writeStringList(out, value.map(Name::asString))
}