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

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackageFragmentProviderImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.StorageManager
import java.io.InputStream

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
        BuiltInsPackageFragment(fqName, storageManager, module, inputStream)
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
            additionalClassPartsProvider = additionalClassPartsProvider,
            platformDependentDeclarationFilter = platformDependentDeclarationFilter
    )

    for (packageFragment in packageFragments) {
        packageFragment.components = components
    }

    return provider
}
