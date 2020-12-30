/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor

class FirWrapperSimpleFunctionDescriptor(val ktFunctionSymbol: KtFunctionSymbol) : FunctionDescriptor {
    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? {
        TODO("Not yet implemented")
    }

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        TODO("Not yet implemented")
    }

    override fun getTypeParameters(): List<TypeParameterDescriptor> {
        TODO("Not yet implemented")
    }

    override fun getReturnType(): KotlinType? {
        TODO("Not yet implemented")
    }

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> {
        TODO("Not yet implemented")
    }

    override fun hasStableParameterNames(): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasSynthesizedParameterNames(): Boolean {
        TODO("Not yet implemented")
    }

    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? {
        TODO("Not yet implemented")
    }

    override fun setOverriddenDescriptors(overriddenDescriptors: MutableCollection<out CallableMemberDescriptor>) {
        TODO("Not yet implemented")
    }

    override fun getKind(): CallableMemberDescriptor.Kind {
        TODO("Not yet implemented")
    }

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R {
        TODO("Not yet implemented")
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) {
        TODO("Not yet implemented")
    }

    override fun getSource(): SourceElement {
        TODO("Not yet implemented")
    }

    override fun getVisibility(): DescriptorVisibility {
        TODO("Not yet implemented")
    }

    override fun getContainingDeclaration(): DeclarationDescriptor {
        return this // TODO hack -- incorrect work
    }

    override fun getOriginal(): FunctionDescriptor {
        TODO("Not yet implemented")
    }

    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor? {
        TODO("Not yet implemented")
    }

    override fun getOverriddenDescriptors(): MutableCollection<out FunctionDescriptor> {
        TODO("Not yet implemented")
    }

    override fun getInitialSignatureDescriptor(): FunctionDescriptor? {
        TODO("Not yet implemented")
    }

    override fun isHiddenToOvercomeSignatureClash(): Boolean {
        TODO("Not yet implemented")
    }

    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: DescriptorVisibility?,
        kind: CallableMemberDescriptor.Kind?,
        copyOverrides: Boolean
    ): FunctionDescriptor {
        TODO("Not yet implemented")
    }

    override fun isOperator(): Boolean = ktFunctionSymbol.isOperator

    override fun isInfix(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isInline(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isTailrec(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isHiddenForResolutionEverywhereBesideSupercalls(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSuspend(): Boolean {
        TODO("Not yet implemented")
    }

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out FunctionDescriptor> {
        TODO("Not yet implemented")
    }

    override fun getModality(): Modality {
        TODO("Not yet implemented")
    }

    override fun isExpect(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isActual(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isExternal(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getName(): Name = ktFunctionSymbol.name

    override val annotations: Annotations
        get() = TODO("Not yet implemented")
}