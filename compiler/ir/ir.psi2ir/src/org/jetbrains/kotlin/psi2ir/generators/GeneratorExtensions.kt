/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.util.StubGeneratorExtensions
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.backend.common.SamTypeFactory

open class GeneratorExtensions : StubGeneratorExtensions() {
    open val samConversion: SamConversion
        get() = SamConversion

    open class SamConversion {
        open fun isPlatformSamType(type: KotlinType): Boolean = false

        open fun getSamTypeForValueParameter(valueParameter: ValueParameterDescriptor): KotlinType? =
            SamTypeFactory.INSTANCE.createByValueParameter(valueParameter)?.type

        companion object Instance : SamConversion()
    }

    open fun computeFieldVisibility(descriptor: PropertyDescriptor): DescriptorVisibility? = null

    open fun getParentClassStaticScope(descriptor: ClassDescriptor): MemberScope? = null

    open fun createCustomSuperConstructorCall(
        ktPureClassOrObject: KtPureClassOrObject,
        descriptor: ClassDescriptor,
        context: GeneratorContext,
    ): IrDelegatingConstructorCall? = null

    open val shouldPreventDeprecatedIntegerValueTypeLiteralConversion: Boolean
        get() = false

    open fun getPreviousScripts(): List<IrScriptSymbol>? = null
}
