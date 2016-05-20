/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.resolve.descriptorUtil.hasOnlyInputTypesAnnotation
import org.jetbrains.kotlin.types.KotlinType

class TypeVariable(
        val call: CallHandle,
        internal val freshTypeParameter: TypeParameterDescriptor,
        val originalTypeParameter: TypeParameterDescriptor,
        val isExternal: Boolean
) {
    val name: Name get() = originalTypeParameter.name

    val type: KotlinType get() = freshTypeParameter.defaultType

    fun hasOnlyInputTypesAnnotation(): Boolean =
            originalTypeParameter.hasOnlyInputTypesAnnotation()
}

interface CallHandle {
    object NONE : CallHandle
}

class CallBasedCallHandle(val call: Call): CallHandle {
    override fun equals(other: Any?) =
            other is CallBasedCallHandle && call === other.call

    override fun hashCode() =
            System.identityHashCode(call)

    override fun toString() =
            call.toString()
}

fun Call.toHandle(): CallHandle = CallBasedCallHandle(this)
