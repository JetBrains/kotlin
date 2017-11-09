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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.types.TypeSubstitutor

fun FunctionDescriptor.asImportedFromObject(original: FunctionImportedFromObject? = null) =
        FunctionImportedFromObject(this, original)

fun PropertyDescriptor.asImportedFromObject(original: PropertyImportedFromObject? = null) =
        PropertyImportedFromObject(this, original)

abstract class ImportedFromObjectCallableDescriptor<out TCallable : CallableMemberDescriptor>(
        val callableFromObject: TCallable,
        private val originalOrNull: TCallable?
) : CallableDescriptor {

    val containingObject = callableFromObject.containingDeclaration as ClassDescriptor

    protected val _original
        get() = originalOrNull ?: this
}

// members imported from object should be wrapped to not require dispatch receiver
class FunctionImportedFromObject(
        functionFromObject: FunctionDescriptor,
        originalOrNull: FunctionDescriptor? = null
) : ImportedFromObjectCallableDescriptor<FunctionDescriptor>(functionFromObject, originalOrNull),
        FunctionDescriptor by functionFromObject {

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun substitute(substitutor: TypeSubstitutor) =
            callableFromObject.substitute(substitutor)?.asImportedFromObject(this)

    override fun getOriginal() = _original as FunctionDescriptor

    override fun copy(
            newOwner: DeclarationDescriptor?, modality: Modality?, visibility: Visibility?,
            kind: CallableMemberDescriptor.Kind?, copyOverrides: Boolean
    ): FunctionDescriptor {
        throw UnsupportedOperationException("copy() should not be called on ${this::class.java.simpleName}, was called for $this")
    }
}

class PropertyImportedFromObject(
        propertyFromObject: PropertyDescriptor,
        originalOrNull: PropertyDescriptor? = null
) : ImportedFromObjectCallableDescriptor<PropertyDescriptor>(propertyFromObject, originalOrNull),
        PropertyDescriptor by propertyFromObject {

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun substitute(substitutor: TypeSubstitutor) = callableFromObject.substitute(substitutor)?.asImportedFromObject(this)

    override fun getOriginal() = _original as PropertyDescriptor

    override fun copy(
            newOwner: DeclarationDescriptor?, modality: Modality?, visibility: Visibility?,
            kind: CallableMemberDescriptor.Kind?, copyOverrides: Boolean
    ): FunctionDescriptor {
        throw UnsupportedOperationException("copy() should not be called on ${this::class.java.simpleName}, was called for $this")
    }
}

