package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf

fun parsePackageFragment(packageMetadata: ByteArray): KonanProtoBuf.LinkDataPackageFragment =
        KonanProtoBuf.LinkDataPackageFragment.parseFrom(packageMetadata, KonanSerializerProtocol.extensionRegistry)

fun parseModuleHeader(libraryMetadata: ByteArray): KonanProtoBuf.LinkDataLibrary =
        KonanProtoBuf.LinkDataLibrary.parseFrom(libraryMetadata, KonanSerializerProtocol.extensionRegistry)
