// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.psi.text.DecompiledText
import org.jetbrains.kotlin.analysis.decompiler.psi.text.buildDecompiledText
import org.jetbrains.kotlin.analysis.decompiler.psi.text.createIncompatibleAbiVersionDecompiledText
import org.jetbrains.kotlin.analysis.decompiler.psi.text.defaultDecompilerRendererOptions
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.IOException

abstract class KotlinMetadataDecompiler<out V : BinaryVersion>(
    private val fileType: FileType,
    private val serializerProtocol: () -> SerializerExtensionProtocol,
    private val flexibleTypeDeserializer: FlexibleTypeDeserializer,
    private val expectedBinaryVersion: () -> V,
    private val invalidBinaryVersion: () -> V,
    stubVersion: Int
) : ClassFileDecompilers.Full() {
    protected open val metadataStubBuilder: KotlinMetadataStubBuilder =
        KotlinMetadataStubBuilder(stubVersion, fileType, serializerProtocol, ::readFileSafely)

    private val renderer: DescriptorRenderer by lazy {
        DescriptorRenderer.withOptions { defaultDecompilerRendererOptions() }
    }

    abstract fun readFile(bytes: ByteArray, file: VirtualFile): KotlinMetadataStubBuilder.FileWithMetadata?

    override fun accepts(file: VirtualFile) = file.extension == fileType.defaultExtension || file.fileType == fileType

    override fun getStubBuilder() = metadataStubBuilder

    override fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): FileViewProvider {
        return KotlinDecompiledFileViewProvider(manager, file, physical) { provider ->
            val virtualFile = provider.virtualFile
            readFileSafely(virtualFile)?.let { fileWithMetadata ->
                KtDecompiledFile(provider) {
                    check(it == virtualFile) {
                        "Unexpected file $it, expected ${virtualFile.fileType}"
                    }
                    buildDecompiledText(fileWithMetadata)
                }
            }
        }
    }

    @TestOnly
    fun readFile(file: VirtualFile) = readFileSafely(file)

    protected fun readFileSafely(file: VirtualFile, content: ByteArray? = null): KotlinMetadataStubBuilder.FileWithMetadata? {
        if (!file.isValid) return null

        return try {
            readFile(content ?: file.contentsToByteArray(false), file)
        } catch (e: IOException) {
            // This is needed because sometimes we're given VirtualFile instances that point to non-existent .jar entries.
            // Such files are valid (isValid() returns true), but an attempt to read their contents results in a FileNotFoundException.
            // Note that although calling "refresh()" instead of catching an exception would seem more correct here,
            // it's not always allowed and also is likely to degrade performance
            null
        }
    }

    fun buildDecompiledText(file: KotlinMetadataStubBuilder.FileWithMetadata): DecompiledText {
        return when (file) {
            is KotlinMetadataStubBuilder.FileWithMetadata.Incompatible -> {
                createIncompatibleAbiVersionDecompiledText(expectedBinaryVersion(), file.version)
            }
            is KotlinMetadataStubBuilder.FileWithMetadata.Compatible -> {
                val packageFqName = file.packageFqName
                val resolver = KotlinMetadataDeserializerForDecompiler(
                    packageFqName, file.proto, file.nameResolver, file.version,
                    serializerProtocol(), flexibleTypeDeserializer
                )
                val declarations = arrayListOf<DeclarationDescriptor>()
                declarations.addAll(resolver.resolveDeclarationsInFacade(packageFqName))
                for (classProto in file.classesToDecompile) {
                    val classId = file.nameResolver.getClassId(classProto.fqName)
                    declarations.addIfNotNull(resolver.resolveTopLevelClass(classId))
                }
                buildDecompiledText(packageFqName, declarations, renderer)
            }
        }
    }
}

