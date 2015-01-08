/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.lazy

import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.storage.MemoizedFunctionToNullable
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.*
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.kotlin.utils.emptyOrSingletonList

public class LazyJavaPackageFragmentProvider(
        outerContext: GlobalJavaResolverContext,
        val module: ModuleDescriptor
) : PackageFragmentProvider {

    private val c = LazyJavaResolverContext(outerContext, this, FragmentClassResolver(), TypeParameterResolver.EMPTY)

    private val packageFragments: MemoizedFunctionToNullable<FqName, LazyJavaPackageFragment> =
            c.storageManager.createMemoizedFunctionWithNullableValues {
                fqName ->
                val jPackage = c.finder.findPackage(fqName)
                if (jPackage != null) {
                    LazyJavaPackageFragment(c, jPackage)
                }
                else null
            }

    private val topLevelClasses = c.storageManager.createMemoizedFunctionWithNullableValues @lambda {
        (jClass: JavaClass): LazyJavaClassDescriptor? ->
        val fqName = jClass.getFqName()
        if (fqName == null) return@lambda null

        val packageFragment = getPackageFragment(fqName.parent())
        if (packageFragment == null) return@lambda null

        LazyJavaClassDescriptor(c, packageFragment, fqName, jClass)
    }

    private fun getPackageFragment(fqName: FqName) = packageFragments(fqName)

    override fun getPackageFragments(fqName: FqName) = emptyOrSingletonList(getPackageFragment(fqName))

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean) =
            getPackageFragment(fqName)?.getMemberScope()?.getSubPackages().orEmpty()

    fun getClass(javaClass: JavaClass): ClassDescriptor? = c.javaClassResolver.resolveClass(javaClass)

    private inner class FragmentClassResolver : LazyJavaClassResolver {
        override fun resolveClass(javaClass: JavaClass): ClassDescriptor? {
            val fqName = javaClass.getFqName()
            if (fqName != null) {
                if (javaClass.getOriginKind() == JavaClass.OriginKind.KOTLIN_LIGHT_CLASS) {
                    return c.javaResolverCache.getClassResolvedFromSource(fqName)
                }
            }
            val outerClass = javaClass.getOuterClass()
            if (outerClass == null) {
                return c.lookupBinaryClass(javaClass) ?: topLevelClasses(javaClass)
            }
            val outerClassScope = resolveClass(outerClass)?.getUnsubstitutedInnerClassesScope()
            return outerClassScope?.getClassifier(javaClass.getName()) as? ClassDescriptor
        }
    }
}
