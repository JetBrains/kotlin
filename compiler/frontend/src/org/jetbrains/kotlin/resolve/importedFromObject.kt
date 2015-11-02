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

abstract class ImportedFromObjectCallableDescriptor<TCallable : CallableDescriptor>(val callableFromObject: TCallable) : CallableDescriptor {
    val containingObject = callableFromObject.containingDeclaration as ClassDescriptor
}

// members imported from object should be wrapped to not require dispatch receiver
class FunctionImportedFromObject(functionFromObject: FunctionDescriptor) :
        FunctionDescriptor by functionFromObject,
        ImportedFromObjectCallableDescriptor<FunctionDescriptor>(functionFromObject) {
    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun substitute(substitutor: TypeSubstitutor) = callableFromObject.substitute(substitutor).wrap()

    private val _original by lazy {
        functionFromObject.original.wrap()
    }

    override fun getOriginal() = _original

    override fun copy(
            newOwner: DeclarationDescriptor?, modality: Modality?, visibility: Visibility?,
            kind: CallableMemberDescriptor.Kind?, copyOverrides: Boolean
    ): FunctionDescriptor {
        throw IllegalStateException("copy() should not be called on ${this.javaClass.simpleName}, was called for $this")
    }
}

class PropertyImportedFromObject(propertyFromObject: PropertyDescriptor) :
        PropertyDescriptor by propertyFromObject,
        ImportedFromObjectCallableDescriptor<PropertyDescriptor>(propertyFromObject) {

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun substitute(substitutor: TypeSubstitutor) = callableFromObject.substitute(substitutor)?.wrap()

    private val _original by lazy {
        propertyFromObject.original.wrap()
    }

    override fun getOriginal() = _original

    override fun copy(
            newOwner: DeclarationDescriptor?, modality: Modality?, visibility: Visibility?,
            kind: CallableMemberDescriptor.Kind?, copyOverrides: Boolean
    ): FunctionDescriptor {
        throw IllegalStateException("copy() should not be called on ${this.javaClass.simpleName}, was called for $this")
    }
}

private fun FunctionDescriptor.wrap() = FunctionImportedFromObject(this)
private fun PropertyDescriptor.wrap() = PropertyImportedFromObject(this)