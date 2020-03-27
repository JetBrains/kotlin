/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.call

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.firClassLike
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirSuperclassNotAccessibleFromInterfaceChecker : FirQualifiedAccessChecker() {
    override fun check(functionCall: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val closestClass = context.findClosest<FirRegularClass>() ?: return

        // check if super receiver is present
        functionCall.explicitReceiver.safeAs<FirQualifiedAccessExpression>()
            ?.calleeReference.safeAs<FirSuperReference>()
            ?: return

        if (closestClass.classKind == ClassKind.INTERFACE) {
            val origin = getClassLikeDeclaration(functionCall, context)
                ?.symbol.safeAs<FirRegularClassSymbol>()
                ?.fir
                ?: return

            if (origin.source != null && origin.isSuperclassOf(closestClass)) {
                reporter.report(functionCall.explicitReceiver?.source)
            }
        }
    }

    /**
     * Returns the ClassLikeDeclaration where the function has been defined
     * or null if no proper declaration has been found.
     */
    private fun getClassLikeDeclaration(functionCall: FirQualifiedAccessExpression, context: CheckerContext): FirClassLikeDeclaration<*>? {
        val classId = functionCall.calleeReference.safeAs<FirResolvedNamedReference>()
            ?.resolvedSymbol.safeAs<FirNamedFunctionSymbol>()
            ?.callableId
            ?.classId
            ?: return null

        if (!classId.isLocal) {
            return context.session.firSymbolProvider.getClassLikeSymbolByFqName(classId)?.fir
        }

        return null
    }

    /**
     * Returns true if this is a superclass of other.
     */
    private fun FirClass<*>.isSuperclassOf(other: FirClass<*>): Boolean {
        /**
         * Hides additional parameters.
         */
        fun FirClass<*>.isSuperclassOf(other: FirClass<*>, exclude: MutableSet<FirClass<*>>): Boolean {
            for (it in other.superTypeRefs) {
                var that = it.firClassLike(session)

                if (that is FirTypeAlias) {
                    that = that.expandedTypeRef.firClassLike(session)
                }

                val candidate = that as? FirClass<*> ?: continue

                if (candidate in exclude) {
                    continue
                }

                if (candidate.classKind == ClassKind.CLASS) {
                    if (candidate == this) {
                        return true
                    }

                    exclude.add(candidate)
                    return this.isSuperclassOf(candidate, exclude)
                }
            }

            return false
        }

        return isSuperclassOf(other, mutableSetOf())
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let {
            report(FirErrors.SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE.on(it))
        }
    }
}