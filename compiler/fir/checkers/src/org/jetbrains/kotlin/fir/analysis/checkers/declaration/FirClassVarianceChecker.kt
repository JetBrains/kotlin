/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extractTypeRefAndSourceFromTypeArgument
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure

object FirClassVarianceChecker : FirClassChecker() {
    override fun check(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        checkTypeParameters(declaration.typeParameters, Variance.OUT_VARIANCE, context, reporter)

        for (superTypeRef in declaration.superTypeRefs) {
            checkVarianceConflict(superTypeRef, Variance.OUT_VARIANCE, context, reporter)
        }

        for (member in declaration.declarations) {
            if (member is FirMemberDeclaration) {
                if (Visibilities.isPrivate(member.status.visibility)) {
                    continue
                }
            }

            if (member is FirTypeParameterRefsOwner) {
                checkTypeParameters(member.typeParameters, Variance.IN_VARIANCE, context, reporter)
            }

            if (member is FirCallableDeclaration<*>) {
                checkCallableDeclaration(member, context, reporter)
            }
        }
    }

    private fun checkCallableDeclaration(
        member: FirCallableDeclaration<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val memberSource = member.source
        if (member is FirSimpleFunction) {
            if (memberSource != null && memberSource.kind !is FirFakeSourceElementKind) {
                for (param in member.valueParameters) {
                    checkVarianceConflict(param.returnTypeRef, Variance.IN_VARIANCE, context, reporter)
                }
            }
        }

        val returnTypeVariance =
            if (member is FirProperty && member.isVar) Variance.INVARIANT else Variance.OUT_VARIANCE

        var returnSource = member.returnTypeRef.source
        if (returnSource != null && memberSource != null) {
            if (returnSource.kind is FirFakeSourceElementKind && memberSource.kind !is FirFakeSourceElementKind) {
                returnSource = memberSource
            }
        }

        checkVarianceConflict(member.returnTypeRef, returnTypeVariance, context, reporter, returnSource)

        val receiverTypeRef = member.receiverTypeRef
        if (receiverTypeRef != null) {
            checkVarianceConflict(receiverTypeRef, Variance.IN_VARIANCE, context, reporter)
        }
    }

    private fun checkTypeParameters(
        typeParameters: List<FirTypeParameterRef>, variance: Variance,
        context: CheckerContext, reporter: DiagnosticReporter
    ) {
        for (typeParameter in typeParameters) {
            if (typeParameter is FirTypeParameter) {
                for (bound in typeParameter.bounds) {
                    checkVarianceConflict(bound, variance, context, reporter)
                }
            }
        }
    }

    private fun checkVarianceConflict(
        type: FirTypeRef, variance: Variance,
        context: CheckerContext, reporter: DiagnosticReporter,
        source: FirSourceElement? = null
    ) {
        checkVarianceConflict(type.coneType, variance, type, type.coneType, context, reporter, source)
    }

    private fun checkVarianceConflict(
        type: ConeKotlinType,
        variance: Variance,
        typeRef: FirTypeRef?,
        containingType: ConeKotlinType,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        source: FirSourceElement? = null,
        isInAbbreviation: Boolean = false
    ) {
        if (type is ConeTypeParameterType) {
            val fullyExpandedType = type.fullyExpandedType(context.session)
            val typeParameter = type.lookupTag.typeParameterSymbol.fir
            val resultSource = source ?: typeRef?.source
            if (resultSource != null &&
                !typeParameter.variance.allowsPosition(variance) &&
                !fullyExpandedType.attributes.contains(CompilerConeAttributes.UnsafeVariance)
            ) {
                val factory =
                    if (isInAbbreviation) FirErrors.TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE else FirErrors.TYPE_VARIANCE_CONFLICT
                reporter.reportOn(
                    resultSource,
                    factory,
                    typeParameter.symbol,
                    typeParameter.variance,
                    variance,
                    containingType,
                    context
                )
            }
            return
        }

        if (type is ConeClassLikeType) {
            val fullyExpandedType = type.fullyExpandedType(context.session)
            val declFir = fullyExpandedType.lookupTag.toSymbol(context.session)?.fir
            if (declFir is FirClass<*>) {
                for ((index, typeArgument) in fullyExpandedType.typeArguments.withIndex()) {
                    val paramVariance = (declFir.typeParameters.getOrNull(index) as? FirTypeParameter)?.variance ?: continue

                    val argVariance = when (typeArgument.kind) {
                        ProjectionKind.IN -> Variance.IN_VARIANCE
                        ProjectionKind.OUT -> Variance.OUT_VARIANCE
                        ProjectionKind.INVARIANT -> Variance.INVARIANT
                        else -> continue
                    }

                    val typeArgumentType = typeArgument.type ?: continue

                    val projectionKind = TypeCheckingProcedure.getEffectiveProjectionKind(paramVariance, argVariance)!!
                    val newVariance = when (projectionKind) {
                        TypeCheckingProcedure.EnrichedProjectionKind.OUT -> variance
                        TypeCheckingProcedure.EnrichedProjectionKind.IN -> variance.opposite()
                        TypeCheckingProcedure.EnrichedProjectionKind.INV -> Variance.INVARIANT
                        TypeCheckingProcedure.EnrichedProjectionKind.STAR -> null // CONFLICTING_PROJECTION error was reported
                    }

                    if (newVariance != null) {
                        val subTypeRefAndSource = extractTypeRefAndSourceFromTypeArgument(typeRef, index)

                        checkVarianceConflict(
                            typeArgumentType, newVariance, subTypeRefAndSource?.first, containingType,
                            context, reporter, subTypeRefAndSource?.first?.source ?: source,
                            fullyExpandedType != type
                        )
                    }
                }
            }
        }
    }
}
