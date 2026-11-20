/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.getNonSubsumedOverriddenSymbols
import org.jetbrains.kotlin.fir.declarations.isEquals
import org.jetbrains.kotlin.fir.declarations.utils.equalityBoundTypeOfParameter
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.isDelegated
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ScopeFunctionRequiresPrewarm
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenMembers
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionOverrideFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.util.OperatorNameConventions

sealed class FirEqualityBoundOverrideChecker(mppKind: MppCheckerKind) : FirClassChecker(mppKind) {
    object ForExpect : FirEqualityBoundOverrideChecker(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirClass) {
            if (declaration.isExpect) super.check(declaration)
        }
    }

    object Regular : FirEqualityBoundOverrideChecker(MppCheckerKind.Platform) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirClass) {
            if (!declaration.isExpect) super.check(declaration)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (LanguageFeature.StrictEquals.isEnabled()) {
            val typeCheckerState = context.session.typeContext.newTypeCheckerState(
                errorTypesEqualToAnything = false,
                stubTypesEqualToAnything = false,
                dnnTypesEqualToFlexible = LanguageFeature.AllowDnnTypeOverridingFlexibleType.isEnabled()
            )
            val scope = declaration.unsubstitutedScope()
            scope.processFunctionsByName(OperatorNameConventions.EQUALS) {
                if (it.isEquals(context.session)) {
                    checkImpl(it, declaration, scope, typeCheckerState)
                }
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkImpl(
        equals: FirNamedFunctionSymbol,
        klass: FirClass,
        scope: FirTypeScope,
        typeCheckerState: TypeCheckerState,
    ) {
        val isFromThis =
            equals.containingClassLookupTag() == klass.symbol.toLookupTag() && equals.origin in OverrideCheckerUtils.checkedOrigins

        val sourceForDiagnostic = equals.source.takeIf { isFromThis } ?: klass.source

        fun validateAgainstOtherMembers(
            validated: FirNamedFunctionSymbol,
            other: List<FirNamedFunctionSymbol>,
        ): Boolean {
            if (validated.equalityBoundTypeOfParameter is ConeErrorType || other.any { it.equalityBoundTypeOfParameter is ConeErrorType }) {
                return false
            }
            for (member in other) {
                if (!equalityBoundIsSubtype(validated, member, typeCheckerState)) {
                    reporter.reportOn(
                        sourceForDiagnostic,
                        if (validated.isDelegated) FirErrors.EQUALITY_BOUND_MISMATCH_BY_DELEGATION
                        else FirErrors.EQUALITY_BOUND_MISMATCH_ON_INHERITANCE,
                        validated,
                        member,
                    )
                    return false
                }
            }
            return true
        }

        if (isFromThis) {
            @OptIn(ScopeFunctionRequiresPrewarm::class) // `check` calls `processFunctionsByName`
            val overriddenMembers = scope.getDirectOverriddenMembers(equals).filterIsInstance<FirNamedFunctionSymbol>()
            if (!validateAgainstOtherMembers(validated = equals, other = overriddenMembers)) return
        }

        // We'd like to report this even if `equals` in question is defined in the supertype:
        // the main reason is that it can originate from java class.
        if (equals.equalityBoundTypeOfParameter is ConeIntersectionType) {
            reporter.reportOn(
                sourceForDiagnostic,
                FirErrors.INHERITED_INTERSECTION_EQUALITY_BOUND,
                equals,
                equals.equalityBoundTypeOfParameter!!,
            )
            return
        }

        // See the comment above
        (equals.unwrapSubstitutionOverrides() as? FirIntersectionOverrideFunctionSymbol)?.let { unwrappedEquals ->
            @OptIn(ScopeFunctionRequiresPrewarm::class)
            val nonSubsumed = unwrappedEquals.getNonSubsumedOverriddenSymbols().filterIsInstance<FirNamedFunctionSymbol>()

            nonSubsumed.singleOrNull { !it.isAbstract }?.let { singleImplementation ->
                validateAgainstOtherMembers(validated = singleImplementation, other = nonSubsumed)
            }
        }
    }

    private fun equalityBoundIsSubtype(
        override: FirNamedFunctionSymbol,
        base: FirNamedFunctionSymbol,
        typeCheckerState: TypeCheckerState,
    ): Boolean {
        if (override === base) return true
        val overrideEB = override.equalityBoundTypeOfParameter
        val baseEB = base.equalityBoundTypeOfParameter
        if (overrideEB == null && baseEB == null) return true
        if (overrideEB == null) return false
        if (baseEB == null) return true
        return AbstractTypeChecker.isSubtypeOf(typeCheckerState, overrideEB, baseEB)
    }
}
