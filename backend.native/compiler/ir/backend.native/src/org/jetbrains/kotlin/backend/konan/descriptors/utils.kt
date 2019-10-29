/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.klibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.kotlinLibrary
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.library.BaseKotlinLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*

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

fun DeclarationDescriptor.findTopLevelDescriptor(): DeclarationDescriptor {
    return if (this.containingDeclaration is org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor) this.propertyIfAccessor
    else this.containingDeclaration!!.findTopLevelDescriptor()
}

val ModuleDescriptor.isForwardDeclarationModule get() =
    name == Name.special("<forward declarations>")

fun BaseKotlinLibrary.isInteropLibrary() =
        manifestProperties["ir_provider"] == KLIB_INTEROP_IR_PROVIDER_IDENTIFIER

fun ModuleDescriptor.isFromInteropLibrary() =
        if (klibModuleOrigin !is DeserializedKlibModuleOrigin) false
        else kotlinLibrary.isInteropLibrary()
