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

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.load.java.descriptors.getImplClassNameForDeserialized
import org.jetbrains.kotlin.load.java.descriptors.getImplClassNameForProto
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor


class KotlinJvmBinaryPackageSourceElement(
        private val jPackage: JavaPackage,
        kotlinBinaryClasses: List<KotlinJvmBinaryClass>
) : SourceElement {
    private val implClassNameToBinaryClass = run {
        val result = hashMapOf<String, KotlinJvmBinaryClass>()
        for (kotlinBinaryClass in kotlinBinaryClasses) {
            result[kotlinBinaryClass.classId.shortClassName.asString()] = kotlinBinaryClass
        }
        result
    }

    override fun toString(): String = "Binary package ${jPackage.getFqName()}: ${implClassNameToBinaryClass.keys}"
    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE

    public fun getRepresentativeBinaryClass(): KotlinJvmBinaryClass {
        return implClassNameToBinaryClass.values.first()
    }

    public fun getContainingBinaryClass(descriptor: DeserializedCallableMemberDescriptor): KotlinJvmBinaryClass? {
        val name = descriptor.getImplClassNameForDeserialized() ?: return null
        return implClassNameToBinaryClass[name.asString()]
    }
}