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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.ClassDataProvider
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil

public class JavaClassDataFinder(
        private val kotlinClassFinder: KotlinClassFinder,
        private val deserializedDescriptorResolver: DeserializedDescriptorResolver
) : ClassDataFinder {
    override fun findClassData(classId: ClassId): ClassDataProvider? {
        val kotlinJvmBinaryClass = kotlinClassFinder.findKotlinClass(classId) ?: return null
        assert(kotlinJvmBinaryClass.getClassId() == classId) {
            "Class with incorrect id found: expected $classId, actual ${kotlinJvmBinaryClass.getClassId()}"
        }
        val data = deserializedDescriptorResolver.readData(kotlinJvmBinaryClass, KotlinClassHeader.Kind.CLASS) ?: return null
        val classData = JvmProtoBufUtil.readClassDataFrom(data)
        return ClassDataProvider(classData, KotlinJvmBinarySourceElement(kotlinJvmBinaryClass))
    }
}
