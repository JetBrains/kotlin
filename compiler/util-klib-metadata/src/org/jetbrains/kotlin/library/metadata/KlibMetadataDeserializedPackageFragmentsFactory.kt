package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.storage.StorageManager

interface KlibMetadataDeserializedPackageFragmentsFactory {
    fun createDeserializedPackageFragments(
        library: KotlinLibrary,
        moduleDescriptor: ModuleDescriptor,
        customMetadataProtoLoader: CustomMetadataProtoLoader?,
        storageManager: StorageManager,
        configuration: DeserializationConfiguration
    ): List<KlibMetadataPackageFragment>

    fun createCachedPackageFragments(
        packageFragments: List<ByteArray>,
        moduleDescriptor: ModuleDescriptor,
        storageManager: StorageManager
    ): List<KlibMetadataPackageFragment>
}
