/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.descriptors.impl

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackageFragmentProviderWithSecondaryFragments
import org.jetbrains.kotlin.name.FqName
import java.util.*
import org.jetbrains.kotlin.name.Name

class CompositePackageFragmentProvider(
    private val providers: List<PackageFragmentProvider>,
    private val secondaryProviders: List<PackageFragmentProvider> = emptyList(),
) : PackageFragmentProviderWithSecondaryFragments {

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> =
        providers.collectPackageFragmentDescriptors(fqName)

    override fun getSecondaryPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> =
        secondaryProviders.collectPackageFragmentDescriptors(fqName)

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> =
        providers.collectSubPackagesOf(fqName, nameFilter)

    override fun getSecondarySubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> =
        secondaryProviders.collectSubPackagesOf(fqName, nameFilter)

    private fun Collection<PackageFragmentProvider>.collectSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        val result = HashSet<FqName>()
        for (provider in this) {
            result.addAll(provider.getSubPackagesOf(fqName, nameFilter))
        }
        return result
    }

    private fun Collection<PackageFragmentProvider>.collectPackageFragmentDescriptors(fqName: FqName): List<PackageFragmentDescriptor> {
        val result = ArrayList<PackageFragmentDescriptor>()
        for (provider in this) {
            result.addAll(provider.getPackageFragments(fqName))
        }
        return result.toList()
    }
}
