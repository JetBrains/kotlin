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

package org.jetbrains.kotlin.ir.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.VariableDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor

interface IrDelegateDescriptor : PropertyDescriptor

interface IrPropertyDelegateDescriptor : IrDelegateDescriptor {
    val correspondingProperty: PropertyDescriptor
    val kPropertyType: KotlinType
}

interface IrLocalDelegateDescriptor : VariableDescriptor

interface IrLocalDelegatedPropertyDelegateDescriptor : IrLocalDelegateDescriptor {
    val correspondingLocalProperty: VariableDescriptorWithAccessors
    val kPropertyType: KotlinType
}

interface IrImplementingDelegateDescriptor : IrDelegateDescriptor {
    val correspondingSuperType: KotlinType
}

abstract class IrDelegateDescriptorBase(
    containingDeclaration: DeclarationDescriptor,
    name: Name,
    delegateType: KotlinType,
    annotations: Annotations = Annotations.EMPTY
) :
    PropertyDescriptorImpl(
        containingDeclaration,
        /* original = */ null,
        annotations,
        Modality.FINAL,
        Visibilities.PRIVATE,
        /* isVar = */ false,
        name,
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        SourceElement.NO_SOURCE,
        /* lateInit = */ false,
        /* isConst = */ false,
        /* isExpect = */ false,
        /* isActual = */ false,
        /* isExternal = */ false,
        /* isDelegated = */ true
    ) {
    init {
        setType(delegateType, emptyList(), (containingDeclaration as? ClassDescriptor)?.thisAsReceiverParameter, null)
    }

    final override fun setOutType(outType: KotlinType?) {
        super.setOutType(outType)
    }

    override fun getCompileTimeInitializer(): ConstantValue<*>? = null

    override fun getVisibility(): Visibility = Visibilities.PRIVATE

    override fun substitute(substitutor: TypeSubstitutor): PropertyDescriptor {
        throw UnsupportedOperationException("Property delegate descriptor shouldn't be substituted: $this")
    }

    override fun isVar(): Boolean = false

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
        visitor.visitVariableDescriptor(this, data)
}

class IrPropertyDelegateDescriptorImpl(
    override val correspondingProperty: PropertyDescriptor,
    delegateType: KotlinType,
    override val kPropertyType: KotlinType
) :
    IrDelegateDescriptorBase(
        correspondingProperty.containingDeclaration,
        getDelegateName(correspondingProperty.name),
        delegateType,
        correspondingProperty.annotations
    ),
    IrPropertyDelegateDescriptor

class IrImplementingDelegateDescriptorImpl(
    containingDeclaration: ClassDescriptor,
    delegateType: KotlinType,
    override val correspondingSuperType: KotlinType
) :
    IrDelegateDescriptorBase(
        containingDeclaration,
        getDelegateName(containingDeclaration, correspondingSuperType),
        delegateType
    ),
    IrImplementingDelegateDescriptor

internal fun getDelegateName(name: Name): Name =
    Name.identifier(name.asString() + "\$delegate")

internal fun getDelegateName(classDescriptor: ClassDescriptor, superType: KotlinType): Name =
    Name.identifier(
        classDescriptor.name.asString() + "\$" +
                (superType.constructor.declarationDescriptor?.name ?: "\$") +
                "\$delegate"
    )

class IrLocalDelegatedPropertyDelegateDescriptorImpl(
    override val correspondingLocalProperty: VariableDescriptorWithAccessors,
    delegateType: KotlinType,
    override val kPropertyType: KotlinType
) : IrLocalDelegatedPropertyDelegateDescriptor,
    VariableDescriptorImpl(
        correspondingLocalProperty.containingDeclaration,
        Annotations.EMPTY,
        getDelegateName(correspondingLocalProperty.name),
        delegateType,
        org.jetbrains.kotlin.descriptors.SourceElement.NO_SOURCE
    ) {

    override fun getCompileTimeInitializer(): ConstantValue<*>? = null
    override fun isVar(): Boolean = false
    override fun isLateInit(): Boolean = false
    override fun substitute(substitutor: TypeSubstitutor): VariableDescriptor? = throw UnsupportedOperationException()
    override fun getVisibility(): Visibility = Visibilities.LOCAL

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
        visitor.visitVariableDescriptor(this, data)
}
