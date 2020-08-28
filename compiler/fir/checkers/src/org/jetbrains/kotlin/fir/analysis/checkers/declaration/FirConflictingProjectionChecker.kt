/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirConflictingProjectionChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        // we can't just check for FirTypedDeclaration
        // because it leads to duplicate reports for
        // some cases. Maybe we should fix this via not
        // visiting the same firs twice instead.
        when (declaration) {
            is FirPropertyAccessor -> {}
            is FirProperty -> {
                checkTypeRef(declaration.returnTypeRef, context, reporter)
            }
            is FirFunction<*> -> {
                for (it in declaration.valueParameters) {
                    checkTypeRef(it.returnTypeRef, context, reporter)
                }
                checkTypeRef(declaration.returnTypeRef, context, reporter)
            }
            is FirClass<*> -> {
                for (it in declaration.superTypeRefs) {
                    checkTypeRef(it, context, reporter)
                }
            }
            is FirTypeAlias -> {
                for (it in declaration.typeParameters) {
                    if (it.variance != Variance.INVARIANT) {
                        reporter.reportVarianceNotAllowed(it.source)
                    }
                }
                checkTypeRef(declaration.expandedTypeRef, context, reporter)
            }
        }
    }

    private fun checkTypeRef(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        val declaration = typeRef.safeAs<FirResolvedTypeRef>()
            ?.coneTypeSafe<ConeClassLikeType>()
            ?.lookupTag
            ?.toSymbol(context.session)
            ?.fir.safeAs<FirRegularClass>()
            ?: return

        declaration.typeParameters.zip(typeRef.coneType.typeArguments).forEach { (proto, actual) ->
            val protoVariance = proto.safeAs<FirTypeParameterRef>()
                ?.symbol?.fir
                ?.variance
                ?: return@forEach

            if (protoVariance == Variance.INVARIANT) {
                return@forEach
            }

            if (
                actual is ConeKotlinTypeProjectionIn && protoVariance == Variance.OUT_VARIANCE ||
                actual is ConeKotlinTypeProjectionOut && protoVariance == Variance.IN_VARIANCE
            ) {
                reporter.reportConflictingProjections(typeRef.source, typeRef.coneType.toString())
                return
            }
        }
    }

    private fun DiagnosticReporter.reportConflictingProjections(source: FirSourceElement?, desiredProjection: String) {
        source?.let { report(FirErrors.CONFLICTING_PROJECTION.on(it, desiredProjection)) }
    }

    private fun DiagnosticReporter.reportVarianceNotAllowed(source: FirSourceElement?) {
        source?.let { report(FirErrors.VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED.on(it)) }
    }
}