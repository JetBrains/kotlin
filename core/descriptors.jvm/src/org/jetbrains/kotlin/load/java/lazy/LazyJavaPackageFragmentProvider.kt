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

import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.CacheWithNotNullValues

class LazyJavaPackageFragmentProvider(
    components: JavaResolverComponents
) : PackageFragmentProvider {

    private val c = LazyJavaResolverContext(components, TypeParameterResolver.EMPTY, lazyOf(null))

    private val packageFragments: CacheWithNotNullValues<FqName, LazyJavaPackageFragment> =
        c.storageManager.createCacheWithNotNullValues()

    private fun getPackageFragment(fqName: FqName): LazyJavaPackageFragment? {
        val jPackage = c.components.finder.findPackage(fqName) ?: return null

        return packageFragments.computeIfAbsent(fqName) {
            LazyJavaPackageFragment(c, jPackage)
        }
    }

    override fun getPackageFragments(fqName: FqName) = listOfNotNull(getPackageFragment(fqName))

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean) =
        getPackageFragment(fqName)?.getSubPackageFqNames().orEmpty()
}
