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

package org.jetbrains.kotlin.load.java.lazy

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProviderOptimized
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.CacheWithNotNullValues
import org.jetbrains.kotlin.utils.addIfNotNull

class LazyJavaPackageFragmentProvider(
    components: JavaResolverComponents
) : PackageFragmentProviderOptimized {

    private val c = LazyJavaResolverContext(components, TypeParameterResolver.EMPTY, lazyOf(null))

    private val packageFragments: CacheWithNotNullValues<FqName, LazyJavaPackageFragment> =
        c.storageManager.createCacheWithNotNullValues()

    private fun getPackageFragment(fqName: FqName): LazyJavaPackageFragment? {
        val jPackage = c.components.finder.findPackage(fqName) ?: return null

        return packageFragments.computeIfAbsent(fqName) {
            LazyJavaPackageFragment(c, jPackage)
        }
    }

    @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
    override fun getPackageFragments(fqName: FqName) = listOfNotNull(getPackageFragment(fqName))

    override fun collectPackageFragments(fqName: FqName, packageFragments: MutableCollection<PackageFragmentDescriptor>) =
        packageFragments.addIfNotNull(getPackageFragment(fqName))

    override fun isEmpty(fqName: FqName): Boolean {
        return c.components.finder.findPackage(fqName) == null
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean) =
        getPackageFragment(fqName)?.getSubPackageFqNames().orEmpty()

    override fun toString(): String = "LazyJavaPackageFragmentProvider of module ${c.components.module}"
}
