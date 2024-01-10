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