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

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class PackageFragmentProviderImpl private constructor(
    private val packageFragmentsByFqName: HashMap<FqName, List<PackageFragmentDescriptor>>
) : PackageFragmentProviderOptimized {
    constructor(packageFragments: Collection<PackageFragmentDescriptor>) : this(
        packageFragmentsByFqName = packageFragments.groupBy { it.fqName } as HashMap<FqName, List<PackageFragmentDescriptor>>
    )

    override fun collectPackageFragments(fqName: FqName, packageFragments: MutableCollection<PackageFragmentDescriptor>) {
        packageFragments.addAll(packageFragmentsByFqName.getOrDefault(fqName, listOf()))
    }

    override fun isEmpty(fqName: FqName): Boolean =
        packageFragmentsByFqName.getOrDefault(fqName, null) != null

    @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> =
        packageFragmentsByFqName.getOrDefault(fqName, emptyList())

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> =
        packageFragmentsByFqName.values
            .flatMap { collection -> collection.map { it.fqName } }
            .filter { !it.isRoot && it.parent() == fqName }
            .toList()
}
