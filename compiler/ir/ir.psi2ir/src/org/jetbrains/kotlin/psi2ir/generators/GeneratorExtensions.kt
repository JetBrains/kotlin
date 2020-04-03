/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.util.StubGeneratorExtensions
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType

open class GeneratorExtensions : StubGeneratorExtensions() {
    open val samConversion: SamConversion
        get() = SamConversion

    open class SamConversion {

        open fun isPlatformSamType(type: KotlinType): Boolean = false

        open fun getSamTypeForValueParameter(valueParameter: ValueParameterDescriptor): KotlinType? = null

        companion object Instance : SamConversion()
    }

    open fun computeFieldVisibility(descriptor: PropertyDescriptor): Visibility? = null

    open val enhancedNullability: EnhancedNullability
        get() = EnhancedNullability

    open class EnhancedNullability {
        open fun hasEnhancedNullability(kotlinType: KotlinType): Boolean = false

        open fun stripEnhancedNullability(kotlinType: KotlinType): KotlinType = kotlinType

        companion object Instance : EnhancedNullability()
    }

    open fun getParentClassStaticScope(descriptor: ClassDescriptor): MemberScope? = null
}
