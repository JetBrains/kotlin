package org.jetbrains.kotlin.library.metadata.impl

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.storage.StorageManager

// TODO decouple and move interop-specific logic back to Kotlin/Native.
open class KlibMetadataDeserializedPackageFragmentsFactoryImpl : KlibMetadataDeserializedPackageFragmentsFactory {

    override fun createDeserializedPackageFragments(
        library: KotlinLibrary,
        packageFragmentNames: List<String>,
        moduleDescriptor: ModuleDescriptor,
        packageAccessedHandler: PackageAccessHandler?,
        storageManager: StorageManager,
        configuration: DeserializationConfiguration
    ): List<KlibMetadataDeserializedPackageFragment> {
        val libraryHeader = (packageAccessedHandler ?: SimplePackageAccessHandler).loadModuleHeader(library)

        return packageFragmentNames.flatMap {
            val fqName = FqName(it)
            val containerSource = KlibDeserializedContainerSource(library, libraryHeader, configuration, fqName)
            val parts = library.packageMetadataParts(fqName.asString())
            val isBuiltInModule = moduleDescriptor.builtIns.builtInsModule === moduleDescriptor
            parts.map { partName ->
                if (isBuiltInModule)
                    BuiltInKlibMetadataDeserializedPackageFragment(
                        fqName,
                        library,
                        packageAccessedHandler,
                        storageManager,
                        moduleDescriptor,
                        partName,
                        containerSource
                    )
                else
                    KlibMetadataDeserializedPackageFragment(
                        fqName,
                        library,
                        packageAccessedHandler,
                        storageManager,
                        moduleDescriptor,
                        partName,
                        containerSource
                    )
            }
        }
    }

    override fun createCachedPackageFragments(
        packageFragments: List<ByteArray>,
        moduleDescriptor: ModuleDescriptor,
        storageManager: StorageManager
    ) = packageFragments.map { byteArray ->
        KlibMetadataCachedPackageFragment(byteArray, storageManager, moduleDescriptor)
    }

}
