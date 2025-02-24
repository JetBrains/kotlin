/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions
import org.jetbrains.kotlin.serialization.js.DynamicTypeDeserializer

class KotlinNativeMetadataDecompiler : Fe10KlibMetadataDecompiler<MetadataVersion>(
    KlibMetaFileType,
    { KlibMetadataSerializerProtocol },
    DynamicTypeDeserializer,
    KotlinStubVersions.KOTLIN_NATIVE_STUB_VERSION,
) {
    override fun doReadFile(file: VirtualFile): FileWithMetadata? {
        return FileWithMetadata.forPackageFragment(file)
    }
}
