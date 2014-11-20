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

package org.jetbrains.jet.descriptors.serialization

import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.jet.lang.resolve.name.ClassId
import org.jetbrains.jet.descriptors.serialization.context.DeserializationComponents

public class ClassDeserializer(private val components: DeserializationComponents) {
    private val classes: (ClassKey) -> DeserializedClassDescriptor? = components.storageManager.createMemoizedFunctionWithNullableValues {
        (key: ClassKey) ->
        val classId = key.classId
        val classData = key.classData ?: components.classDataFinder.findClassData(classId)
        if (classData != null) {
            val outerClassContext =
                    if (classId.isTopLevelClass()) null
                    else classes(ClassKey(classId.getOuterClassId(), null))?.context
            // TODO: use outerClassContext
            DeserializedClassDescriptor(components.createContext(classData.getNameResolver()), classData.getClassProto())
        }
        else {
            null
        }
    }

    // Additional ClassData parameter is needed to avoid calling ClassDataFinder#findClassData() if it is already computed at call site
    public fun deserializeClass(classId: ClassId, classData: ClassData? = null): ClassDescriptor? = classes(ClassKey(classId, classData))

    private inner class ClassKey(val classId: ClassId, val classData: ClassData?) {
        override fun equals(other: Any?): Boolean = other is ClassKey && classId == other.classId
        override fun hashCode(): Int = classId.hashCode()
        override fun toString(): String = classId.toString()
    }
}
