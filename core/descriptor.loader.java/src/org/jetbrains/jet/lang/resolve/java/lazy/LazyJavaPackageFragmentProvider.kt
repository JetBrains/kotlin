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
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyPackageFragmentForJavaPackage
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyPackageFragmentForJavaClass
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils
import org.jetbrains.jet.lang.resolve.java.resolver.JavaPackageFragmentProvider
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaClassDescriptor

public class LazyJavaPackageFragmentProvider(
        outerContext: GlobalJavaResolverContext,
        private val _module: ModuleDescriptor
) : JavaPackageFragmentProvider {

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
            outerContext.sourceElementFactory
    )

    override fun getModule() = _module

    private val _packageFragments: MemoizedFunctionToNullable<FqName, LazyJavaPackageFragment> = c.storageManager.createMemoizedFunctionWithNullableValues {
        fqName ->
        val jPackage = c.finder.findPackage(fqName)
        if (jPackage != null) {
            LazyPackageFragmentForJavaPackage(c, _module, jPackage)
        }
        else {
            val jClass = c.findJavaClass(fqName)
            if (jClass != null) {
                packageFragmentsForClasses(jClass)
            }
            else null
        }
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

    private val packageFragmentsForClasses: MemoizedFunctionToNullable<JavaClass, LazyPackageFragmentForJavaClass> = c.storageManager.createMemoizedFunctionWithNullableValues {
        jClass ->
        if (DescriptorResolverUtils.hasStaticMembers(jClass)) {
            val correspondingClass = c.javaClassResolver.resolveClass(jClass)
            if (correspondingClass != null) LazyPackageFragmentForJavaClass(c, _module, jClass) else null
        }
        else null
    }

    override fun getPackageFragment(fqName: FqName) = _packageFragments(fqName)
    fun getPackageFragment(javaClass: JavaClass) = packageFragmentsForClasses(javaClass)

    override fun getPackageFragments(fqName: FqName) = getPackageFragment(fqName)?.let {listOf(it)}.orEmpty()

    override fun getSubPackagesOf(fqName: FqName) = getPackageFragment(fqName)?.getMemberScope()?.getSubPackages().orEmpty()

    fun resolveKotlinBinaryClass(kotlinClass: KotlinJvmBinaryClass) = c.deserializedDescriptorResolver.resolveClass(kotlinClass)

    fun getClass(javaClass: JavaClass): ClassDescriptor? = c.javaClassResolver.resolveClass(javaClass)

    private inner class FragmentClassResolver : LazyJavaClassResolver {
        override fun resolveClass(javaClass: JavaClass): ClassDescriptor? {
            // TODO: there's no notion of module separation here. We must refuse to resolve classes from other modules
            val fqName = javaClass.getFqName()
            if (fqName != null) {
                // TODO: this should be handled by module separation logic
                val builtinClass = DescriptorResolverUtils.getKotlinBuiltinClassDescriptor(fqName)
                if (builtinClass != null) return builtinClass

                if (javaClass.getOriginKind() == JavaClass.OriginKind.KOTLIN_LIGHT_CLASS) {
                    return c.javaResolverCache.getClassResolvedFromSource(fqName)
                }
            }
            val outerClass = javaClass.getOuterClass()
            if (outerClass == null) {
                return c.lookupBinaryClass(javaClass) ?: topLevelClasses(javaClass)
            }
            val outerClassScope = resolveClass(outerClass)?.getUnsubstitutedInnerClassesScope()
            val nestedClass = outerClassScope?.getClassifier(javaClass.getName()) as? ClassDescriptor
            return nestedClass ?: c.javaResolverCache.getClass(javaClass)
        }
    }
}