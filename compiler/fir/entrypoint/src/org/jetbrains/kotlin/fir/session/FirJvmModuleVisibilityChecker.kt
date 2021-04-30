/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirModuleVisibilityChecker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.moduleData

class FirJvmModuleVisibilityChecker(private val session: FirSession) : FirModuleVisibilityChecker {
    override fun isInFriendModule(declaration: FirMemberDeclaration): Boolean {
        val useSiteModuleData = session.moduleData
        val declarationModuleData = declaration.moduleData
        return useSiteModuleData == declarationModuleData || declarationModuleData in useSiteModuleData.friendDependencies
    }
}
