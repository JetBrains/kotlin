/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*

fun isVisibleForOverride(overriding: IrOverridableMember, fromSuper: IrOverridableMember): Boolean {
    return fromSuper.isVisibleInClass(overriding.parentAsClass)
}

fun IrDeclarationWithVisibility.isEffectivelyPrivate(): Boolean {
    fun DescriptorVisibility.isNonPrivate(): Boolean =
        this == DescriptorVisibilities.PUBLIC
                || this == DescriptorVisibilities.PROTECTED
                || this == DescriptorVisibilities.INTERNAL

    return when {
        visibility.isNonPrivate() -> parentClassOrNull?.isEffectivelyPrivate() ?: false

        visibility == DescriptorVisibilities.INVISIBLE_FAKE -> {
            val overridesOnlyPrivateDeclarations = (this as? IrOverridableDeclaration<*>)
                ?.overriddenSymbols
                ?.all { (it.owner as? IrDeclarationWithVisibility)?.isEffectivelyPrivate() == true }
                ?: false

            overridesOnlyPrivateDeclarations || (parentClassOrNull?.isEffectivelyPrivate() ?: false)
        }

        else -> true
    }
}

internal fun IrOverridableMember.isVisibleInClass(klass: IrClass): Boolean {
    if (DescriptorVisibilities.isPrivate(visibility) || visibility == DescriptorVisibilities.INVISIBLE_FAKE) return false

    // OverridingUtil.isVisibleForOverride just calls DescriptorVisibilities.isVisible here. However, we can't use descriptors. Moreover,
    // the current member's parent has already been reassigned to the current class (in OverridingUtil, it is still the original class where
    // the member was declared at this point). So we load the original class from the dispatch receiver.
    val dispatchReceiver = when (this) {
        is IrSimpleFunction -> dispatchReceiverParameter
        is IrProperty -> getter?.dispatchReceiverParameter
        else -> error("Unsupported member: ${render()}")
    } ?: error("Members without dispatch receiver are not possible here: ${render()} (klass=${klass.fqNameWhenAvailable}")

    // Package-private Java members are only overridable within the same package.
    val originalClass = dispatchReceiver.type.getClass()!!
    if (!visibility.visibleFromPackage(klass.getPackageFragment().packageFqName, originalClass.getPackageFragment().packageFqName))
        return false

    // TODO (KT-61384): also check internal.

    return true
}
