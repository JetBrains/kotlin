package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataPackageFragment
import org.jetbrains.kotlin.library.metadata.PackageAccessHandler
import org.jetbrains.kotlin.storage.StorageManager

interface KlibMetadataDeserializedPackageFragmentsFactory {
    fun createDeserializedPackageFragments(
        library: KotlinLibrary,
        packageFragmentNames: List<String>,
        moduleDescriptor: ModuleDescriptor,
        packageAccessedHandler: PackageAccessHandler?,
        storageManager: StorageManager
    ): List<KlibMetadataPackageFragment>

    fun createCachedPackageFragments(
        packageFragments: List<ByteArray>,
        moduleDescriptor: ModuleDescriptor,
        storageManager: StorageManager
    ): List<KlibMetadataPackageFragment>

    fun createSyntheticPackageFragments(
        library: KotlinLibrary,
        deserializedPackageFragments: List<KlibMetadataPackageFragment>,
        moduleDescriptor: ModuleDescriptor
    ): List<PackageFragmentDescriptor>
}