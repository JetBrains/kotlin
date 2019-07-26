package org.jetbrains.kotlin.backend.common.serialization.metadata.impl

import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataDeserializedPackageFragmentsFactory
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataPackageFragment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.PackageAccessedHandler
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.storage.StorageManager

open class KlibMetadataDeserializedPackageFragmentsFactoryImpl: KlibMetadataDeserializedPackageFragmentsFactory {

    override fun createDeserializedPackageFragments(
        library: KotlinLibrary,
        packageFragmentNames: List<String>,
        moduleDescriptor: ModuleDescriptor,
        packageAccessedHandler: PackageAccessedHandler?,
        storageManager: StorageManager
    ) = packageFragmentNames.flatMap {
        val fqName = FqName(it)
        val parts = library.packageMetadataParts(fqName.asString())
        parts.map { partName ->
            KlibMetadataPackageFragment(fqName, library, packageAccessedHandler, storageManager, moduleDescriptor, partName)
        }
    }

    override fun createSyntheticPackageFragments(
        library: KotlinLibrary,
        deserializedPackageFragments: List<KlibMetadataPackageFragment>,
        moduleDescriptor: ModuleDescriptor
    ): List<PackageFragmentDescriptor> {
        // TODO
        println("Don't push me. Figure out proper inheritance here")
        return emptyList()
    }
}