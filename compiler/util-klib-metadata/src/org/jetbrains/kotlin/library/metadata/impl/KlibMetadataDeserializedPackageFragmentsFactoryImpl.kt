/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata.impl

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.storage.StorageManager

// TODO decouple and move interop-specific logic back to Kotlin/Native.
open class KlibMetadataDeserializedPackageFragmentsFactoryImpl : KlibMetadataDeserializedPackageFragmentsFactory {
    override fun createDeserializedPackageFragments(
        library: KotlinLibrary,
        moduleDescriptor: ModuleDescriptor,
        customMetadataProtoLoader: CustomMetadataProtoLoader?,
        storageManager: StorageManager,
        configuration: DeserializationConfiguration
    ): List<KlibMetadataDeserializedPackageFragment> {
        val metadata = library.metadata
        val header = customMetadataProtoLoader?.loadModuleHeader(library)
            ?: parseModuleHeader(metadata.moduleHeaderData)

        val nonEmptyPackageFqNames = buildSet {
            addAll(header.packageFragmentNameList)
            removeAll(header.emptyPackageList)
        }

        return nonEmptyPackageFqNames.flatMap {
            val packageFqName = FqName(it)
            val containerSource = KlibDeserializedContainerSource(
                library, header, configuration, packageFqName, incompatibility = library.getIncompatibility(configuration.metadataVersion)
            )
            val parts = metadata.getPackageFragmentNames(packageFqName.asString())
            val isBuiltInModule = moduleDescriptor.builtIns.builtInsModule === moduleDescriptor
            parts.map { partName ->
                if (isBuiltInModule)
                    BuiltInKlibMetadataDeserializedPackageFragment(
                        fqName = packageFqName,
                        library = library,
                        metadata = metadata,
                        customMetadataProtoLoader = customMetadataProtoLoader,
                        storageManager = storageManager,
                        module = moduleDescriptor,
                        partName = partName,
                        containerSource = containerSource,
                    )
                else
                    KlibMetadataDeserializedPackageFragment(
                        fqName = packageFqName,
                        library = library,
                        metadata = metadata,
                        customMetadataProtoLoader = customMetadataProtoLoader,
                        storageManager = storageManager,
                        module = moduleDescriptor,
                        partName = partName,
                        containerSource = containerSource,
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
