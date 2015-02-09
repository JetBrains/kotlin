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

package org.jetbrains.kotlin.load.java.components

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaElement
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils
import org.jetbrains.kotlin.name.FqName
import javax.inject.Inject
import kotlin.properties.Delegates
import org.jetbrains.kotlin.name.tail

public class LazyResolveBasedCache() : JavaResolverCache {
    private var resolveSession by Delegates.notNull<ResolveSession>()
    private val traceBasedCache = TraceBasedJavaResolverCache()

    Inject
    public fun setSession(resolveSession: ResolveSession) {
        this.resolveSession = resolveSession
        traceBasedCache.setTrace(this.resolveSession.getTrace())
    }

    override fun getClassResolvedFromSource(fqName: FqName): ClassDescriptor? {
        return traceBasedCache.getClassResolvedFromSource(fqName) ?: findInPackageFragments(fqName)
    }

    override fun recordMethod(method: JavaMethod, descriptor: SimpleFunctionDescriptor) {
        traceBasedCache.recordMethod(method, descriptor)
    }

    override fun recordConstructor(element: JavaElement, descriptor: ConstructorDescriptor) {
        traceBasedCache.recordConstructor(element, descriptor)
    }

    override fun recordField(field: JavaField, descriptor: PropertyDescriptor) {
        traceBasedCache.recordField(field, descriptor)
    }

    override fun recordClass(javaClass: JavaClass, descriptor: ClassDescriptor) {
        traceBasedCache.recordClass(javaClass, descriptor)
    }

    private fun findInPackageFragments(fullFqName: FqName): ClassDescriptor? {
        var fqName = if (fullFqName.isRoot()) fullFqName else fullFqName.parent()

        while (true) {
            val packageDescriptor = resolveSession.getPackageFragment(fqName)
            if (packageDescriptor == null) break

            val result = ResolveSessionUtils.findByQualifiedName(packageDescriptor.getMemberScope(), fullFqName.tail(fqName))
            if (result != null) return result

            if (fqName.isRoot()) break
            fqName = fqName.parent()
        }

        return null
    }
}
