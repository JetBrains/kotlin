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
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.singletonOrEmptyList

class JvmBuiltInsPackageFragmentProvider(
        storageManager: StorageManager,
        finder: KotlinClassFinder,
        moduleDescriptor: ModuleDescriptor,
        notFoundClasses: NotFoundClasses,
        additionalClassPartsProvider: AdditionalClassPartsProvider,
        platformDependentDeclarationFilter: PlatformDependentDeclarationFilter
) : PackageFragmentProvider {
    private lateinit var components: DeserializationComponents

    private val fragments = storageManager.createMemoizedFunctionWithNullableValues<FqName, PackageFragmentDescriptor> { fqName ->
        finder.findBuiltInsData(fqName)?.let { inputStream ->
            BuiltInsPackageFragment(fqName, storageManager, moduleDescriptor, inputStream).apply {
                components = this@JvmBuiltInsPackageFragmentProvider.components
            }
        }
    }

    init {
        components = DeserializationComponents(
                storageManager,
                moduleDescriptor,
                DeserializationConfiguration.Default,
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
                notFoundClasses, additionalClassPartsProvider, platformDependentDeclarationFilter
        )

    }

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> = fragments(fqName).singletonOrEmptyList()

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> = emptySet()
}
