/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf


fun parsePackageFragment(packageMetadata: ByteArray): KlibMetadataProtoBuf.LinkDataPackageFragment =
    KlibMetadataProtoBuf.LinkDataPackageFragment.parseFrom(packageMetadata, KlibMetadataSerializerProtocol.extensionRegistry)

fun parseModuleHeader(libraryMetadata: ByteArray): KlibMetadataProtoBuf.LinkDataLibrary =
    KlibMetadataProtoBuf.LinkDataLibrary.parseFrom(libraryMetadata, KlibMetadataSerializerProtocol.extensionRegistry)
