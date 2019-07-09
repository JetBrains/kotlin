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

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.lazy.LazyJavaPackageFragmentProvider
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind

class JavaDescriptorResolver(
    val packageFragmentProvider: LazyJavaPackageFragmentProvider,
    private val javaResolverCache: JavaResolverCache
) {
    fun resolveClass(javaClass: JavaClass): ClassDescriptor? {
        val fqName = javaClass.fqName
        if (fqName != null && javaClass.lightClassOriginKind == LightClassOriginKind.SOURCE) {
            return javaResolverCache.getClassResolvedFromSource(fqName)
        }

        javaClass.outerClass?.let { outerClass ->
            val outerClassScope = resolveClass(outerClass)?.unsubstitutedInnerClassesScope
            return outerClassScope?.getContributedClassifier(javaClass.name, NoLookupLocation.FROM_JAVA_LOADER) as? ClassDescriptor
        }

        if (fqName == null) return null

        return packageFragmentProvider.getPackageFragments(fqName.parent()).firstOrNull()?.findClassifierByJavaClass(javaClass)
    }
}
