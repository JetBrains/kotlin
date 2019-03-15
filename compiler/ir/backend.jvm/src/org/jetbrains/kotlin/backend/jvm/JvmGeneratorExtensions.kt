/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.sam.SamAdapterDescriptor
import org.jetbrains.kotlin.load.java.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor
import org.jetbrains.kotlin.types.KotlinType

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

        override fun getFunctionTypeForSAMClass(descriptor: ClassDescriptor): KotlinType {
            if (descriptor !is JavaClassDescriptor) {
                throw AssertionError("SAM should be represented by a Java class: $descriptor")
            }

            val singleAbstractMethod = SingleAbstractMethodUtils.getSingleAbstractMethodOrNull(descriptor)
                ?: throw AssertionError("$descriptor should have a single abstract method")

            return SingleAbstractMethodUtils.getFunctionTypeForAbstractMethod(singleAbstractMethod, false)
        }

        companion object Instance : JvmSamConversion()
    }
}
