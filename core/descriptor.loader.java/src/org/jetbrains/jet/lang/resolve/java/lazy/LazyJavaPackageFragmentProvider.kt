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
import org.jetbrains.jet.storage.MemoizedFunctionToNullable
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.*
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider

public class LazyJavaPackageFragmentProvider(
        outerContext: GlobalJavaResolverContext,
        val module: ModuleDescriptor
) : PackageFragmentProvider {

    private val c = LazyJavaResolverContext(
            this,
            FragmentClassResolver(),
            outerContext.storageManager,
            outerContext.finder,
            outerContext.kotlinClassFinder,
            outerContext.deserializedDescriptorResolver,
            outerContext.externalAnnotationResolver,
            outerContext.externalSignatureResolver,
            outerContext.errorReporter,
            outerContext.methodSignatureChecker,
            outerContext.javaResolverCache,
            outerContext.javaPropertyInitializerEvaluator,
            outerContext.sourceElementFactory,
            outerContext.moduleClassResolver
    )

    private val _packageFragments: MemoizedFunctionToNullable<FqName, LazyJavaPackageFragment> =
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

        LazyJavaClassDescriptor(
                c.withTypes(TypeParameterResolver.EMPTY),
                packageFragment,
                fqName,
                jClass
        )
    }

    fun getPackageFragment(fqName: FqName) = _packageFragments(fqName)

    override fun getPackageFragments(fqName: FqName) = getPackageFragment(fqName)?.let {listOf(it)}.orEmpty()

    override fun getSubPackagesOf(fqName: FqName) = getPackageFragment(fqName)?.getMemberScope()?.getSubPackages().orEmpty()

    fun resolveKotlinBinaryClass(kotlinClass: KotlinJvmBinaryClass) = c.deserializedDescriptorResolver.resolveClass(kotlinClass)

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
