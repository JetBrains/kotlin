package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.KlibModuleDescriptorFactory
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.PackageAccessedHandler
import org.jetbrains.kotlin.storage.StorageManager

interface KlibMetadataModuleDescriptorFactory {

    val descriptorFactory: KlibModuleDescriptorFactory
    val packageFragmentsFactory: KlibMetadataDeserializedPackageFragmentsFactory

    fun createDescriptor(
        library: KotlinLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns,
        packageAccessedHandler: PackageAccessedHandler?
    ) = createDescriptorOptionalBuiltIns(
        library,
        languageVersionSettings,
        storageManager,
        builtIns,
        packageAccessedHandler
    )

    fun createDescriptorAndNewBuiltIns(
        library: KotlinLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        packageAccessedHandler: PackageAccessedHandler?
    ) = createDescriptorOptionalBuiltIns(library, languageVersionSettings, storageManager, null, packageAccessedHandler)

    fun createDescriptorOptionalBuiltIns(
        library: KotlinLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        packageAccessedHandler: PackageAccessedHandler?,
        createBuiltinPackageFragment: Boolean = false
    ): ModuleDescriptorImpl
}
