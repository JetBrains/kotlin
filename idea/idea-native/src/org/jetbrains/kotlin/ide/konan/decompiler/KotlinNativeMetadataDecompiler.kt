/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.decompiler

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.konan.library.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform
import org.jetbrains.kotlin.serialization.konan.KonanSerializerProtocol
import org.jetbrains.kotlin.serialization.konan.NullFlexibleTypeDeserializer

class KotlinNativeMetadataDecompiler : KotlinNativeMetadataDecompilerBase<KotlinNativeMetadataVersion>(
    KotlinNativeMetaFileType, KonanPlatform, KonanSerializerProtocol, NullFlexibleTypeDeserializer,
    KotlinNativeMetadataVersion.DEFAULT_INSTANCE,
    KotlinNativeMetadataVersion.INVALID_VERSION,
    KotlinNativeMetaFileType.STUB_VERSION
) {

    override fun doReadFile(file: VirtualFile): FileWithMetadata? {
        val proto = KotlinNativeLoadingMetadataCache.getInstance().getCachedPackageFragment(file)
        return FileWithMetadata.Compatible(proto, KonanSerializerProtocol) //todo: check version compatibility
    }
}

class KotlinNativeMetadataVersion(vararg numbers: Int) : BinaryVersion(*numbers) {
    override fun isCompatible(): Boolean = true //todo: ?

    companion object {
        @JvmField
        val DEFAULT_INSTANCE = KotlinNativeMetadataVersion(1, 1, 0)

        @JvmField
        val INVALID_VERSION = KotlinNativeMetadataVersion()
    }
}

object KotlinNativeMetaFileType : FileType {
    override fun getName() = "KNM"
    override fun getDescription() = "Kotlin/Native Metadata"
    override fun getDefaultExtension() = KLIB_METADATA_FILE_EXTENSION
    override fun getIcon() = null
    override fun isBinary() = true
    override fun isReadOnly() = true
    override fun getCharset(file: VirtualFile, content: ByteArray) = null

    const val STUB_VERSION = 2
}

class KotlinNativeMetaFileTypeFactory : FileTypeFactory() {

    override fun createFileTypes(consumer: FileTypeConsumer) = consumer.consume(KotlinNativeMetaFileType, KotlinNativeMetaFileType.defaultExtension)
}
