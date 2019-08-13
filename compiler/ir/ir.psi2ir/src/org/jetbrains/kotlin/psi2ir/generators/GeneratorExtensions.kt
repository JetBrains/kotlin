/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType

open class GeneratorExtensions {
    open val externalDeclarationOrigin: ((DeclarationDescriptor) -> IrDeclarationOrigin)?
        get() = null

    open val samConversion: SamConversion
        get() = SamConversion

    open class SamConversion {
        // Returns null if descriptor is not a SAM adapter
        open fun getOriginalForSamAdapter(descriptor: CallableDescriptor): CallableDescriptor? = null

        open fun isSamConstructor(descriptor: CallableDescriptor): Boolean = false

        open fun isSamType(type: KotlinType): Boolean = false

        open fun getSubstitutedFunctionTypeForSamType(samType: KotlinType): KotlinType =
            throw UnsupportedOperationException("SAM conversion is not supported in this configuration (samType=$samType)")

        companion object Instance : SamConversion()
    }
}
