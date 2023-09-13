/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.getSingleExpectForActualOrNull
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.scopes.collectAllFunctions
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.getSingleClassifier
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.*

internal object FirDefaultArgumentsInExpectWithActualTypealiasChecker : FirTypeAliasChecker() {
    override fun check(declaration: FirTypeAlias, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects) ||
            !context.languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions)
        ) {
            return
        }
        if (!declaration.isActual) {
            return
        }
        val actualTypealiasSymbol = declaration.symbol
        // We want to report errors even if a candidate is incompatible, but it's single
        val expectedSingleCandidate = actualTypealiasSymbol.getSingleExpectForActualOrNull() ?: return
        val expectClassSymbol = expectedSingleCandidate as FirRegularClassSymbol

        val membersWithDefaultValueParameters = getMembersWithDefaultValueParametersUnlessAnnotation(expectClassSymbol)
        if (membersWithDefaultValueParameters.isEmpty()) return

        reporter.reportOn(
            declaration.source,
            FirErrors.DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS,
            expectClassSymbol,
            membersWithDefaultValueParameters,
            context
        )
    }

    private fun getMembersWithDefaultValueParametersUnlessAnnotation(classSymbol: FirClassSymbol<*>): List<FirFunctionSymbol<*>> {
        val result = mutableListOf<FirFunctionSymbol<*>>()

        fun collectFunctions(classSymbol: FirClassSymbol<*>) {
            if (classSymbol.classKind == ClassKind.ANNOTATION_CLASS) {
                return
            }
            val memberScope = classSymbol.declaredMemberScope(classSymbol.moduleData.session, memberRequiredPhase = null)
            val functionsAndConstructors = memberScope
                .run { collectAllFunctions() + getDeclaredConstructors() }

            functionsAndConstructors.filterTo(result) { it.valueParameterSymbols.any(FirValueParameterSymbol::hasDefaultValue) }

            val nestedClasses = memberScope.getClassifierNames()
                .mapNotNull { memberScope.getSingleClassifier(it) as? FirClassSymbol<*> }

            for (nestedClassSymbol in nestedClasses) {
                collectFunctions(nestedClassSymbol)
            }
        }

        collectFunctions(classSymbol)
        return result
    }
}