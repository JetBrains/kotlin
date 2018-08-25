package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolver.PackageAccessedHandler
import org.jetbrains.kotlin.storage.StorageManager

interface KonanDeserializedPackageFragmentsFactory {

    fun createDeserializedPackageFragments(
            library: KonanLibrary,
            packageFragmentNames: List<String>,
            moduleDescriptor: ModuleDescriptor,
            packageAccessedHandler: PackageAccessedHandler?,
            storageManager: StorageManager
    ): List<KonanPackageFragment>

    fun createSyntheticPackageFragments(
            library: KonanLibrary,
            deserializedPackageFragments: List<KonanPackageFragment>,
            moduleDescriptor: ModuleDescriptor
    ): List<PackageFragmentDescriptor>
}
