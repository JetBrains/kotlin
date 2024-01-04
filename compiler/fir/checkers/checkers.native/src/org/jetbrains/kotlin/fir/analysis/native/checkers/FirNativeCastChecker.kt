/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.fir.FirPlatformSpecificCastChecker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

object FirNativeCastChecker : FirPlatformSpecificCastChecker() {
    override fun shouldSuppressImpossibleCast(session: FirSession, fromType: ConeKotlinType, toType: ConeKotlinType): Boolean {
        return isCastToAForwardDeclaration(session, toType)
    }

    /**
     * Here, we only check that we are casting to a forward declaration to suppress a CAST_NEVER_SUCCEEDS warning.
     * The cast would be further checked with FirNativeForwardDeclarationTypeOperatorChecker and FirNativeForwardDeclarationGetClassCallChecker.
     */
    private fun isCastToAForwardDeclaration(session: FirSession, forwardDeclarationType: ConeKotlinType): Boolean {
        return forwardDeclarationType.toRegularClassSymbol(session)?.forwardDeclarationKindOrNull() != null
    }
}