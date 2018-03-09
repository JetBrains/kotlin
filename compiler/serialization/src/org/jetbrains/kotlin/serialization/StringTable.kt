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

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.ErrorUtils
import java.io.OutputStream

interface StringTable {
    fun getStringIndex(string: String): Int

    /**
     * @param className the fully qualified name of some class in the format: `org/foo/bar/Test.Inner`
     */
    fun getQualifiedClassNameIndex(className: String, isLocal: Boolean): Int

    fun serializeTo(output: OutputStream)
}

interface DescriptorAwareStringTable : StringTable {
    fun getFqNameIndex(descriptor: ClassifierDescriptorWithTypeParameters): Int {
        if (ErrorUtils.isError(descriptor)) {
            throw IllegalStateException("Cannot get FQ name of error class: $descriptor")
        }

        val classId = descriptor.classId
                ?: getLocalClassIdReplacement(descriptor)
                ?: throw IllegalStateException("Cannot get FQ name of local class: $descriptor")

        return getQualifiedClassNameIndex(classId.asString(), classId.isLocal)
    }

    fun getLocalClassIdReplacement(descriptor: ClassifierDescriptorWithTypeParameters): ClassId? = null
}
