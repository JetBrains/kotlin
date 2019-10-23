/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.konan.impl

import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataDeserializedPackageFragmentsFactory
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataModuleDescriptorFactory
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.functionInterfacePackageFragmentProvider
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.ContractDeserializerImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.KlibModuleDescriptorFactory
import org.jetbrains.kotlin.descriptors.konan.klibModuleOrigin
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.library.metadata.PackageAccessHandler
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.StorageManager

private val ModuleDescriptorImpl.isStdlibModule
    get() = (this.klibModuleOrigin as? DeserializedKlibModuleOrigin)?.library?.unresolvedDependencies?.isEmpty() ?: false

class KlibMetadataModuleDescriptorFactoryImpl(
    override val descriptorFactory: KlibModuleDescriptorFactory,
    override val packageFragmentsFactory: KlibMetadataDeserializedPackageFragmentsFactory,
    override val flexibleTypeDeserializer: FlexibleTypeDeserializer
) : KlibMetadataModuleDescriptorFactory {

    override fun createDescriptorOptionalBuiltIns(
        library: KotlinLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        packageAccessHandler: PackageAccessHandler?
    ): ModuleDescriptorImpl {

        val libraryProto = parseModuleHeader(library.moduleHeaderData)

        val moduleName = Name.special(libraryProto.moduleName)
        val moduleOrigin = DeserializedKlibModuleOrigin(library)

        val moduleDescriptor = if (builtIns != null )
            descriptorFactory.createDescriptor(moduleName, storageManager, builtIns, moduleOrigin)
        else
            descriptorFactory.createDescriptorAndNewBuiltIns(moduleName, storageManager, moduleOrigin)

        val deserializationConfiguration = CompilerDeserializationConfiguration(languageVersionSettings)

        val compositePackageFragmentAddend =
            if (moduleDescriptor.isStdlibModule) {
                functionInterfacePackageFragmentProvider(storageManager, moduleDescriptor)
            } else null

        val provider = createPackageFragmentProvider(
            library,
            packageAccessHandler,
            libraryProto.packageFragmentNameList,
            storageManager,
            moduleDescriptor,
            deserializationConfiguration,
            compositePackageFragmentAddend
        )

        moduleDescriptor.initialize(provider)

        return moduleDescriptor
    }

    override fun createCachedPackageFragmentProvider(
        byteArrays: List<ByteArray>,
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        configuration: DeserializationConfiguration

    ):  PackageFragmentProvider {
        val deserializedPackageFragments = packageFragmentsFactory.createCachedPackageFragments(
            byteArrays, moduleDescriptor, storageManager
        )

        val provider = PackageFragmentProviderImpl(deserializedPackageFragments)
        return initializePackageFragmentProvider(provider, deserializedPackageFragments, storageManager,
            moduleDescriptor, configuration, null)
    }

    override fun createPackageFragmentProvider(
        library: KotlinLibrary,
        packageAccessHandler: PackageAccessHandler?,
        packageFragmentNames: List<String>,
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        configuration: DeserializationConfiguration,
        compositePackageFragmentAddend: PackageFragmentProvider?
    ): PackageFragmentProvider {

        val deserializedPackageFragments = packageFragmentsFactory.createDeserializedPackageFragments(
            library, packageFragmentNames, moduleDescriptor, packageAccessHandler, storageManager
        )

        // TODO: this is native specific. Move to a child class.
        val syntheticPackageFragments = packageFragmentsFactory.createSyntheticPackageFragments(
            library, deserializedPackageFragments, moduleDescriptor
        )

        val provider = PackageFragmentProviderImpl(deserializedPackageFragments + syntheticPackageFragments)
        return initializePackageFragmentProvider(provider, deserializedPackageFragments, storageManager,
            moduleDescriptor, configuration, compositePackageFragmentAddend)
    }

    fun initializePackageFragmentProvider(
        provider: PackageFragmentProviderImpl,
        fragmentsToInitialize: List<DeserializedPackageFragment>,
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        configuration: DeserializationConfiguration,
        compositePackageFragmentAddend: PackageFragmentProvider?
    ): PackageFragmentProvider {

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
            flexibleTypeDeserializer,
            emptyList(),
            notFoundClasses,
            ContractDeserializerImpl(configuration, storageManager),
            extensionRegistryLite = KlibMetadataSerializerProtocol.extensionRegistry
        )

        fragmentsToInitialize.forEach {
            it.initialize(components)
        }

        return compositePackageFragmentAddend?.let {
            CompositePackageFragmentProvider(listOf(it, provider))
        } ?: provider
    }

    fun createForwardDeclarationHackPackagePartProvider(
        storageManager: StorageManager,
        module: ModuleDescriptorImpl
    ): PackageFragmentProviderImpl {
        fun createPackage(fqName: FqName, supertypeName: String, classKind: ClassKind) =
            ForwardDeclarationsPackageFragmentDescriptor(
                storageManager,
                module,
                fqName,
                Name.identifier(supertypeName),
                classKind
            )

        val packageFragmentProvider = PackageFragmentProviderImpl(
            listOf(
                createPackage(ForwardDeclarationsFqNames.cNamesStructs, "COpaque", ClassKind.CLASS),
                createPackage(ForwardDeclarationsFqNames.objCNamesClasses, "ObjCObjectBase", ClassKind.CLASS),
                createPackage(ForwardDeclarationsFqNames.objCNamesProtocols, "ObjCObject", ClassKind.INTERFACE)
            )
        )
        return packageFragmentProvider
    }
}
