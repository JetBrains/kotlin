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

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.metadata.KonanLinkData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor

val DeserializedPropertyDescriptor.backingField: PropertyDescriptor?
    get() = 
        if (this.proto.getExtension(KonanLinkData.hasBackingField)) 
            this 
        else null

fun DeclarationDescriptor.deepPrint() {
    this.accept(DeepPrintVisitor(PrintVisitor()), 0)
}

internal val String.synthesizedName get() = Name.identifier(this.synthesizedString)

internal val String.synthesizedString get() = "\$$this"


internal val DeclarationDescriptor.propertyIfAccessor
    get() = if (this is PropertyAccessorDescriptor)
                this.correspondingProperty
                else this

internal val CallableMemberDescriptor.propertyIfAccessor
    get() = if (this is PropertyAccessorDescriptor)
                this.correspondingProperty
                else this

internal val FunctionDescriptor.deserializedPropertyIfAccessor: DeserializedCallableMemberDescriptor
    get() {
        val member = this.propertyIfAccessor
        if (member is DeserializedCallableMemberDescriptor) 
            return member
        else 
            error("Unexpected deserializable callable descriptor")
    }

internal val CallableMemberDescriptor.isDeserializableCallable
    get () = (this.propertyIfAccessor is DeserializedCallableMemberDescriptor)


