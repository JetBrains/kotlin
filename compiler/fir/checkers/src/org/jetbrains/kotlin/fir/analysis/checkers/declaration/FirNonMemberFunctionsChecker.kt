/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.FirPlatformDiagnosticSuppressor
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.hasBody
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.lexer.KtTokens

val FirSession.platformDiagnosticSuppressor: FirPlatformDiagnosticSuppressor? by FirSession.nullableSessionComponentAccessor()

// See old FE's [DeclarationsChecker]
object FirNonMemberFunctionsChecker : FirFunctionChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.containingClassLookupTag() != null || declaration is FirPropertyAccessor) {
            return
        }

        val source = declaration.source ?: return
        if (source.kind is KtFakeSourceElementKind) return
        if (declaration.hasModifier(KtTokens.ABSTRACT_KEYWORD)) return
        if (declaration.isExternal) return
        if (!declaration.hasBody &&
            !declaration.isExpect &&
            context.session.platformDiagnosticSuppressor?.shouldReportNoBody(declaration, context) != false
        ) {
            reporter.reportOn(source, FirErrors.NON_MEMBER_FUNCTION_NO_BODY, declaration.symbol, context)
        }
    }
}
