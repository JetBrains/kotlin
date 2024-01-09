/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isEffectivelyExternal
import org.jetbrains.kotlin.fir.analysis.js.checkers.isOverridingExternalWithOptionalParams
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.scopes.collectAllFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionOverrideFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.isSuspendOrKSuspendFunctionType
import org.jetbrains.kotlin.fir.types.typeContext

sealed class FirJsInheritanceClassChecker(mppKind: MppCheckerKind) : FirClassChecker(mppKind) {
    object Regular : FirJsInheritanceClassChecker(MppCheckerKind.Platform) {
        override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    object ForExpectClass : FirJsInheritanceClassChecker(MppCheckerKind.Common) {
        override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (!declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.symbol.isEffectivelyExternal(context)) {
            val fakeOverriddenMethod = declaration.findFakeMethodOverridingExternalWithOptionalParams(context)

            if (fakeOverriddenMethod != null) {
                reporter.reportOn(
                    declaration.source, FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE,
                    fakeOverriddenMethod, context
                )
            }
        }

        if (
            !context.languageVersionSettings.supportsFeature(LanguageFeature.JsAllowImplementingFunctionInterface) &&
            declaration.superConeTypes.any {
                it.isBuiltinFunctionalTypeOrSubtype(context.session) && !it.isSuspendFunctionTypeOrSubtype(context.session)
            }
        ) {
            reporter.reportOn(declaration.source, FirJsErrors.IMPLEMENTING_FUNCTION_INTERFACE, context)
        }
    }

    private fun ConeClassLikeType.isBuiltinFunctionalTypeOrSubtype(session: FirSession): Boolean {
        return with(session.typeContext) { isBuiltinFunctionTypeOrSubtype() }
    }

    private fun ConeClassLikeType.isSuspendFunctionTypeOrSubtype(session: FirSession): Boolean {
        return with(session.typeContext) { isTypeOrSubtypeOf { it.isSuspendOrKSuspendFunctionType(session) } }
    }

    private fun FirClass.findFakeMethodOverridingExternalWithOptionalParams(context: CheckerContext): FirNamedFunctionSymbol? {
        val scope = symbol.unsubstitutedScope(context)

        val members = scope.collectAllFunctions()
            .filterIsInstance<FirIntersectionOverrideFunctionSymbol>()
            .filter {
                val container = it.getContainingClassSymbol(context.session)
                container == symbol && it.intersections.isNotEmpty()
            }

        return members.firstOrNull {
            it.isOverridingExternalWithOptionalParams(context)
        }
    }
}
