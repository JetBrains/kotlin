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

package org.jetbrains.jet.lang.resolve.kotlin

import org.jetbrains.jet.descriptors.serialization.ClassId
import org.jetbrains.jet.descriptors.serialization.ClassDataFinder
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.java.resolver.JavaPackageFragmentProvider
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils.kotlinFqNameToJavaFqName
import java.util.Collections
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaPackageFragmentProvider
import org.jetbrains.jet.descriptors.serialization.ClassData
import org.jetbrains.jet.descriptors.serialization.JavaProtoBufUtil
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader

//TODO: correct dependencies
public class JavaClassDataFinder(
        private val javaDescriptorResolver: JavaDescriptorResolver,
        private val javaPackageFragmentProvider: JavaPackageFragmentProvider
) : ClassDataFinder {

    override fun findClassData(classId: ClassId): ClassData? {
        val lazyJavaPackageFragmentProvider = javaDescriptorResolver.getPackageFragmentProvider() as LazyJavaPackageFragmentProvider
        val c = lazyJavaPackageFragmentProvider.c
        val kotlinJvmBinaryClass = c.kotlinClassFinder.findKotlinClass(kotlinFqNameToJavaFqName(classId.asSingleFqName())) ?: return null
        val deserializedDescriptorResolver = c.deserializedDescriptorResolver
        val data = deserializedDescriptorResolver.readData(kotlinJvmBinaryClass, KotlinClassHeader.Kind.CLASS) ?: return null
        return JavaProtoBufUtil.readClassDataFrom(data)
    }

    override fun getClassNames(packageName: FqName): Collection<Name> {
        return Collections.emptyList()
    }
}
