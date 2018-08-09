/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.types.KotlinType

val IrConstructorSymbol.constructedClass get() = descriptor.constructedClass

fun createValueParameter(containingDeclaration: CallableDescriptor, index: Int, name: String, type: KotlinType): ValueParameterDescriptor {
    return ValueParameterDescriptorImpl(
        containingDeclaration = containingDeclaration,
        original = null,
        index = index,
        annotations = Annotations.EMPTY,
        name = Name.identifier(name),
        outType = type,
        declaresDefaultValue = false,
        isCrossinline = false,
        isNoinline = false,
        varargElementType = null,
        source = SourceElement.NO_SOURCE
    )
}
val CallableMemberDescriptor.propertyIfAccessor
    get() = if (this is PropertyAccessorDescriptor)
        this.correspondingProperty
    else this

val IrTypeParameter.isReified
    get() = descriptor.isReified

// Return is method has no real implementation except fake overrides from Any
fun CallableMemberDescriptor.isFakeOverriddenFromAny(): Boolean {
    if (kind.isReal) {
        return (containingDeclaration is ClassDescriptor) && KotlinBuiltIns.isAny(containingDeclaration as ClassDescriptor)
    }
    return overriddenDescriptors.all { it.isFakeOverriddenFromAny() }
}

fun IrDeclaration.isEffectivelyExternal() = descriptor.isEffectivelyExternal()

fun IrSymbol.isEffectivelyExternal() = descriptor.isEffectivelyExternal()

fun IrSymbol.isDynamic() = descriptor.isDynamic()

fun IrCall.isSuperToAny() =
    superQualifier?.let { this.symbol.owner.descriptor.isFakeOverriddenFromAny() } ?: false

