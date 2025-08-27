/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.decompiler.konan

import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions

class KotlinNativeMetadataDecompiler : Fe10KlibMetadataDecompiler<MetadataVersion>(
    KlibMetaFileType,
    { KlibMetadataSerializerProtocol },
    KotlinStubVersions.KOTLIN_NATIVE_STUB_VERSION,
)
