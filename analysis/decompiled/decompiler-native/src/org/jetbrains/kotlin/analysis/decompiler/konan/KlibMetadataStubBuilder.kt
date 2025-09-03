/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder

object KlibMetadataStubBuilder : KotlinMetadataStubBuilder() {
    override fun getStubVersion(): Int = KotlinStubVersions.KLIB_STUB_VERSION
    override val supportedFileType: FileType get() = KlibMetaFileType
    override val expectedBinaryVersion: BinaryVersion get() = MetadataVersion.INSTANCE

    override fun hasMetadata(virtualFile: VirtualFile): Boolean {
        val metadataCache = KlibLoadingMetadataCache.getInstance()
        return metadataCache.getCachedPackageFragment(virtualFile) != null
    }

    override fun readFile(
        virtualFile: VirtualFile,
        content: ByteArray?,
    ): FileWithMetadata? {
        val klibMetadataLoadingCache = KlibLoadingMetadataCache.getInstance()
        val (fragment, version) = klibMetadataLoadingCache.getCachedPackageFragmentWithVersion(virtualFile)
        if (fragment == null || version == null) return null
        if (!version.isCompatibleWithCurrentCompilerVersion()) {
            return FileWithMetadata.Incompatible(version)
        }

        return KlibFileWithMetadata(fragment, version)
    }

    override fun classDataFinder(original: ClassDataFinder, file: VirtualFile): ClassDataFinder {
        return NearFileClassDataFinder.wrapIfNeeded(original, file) { readFileSafely(it) }
    }

    class KlibFileWithMetadata(
        proto: ProtoBuf.PackageFragment,
        version: BinaryVersion,
    ) : FileWithMetadata.Compatible(
        proto = proto,
        version = version,
        serializerProtocol = KlibMetadataSerializerProtocol,
    ) {
        override val packageFqName: FqName
            get() = FqName(proto.getExtension(KlibMetadataProtoBuf.fqName))
    }
}
