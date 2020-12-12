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

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProviderOptimized
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.addIfNotNull

abstract class AbstractDeserializedPackageFragmentProvider(
    protected val storageManager: StorageManager,
    protected val finder: KotlinMetadataFinder,
    protected val moduleDescriptor: ModuleDescriptor
) : PackageFragmentProviderOptimized {
    protected lateinit var components: DeserializationComponents

    private val fragments = storageManager.createMemoizedFunctionWithNullableValues<FqName, PackageFragmentDescriptor> { fqName ->
        findPackage(fqName)?.apply {
            initialize(components)
        }
    }

    protected abstract fun findPackage(fqName: FqName): DeserializedPackageFragment?

    override fun collectPackageFragments(fqName: FqName, packageFragments: MutableCollection<PackageFragmentDescriptor>) {
        packageFragments.addIfNotNull(fragments(fqName))
    }

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> = listOfNotNull(fragments(fqName))

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> = emptySet()
}
