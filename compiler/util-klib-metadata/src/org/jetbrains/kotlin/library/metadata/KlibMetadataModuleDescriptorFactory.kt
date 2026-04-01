/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.storage.StorageManager

interface KlibMetadataModuleDescriptorFactory {

    val descriptorFactory: KlibModuleDescriptorFactory
    val packageFragmentsFactory: KlibMetadataDeserializedPackageFragmentsFactory
    val flexibleTypeDeserializer: FlexibleTypeDeserializer

    fun createDescriptor(
        library: KotlinLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns,
    ) = createDescriptorOptionalBuiltIns(
        library,
        languageVersionSettings,
        storageManager,
        builtIns,
        LookupTracker.DO_NOTHING
    )

    fun createDescriptorAndNewBuiltIns(
        library: KotlinLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
    ) = createDescriptorOptionalBuiltIns(
        library, languageVersionSettings, storageManager, null, LookupTracker.DO_NOTHING
    )

    @Deprecated(
        "Preserved for binary compatibility with existing versions of the kotlinx-benchmarks Gradle plugin. See KT-82882.",
        level = DeprecationLevel.HIDDEN
    )
    @Suppress("DEPRECATION_ERROR")
    fun createDescriptorOptionalBuiltIns(
        library: KotlinLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        packageAccessHandler: PackageAccessHandler?,
        lookupTracker: LookupTracker
    ): ModuleDescriptorImpl = createDescriptorOptionalBuiltIns(library, languageVersionSettings, storageManager, builtIns, lookupTracker)

    fun createDescriptorOptionalBuiltIns(
        library: KotlinLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        lookupTracker: LookupTracker
    ): ModuleDescriptorImpl

    fun createPackageFragmentProvider(
        library: KotlinLibrary,
        customMetadataProtoLoader: CustomMetadataProtoLoader?,
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        configuration: DeserializationConfiguration,
        compositePackageFragmentAddend: PackageFragmentProvider?,
        lookupTracker: LookupTracker
    ): PackageFragmentProvider

    fun createCachedPackageFragmentProvider(
        byteArrays: List<ByteArray>,
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        configuration: DeserializationConfiguration,
        lookupTracker: LookupTracker
    ): PackageFragmentProvider
}
