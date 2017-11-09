/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.common.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType

fun ClassDescriptor?.getter2Descriptor(methodName: Name) = this?.let {
    this.unsubstitutedMemberScope.getContributedDescriptors{true}
            .firstOrNull {
                it.name == methodName
            } ?.let {
        return@let (it as? PropertyDescriptor)?.getter
    }
}

fun ClassDescriptor?.signature2Descriptor(methodName: Name, signature:Array<KotlinType> = emptyArray()) = this?.let {
    this
            .unsubstitutedMemberScope
            .getContributedFunctions(methodName, NoLookupLocation.FROM_BACKEND)
            .firstOrNull {
                return@firstOrNull it.valueParameters.size == signature.size
                        && (signature.isEmpty() || it.valueParameters.any {
                    p -> val index = it.valueParameters.indexOf(p)
                    return@any p.type == signature[index]
        })
    }
}

val String.synthesizedName get() = Name.identifier(this.synthesizedString)

val String.synthesizedString get() = "\$$this"

val DeclarationDescriptor.propertyIfAccessor
    get() = if (this is PropertyAccessorDescriptor)
                this.correspondingProperty
                else this

val CallableMemberDescriptor.propertyIfAccessor
    get() = if (this is PropertyAccessorDescriptor)
                this.correspondingProperty
                else this

val FunctionDescriptor.deserializedPropertyIfAccessor: DeserializedCallableMemberDescriptor
    get() {
        val member = this.propertyIfAccessor
        if (member is DeserializedCallableMemberDescriptor)
            return member
        else
            error("Unexpected deserializable callable descriptor")
    }

val CallableMemberDescriptor.isDeserializableCallable
    get () = (this.propertyIfAccessor is DeserializedCallableMemberDescriptor)