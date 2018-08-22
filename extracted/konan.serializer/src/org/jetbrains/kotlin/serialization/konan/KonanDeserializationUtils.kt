package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf

fun parsePackageFragment(packageData: ByteArray): KonanProtoBuf.LinkDataPackageFragment =
        KonanProtoBuf.LinkDataPackageFragment.parseFrom(packageData, KonanSerializerProtocol.extensionRegistry)

fun parseModuleHeader(libraryData: ByteArray): KonanProtoBuf.LinkDataLibrary =
        KonanProtoBuf.LinkDataLibrary.parseFrom(libraryData, KonanSerializerProtocol.extensionRegistry)

fun emptyPackages(libraryData: ByteArray) = parseModuleHeader(libraryData).emptyPackageList
