/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extractArgumentsTypeRefAndSource
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.Variance

object FirProjectionRelationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirPropertyAccessor) {
            return
        }

        if (declaration is FirCallableDeclaration) {
            checkTypeRef(declaration.returnTypeRef, context, reporter)
        }

        when (declaration) {
            is FirClass -> {
                for (it in declaration.superTypeRefs) {
                    checkTypeRef(it, context, reporter)
                }
            }
            is FirTypeAlias ->
                checkTypeRef(declaration.expandedTypeRef, context, reporter)
            else -> {}
        }
    }

    private fun checkTypeRef(
        typeRef: FirTypeRef,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (typeRef.source?.kind?.shouldSkipErrorTypeReporting != false) return
        val type = typeRef.coneTypeSafe<ConeClassLikeType>()
        val fullyExpandedType = type?.fullyExpandedType(context.session) ?: return
        val declaration = fullyExpandedType.toSymbol(context.session) as? FirRegularClassSymbol ?: return
        val typeParameters = declaration.typeParameterSymbols
        val typeArguments = type.typeArguments

        val size = minOf(typeParameters.size, typeArguments.size)

        val typeRefAndSourcesForArguments = extractArgumentsTypeRefAndSource(typeRef) ?: return
        for (it in 0 until size) {
            val proto = typeParameters[it]
            val actual = typeArguments[it]
            val fullyExpandedProjection = fullyExpandedType.typeArguments[it]

            val protoVariance = proto.variance

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

            val argTypeRefSource = typeRefAndSourcesForArguments.getOrNull(it) ?: continue

            if (projectionRelation != ProjectionRelation.None && typeRef.source?.kind !is KtFakeSourceElementKind) {
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
