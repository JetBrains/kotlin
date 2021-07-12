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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.types.*
import java.util.*

/**
 * Given a function descriptor, creates another function descriptor with type parameters copied from outer context(s).
 * This is needed because once we're serializing this to a proto, there's no place to store information about external type parameters.
 */
fun createFreeFakeLambdaDescriptor(descriptor: FunctionDescriptor, typeApproximator: TypeApproximator?): FunctionDescriptor {
    return createFreeDescriptor(descriptor, typeApproximator)
}

private fun <D : CallableMemberDescriptor> createFreeDescriptor(descriptor: D, typeApproximator: TypeApproximator?): D {
    @Suppress("UNCHECKED_CAST")
    val builder = descriptor.newCopyBuilder() as CallableMemberDescriptor.CopyBuilder<D>

    val typeParameters = ArrayList<TypeParameterDescriptor>(0)
    builder.setTypeParameters(typeParameters)

    var container: DeclarationDescriptor? = descriptor.containingDeclaration
    while (container != null) {
        if (container is ClassDescriptor) {
            typeParameters.addAll(container.declaredTypeParameters)
        } else if (container is CallableDescriptor && container !is ConstructorDescriptor) {
            typeParameters.addAll(container.typeParameters)
        }
        container = container.containingDeclaration
    }

    val approximated = typeApproximator?.approximate(descriptor, builder) ?: false

    return if (typeParameters.isEmpty() && !approximated) descriptor else builder.build()!!
}

private fun TypeApproximator.approximate(descriptor: CallableMemberDescriptor, builder: CallableMemberDescriptor.CopyBuilder<*>): Boolean {
    var approximated = false

    val returnType = descriptor.returnType
    if (returnType != null) {
        // unwrap to avoid instances of DeferredType
        val approximatedType = approximate(returnType.unwrap(), toSuper = true)
        if (approximatedType != null) {
            builder.setReturnType(approximatedType)
            approximated = true
        }
    }

    if (builder !is FunctionDescriptor.CopyBuilder<*>) return approximated

    val extensionReceiverParameter = descriptor.extensionReceiverParameter
    if (extensionReceiverParameter != null) {
        val approximatedExtensionReceiver = approximate(extensionReceiverParameter.type.unwrap(), toSuper = false)
        if (approximatedExtensionReceiver != null) {
            builder.setExtensionReceiverParameter(
                extensionReceiverParameter.substituteTopLevelType(approximatedExtensionReceiver)
            )
            approximated = true
        }
    }

    var valueParameterApproximated = false
    val newParameters = descriptor.valueParameters.map {
        val approximatedType = approximate(it.type.unwrap(), toSuper = false)
        if (approximatedType != null) {
            valueParameterApproximated = true
            // invoking constructor explicitly as substitution on value parameters is not supported
            ValueParameterDescriptorImpl(
                it.containingDeclaration, it.original, it.index, it.annotations,
                it.name, outType = approximatedType, it.declaresDefaultValue(),
                it.isCrossinline, it.isNoinline, it.varargElementType, it.source
            )
        } else {
            it
        }
    }

    if (valueParameterApproximated) {
        builder.setValueParameters(newParameters)
        approximated = true
    }

    return approximated
}

private fun ReceiverParameterDescriptor.substituteTopLevelType(newType: KotlinType): ReceiverParameterDescriptor? {
    val wrappedSubstitution = object : TypeSubstitution() {
        override fun get(key: KotlinType): TypeProjection? = null
        override fun prepareTopLevelType(topLevelType: KotlinType, position: Variance): KotlinType = newType
    }

    return substitute(TypeSubstitutor.create(wrappedSubstitution))
}

private fun TypeApproximator.approximate(type: UnwrappedType, toSuper: Boolean): KotlinType? {
    if (type.arguments.isEmpty() && type.constructor.isDenotable) return null
    return if (toSuper)
        approximateToSuperType(type, TypeApproximatorConfiguration.PublicDeclaration)
    else
        approximateToSubType(type, TypeApproximatorConfiguration.PublicDeclaration)
}

/**
 * Given a local delegated variable descriptor, creates a descriptor of a property that should be observed
 * when using reflection on that local variable at runtime.
 * Only members used by [DescriptorSerializer.propertyProto] are implemented correctly in this property descriptor.
 */
fun createFreeFakeLocalPropertyDescriptor(descriptor: LocalVariableDescriptor, typeApproximator: TypeApproximator?): PropertyDescriptor {
    val property = PropertyDescriptorImpl.create(
        descriptor.containingDeclaration, descriptor.annotations, Modality.FINAL, descriptor.visibility, descriptor.isVar,
        descriptor.name, CallableMemberDescriptor.Kind.DECLARATION, descriptor.source, false, descriptor.isConst,
        false, false, false, descriptor.isDelegated
    )
    property.setType(
        descriptor.type, descriptor.typeParameters,
        descriptor.dispatchReceiverParameter, descriptor.extensionReceiverParameter, descriptor.contextReceiverParameters
    )

    property.initialize(
        descriptor.getter?.run {
            PropertyGetterDescriptorImpl(property, annotations, modality, visibility, true, isExternal, isInline, kind, null, source)
                .apply {
                    initialize(this@run.returnType)
                }
        },
        descriptor.setter?.run {
            PropertySetterDescriptorImpl(property, annotations, modality, visibility, true, isExternal, isInline, kind, null, source)
                .apply {
                    initialize(this@run.valueParameters.single())
                }
        }
    )

    return createFreeDescriptor(property, typeApproximator)
}
