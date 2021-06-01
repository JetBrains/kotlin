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
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirConflictingProjectionChecker : FirBasicDeclarationChecker() {
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
        reporter: DiagnosticReporter,
    ) {
        val type = typeRef.coneTypeSafe<ConeClassLikeType>()
        val fullyExpandedType = type?.fullyExpandedType(context.session) ?: return
        val declaration = fullyExpandedType.toSymbol(context.session)?.fir.safeAs<FirRegularClass>() ?: return

        val size = minOf(declaration.typeParameters.size, typeRef.coneType.typeArguments.size)

        for (it in 0 until size) {
            val proto = declaration.typeParameters[it]
            val actual = typeRef.coneType.typeArguments[it]
            val fullyExpandedProjection = fullyExpandedType.typeArguments[it]

            val protoVariance = proto.safeAs<FirTypeParameterRef>()
                ?.symbol?.fir
                ?.variance

            if (
                (fullyExpandedProjection is ConeKotlinTypeConflictingProjection ||
                        actual is ConeKotlinTypeProjectionIn && protoVariance == Variance.OUT_VARIANCE ||
                        actual is ConeKotlinTypeProjectionOut && protoVariance == Variance.IN_VARIANCE) &&
                typeRef.source?.kind !is FirFakeSourceElementKind
            ) {
                val typeArgSource = extractTypeArgumentSource(typeRef, it)
                reporter.reportOn(
                    typeArgSource ?: typeRef.source,
                    if (type != fullyExpandedType) FirErrors.CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION else FirErrors.CONFLICTING_PROJECTION,
                    fullyExpandedType,
                    context
                )
            }
        }
    }

    private fun extractTypeArgumentSource(typeRef: FirTypeRef, index: Int): FirSourceElement? {
        if (typeRef is FirResolvedTypeRef) {
            val delegatedTypeRef = typeRef.delegatedTypeRef
            if (delegatedTypeRef is FirUserTypeRef) {
                var currentTypeArguments: List<FirTypeProjection>? = null
                var currentIndex = index
                val qualifier = delegatedTypeRef.qualifier

                for (i in qualifier.size - 1 downTo 0) {
                    val typeArguments = qualifier[i].typeArgumentList.typeArguments
                    if (currentIndex < typeArguments.size) {
                        currentTypeArguments = typeArguments
                        break
                    } else {
                        currentIndex -= typeArguments.size
                    }
                }

                val typeArgument = currentTypeArguments?.elementAtOrNull(currentIndex)
                if (typeArgument is FirTypeProjectionWithVariance) {
                    return typeArgument.source
                }
            }
        }

        return null
    }
}
