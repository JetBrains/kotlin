/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata.impl

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.functionInterfacePackageFragmentProvider
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.ContractDeserializerImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.isAnyPlatformStdlib
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.resolve.CommonCompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
import org.jetbrains.kotlin.resolve.sam.SamConversionResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class KlibMetadataModuleDescriptorFactoryImpl(
    override val createBuiltIns: (StorageManager) -> KotlinBuiltIns,
    @OptIn(K1Deprecation::class)
    override val flexibleTypeDeserializer: FlexibleTypeDeserializer,
    val additionalClassPartsProvider: AdditionalClassPartsProvider = AdditionalClassPartsProvider.None,
    val fictitiousClassDescriptorFactories: List<ClassDescriptorFactory> = emptyList(),
) : KlibMetadataModuleDescriptorFactory {

    override fun createDescriptorOptionalBuiltIns(
        library: KotlinLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        lookupTracker: LookupTracker
    ): ModuleDescriptorImpl {

        val moduleName = Name.special("<${library.uniqueName}>")
        val moduleOrigin = DeserializedKlibModuleOrigin(library)
        val builtInsToUse = builtIns ?: createBuiltIns(storageManager)

        val moduleDescriptor = ModuleDescriptorImpl(
            moduleName,
            storageManager,
            builtInsToUse,
            capabilities = mapOf(
                KlibModuleOrigin.CAPABILITY to moduleOrigin,
                @OptIn(K1Deprecation::class)
                ImplicitIntegerCoercion.MODULE_CAPABILITY to moduleOrigin.isCInteropLibrary()
            ),
            platform = NativePlatforms.unspecifiedNativePlatform
        )

        if (builtIns == null) {
            builtInsToUse.builtInsModule = moduleDescriptor
        }

        val provider = createPackageFragmentProvider(
            library = library,
            storageManager = storageManager,
            moduleDescriptor = moduleDescriptor,
            configuration = CommonCompilerDeserializationConfiguration(languageVersionSettings),
            compositePackageFragmentAddend = runIf(library.isAnyPlatformStdlib) {
                functionInterfacePackageFragmentProvider(storageManager, moduleDescriptor)
            },
            lookupTracker = lookupTracker
        )

        moduleDescriptor.initialize(provider)

        return moduleDescriptor
    }

    private fun createPackageFragmentProvider(
        library: KotlinLibrary,
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        configuration: DeserializationConfiguration,
        compositePackageFragmentAddend: PackageFragmentProvider?,
        lookupTracker: LookupTracker
    ): PackageFragmentProvider {

        val deserializedPackageFragments = createDeserializedPackageFragments(
            library = library,
            moduleDescriptor = moduleDescriptor,
            storageManager = storageManager,
            configuration = configuration
        )

        // Generate empty PackageFragmentDescriptor instances for packages that aren't mentioned in compilation units directly.
        // For example, if there's `package foo.bar` directive, we'll get only PackageFragmentDescriptor for `foo.bar`, but
        // none for `foo`. Various descriptor/scope code relies on presence of such package fragments, and currently we
        // don't know if it's possible to fix this.
        // TODO: think about fixing issues in descriptors/scopes
        val packageFqNames = deserializedPackageFragments.mapTo(mutableSetOf()) { it.fqName }
        val emptyPackageFragments = mutableListOf<PackageFragmentDescriptor>()
        for (packageFqName in packageFqNames.mapNotNull { it.parentOrNull() }) {
            var ancestorFqName = packageFqName
            while (!ancestorFqName.isRoot && packageFqNames.add(ancestorFqName)) {
                emptyPackageFragments += EmptyPackageFragmentDescriptor(moduleDescriptor, ancestorFqName)
                ancestorFqName = ancestorFqName.parent()
            }
        }

        val provider = PackageFragmentProviderImpl(deserializedPackageFragments + emptyPackageFragments)
        @OptIn(K1Deprecation::class)
        return initializePackageFragmentProvider(provider, deserializedPackageFragments, storageManager,
            moduleDescriptor, configuration, compositePackageFragmentAddend, lookupTracker)
    }

    fun initializePackageFragmentProvider(
        provider: PackageFragmentProviderImpl,
        @OptIn(K1Deprecation::class)
        fragmentsToInitialize: List<DeserializedPackageFragment>,
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        configuration: DeserializationConfiguration,
        compositePackageFragmentAddend: PackageFragmentProvider?,
        lookupTracker: LookupTracker
    ): PackageFragmentProvider {

        val notFoundClasses = NotFoundClasses(storageManager, moduleDescriptor)

        @OptIn(K1Deprecation::class)
        val annotationAndConstantLoader = AnnotationAndConstantLoaderImpl(
            moduleDescriptor,
            notFoundClasses,
            KlibMetadataSerializerProtocol
        )

        @OptIn(K1Deprecation::class)
        val enumEntriesDeserializationSupport = object : EnumEntriesDeserializationSupport {
            override fun canSynthesizeEnumEntries(): Boolean = moduleDescriptor.platform.isJvm()
        }

        @OptIn(K1Deprecation::class)
        val components = DeserializationComponents(
            storageManager,
            moduleDescriptor,
            configuration,
            DeserializedClassDataFinder(provider),
            annotationAndConstantLoader,
            provider,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            lookupTracker,
            flexibleTypeDeserializer,
            fictitiousClassDescriptorFactories,
            notFoundClasses,
            ContractDeserializerImpl(configuration, storageManager),
            additionalClassPartsProvider = additionalClassPartsProvider,
            extensionRegistryLite = KlibMetadataSerializerProtocol.extensionRegistry,
            samConversionResolver = SamConversionResolverImpl(storageManager, samWithReceiverResolvers = emptyList()),
            enumEntriesDeserializationSupport = enumEntriesDeserializationSupport,
        )

        @OptIn(K1Deprecation::class)
        fragmentsToInitialize.forEach {
            it.initialize(components)
        }

        return compositePackageFragmentAddend?.let {
            CompositePackageFragmentProvider(
                listOf(it, provider),
                "CompositeProvider@KlibMetadataModuleDescriptorFactory for $moduleDescriptor"
            )
        } ?: provider
    }
}


private fun createDeserializedPackageFragments(
    library: KotlinLibrary,
    moduleDescriptor: ModuleDescriptor,
    storageManager: StorageManager,
    configuration: DeserializationConfiguration
): List<KlibMetadataPackageFragment> {
    val metadata = library.metadata
    val header = parseModuleHeader(metadata.moduleHeaderData)

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
                    metadata = metadata,
                    storageManager = storageManager,
                    module = moduleDescriptor,
                    partName = partName,
                    containerSource = containerSource,
                )
            else
                KlibMetadataDeserializedPackageFragment(
                    fqName = packageFqName,
                    metadata = metadata,
                    storageManager = storageManager,
                    module = moduleDescriptor,
                    partName = partName,
                    containerSource = containerSource,
                )
        }
    }
}
