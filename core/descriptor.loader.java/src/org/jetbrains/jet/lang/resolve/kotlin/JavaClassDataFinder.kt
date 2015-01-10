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

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder
import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader

public class JavaClassDataFinder(
        private val kotlinClassFinder: KotlinClassFinder,
        private val deserializedDescriptorResolver: DeserializedDescriptorResolver
) : ClassDataFinder {
    override fun findClassData(classId: ClassId): ClassData? {
        val javaClassId = DeserializedResolverUtils.kotlinClassIdToJavaClassId(classId)
        val kotlinJvmBinaryClass = kotlinClassFinder.findKotlinClass(javaClassId) ?: return null
        val data = deserializedDescriptorResolver.readData(kotlinJvmBinaryClass, KotlinClassHeader.Kind.CLASS) ?: return null
        return JvmProtoBufUtil.readClassDataFrom(data)
    }
}
