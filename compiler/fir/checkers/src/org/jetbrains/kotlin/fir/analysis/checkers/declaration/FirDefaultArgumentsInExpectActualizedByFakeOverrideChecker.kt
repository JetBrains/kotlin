/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirExpectActualMatchingContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getSingleExpectForActualOrNull
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.expectActualMatchingContextFactory
import org.jetbrains.kotlin.fir.isDelegated
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualMatcher
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

// TODO KT-62913 create one more ExpectActualCheckingCompatibility incompatibility, and replace this checker with this incompatibility
internal object FirDefaultArgumentsInExpectActualizedByFakeOverrideChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects) ||
            !context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitDefaultArgumentsInExpectActualizedByFakeOverride)) {
            return
        }
        if (!declaration.isActual) {
            return
        }
        val actualClassSymbol = declaration.symbol
        // We want to report errors even if a candidate is incompatible, but it's single
        val expectedSingleCandidate = actualClassSymbol.getSingleExpectForActualOrNull() ?: return
        val expectClassSymbol = expectedSingleCandidate as FirRegularClassSymbol

        val expectActualMatchingContext = context.session.expectActualMatchingContextFactory.create(
            context.session, context.scopeSession,
            allowedWritingMemberExpectForActualMapping = true,
        )
        AbstractExpectActualMatcher.recursivelyMatchClassScopes(expectClassSymbol, actualClassSymbol, expectActualMatchingContext)

        val matchingContext = context.session.expectActualMatchingContextFactory.create(context.session, context.scopeSession)
        val problematicExpectMembers = with(matchingContext) { findProblematicExpectMembers(expectClassSymbol, actualClassSymbol) }
        if (problematicExpectMembers.isNotEmpty()) {
            reporter.reportOn(
                declaration.source, FirErrors.DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE,
                expectClassSymbol, problematicExpectMembers, context
            )
        }
    }

    private fun FirExpectActualMatchingContext.findProblematicExpectMembers(
        expectClassSymbol: FirRegularClassSymbol, actualClassSymbol: FirRegularClassSymbol,
    ): List<FirNamedFunctionSymbol> {
        val actualFakeOverrideMembers = actualClassSymbol.collectAllMembers(isActualDeclaration = true)
            .filterIsInstance<FirNamedFunctionSymbol>()
            .filter { isFakeOverride(it, actualClassSymbol) }

        return actualFakeOverrideMembers
            .mapNotNull { getSingleMatchingExpect(it, expectClassSymbol, actualClassSymbol) }
            .filter(::hasDefaultArgumentValues)
    }

    private fun isFakeOverride(member: FirNamedFunctionSymbol, containingClassSymbol: FirRegularClassSymbol): Boolean {
        val memberReceiverClassId = member.dispatchReceiverType?.classId ?: return false
        if (memberReceiverClassId != containingClassSymbol.classId) {
            return true
        }
        return member.isSubstitutionOrIntersectionOverride || member.isDelegated
    }

    private fun FirExpectActualMatchingContext.getSingleMatchingExpect(
        actualMember: FirNamedFunctionSymbol,
        expectSymbol: FirRegularClassSymbol,
        actualSymbol: FirRegularClassSymbol
    ): FirNamedFunctionSymbol? {
        val potentialExpects = findPotentialExpectClassMembersForActual(
            expectSymbol, actualSymbol, actualMember,
        )
        val expectMember: DeclarationSymbolMarker = potentialExpects.entries
            .singleOrNull { it.value == ExpectActualMatchingCompatibility.MatchedSuccessfully }?.key
            ?: potentialExpects.keys.singleOrNull()
            ?: return null
        return expectMember as FirNamedFunctionSymbol
    }

    private fun hasDefaultArgumentValues(function: FirFunctionSymbol<*>): Boolean {
        return function.valueParameterSymbols.any { it.hasDefaultValue }
    }
}