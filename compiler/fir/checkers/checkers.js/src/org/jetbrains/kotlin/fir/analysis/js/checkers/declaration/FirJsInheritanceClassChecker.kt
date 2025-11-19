/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isOverridingExternalWithOptionalParams
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.collectAllFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionOverrideFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*

sealed class FirJsInheritanceClassChecker(mppKind: MppCheckerKind) : FirClassChecker(mppKind) {
    object Regular : FirJsInheritanceClassChecker(MppCheckerKind.Platform) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirClass) {
            if (declaration.isExpect) return
            super.check(declaration)
        }
    }

    object ForExpectClass : FirJsInheritanceClassChecker(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirClass) {
            if (!declaration.isExpect) return
            super.check(declaration)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        val session = context.session
        val isEffectivelyExternal = declaration.symbol.isEffectivelyExternal(session)

        if (isEffectivelyExternal && declaration.classKind != ClassKind.ANNOTATION_CLASS) {
            for (superType in declaration.superConeTypes) {
                if (superType.isAnyOrNullableAny || superType.isThrowableOrNullableThrowable || superType.isEnum) continue
                val fullyExpandedClass = superType.toSymbol()?.fullyExpandedClass() ?: continue
                if (fullyExpandedClass.isEffectivelyExternal(session) || fullyExpandedClass.isExpect) continue

                reporter.reportOn(declaration.source, FirWebCommonErrors.EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE, superType)
            }
        }

        if (!isEffectivelyExternal) {
            val fakeOverriddenMethod = declaration.findFakeMethodOverridingExternalWithOptionalParams()

            if (fakeOverriddenMethod != null) {
                reporter.reportOn(
                    declaration.source, FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE,
                    fakeOverriddenMethod
                )
            }
        }

        if (
            !LanguageFeature.JsAllowImplementingFunctionInterface.isEnabled() &&
            declaration.superConeTypes.any {
                it.isBuiltinFunctionalTypeOrSubtype(session) && !it.isSuspendFunctionTypeOrSubtype(session)
            }
        ) {
            reporter.reportOn(declaration.source, FirJsErrors.IMPLEMENTING_FUNCTION_INTERFACE)
        }
    }

    private fun ConeClassLikeType.isBuiltinFunctionalTypeOrSubtype(session: FirSession): Boolean {
        return with(session.typeContext) { isBuiltinFunctionTypeOrSubtype() }
    }

    private fun ConeClassLikeType.isSuspendFunctionTypeOrSubtype(session: FirSession): Boolean {
        return with(session.typeContext) { isTypeOrSubtypeOf { it.isSuspendOrKSuspendFunctionType(session) } }
    }

    context(context: CheckerContext)
    private fun FirClass.findFakeMethodOverridingExternalWithOptionalParams(): FirNamedFunctionSymbol? {
        val scope = symbol.unsubstitutedScope()

        val members = scope.collectAllFunctions()
            .filterIsInstance<FirIntersectionOverrideFunctionSymbol>()
            .filter {
                val container = it.getContainingClassSymbol()
                container == symbol && it.intersections.isNotEmpty()
            }

        return members.firstOrNull {
            it.isOverridingExternalWithOptionalParams()
        }
    }
}
