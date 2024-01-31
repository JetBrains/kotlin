/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.forEachDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

/**
 * [LLFirResolveTarget] representing a class with all class members to be resolved (this includes callables, init blocks, and classifiers)
 */
internal class LLFirClassWithAllMembersResolveTarget(designation: FirDesignation) : LLFirRegularClassResolveTarget(designation) {
    override fun visitMembers(visitor: LLFirResolveTargetVisitor, firRegularClass: FirRegularClass) {
        firRegularClass.forEachDeclaration(visitor::performAction)
    }
}
