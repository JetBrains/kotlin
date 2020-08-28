/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.overrides

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrOverridableMember

// The contents of this file is from VisibilityUtil.kt adapted to IR.
// TODO: The code would better be commonized for descriptors, ir and fir.

fun isVisibleForOverride(
    @Suppress("UNUSED_PARAMETER") overriding: IrOverridableMember,
    fromSuper: IrOverridableMember
): Boolean {
    return !Visibilities.isPrivate((fromSuper as IrDeclarationWithVisibility).visibility)
}

fun findMemberWithMaxVisibility(members: Collection<IrOverridableMember>): IrOverridableMember {
    assert(members.isNotEmpty())

    var member: IrOverridableMember? = null
    for (candidate in members) {
        if (member == null) {
            member = candidate
            continue
        }

        val result = Visibilities.compare(member.visibility, candidate.visibility)
        if (result != null && result < 0) {
            member = candidate
        }
    }
    return member ?: error("Could not find a visible member")
}
