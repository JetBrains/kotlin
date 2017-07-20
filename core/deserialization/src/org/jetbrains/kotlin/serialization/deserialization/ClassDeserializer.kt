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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.ClassDataWithSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.SinceKotlinInfoTable

class ClassDeserializer(private val components: DeserializationComponents) {
    private val classes: (ClassKey) -> ClassDescriptor? =
            components.storageManager.createMemoizedFunctionWithNullableValues { key -> createClass(key) }

    // Additional ClassDataWithSource parameter is needed to avoid calling ClassDataFinder#findClassData()
    // if it is already computed at the call site
    fun deserializeClass(classId: ClassId, classDataWithSource: ClassDataWithSource? = null): ClassDescriptor? =
            classes(ClassKey(classId, classDataWithSource))

    private fun createClass(key: ClassKey): ClassDescriptor? {
        val classId = key.classId
        for (factory in components.fictitiousClassDescriptorFactories) {
            factory.createClass(classId)?.let { return it }
        }
        if (classId in BLACK_LIST) return null

        val (classData, sourceElement) = key.classDataWithSource
                                         ?: components.classDataFinder.findClassData(classId)
                                         ?: return null
        val (nameResolver, classProto) = classData

        val outerClassId = classId.outerClassId
        val outerContext = if (outerClassId != null) {
            val outerClass = deserializeClass(outerClassId) as? DeserializedClassDescriptor ?: return null

            // Find the outer class first and check if he knows anything about the nested class we're looking for
            if (!outerClass.hasNestedClass(classId.shortClassName)) return null

            outerClass.c
        }
        else {
            val fragments = components.packageFragmentProvider.getPackageFragments(classId.packageFqName)
            val fragment = fragments.firstOrNull { it !is DeserializedPackageFragment || it.hasTopLevelClass(classId.shortClassName) }
                           ?: return null

            components.createContext(
                    fragment, nameResolver,
                    TypeTable(classProto.typeTable),
                    SinceKotlinInfoTable.create(classProto.sinceKotlinInfoTable),
                    containerSource = null
            )
        }

        return DeserializedClassDescriptor(outerContext, classProto, nameResolver, sourceElement)
    }

    private class ClassKey(val classId: ClassId, val classDataWithSource: ClassDataWithSource?) {
        // classDataWithSource *intentionally* not used in equals() / hashCode()
        override fun equals(other: Any?) = other is ClassKey && classId == other.classId

        override fun hashCode() = classId.hashCode()
    }

    companion object {
        /**
         * FQ names of classes that should be ignored during deserialization.
         *
         * We ignore kotlin.Cloneable because since Kotlin 1.1, the descriptor for it is created via JvmBuiltInClassDescriptorFactory,
         * but the metadata is still serialized for kotlin-reflect 1.0 to work (see BuiltInsSerializer.kt).
         */
        val BLACK_LIST = setOf(
                ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.cloneable.toSafe())
        )
    }
}
