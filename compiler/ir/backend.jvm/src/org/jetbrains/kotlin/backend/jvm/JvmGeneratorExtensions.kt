/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.sam.SamAdapterDescriptor
import org.jetbrains.kotlin.load.java.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance

object JvmGeneratorExtensions : GeneratorExtensions() {
    override val externalDeclarationOrigin: ((DeclarationDescriptor) -> IrDeclarationOrigin)? = { descriptor ->
        if (descriptor is JavaCallableMemberDescriptor)
            IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        else
            IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
    }

    override val samConversion: SamConversion
        get() = JvmSamConversion

    open class JvmSamConversion : SamConversion() {
        override fun getOriginalForSamAdapter(descriptor: CallableDescriptor): CallableDescriptor? =
            when (descriptor) {
                is SamAdapterDescriptor<*> -> descriptor.baseDescriptorForSynthetic
                is SamAdapterExtensionFunctionDescriptor -> descriptor.baseDescriptorForSynthetic
                else -> null
            }

        override fun isSamConstructor(descriptor: CallableDescriptor): Boolean =
            descriptor is SamConstructorDescriptor

        override fun isSamType(type: KotlinType): Boolean =
            SingleAbstractMethodUtils.isSamType(type)

        override fun getSubstitutedFunctionTypeForSamType(samType: KotlinType): KotlinType {
            val descriptor = samType.constructor.declarationDescriptor as? JavaClassDescriptor
                ?: throw AssertionError("SAM should be represented by a Java class: $samType")
            val singleAbstractMethod = SingleAbstractMethodUtils.getSingleAbstractMethodOrNull(descriptor)
                ?: throw AssertionError("$descriptor should have a single abstract method")
            val unsubstitutedFunctionType = SingleAbstractMethodUtils.getFunctionTypeForAbstractMethod(singleAbstractMethod, false)
            return TypeSubstitutor.create(samType).substitute(unsubstitutedFunctionType, Variance.INVARIANT)
                ?: throw AssertionError("Failed to substitute function type $unsubstitutedFunctionType corresponding to $samType")
        }

        companion object Instance : JvmSamConversion()
    }

    override fun computeFieldVisibility(descriptor: PropertyDescriptor): Visibility? =
        if (descriptor.hasJvmFieldAnnotation() || descriptor is JavaCallableMemberDescriptor)
            descriptor.visibility
        else
            null
}
