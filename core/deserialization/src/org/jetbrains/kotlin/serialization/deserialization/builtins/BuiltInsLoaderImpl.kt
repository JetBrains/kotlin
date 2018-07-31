/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization.builtins

import org.jetbrains.kotlin.builtins.BuiltInsLoader
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackageFragmentProviderImpl
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentDeclarationFilter
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.StorageManager
import java.io.InputStream

class BuiltInsLoaderImpl : BuiltInsLoader {
    private val resourceLoader = BuiltInsResourceLoader()

    override fun createPackageFragmentProvider(
        storageManager: StorageManager,
        builtInsModule: ModuleDescriptor,
        classDescriptorFactories: Iterable<ClassDescriptorFactory>,
        platformDependentDeclarationFilter: PlatformDependentDeclarationFilter,
        additionalClassPartsProvider: AdditionalClassPartsProvider
    ): PackageFragmentProvider {
        return createBuiltInPackageFragmentProvider(
            storageManager,
            builtInsModule,
            KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAMES,
            classDescriptorFactories,
            platformDependentDeclarationFilter,
            additionalClassPartsProvider,
            resourceLoader::loadResource
        )
    }

    fun createBuiltInPackageFragmentProvider(
        storageManager: StorageManager,
        module: ModuleDescriptor,
        packageFqNames: Set<FqName>,
        classDescriptorFactories: Iterable<ClassDescriptorFactory>,
        platformDependentDeclarationFilter: PlatformDependentDeclarationFilter,
        additionalClassPartsProvider: AdditionalClassPartsProvider = AdditionalClassPartsProvider.None,
        loadResource: (String) -> InputStream?
    ): PackageFragmentProvider {
        val packageFragments = packageFqNames.map { fqName ->
            val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(fqName)
            val inputStream = loadResource(resourcePath) ?: throw IllegalStateException("Resource not found in classpath: $resourcePath")
            BuiltInsPackageFragmentImpl.create(fqName, storageManager, module, inputStream)
        }
        val provider = PackageFragmentProviderImpl(packageFragments)

        val notFoundClasses = NotFoundClasses(storageManager, module)

        val components = DeserializationComponents(
            storageManager,
            module,
            DeserializationConfiguration.Default,
            DeserializedClassDataFinder(provider),
            AnnotationAndConstantLoaderImpl(module, notFoundClasses, BuiltInSerializerProtocol),
            provider,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING,
            FlexibleTypeDeserializer.ThrowException,
            classDescriptorFactories,
            notFoundClasses,
            ContractDeserializer.DEFAULT,
            additionalClassPartsProvider,
            platformDependentDeclarationFilter,
            BuiltInSerializerProtocol.extensionRegistry
        )

        for (packageFragment in packageFragments) {
            packageFragment.initialize(components)
        }

        return provider
    }
}
