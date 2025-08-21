/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsStableName
import org.jetbrains.kotlin.fir.analysis.js.checkers.collectNameClashesWith
import org.jetbrains.kotlin.fir.analysis.js.checkers.isPresentInGeneratedCode
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.getNonSubsumedOverriddenSymbols
import org.jetbrains.kotlin.fir.declarations.processAllClassifiers
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.unwrapFakeOverridesOrDelegated
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.popLast

sealed class FirJsNameClashClassMembersChecker(mppKind: MppCheckerKind) : FirClassChecker(mppKind) {
    object Regular : FirJsNameClashClassMembersChecker(MppCheckerKind.Platform) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirClass) {
            if (declaration.isExpect) return
            super.check(declaration)
        }
    }

    object ForExpectClass : FirJsNameClashClassMembersChecker(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirClass) {
            if (!declaration.isExpect) return
            super.check(declaration)
        }
    }

    private class StableNamesCollector {
        val jsStableNames = mutableSetOf<FirJsStableName>()
        val overrideIntersections = hashMapOf<FirCallableSymbol<*>, HashSet<FirCallableSymbol<*>>>()

        private val allSymbols = mutableSetOf<FirCallableSymbol<*>>()

        private fun FirTypeScope.collectOverriddenLeaves(classMemberSymbol: FirCallableSymbol<*>): Set<FirCallableSymbol<*>> {
            val startMemberWithScope = MemberWithBaseScope(classMemberSymbol, this)
            val visitedSymbols = hashSetOf(startMemberWithScope)
            val symbolsToProcess = mutableListOf(startMemberWithScope)
            val leaves = mutableSetOf<FirCallableSymbol<*>>()
            while (symbolsToProcess.isNotEmpty()) {
                val (processingSymbol, scope) = symbolsToProcess.popLast()
                val overriddenMembers = scope.getDirectOverriddenMembersWithBaseScopeSafe(processingSymbol)
                for (overriddenMemberWithScope in overriddenMembers) {
                    if (visitedSymbols.add(overriddenMemberWithScope)) {
                        symbolsToProcess.add(overriddenMemberWithScope)
                    }
                }
                if (overriddenMembers.isEmpty()) {
                    leaves.add(processingSymbol)
                }
            }
            return leaves
        }

        context(context: CheckerContext)
        private fun MutableSet<FirJsStableName>.addStableJavaScriptName(
            targetSymbol: FirCallableSymbol<*>?,
            overriddenSymbol: FirCallableSymbol<*>?,
        ) {
            val stableName = when {
                targetSymbol == null || overriddenSymbol == null -> return
                (targetSymbol as? FirConstructorSymbol)?.isPrimary == true -> return
                !overriddenSymbol.isPresentInGeneratedCode(context.session) && overriddenSymbol.isFinal -> return
                else -> FirJsStableName.createStableNameOrNull(overriddenSymbol, context.session) ?: return
            }

            if (stableName.isPresentInGeneratedCode) {
                add(stableName.copy(symbol = targetSymbol))
            } else {
                val isPresentInGeneratedCode = when (targetSymbol.origin) {
                    is FirDeclarationOrigin.SubstitutionOverride -> overriddenSymbol.isPresentInGeneratedCode(context.session)
                    else -> targetSymbol.isPresentInGeneratedCode(context.session)
                }
                val inheritedExternalName = stableName.copy(
                    symbol = targetSymbol,
                    canBeMangled = false,
                    isPresentInGeneratedCode = isPresentInGeneratedCode
                )
                add(inheritedExternalName)
            }
        }

        context(context: CheckerContext)
        private fun MutableSet<FirJsStableName>.addAllStableJavaScriptNames(
            targetSymbol: FirCallableSymbol<*>,
            overriddenSymbol: FirCallableSymbol<*>,
        ) {
            addStableJavaScriptName(targetSymbol, overriddenSymbol)
            if (targetSymbol is FirPropertySymbol && overriddenSymbol is FirPropertySymbol) {
                addStableJavaScriptName(targetSymbol.getterSymbol, overriddenSymbol.getterSymbol)
                addStableJavaScriptName(targetSymbol.setterSymbol, overriddenSymbol.setterSymbol)
            }
        }

        fun addAllSymbolsFrom(symbols: Collection<FirCallableSymbol<*>>, sessionHolder: SessionAndScopeSessionHolder) {
            for (symbol in symbols) {
                when (symbol) {
                    is FirIntersectionCallableSymbol -> {
                        @OptIn(ScopeFunctionRequiresPrewarm::class) // the symbols come from calling process*ByName
                        val nonSubsumedOverriddenSymbols = symbol.getNonSubsumedOverriddenSymbols(
                            sessionHolder.session,
                            sessionHolder.scopeSession
                        )
                        val overriddenSymbols = nonSubsumedOverriddenSymbols.map { it.originalForSubstitutionOverride ?: it }
                        addAllSymbolsFrom(overriddenSymbols, sessionHolder)
                        for (intersectedSymbol in overriddenSymbols) {
                            overrideIntersections.getOrPut(intersectedSymbol) { hashSetOf() }.addAll(overriddenSymbols)
                        }
                    }
                    else -> allSymbols.add(symbol)
                }
            }
        }

        context(context: CheckerContext)
        fun processStableJavaScriptNamesForMembers(declaration: FirClass) {
            declaration.symbol.processAllClassifiers(context.session) { classMemberSymbol ->
                if (classMemberSymbol is FirClassLikeSymbol) {
                    jsStableNames.addIfNotNull(FirJsStableName.createStableNameOrNull(classMemberSymbol, context.session))
                }
            }

            val scope = declaration.symbol.unsubstitutedScope()

            scope.processDeclaredConstructors(allSymbols::add)
            addAllSymbolsFrom(scope.collectAllFunctions(), context.sessionHolder)
            addAllSymbolsFrom(scope.collectAllProperties(), context.sessionHolder)

            for (callableMemberSymbol in allSymbols) {
                val overriddenLeaves = scope.collectOverriddenLeaves(callableMemberSymbol)
                for (symbol in overriddenLeaves) {
                    jsStableNames.addAllStableJavaScriptNames(callableMemberSymbol, symbol)
                }
            }
        }
    }

    private fun List<FirJsStableName>.filterFakeOverrideNames(declaration: FirClass) = filterTo(mutableSetOf()) {
        it.symbol.getContainingClassSymbol() != declaration.symbol
    }

    private data class ClashedSymbol(val symbol: FirBasedSymbol<*>, val clashedWith: List<FirBasedSymbol<*>>)

    context(context: CheckerContext)
    private fun List<FirJsStableName>.collectNonFakeOverrideClashes(isFakeOverrideName: (FirJsStableName) -> Boolean): List<ClashedSymbol> {
        return buildList {
            for (stableName in this@collectNonFakeOverrideClashes) {
                var clashed = collectNameClashesWith(stableName)
                if (isFakeOverrideName(stableName)) {
                    // Do not check for clashes between fake overrides here.
                    // Such clashes will be checked with JS_FAKE_NAME_CLASH.
                    clashed = clashed.filter { !isFakeOverrideName(it) }
                }
                if (clashed.isNotEmpty()) {
                    add(ClashedSymbol(stableName.symbol, clashed.map { it.symbol }))
                }
            }
        }
    }

    context(context: CheckerContext)
    private fun Set<FirJsStableName>.findFirstFakeOverrideClash(stableNameCollector: StableNamesCollector): ClashedSymbol? {
        for (stableName in this) {
            val intersectedSymbols = stableNameCollector.overrideIntersections[stableName.symbol] ?: emptySet()
            val clashed = collectNameClashesWith(stableName).filter {
                // intersected fake override symbols are not clashed
                it.symbol !in intersectedSymbols
            }
            if (clashed.isNotEmpty()) {
                return ClashedSymbol(stableName.symbol, clashed.map { it.symbol })
            }
        }
        return null
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (!declaration.symbol.isPresentInGeneratedCode(context.session)) {
            return
        }

        val stableNameCollector = StableNamesCollector()
        stableNameCollector.processStableJavaScriptNamesForMembers(declaration)

        val membersGroupedByName = stableNameCollector.jsStableNames.groupBy { it.name }

        for ((name, stableNames) in membersGroupedByName.entries) {
            val fakeOverrideStableNames = stableNames.filterFakeOverrideNames(declaration)

            val nonFakeOverrideClashes = stableNames.collectNonFakeOverrideClashes { it in fakeOverrideStableNames }
            for ((symbol, clashedWith) in nonFakeOverrideClashes) {
                val source = when (symbol) {
                    is FirCallableSymbol<*> -> symbol.unwrapFakeOverridesOrDelegated().source
                    else -> symbol.source
                } ?: declaration.source
                reporter.reportOn(source, FirJsErrors.JS_NAME_CLASH, name, clashedWith)
            }

            fakeOverrideStableNames.findFirstFakeOverrideClash(stableNameCollector)?.let { (fakeOverrideSymbol, clashedWith) ->
                reporter.reportOn(declaration.source, FirJsErrors.JS_FAKE_NAME_CLASH, name, fakeOverrideSymbol, clashedWith)
            }
        }
    }
}
