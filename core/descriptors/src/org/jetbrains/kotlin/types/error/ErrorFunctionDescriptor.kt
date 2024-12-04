/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.types.error

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.Annotations.Companion.EMPTY
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitution

class ErrorFunctionDescriptor(containingDeclaration: ClassDescriptor) :
    SimpleFunctionDescriptorImpl(
        containingDeclaration, null, EMPTY, Name.special(ErrorEntity.ERROR_FUNCTION.debugText), CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE
) {
    init {
        initialize(
            null, null, emptyList(), emptyList(), emptyList(),
            ErrorUtils.createErrorType(ErrorTypeKind.RETURN_TYPE_FOR_FUNCTION), Modality.OPEN, DescriptorVisibilities.PUBLIC
        )
    }

    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): FunctionDescriptorImpl = this

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): SimpleFunctionDescriptor = this

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
        object : FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> {
            override fun setOwner(owner: DeclarationDescriptor): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setModality(modality: Modality): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setVisibility(visibility: DescriptorVisibility): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setKind(kind: CallableMemberDescriptor.Kind): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setCopyOverrides(copyOverrides: Boolean): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setName(name: Name): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setSubstitution(substitution: TypeSubstitution): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setValueParameters(
                parameters: List<ValueParameterDescriptor>
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun <V> putUserData(userDataKey: CallableDescriptor.UserDataKey<V>,
                                         value: V
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setTypeParameters(
                parameters: List<TypeParameterDescriptor>
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setReturnType(type: KotlinType): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setContextReceiverParameters(
                contextReceiverParameters: List<ReceiverParameterDescriptor>
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setExtensionReceiverParameter(
                extensionReceiverParameter: ReceiverParameterDescriptor?
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun setDispatchReceiverParameter(
                dispatchReceiverParameter: ReceiverParameterDescriptor?
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setOriginal(original: CallableMemberDescriptor?): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setSignatureChange(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setPreserveSourceElement(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setDropOriginalInContainingParts(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setHiddenToOvercomeSignatureClash(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setHiddenForResolutionEverywhereBesideSupercalls(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setAdditionalAnnotations(
                additionalAnnotations: Annotations
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun build(): SimpleFunctionDescriptor = this@ErrorFunctionDescriptor
        }

    override fun isSuspend(): Boolean = false
    override fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>): V? = null
    override fun setOverriddenDescriptors(overriddenDescriptors: Collection<CallableMemberDescriptor?>) {}
}
