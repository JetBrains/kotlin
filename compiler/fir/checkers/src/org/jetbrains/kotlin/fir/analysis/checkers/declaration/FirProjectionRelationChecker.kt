/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extractArgumentTypeRefAndSource
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirProjectionRelationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirPropertyAccessor) {
            return
        }

        if (declaration is FirTypedDeclaration) {
            // The body of function contract is not fully resolved.
            if (declaration.resolvePhase == FirResolvePhase.CONTRACTS) {
                return
            }
            checkTypeRef(declaration.returnTypeRef, context, reporter)
        }

        when (declaration) {
            is FirClass<*> -> {
                for (it in declaration.superTypeRefs) {
                    checkTypeRef(it, context, reporter)
                }
            }
            is FirTypeAlias ->
                checkTypeRef(declaration.expandedTypeRef, context, reporter)
        }
    }

    private fun checkTypeRef(
        typeRef: FirTypeRef,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val type = typeRef.coneTypeSafe<ConeClassLikeType>()
        val fullyExpandedType = type?.fullyExpandedType(context.session) ?: return
        val declaration = fullyExpandedType.toSymbol(context.session)?.fir.safeAs<FirRegularClass>() ?: return
        val typeParameters = declaration.typeParameters
        val typeArguments = type.typeArguments

        val size = minOf(typeParameters.size, typeArguments.size)

        for (it in 0 until size) {
            val proto = typeParameters[it]
            val actual = typeArguments[it]
            val fullyExpandedProjection = fullyExpandedType.typeArguments[it]

            val protoVariance = proto.safeAs<FirTypeParameterRef>()
                ?.symbol?.fir
                ?.variance

            val projectionRelation = if (fullyExpandedProjection is ConeKotlinTypeConflictingProjection ||
                actual is ConeKotlinTypeProjectionIn && protoVariance == Variance.OUT_VARIANCE ||
                actual is ConeKotlinTypeProjectionOut && protoVariance == Variance.IN_VARIANCE
            ) {
                ProjectionRelation.Conflicting
            } else if (actual is ConeKotlinTypeProjectionIn && protoVariance == Variance.IN_VARIANCE ||
                actual is ConeKotlinTypeProjectionOut && protoVariance == Variance.OUT_VARIANCE
            ) {
                ProjectionRelation.Redundant
            } else {
                ProjectionRelation.None
            }

            val argTypeRefSource = extractArgumentTypeRefAndSource(typeRef, it) ?: continue

            if (projectionRelation != ProjectionRelation.None && typeRef.source?.kind !is FirFakeSourceElementKind) {
                reporter.reportOn(
                    argTypeRefSource.source ?: argTypeRefSource.typeRef?.source,
                    if (projectionRelation == ProjectionRelation.Conflicting)
                        if (type != fullyExpandedType) FirErrors.CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION else FirErrors.CONFLICTING_PROJECTION
                    else
                        FirErrors.REDUNDANT_PROJECTION,
                    fullyExpandedType,
                    context
                )
            }

            argTypeRefSource.typeRef?.let { checkTypeRef(it, context, reporter) }
        }
    }

    private enum class ProjectionRelation {
        Conflicting,
        Redundant,
        None
    }
}
