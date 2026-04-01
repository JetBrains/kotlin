/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.FirPlatformSpecificCastChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCastOperatorsChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toTypeInfo
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

object FirNativeCastChecker : FirPlatformSpecificCastChecker() {
    context(context: CheckerContext)
    override fun runApplicabilityCheck(
        expression: FirTypeOperatorCall,
        fromType: ConeKotlinType,
        toType: ConeKotlinType,
        checker: FirCastOperatorsChecker,
    ): FirCastOperatorsChecker.Applicability =
        checker.checkGeneralApplicability(expression, fromType.toTypeInfo(context.session), toType.toTypeInfo(context.session))
            .takeUnless { it == FirCastOperatorsChecker.Applicability.IMPOSSIBLE_CAST && isCastToAForwardDeclaration(toType) }
            ?: FirCastOperatorsChecker.Applicability.APPLICABLE

    /**
     * Here, we only check that we are casting to a forward declaration to suppress a CAST_NEVER_SUCCEEDS warning.
     * The cast would be further checked with FirNativeForwardDeclarationTypeOperatorChecker and FirNativeForwardDeclarationGetClassCallChecker.
     */
    context(context: CheckerContext)
    private fun isCastToAForwardDeclaration(forwardDeclarationType: ConeKotlinType): Boolean {
        return forwardDeclarationType.toRegularClassSymbol(context.session)?.forwardDeclarationKindOrNull() != null
    }
}
