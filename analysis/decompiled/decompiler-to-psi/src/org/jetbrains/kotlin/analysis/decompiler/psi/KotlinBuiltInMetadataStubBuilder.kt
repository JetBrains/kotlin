/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

object KotlinBuiltInMetadataStubBuilder : KotlinMetadataStubBuilder() {
    /**
     * This version is used for .kotlin_builtins and is not used for .kotlin_metadata files:
     * K1 IDE and K2 IDE produce different decompiled files and stubs for .kotlin_builtins, but not for .kotlin_metadata
     */
    override fun getStubVersion(): Int = KotlinStubVersions.BUILTIN_STUB_VERSION + KotlinBuiltInStubVersionOffsetProvider.getVersionOffset()
    override val supportedFileType: FileType get() = KotlinBuiltInFileType
    override val expectedBinaryVersion: BinaryVersion get() = BuiltInsBinaryVersion.INSTANCE

    override fun readFile(virtualFile: VirtualFile, content: ByteArray?): FileWithMetadata? {
        val content = content ?: virtualFile.contentsToByteArray(false)
        return KotlinBuiltInDecompilationInterceptor.readFile(content, virtualFile) ?: BuiltInDefinitionFile.read(content, virtualFile)
    }

    override fun createCallableSource(file: FileWithMetadata.Compatible, filename: String): SourceElement? {
        val fileNameForFacade = when (val withoutExtension = filename.removeSuffix(BuiltInSerializerProtocol.DOT_DEFAULT_EXTENSION)) {
            // this is the filename used in stdlib, others should match
            "kotlin" -> "library"
            else -> withoutExtension
        }

        val facadeFqName = PackagePartClassUtils.getPackagePartFqName(file.packageFqName, fileNameForFacade)
        return JvmPackagePartSource(
            JvmClassName.byClassId(ClassId.topLevel(facadeFqName)),
            facadeClassName = null,
            jvmClassName = null,
            file.proto.`package`,
            file.nameResolver
        )
    }
}


