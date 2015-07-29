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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor

public class ClassDeserializer(private val components: DeserializationComponents) {
    private val classes: (ClassKey) -> ClassDescriptor? =
            components.storageManager.createMemoizedFunctionWithNullableValues { key -> createClass(key) }

    // Additional ClassData parameter is needed to avoid calling ClassDataFinder#findClassData() if it is already computed at call site
    public fun deserializeClass(classId: ClassId, classData: ClassData? = null): ClassDescriptor? =
            classes(ClassKey(classId, classData))

    private fun createClass(key: ClassKey): ClassDescriptor? {
        val classId = key.classId
        components.fictitiousClassDescriptorFactory.createClass(classId)?.let { return it }

        var classData = key.classData ?: run {
            val classDataProvider = components.classDataFinder.findClassData(classId)
            classDataProvider?.classData
        } ?: return null

        val outerContext = if (classId.isNestedClass()) {
            val outerClass = deserializeClass(classId.getOuterClassId()) as? DeserializedClassDescriptor ?: return null

            // Find the outer class first and check if he knows anything about the nested class we're looking for
            if (!outerClass.hasNestedClass(classId.getShortClassName())) return null

            outerClass.c
        }
        else {
            val fragments = components.packageFragmentProvider.getPackageFragments(classId.getPackageFqName())
            assert(fragments.size() == 1) { "There should be exactly one package: $fragments, class id is $classId" }

            val fragment = fragments.single()
            if (fragment is DeserializedPackageFragment) {
                // Similarly, verify that the containing package has information about this class
                if (!fragment.hasTopLevelClass(classId.getShortClassName())) return null
            }

            components.createContext(fragment, classData.getNameResolver())
        }

        return DeserializedClassDescriptor(outerContext, classData.getClassProto(), classData.getNameResolver())
    }

    private data class ClassKey(val classId: ClassId, classData: ClassData?) {
        // This property is not declared in the constructor because it shouldn't participate in equals/hashCode
        val classData: ClassData? = classData
    }
}
