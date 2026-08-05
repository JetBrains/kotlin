/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.library.metadata.impl.KlibMetadataModuleDescriptorFactoryImpl
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.storage.StorageManager

/**
 * The default Kotlin/Native factories.
 */
class KlibMetadataFactories(
    private val createBuiltIns: (StorageManager) -> KotlinBuiltIns,
    @OptIn(K1Deprecation::class)
    val flexibleTypeDeserializer: FlexibleTypeDeserializer,
    val additionalClassPartsProvider: AdditionalClassPartsProvider,
    val fictitiousClassDescriptorFactories: List<ClassDescriptorFactory>,
) {
    @OptIn(K1Deprecation::class)
    constructor(
        createBuiltIns: (StorageManager) -> KotlinBuiltIns,
        flexibleTypeDeserializer: FlexibleTypeDeserializer,
    ) : this(createBuiltIns, flexibleTypeDeserializer, AdditionalClassPartsProvider.None, emptyList())

    /**
     * The default [KlibMetadataModuleDescriptorFactory] factory instance.
     */
    val DefaultDeserializedDescriptorFactory: KlibMetadataModuleDescriptorFactory =
        createDefaultKonanDeserializedModuleDescriptorFactory()

    fun createDefaultKonanDeserializedModuleDescriptorFactory(): KlibMetadataModuleDescriptorFactory =
        @OptIn(K1Deprecation::class)
        KlibMetadataModuleDescriptorFactoryImpl(
            createBuiltIns,
            flexibleTypeDeserializer,
            additionalClassPartsProvider,
            fictitiousClassDescriptorFactories,
        )

}
