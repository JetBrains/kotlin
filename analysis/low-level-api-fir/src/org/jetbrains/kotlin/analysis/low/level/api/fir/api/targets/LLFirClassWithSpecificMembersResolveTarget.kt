/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

internal class LLFirClassWithSpecificMembersResolveTarget(
    firFile: FirFile,
    containerClasses: List<FirRegularClass>,
    target: FirRegularClass,
    val members: List<FirDeclaration>,
) : LLFirResolveTarget(firFile, containerClasses, target) {
    override fun visitTargetElement(
        element: FirElementWithResolveState,
        visitor: LLFirResolveTargetVisitor,
    ) {
        visitor.performAction(element)
        visitor.withRegularClass(element as FirRegularClass) {
            members.forEach(visitor::performAction)
        }
    }

    override fun toStringAdditionalSuffix(): String = members.joinToString(prefix = "[", postfix = "]") { it.symbol.toString() }
}
