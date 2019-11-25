/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.metadata.ProtoBuf

fun parsePackageFragment(packageMetadata: ByteArray): ProtoBuf.PackageFragment =
    ProtoBuf.PackageFragment.parseFrom(packageMetadata, KlibMetadataSerializerProtocol.extensionRegistry)

fun parseModuleHeader(libraryMetadata: ByteArray): KlibMetadataProtoBuf.Header =
    KlibMetadataProtoBuf.Header.parseFrom(libraryMetadata, KlibMetadataSerializerProtocol.extensionRegistry)

interface PackageAccessHandler {
    fun loadModuleHeader(library: KotlinLibrary): KlibMetadataProtoBuf.Header
            = parseModuleHeader(library.moduleHeaderData)

    fun loadPackageFragment(
        library: KotlinLibrary,
        packageFqName: String,
        partName: String
    ): ProtoBuf.PackageFragment = loadPackageFragmentByteArray(library.packageMetadata(packageFqName, partName))

    fun loadPackageFragmentByteArray(byteArray: ByteArray): ProtoBuf.PackageFragment = parsePackageFragment(byteArray)

    fun markNeededForLink(library: KotlinLibrary, fqName: String) {}
}

object SimplePackageAccessHandler : PackageAccessHandler {
    override fun loadPackageFragment(
        library: KotlinLibrary,
        packageFqName: String,
        partName: String
    ): ProtoBuf.PackageFragment = parsePackageFragment(library.packageMetadata(packageFqName, partName))
}

