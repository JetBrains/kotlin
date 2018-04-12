/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor

//val DeserializedPropertyDescriptor.backingField: PropertyDescriptor?
//    get() =
//        if (this.proto.getExtension(KonanLinkData.hasBackingField))
//            this
//        else null

//fun DeclarationDescriptor.deepPrint() {
//    this.accept(DeepPrintVisitor(PrintVisitor()), 0)
//}

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


