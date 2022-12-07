/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrOverridableMember
import org.jetbrains.kotlin.ir.util.parentClassOrNull

// The contents of this file is from VisibilityUtil.kt adapted to IR.
// TODO: The code would better be commonized for descriptors, ir and fir.

fun isVisibleForOverride(
    @Suppress("UNUSED_PARAMETER") overriding: IrOverridableMember,
    fromSuper: IrOverridableMember
): Boolean {
    return !DescriptorVisibilities.isPrivate((fromSuper as IrDeclarationWithVisibility).visibility)
}

fun findMemberWithMaxVisibility(members: Collection<IrOverridableMember>): IrOverridableMember {
    assert(members.isNotEmpty())

    var member: IrOverridableMember? = null
    for (candidate in members) {
        if (member == null) {
            member = candidate
            continue
        }

        val result = DescriptorVisibilities.compare(member.visibility, candidate.visibility)
        if (result != null && result < 0) {
            member = candidate
        }
    }
    return member ?: error("Could not find a visible member")
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
