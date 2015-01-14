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

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.ClassData

public class ClassDeserializer(private val components: DeserializationComponents) {
    private val classes: (ClassKey) -> DeserializedClassDescriptor? =
            components.storageManager.createMemoizedFunctionWithNullableValues { key -> createClass(key) }

    // Additional ClassData parameter is needed to avoid calling ClassDataFinder#findClassData() if it is already computed at call site
    public fun deserializeClass(classId: ClassId, classData: ClassData? = null): DeserializedClassDescriptor? =
            classes(ClassKey(classId, classData))

    private fun createClass(key: ClassKey): DeserializedClassDescriptor? {
        val classId = key.classId
        val classData = key.classData ?: components.classDataFinder.findClassData(classId) ?: return null
        val outerContext = if (classId.isTopLevelClass()) {
            val fragments = components.packageFragmentProvider.getPackageFragments(classId.getPackageFqName())
            assert(fragments.size() == 1) { "There should be exactly one package: $fragments, class id is $classId" }
            components.createContext(fragments.single(), classData.getNameResolver())
        }
        else {
            deserializeClass(classId.getOuterClassId())?.c ?: return null
        }

        return DeserializedClassDescriptor(outerContext, classData.getClassProto(), classData.getNameResolver())
    }

    private inner class ClassKey(val classId: ClassId, val classData: ClassData?) {
        override fun equals(other: Any?): Boolean = other is ClassKey && classId == other.classId
        override fun hashCode(): Int = classId.hashCode()
        override fun toString(): String = classId.toString()
    }
}
