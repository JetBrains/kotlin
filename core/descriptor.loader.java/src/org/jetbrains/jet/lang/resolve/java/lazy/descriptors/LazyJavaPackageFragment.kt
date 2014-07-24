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

package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.kotlin.util.sure
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPackageFragmentDescriptor
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassStaticsPackageFragmentDescriptor
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor
import org.jetbrains.jet.lang.descriptors.impl.PackageFragmentDescriptorImpl
import kotlin.properties.Delegates

class LazyPackageFragmentForJavaPackage(
        c: LazyJavaResolverContext,
        containingDeclaration: ModuleDescriptor,
        private val jPackage: JavaPackage
) : LazyJavaPackageFragment(c, containingDeclaration, jPackage.getFqName()) {
    override fun createMemberScope() = LazyPackageFragmentScopeForJavaPackage(c, jPackage, this)
}

class LazyPackageFragmentForJavaClass(
        c: LazyJavaResolverContext,
        containingDeclaration: ModuleDescriptor,
        private val jClass: JavaClass
) : JavaClassStaticsPackageFragmentDescriptor,
    LazyJavaPackageFragment(c, containingDeclaration,
                            jClass.getFqName().sure("Attempt to build a package of an anonymous/local class: $jClass")) {

    override fun createMemberScope() = LazyPackageFragmentScopeForJavaClass(c, jClass, this)

    override fun getCorrespondingClass(): JavaClassDescriptor {
        val classDescriptor = c.javaClassResolver.resolveClass(jClass)
        if (classDescriptor !is JavaClassDescriptor) {
            throw AssertionError("Corresponding class not found for package with static members: $this")
        }
        return classDescriptor
    }
}

abstract class LazyJavaPackageFragment(
        protected val c: LazyJavaResolverContext,
        containingDeclaration: ModuleDescriptor,
        fqName: FqName
) : PackageFragmentDescriptorImpl(containingDeclaration, fqName),
    JavaPackageFragmentDescriptor, LazyJavaDescriptor
{
    private val _memberScope by Delegates.lazy { createMemberScope() }

    abstract fun createMemberScope(): LazyJavaPackageFragmentScope

    override fun getMemberScope(): LazyJavaPackageFragmentScope = _memberScope

    override fun toString() = "lazy java package fragment: " + fqName
}