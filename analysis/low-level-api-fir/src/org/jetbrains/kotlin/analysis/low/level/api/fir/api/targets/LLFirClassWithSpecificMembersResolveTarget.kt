/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

internal class LLFirClassWithSpecificMembersResolveTarget(
    designation: FirDesignation,
    val members: List<FirDeclaration>,
) : LLFirRegularClassResolveTarget(designation) {
    override fun visitMembers(visitor: LLFirResolveTargetVisitor, firRegularClass: FirRegularClass) {
        members.forEach(visitor::performAction)
    }

    override fun toStringAdditionalSuffix(): String = members.joinToString(prefix = "[", postfix = "]") { it.symbol.toString() }
}
