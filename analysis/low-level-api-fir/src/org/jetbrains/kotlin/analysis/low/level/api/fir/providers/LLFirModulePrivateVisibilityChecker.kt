/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.FirModulePrivateVisibilityChecker
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration

class LLFirModulePrivateVisibilityChecker(val llFirSession: LLFirSession) :
    FirModulePrivateVisibilityChecker.Standard(llFirSession) {
    /**
     * When analyzing a dangling file we may resolve to declaration in the original module in case of
     * [org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode.IGNORE_SELF] mode.
     * Dangling file must be able to see declarations of the original file, thus it needs special handling.
     */
    override fun canSeePrivateDeclaration(declaration: FirMemberDeclaration): Boolean {
        if (super.canSeePrivateDeclaration(declaration)) {
            return true
        }

        return llFirSession.ktModule.unwrapDangling() == declaration.llFirModuleData.ktModule
    }

    fun KaModule.unwrapDangling(): KaModule = if (this is KaDanglingFileModule) contextModule else this
}
