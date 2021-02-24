package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentTypeTransformer
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.KlibModuleDescriptorFactory
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.PackageAccessHandler
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.storage.StorageManager

interface KlibMetadataModuleDescriptorFactory {

    val descriptorFactory: KlibModuleDescriptorFactory
    val packageFragmentsFactory: KlibMetadataDeserializedPackageFragmentsFactory
    val flexibleTypeDeserializer: FlexibleTypeDeserializer
    val platformDependentTypeTransformer: PlatformDependentTypeTransformer

    fun createDescriptor(
        library: KotlinLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns,
        packageAccessHandler: PackageAccessHandler?
    ) = createDescriptorOptionalBuiltIns(
        library,
        languageVersionSettings,
        storageManager,
        builtIns,
        packageAccessHandler,
        LookupTracker.DO_NOTHING
    )

    fun createDescriptorAndNewBuiltIns(
        library: KotlinLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        packageAccessHandler: PackageAccessHandler?
    ) = createDescriptorOptionalBuiltIns(
        library, languageVersionSettings, storageManager, null, packageAccessHandler, LookupTracker.DO_NOTHING
    )

    fun createDescriptorOptionalBuiltIns(
        library: KotlinLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        packageAccessHandler: PackageAccessHandler?,
        lookupTracker: LookupTracker
    ): ModuleDescriptorImpl

    fun createPackageFragmentProvider(
        library: KotlinLibrary,
        packageAccessHandler: PackageAccessHandler?,
        packageFragmentNames: List<String>,
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
