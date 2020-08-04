/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirCommonConstructorDelegationIssuesChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirRegularClass) {
            return
        }

        val cyclicConstructors = mutableSetOf<FirConstructor>()
        var hasPrimaryConstructor = false

        // secondary; non-cyclic;
        // candidates for further analysis
        val otherConstructors = mutableSetOf<FirConstructor>()

        for (it in declaration.declarations) {
            if (it is FirConstructor) {
                if (!it.isPrimary) {
                    otherConstructors += it

                    it.findCycle(cyclicConstructors)?.let { visited ->
                        cyclicConstructors += visited
                    }
                } else {
                    hasPrimaryConstructor = true
                }
            }
        }

        otherConstructors -= cyclicConstructors

        if (hasPrimaryConstructor) {
            for (it in otherConstructors) {
                if (it.delegatedConstructor?.isThis != true) {
                    if (it.delegatedConstructor?.source != null) {
                        reporter.reportPrimaryConstructorDelegationCallExpected(it.delegatedConstructor?.source)
                    } else {
                        reporter.reportPrimaryConstructorDelegationCallExpected(it.source)
                    }
                }
            }
        } else {
            for (it in otherConstructors) {
                val callee = it.delegatedConstructor?.calleeReference

                // couldn't find proper super() constructor implicitly
                if (
                    callee is FirErrorNamedReference && callee.diagnostic is ConeAmbiguityError &&
                    it.delegatedConstructor?.source?.kind is FirFakeSourceElementKind
                ) {
                    reporter.reportExplicitDelegationCallRequired(it.source)
                }
            }
        }

        cyclicConstructors.forEach {
            reporter.reportCyclicConstructorDelegationCall(it.delegatedConstructor?.source)
        }
    }

    private fun FirConstructor.findCycle(knownCyclicConstructors: Set<FirConstructor> = emptySet()): Set<FirConstructor>? {
        val visitedConstructors = mutableSetOf(this)

        var it = this
        var delegated = this.getDelegated()

        while (!it.isPrimary && delegated != null) {
            if (delegated in visitedConstructors || delegated in knownCyclicConstructors) {
                return visitedConstructors
            }

            it = delegated
            delegated = delegated.getDelegated()
            visitedConstructors.add(it)
        }

        return null
    }

    private fun FirConstructor.getDelegated(): FirConstructor? = delegatedConstructor
        ?.calleeReference.safeAs<FirResolvedNamedReference>()
        ?.resolvedSymbol
        ?.fir.safeAs()

    private fun DiagnosticReporter.reportCyclicConstructorDelegationCall(source: FirSourceElement?) {
        source?.let { report(FirErrors.CYCLIC_CONSTRUCTOR_DELEGATION_CALL.on(it)) }
    }

    private fun DiagnosticReporter.reportPrimaryConstructorDelegationCallExpected(source: FirSourceElement?) {
        source?.let { report(FirErrors.PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED.on(it)) }
    }

    private fun DiagnosticReporter.reportExplicitDelegationCallRequired(source: FirSourceElement?) {
        source?.let { report(FirErrors.EXPLICIT_DELEGATION_CALL_REQUIRED.on(it)) }
    }
}