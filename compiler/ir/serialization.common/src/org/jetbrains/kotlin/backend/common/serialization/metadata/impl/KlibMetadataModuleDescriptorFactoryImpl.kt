/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.konan.impl

import org.jetbrains.kotlin.backend.common.serialization.metadata.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.functionInterfacePackageFragmentProvider
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.ContractDeserializerImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackageFragmentProviderImpl
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.KlibModuleDescriptorFactory
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.PackageAccessedHandler
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.konan.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.storage.StorageManager

internal class KlibMetadataModuleDescriptorFactoryImpl(
    override val descriptorFactory: KlibModuleDescriptorFactory,
    override val packageFragmentsFactory: KlibMetadataDeserializedPackageFragmentsFactory
): KlibMetadataModuleDescriptorFactory {

    override fun createDescriptorOptionalBuiltIns(
        library: KotlinLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        packageAccessedHandler: PackageAccessedHandler?,
        createBuiltinPackageFragment: Boolean
    ): ModuleDescriptorImpl {

        val libraryProto = parseModuleHeader(library.moduleHeaderData)

        val moduleName = Name.special(libraryProto.moduleName)
        val moduleOrigin = DeserializedKlibModuleOrigin(library)

        println("createDescriptorOptionalBuiltIns: $moduleName, builtIns = $builtIns")

        val moduleDescriptor = if (builtIns != null )
            descriptorFactory.createDescriptor(moduleName, storageManager, builtIns, moduleOrigin)
        else
            descriptorFactory.createDescriptorAndNewBuiltIns(moduleName, storageManager, moduleOrigin)

        val deserializationConfiguration = CompilerDeserializationConfiguration(languageVersionSettings)

        // TODO: don't push me. Commonize for JS.
        val compositePackageFragmentAddend = if (moduleDescriptor.name == Name.special("<stdlib>")) {
            functionInterfacePackageFragmentProvider(storageManager, moduleDescriptor)
        } else null

        println("got compositePackageFragmentAddend: $compositePackageFragmentAddend")

        val provider = createPackageFragmentProvider(
            library,
            packageAccessedHandler,
            libraryProto.packageFragmentNameList,
            storageManager,
            moduleDescriptor,
            deserializationConfiguration,
            compositePackageFragmentAddend
        )

        moduleDescriptor.initialize(provider)

        return moduleDescriptor
    }

    private fun createPackageFragmentProvider(
        library: KotlinLibrary,
        packageAccessedHandler: PackageAccessedHandler?,
        packageFragmentNames: List<String>,
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        configuration: DeserializationConfiguration,
        compositePackageFragmentAddend: PackageFragmentProvider?
    ): PackageFragmentProvider {


        println("createPackageFragmentProvider(${library.libraryName}, compositePackageFragmentAddend=$compositePackageFragmentAddend ")
        println("\t$packageFragmentNames")

        val deserializedPackageFragments = packageFragmentsFactory.createDeserializedPackageFragments(
            library, packageFragmentNames, moduleDescriptor, packageAccessedHandler, storageManager)

        // TODO: this is native specific. Move to a child class.
        val syntheticPackageFragments = packageFragmentsFactory.createSyntheticPackageFragments(
            library, deserializedPackageFragments, moduleDescriptor)

        val provider = PackageFragmentProviderImpl(deserializedPackageFragments + syntheticPackageFragments)

        val notFoundClasses = NotFoundClasses(storageManager, moduleDescriptor)

        val annotationAndConstantLoader = AnnotationAndConstantLoaderImpl(
            moduleDescriptor,
            notFoundClasses,
            KlibMetadataSerializerProtocol
        )

        val components = DeserializationComponents(
            storageManager,
            moduleDescriptor,
            configuration,
            DeserializedClassDataFinder(provider),
            annotationAndConstantLoader,
            provider,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING,
            NullFlexibleTypeDeserializer,
            emptyList(),
            notFoundClasses,
            ContractDeserializerImpl(configuration, storageManager),
            extensionRegistryLite = KlibMetadataSerializerProtocol.extensionRegistry)

        for (packageFragment in deserializedPackageFragments) {
            packageFragment.initialize(components)
        }

        return compositePackageFragmentAddend ?.let {
            CompositePackageFragmentProvider(listOf(it, provider))
        } ?: provider
    }
}
