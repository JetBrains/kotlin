// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
@file:OptIn(UnsafeCastFunction::class)

package org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileBasedIndexExtension
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibLoadingMetadataCache
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibMetaFileType
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.DataInput
import java.io.DataOutput

private val LOG = logger<KotlinPartialPackageNamesIndex>()

/**
 * An index from Kotlin packages to their direct subpackage names (or `null` if the package is a leaf).
 *
 * #### Example
 *
 * ```
 * package foo.bar.baz
 * ```
 *
 * For this file, the index will contain the following entries:
 *
 * ```
 * "foo.bar.baz" --> null
 * "foo.bar"     --> "baz"
 * "foo"         --> "bar
 * ```
 */
class KotlinPartialPackageNamesIndex : FileBasedIndexExtension<FqName, Name?> {
    companion object {
        val NAME: ID<FqName, Name?> = ID.create(KotlinPartialPackageNamesIndex::class.java.canonicalName)
    }

    private object NullableNameExternalizer: DataExternalizer<Name?> {
        override fun save(out: DataOutput, value: Name?) {
            out.writeBoolean(value == null)
            if (value != null) {
                IOUtil.writeUTF(out, value.asString())
            }
        }

        override fun read(input: DataInput): Name? =
            if (input.readBoolean()) null else Name.guessByFirstCharacter(IOUtil.readUTF(input))
    }

    override val name 
        get() = NAME

//    override fun dependsOnFileContent() = true

    override val keyDescriptor 
        get() = FqNameKeyDescriptor

    override val valueExternalizer: DataExternalizer<Name?>
        get() = NullableNameExternalizer

    override val inputFilter: List<FileType>
        get() = listOf(
            JavaClassFileType.INSTANCE,
            KotlinFileType.INSTANCE,
//            KotlinJavaScriptMetaFileType,
            KotlinBuiltInFileType,
            KlibMetaFileType,
        )

    override val version 
        get() = 5

//    override fun traceKeyHashToVirtualFileMapping(): Boolean = true

    private fun FileContent.toPackageFqName(): FqName? =
        when (this.fileType) {
            KotlinFileType.INSTANCE -> this.psiFile.safeAs<KtFile>()?.packageFqName
            JavaClassFileType.INSTANCE -> ClsKotlinBinaryClassCache.getInstance()
                .getKotlinBinaryClassHeaderData(this.file, this.content)?.packageNameWithFallback
//            KotlinJavaScriptMetaFileType -> this.fqNameFromJsMetadata()
            KotlinBuiltInFileType -> this.classIdFromKotlinMetadata()?.packageFqName
            KlibMetaFileType -> KlibLoadingMetadataCache.getInstance().getCachedPackageFragment(file)
                ?.getExtension(KlibMetadataProtoBuf.fqName)?.let(::FqName)
            else -> null
        }

    override val indexer
        get() = DataIndexer<FqName, Name?, FileContent> { fileContent ->
            try {
                val packageFqName = fileContent.toPackageFqName() ?: return@DataIndexer emptyMap<FqName, Name?>()

                generateSequence(packageFqName) {
                    it.parentOrNull()
                }.filterNot { it.isRoot }.associateBy({ it.parent() }, { it.shortName() }) + mapOf(packageFqName to null)
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: Throwable) {
                LOG.warn("Error `(${e.javaClass.simpleName}: ${e.message})` while indexing file ${fileContent.fileName} using ${name} index. Probably the file is broken.")
                emptyMap()
            }
        }
}
