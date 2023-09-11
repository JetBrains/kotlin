/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.collectUpperBounds
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.getOwnerLookupTag
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.*

/**
 * @see org.jetbrains.kotlin.resolve.checkers.MissingDependencySupertypeChecker
 */
object FirMissingDependencySupertypeChecker {
    object ForDeclarations : FirBasicDeclarationChecker() {
        override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
            if (declaration is FirClass) {
                checkSupertypes(declaration.symbol, declaration.source, reporter, context)
            }

            if (declaration is FirTypeParameterRefsOwner) {
                for (typeParameter in declaration.typeParameters) {
                    for (upperBound in typeParameter.toConeType().collectUpperBounds()) {
                        checkSupertypes(upperBound, typeParameter.source, reporter, context)
                    }
                }
            }
        }
    }

    object ForQualifiedAccessExpressions : FirQualifiedAccessExpressionChecker() {
        override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
            val source = expression.source

            val symbol = expression.calleeReference.toResolvedCallableSymbol()
            if (symbol == null) {
                val receiverType = expression.explicitReceiver?.resolvedType
                    ?.lowerBoundIfFlexible()?.originalIfDefinitelyNotNullable()?.fullyExpandedType(context.session)
                checkSupertypes(receiverType, source, reporter, context)
                return
            }

            val missingSupertype = checkSupertypes(symbol.dispatchReceiverType, source, reporter, context)

            val eagerChecksAllowed = context.languageVersionSettings.getFlag(AnalysisFlags.extendedCompilerChecks)
            val unresolvedLazySupertypesByDefault = symbol is FirConstructorSymbol || symbol is FirAnonymousFunctionSymbol

            if (eagerChecksAllowed || !unresolvedLazySupertypesByDefault && !missingSupertype) {
                checkSupertypes(symbol.getOwnerLookupTag()?.toSymbol(context.session), source, reporter, context)
                checkSupertypes(symbol.resolvedReceiverTypeRef?.coneTypeOrNull, source, reporter, context)
            }
        }
    }

    fun checkSupertypes(
        classifierType: ConeKotlinType?,
        source: KtSourceElement?,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ): Boolean = checkSupertypes(classifierType?.toSymbol(context.session), source, reporter, context)

    fun checkSupertypes(
        declaration: FirBasedSymbol<*>?,
        source: KtSourceElement?,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ): Boolean {
        if (declaration !is FirClassSymbol<*>) return false

        var missingSupertype = false
        val superTypes = declaration.collectSuperTypes(context.session)
        for (superType in superTypes) {
            if (superType is ConeErrorType || superType is ConeDynamicType) continue // Ignore types which are already errors.

            val superTypeSymbol = superType.toSymbol(context.session)
            if (superTypeSymbol == null) {
                missingSupertype = true

                reporter.reportOn(
                    source,
                    FirErrors.MISSING_DEPENDENCY_SUPERCLASS,
                    superType.withArguments(emptyArray()).withNullability(ConeNullability.NOT_NULL, context.session.typeContext),
                    declaration.constructType(emptyArray(), false),
                    context
                )
            }
        }

        return missingSupertype
    }

    private fun FirClassSymbol<*>.collectSuperTypes(session: FirSession): Set<ConeKotlinType> {
        val superTypes = mutableSetOf<ConeKotlinType>()
        fun collect(symbol: FirClassSymbol<*>) {
            for (superType in symbol.resolvedSuperTypes) {
                if (superTypes.add(superType)) {
                    (superType.toSymbol(session) as? FirClassSymbol<*>)?.let(::collect)
                }
            }
        }
        collect(this)
        return superTypes
    }
}
