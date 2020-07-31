/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirCyclicConstructorDelegationCallChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirRegularClass) {
            return
        }

        val cyclicConstructors = mutableSetOf<FirConstructor>()
        var hasPrimaryConstructor = false

        for (it in declaration.declarations) {
            if (it is FirConstructor) {
                if (!it.isPrimary) {
                    it.findCycle(cyclicConstructors)?.let { visited ->
                        cyclicConstructors += visited
                    }
                } else {
                    hasPrimaryConstructor = true
                }
            }
        }

        if (hasPrimaryConstructor) {
            for (it in declaration.declarations) {
                if (
                    it is FirConstructor && !it.isPrimary && it !in cyclicConstructors &&
                    it.delegatedConstructor?.constructedTypeRef?.getType() != it.returnTypeRef.getType()
                ) {
                    reporter.reportPrimaryConstructorDelegationCallExpected(it.delegatedConstructor?.source)
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

    private fun FirTypeRef.getType() = when (this) {
        is FirResolvedTypeRef -> type
        else -> null
    }

    private fun DiagnosticReporter.reportCyclicConstructorDelegationCall(source: FirSourceElement?) {
        source?.let { report(FirErrors.CYCLIC_CONSTRUCTOR_DELEGATION_CALL.on(it)) }
    }

    private fun DiagnosticReporter.reportPrimaryConstructorDelegationCallExpected(source: FirSourceElement?) {
        source?.let { report(FirErrors.PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED.on(it)) }
    }
}