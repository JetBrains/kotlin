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
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorNonRootImpl
import org.jetbrains.kotlin.descriptors.impl.VariableDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import java.util.*

interface IrBuiltinOperatorDescriptor : FunctionDescriptor

interface IrBuiltinValueParameterDescriptor : ValueParameterDescriptor

abstract class IrBuiltinOperatorDescriptorBase(containingDeclaration: DeclarationDescriptor, name: Name) :
        DeclarationDescriptorNonRootImpl(containingDeclaration, Annotations.EMPTY, name, SourceElement.NO_SOURCE),
        IrBuiltinOperatorDescriptor
{
    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = null
    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? = null
    override fun getOriginal(): FunctionDescriptor = this
    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor = throw UnsupportedOperationException()
    override fun getOverriddenDescriptors(): Collection<FunctionDescriptor> = emptyList()
    override fun setOverriddenDescriptors(overriddenDescriptors: Collection<CallableMemberDescriptor>) = throw UnsupportedOperationException()
    override fun getTypeParameters(): List<TypeParameterDescriptor> = emptyList()
    override fun getVisibility(): Visibility = Visibilities.PUBLIC
    override fun getModality(): Modality = Modality.FINAL
    override fun getKind(): CallableMemberDescriptor.Kind = CallableMemberDescriptor.Kind.SYNTHESIZED
    override fun getInitialSignatureDescriptor(): FunctionDescriptor? = null
    override fun isExternal(): Boolean = false
    override fun <V : Any> getUserData(key: FunctionDescriptor.UserDataKey<V>?): V? = null
    override fun isHiddenForResolutionEverywhereBesideSupercalls(): Boolean = false
    override fun isHiddenToOvercomeSignatureClash(): Boolean = false
    override fun isInfix(): Boolean = false
    override fun isInline(): Boolean = false
    override fun isOperator(): Boolean = false
    override fun isSuspend(): Boolean = false
    override fun isTailrec(): Boolean = false
    override fun hasStableParameterNames(): Boolean = true
    override fun hasSynthesizedParameterNames(): Boolean = false

    override fun copy(newOwner: DeclarationDescriptor?, modality: Modality?, visibility: Visibility?, kind: CallableMemberDescriptor.Kind?, copyOverrides: Boolean): FunctionDescriptor =
            throw UnsupportedOperationException()

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out FunctionDescriptor> =
            throw UnsupportedOperationException()

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitFunctionDescriptor(this, data)
    }
}

class IrSimpleBuiltinOperatorDescriptorImpl(
        containingDeclaration: DeclarationDescriptor,
        name: Name,
        private val returnType: KotlinType
) : IrBuiltinOperatorDescriptorBase(containingDeclaration, name), IrBuiltinOperatorDescriptor {
    private val valueParameters: MutableList<IrBuiltinValueParameterDescriptor> = ArrayList()

    fun addValueParameter(valueParameter: IrBuiltinValueParameterDescriptor) {
        valueParameters.add(valueParameter)
    }

    override fun getReturnType(): KotlinType = returnType
    override fun getValueParameters(): List<ValueParameterDescriptor> = valueParameters
}

class IrBuiltinValueParameterDescriptorImpl(
        private val containingDeclaration: CallableDescriptor,
        name: Name,
        override val index: Int,
        outType: KotlinType
) : VariableDescriptorImpl(containingDeclaration, Annotations.EMPTY, name, outType, SourceElement.NO_SOURCE),
        IrBuiltinValueParameterDescriptor {

    override fun getContainingDeclaration(): CallableDescriptor = containingDeclaration

    override fun declaresDefaultValue(): Boolean = false
    override fun getOriginal(): ValueParameterDescriptor = this
    override fun getOverriddenDescriptors(): Collection<ValueParameterDescriptor> = emptyList()
    override val isCoroutine: Boolean get() = false
    override val isCrossinline: Boolean get() = false
    override val isNoinline: Boolean get() = false
    override val varargElementType: KotlinType? get() = null
    override fun getCompileTimeInitializer(): ConstantValue<*>? = null
    override fun isVar(): Boolean = false
    override fun getVisibility(): Visibility = Visibilities.LOCAL

    override fun copy(newOwner: CallableDescriptor, newName: Name, newIndex: Int): ValueParameterDescriptor =
            throw UnsupportedOperationException()

    override fun substitute(substitutor: TypeSubstitutor): ValueParameterDescriptor =
            throw UnsupportedOperationException()

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitValueParameterDescriptor(this, data)
    }
}