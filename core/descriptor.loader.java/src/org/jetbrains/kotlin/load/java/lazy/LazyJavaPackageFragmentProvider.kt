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

import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.MemoizedFunctionToNullable
import org.jetbrains.kotlin.utils.emptyOrSingletonList

public class LazyJavaPackageFragmentProvider(
        outerContext: GlobalJavaResolverContext,
        module: ModuleDescriptor,
        reflectionTypes: ReflectionTypes
) : PackageFragmentProvider {

    private val c =
            LazyJavaResolverContext(outerContext, this, FragmentClassResolver(), module, reflectionTypes, TypeParameterResolver.EMPTY)

    private val packageFragments: MemoizedFunctionToNullable<FqName, LazyJavaPackageFragment> =
            c.storageManager.createMemoizedFunctionWithNullableValues {
                fqName ->
                val jPackage = c.finder.findPackage(fqName)
                if (jPackage != null) {
                    LazyJavaPackageFragment(c, jPackage)
                }
                else null
            }

    private fun getPackageFragment(fqName: FqName) = packageFragments(fqName)

    override fun getPackageFragments(fqName: FqName) = emptyOrSingletonList(getPackageFragment(fqName))

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean) =
            getPackageFragment(fqName)?.getMemberScope()?.getSubPackages().orEmpty()

    fun getClass(javaClass: JavaClass): ClassDescriptor? = c.javaClassResolver.resolveClass(javaClass)

    private inner class FragmentClassResolver : LazyJavaClassResolver {
        override fun resolveClass(javaClass: JavaClass): ClassDescriptor? {
            val fqName = javaClass.fqName
            if (fqName != null && javaClass.originKind == JavaClass.OriginKind.KOTLIN_LIGHT_CLASS) {
                return c.javaResolverCache.getClassResolvedFromSource(fqName)
            }

            javaClass.outerClass?.let { outerClass ->
                val outerClassScope = resolveClass(outerClass)?.unsubstitutedInnerClassesScope
                return outerClassScope?.getClassifier(javaClass.name) as? ClassDescriptor
            }

            val kotlinResult = c.resolveKotlinBinaryClass(c.kotlinClassFinder.findKotlinClass(javaClass))
            if (kotlinResult is KotlinClassLookupResult.Found) return kotlinResult.descriptor

            if (fqName == null) return null

            return getPackageFragment(fqName.parent())?.resolveTopLevelClass(javaClass)
        }
    }
}
