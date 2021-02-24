/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.decompiler

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.idea.klib.FileWithMetadata
import org.jetbrains.kotlin.idea.klib.KlibLoadingMetadataCache
import org.jetbrains.kotlin.idea.klib.KlibMetaFileType
import org.jetbrains.kotlin.idea.klib.KlibMetadataDecompiler
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.serialization.js.DynamicTypeDeserializer

class KotlinNativeMetadataDecompiler : KlibMetadataDecompiler<KlibMetadataVersion>(
    KlibMetaFileType,
    { KlibMetadataSerializerProtocol },
    DynamicTypeDeserializer,
    { KlibMetadataVersion.INSTANCE },
    { KlibMetadataVersion.INVALID_VERSION },
    KlibMetaFileType.STUB_VERSION
) {
    override fun doReadFile(file: VirtualFile): FileWithMetadata? {
        val fragment = KlibLoadingMetadataCache.getInstance().getCachedPackageFragment(file) ?: return null
        return FileWithMetadata.Compatible(fragment) //todo: check version compatibility
    }
}
