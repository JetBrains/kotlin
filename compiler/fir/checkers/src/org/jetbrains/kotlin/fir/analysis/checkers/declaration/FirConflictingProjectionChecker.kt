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
import org.jetbrains.kotlin.utils.addToStdlib.min
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirConflictingProjectionChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirPropertyAccessor) {
            return
        }

        if (declaration is FirTypedDeclaration) {
            checkTypeRef(declaration.returnTypeRef, context, reporter)
        }

        when (declaration) {
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

        val size = min(declaration.typeParameters.size, typeRef.coneType.typeArguments.size)

        for (it in 0 until size) {
            val proto = declaration.typeParameters[it]
            val actual = typeRef.coneType.typeArguments[it]

            val protoVariance = proto.safeAs<FirTypeParameterRef>()
                ?.symbol?.fir
                ?.variance
                ?: continue

            if (protoVariance == Variance.INVARIANT) {
                continue
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