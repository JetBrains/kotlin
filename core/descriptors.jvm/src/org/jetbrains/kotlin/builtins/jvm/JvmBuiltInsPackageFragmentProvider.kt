/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.jvm

import org.jetbrains.kotlin.builtins.functions.BuiltInFictitiousFunctionClassFactory
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentDeclarationFilter
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.sam.SamConversionResolver
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInsPackageFragmentImpl
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker

class JvmBuiltInsPackageFragmentProvider(
    storageManager: StorageManager,
    finder: KotlinClassFinder,
    moduleDescriptor: ModuleDescriptor,
    notFoundClasses: NotFoundClasses,
    additionalClassPartsProvider: AdditionalClassPartsProvider,
    platformDependentDeclarationFilter: PlatformDependentDeclarationFilter,
    deserializationConfiguration: DeserializationConfiguration,
    kotlinTypeChecker: NewKotlinTypeChecker,
    samConversionResolver: SamConversionResolver
) : AbstractDeserializedPackageFragmentProvider(storageManager, finder, moduleDescriptor) {
    init {
        components = DeserializationComponents(
            storageManager,
            moduleDescriptor,
            deserializationConfiguration,
            DeserializedClassDataFinder(this),
            AnnotationAndConstantLoaderImpl(moduleDescriptor, notFoundClasses, BuiltInSerializerProtocol),
            this,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING,
            FlexibleTypeDeserializer.ThrowException,
            listOf(
                BuiltInFictitiousFunctionClassFactory(storageManager, moduleDescriptor),
                JvmBuiltInClassDescriptorFactory(storageManager, moduleDescriptor)
            ),
            notFoundClasses,
            ContractDeserializer.DEFAULT,
            additionalClassPartsProvider, platformDependentDeclarationFilter,
            BuiltInSerializerProtocol.extensionRegistry,
            kotlinTypeChecker,
            samConversionResolver
        )
    }

    override fun findPackage(fqName: FqName): DeserializedPackageFragment? =
        finder.findBuiltInsData(fqName)?.let { inputStream ->
            BuiltInsPackageFragmentImpl.create(fqName, storageManager, moduleDescriptor, inputStream, isFallback = false)
        }

    companion object {
        const val DOT_BUILTINS_METADATA_FILE_EXTENSION = ".kotlin_builtins"
    }
}
