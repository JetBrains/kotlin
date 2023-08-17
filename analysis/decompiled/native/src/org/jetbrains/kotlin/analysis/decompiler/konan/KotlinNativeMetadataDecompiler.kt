/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions
import org.jetbrains.kotlin.serialization.js.DynamicTypeDeserializer

class KotlinNativeMetadataDecompiler : Fe10KlibMetadataDecompiler<KlibMetadataVersion>(
    KlibMetaFileType,
    { KlibMetadataSerializerProtocol },
    DynamicTypeDeserializer,
    { KlibMetadataVersion.INSTANCE },
    { KlibMetadataVersion.INVALID_VERSION },
    KlibMetaFileType.STUB_VERSION + KotlinStubVersions.BUILTIN_STUB_VERSION
) {
    override fun doReadFile(file: VirtualFile): FileWithMetadata? {
        return FileWithMetadata.forPackageFragment(file)
    }
}
