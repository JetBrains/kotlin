/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf


fun parsePackageFragment(packageMetadata: ByteArray): ProtoBuf.PackageFragment =
    ProtoBuf.PackageFragment.parseFrom(packageMetadata, KlibMetadataSerializerProtocol.extensionRegistry)

fun parseModuleHeader(libraryMetadata: ByteArray): KlibMetadataProtoBuf.Header =
    KlibMetadataProtoBuf.Header.parseFrom(libraryMetadata, KlibMetadataSerializerProtocol.extensionRegistry)
