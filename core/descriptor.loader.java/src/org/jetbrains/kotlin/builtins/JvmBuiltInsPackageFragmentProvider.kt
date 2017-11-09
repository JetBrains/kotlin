/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.builtins.functions.BuiltInFictitiousFunctionClassFactory
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentDeclarationFilter
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.StorageManager

class JvmBuiltInsPackageFragmentProvider(
        storageManager: StorageManager,
        finder: KotlinClassFinder,
        moduleDescriptor: ModuleDescriptor,
        notFoundClasses: NotFoundClasses,
        additionalClassPartsProvider: AdditionalClassPartsProvider,
        platformDependentDeclarationFilter: PlatformDependentDeclarationFilter
) : AbstractDeserializedPackageFragmentProvider(storageManager, finder, moduleDescriptor) {
    init {
        components = DeserializationComponents(
                storageManager,
                moduleDescriptor,
                DeserializationConfiguration.Default, // TODO
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
                additionalClassPartsProvider, platformDependentDeclarationFilter
        )
    }

    override fun findPackage(fqName: FqName): DeserializedPackageFragment? =
            finder.findBuiltInsData(fqName)?.let { inputStream ->
                BuiltInsPackageFragmentImpl(fqName, storageManager, moduleDescriptor, inputStream)
            }
}
