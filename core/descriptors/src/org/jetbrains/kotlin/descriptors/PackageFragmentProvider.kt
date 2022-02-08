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

interface PackageFragmentProvider {
    @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
    fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor>

    fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName>

    object Empty : PackageFragmentProvider {
        @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
        override fun getPackageFragments(fqName: FqName) = emptyList<PackageFragmentDescriptor>()

        override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean) = emptySet<FqName>()
    }
}

interface PackageFragmentProviderOptimized : PackageFragmentProvider {
    fun collectPackageFragments(fqName: FqName, packageFragments: MutableCollection<PackageFragmentDescriptor>)
    fun isEmpty(fqName: FqName): Boolean
}

fun PackageFragmentProvider.packageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
    val packageFragments = mutableListOf<PackageFragmentDescriptor>()
    collectPackageFragmentsOptimizedIfPossible(fqName, packageFragments)
    return packageFragments
}

fun PackageFragmentProvider.isEmpty(fqName: FqName): Boolean {
    return when (this) {
        is PackageFragmentProviderOptimized -> isEmpty(fqName)
        else -> packageFragments(fqName).isEmpty()
    }
}

fun PackageFragmentProvider.collectPackageFragmentsOptimizedIfPossible(
    fqName: FqName,
    packageFragments: MutableCollection<PackageFragmentDescriptor>
) {
    when (this) {
        is PackageFragmentProviderOptimized -> collectPackageFragments(fqName, packageFragments)
        else -> packageFragments.addAll(@Suppress("DEPRECATION") getPackageFragments(fqName))
    }
}
