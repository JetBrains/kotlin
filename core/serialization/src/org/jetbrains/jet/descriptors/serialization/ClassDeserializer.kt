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
import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.jet.descriptors.serialization.context.DeserializationGlobalContext
import kotlin.properties.Delegates

public class ClassDeserializer(val storageManager: StorageManager, val classDataFinder: ClassDataFinder) {
    private val classes = storageManager.createMemoizedFunctionWithNullableValues {
        (classId: ClassId) ->
        val classData = classDataFinder.findClassData(classId)
        if (classData != null) {
            DeserializedClassDescriptor(context, classData)
        }
        else {
            null
        }
    }

    var context: DeserializationGlobalContext by Delegates.notNull()

    public fun deserializeClass(classId: ClassId): ClassDescriptor? = classes(classId)
}