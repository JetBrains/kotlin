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

@Deprecated("Restored to keep ABI compatibility with kotlinx-benchmark Gradle plugin (KT-71414)", level = DeprecationLevel.HIDDEN)
interface PackageAccessHandler

/**
 * A special interceptor that allows customizing the way how metadata proto objects are loaded.
 * The single real usage is in IntelliJ IDEA.
 */
interface CustomMetadataProtoLoader {
    fun loadModuleHeader(library: KotlinLibrary): KlibMetadataProtoBuf.Header
    fun loadPackageFragment(library: KotlinLibrary, packageFqName: String, partName: String): ProtoBuf.PackageFragment
}
