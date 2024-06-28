/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.deserialization.*
import org.jetbrains.kotlin.library.metadata.impl.KlibMetadataDeserializedPackageFragmentsFactoryImpl
import org.jetbrains.kotlin.library.metadata.impl.KlibMetadataModuleDescriptorFactoryImpl
import org.jetbrains.kotlin.library.metadata.impl.KlibModuleDescriptorFactoryImpl
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.storage.StorageManager

/**
 * The default Kotlin/Native factories.
 */
class KlibMetadataFactories(
    createBuiltIns: (StorageManager) -> KotlinBuiltIns,
    val flexibleTypeDeserializer: FlexibleTypeDeserializer
) {
    /**
     * The default [KlibModuleDescriptorFactory] factory instance.
     */
    val DefaultDescriptorFactory: KlibModuleDescriptorFactory = KlibModuleDescriptorFactoryImpl(createBuiltIns)

    /**
     * The default [KlibMetadataDeserializedPackageFragmentsFactory] factory instance.
     */
    val DefaultPackageFragmentsFactory: KlibMetadataDeserializedPackageFragmentsFactory =
        KlibMetadataDeserializedPackageFragmentsFactoryImpl()

    /**
     * The default [KlibMetadataModuleDescriptorFactory] factory instance.
     */
    val DefaultDeserializedDescriptorFactory: KlibMetadataModuleDescriptorFactory =
        createDefaultKonanDeserializedModuleDescriptorFactory(
            DefaultDescriptorFactory, DefaultPackageFragmentsFactory
        )

    /**
     * The default [KlibResolvedModuleDescriptorsFactory] factory instance.
     */
    val DefaultResolvedDescriptorsFactory: KlibResolvedModuleDescriptorsFactory =
        createDefaultKonanResolvedModuleDescriptorsFactory(DefaultDeserializedDescriptorFactory)

    fun createDefaultKonanDeserializedModuleDescriptorFactory(
        descriptorFactory: KlibModuleDescriptorFactory,
        packageFragmentsFactory: KlibMetadataDeserializedPackageFragmentsFactory
    ): KlibMetadataModuleDescriptorFactory =
        KlibMetadataModuleDescriptorFactoryImpl(
            descriptorFactory,
            packageFragmentsFactory,
            flexibleTypeDeserializer
        )

    fun createDefaultKonanResolvedModuleDescriptorsFactory(
        moduleDescriptorFactory: KlibMetadataModuleDescriptorFactory
    ): KlibResolvedModuleDescriptorsFactory = KlibResolvedModuleDescriptorsFactoryImpl(moduleDescriptorFactory)
}
