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

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.resolve.scopes.DecapitalizedAnnotationScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.storage.get

class LazyJavaPackageFragment(
        private val c: LazyJavaResolverContext,
        private val jPackage: JavaPackage
) : PackageFragmentDescriptorImpl(c.module, jPackage.getFqName()) {
    val scope: LazyJavaPackageScope by lazy { LazyJavaPackageScope(c, jPackage, this) }
    // Just a temporary hack to inject deprecated decapitalized annotation
    private val wrappedScope by lazy { DecapitalizedAnnotationScope.wrapIfNeeded(scope, jPackage.getFqName()) }

    private val topLevelClasses = c.storageManager.createMemoizedFunctionWithNullableValues {
        javaClass: JavaClass ->
        LazyJavaClassDescriptor(c, this, javaClass.fqName!!, javaClass)
    }

    internal val oldPackageFacade by c.storageManager.createNullableLazyValue {
        c.components.kotlinClassFinder.findKotlinClass(PackageClassUtils.getPackageClassId(fqName))
    }

    internal fun resolveTopLevelClass(javaClass: JavaClass) = topLevelClasses(javaClass)

    override fun getMemberScope(): JetScope = wrappedScope

    override fun toString() = "lazy java package fragment: $fqName"

    override fun getSource(): SourceElement {
        return KotlinJvmBinarySourceElement(oldPackageFacade ?: return SourceElement.NO_SOURCE)
    }
}
