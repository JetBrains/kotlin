/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extractArgumentsTypeRefAndSource
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.EnrichedProjectionKind
import org.jetbrains.kotlin.types.Variance

object FirClassVarianceChecker : FirClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        checkTypeParameters(
            declaration.typeParameters.filterIsInstance<FirTypeParameter>().map { it.symbol },
            Variance.OUT_VARIANCE
        )

        for (superTypeRef in declaration.superTypeRefs) {
            checkVarianceConflict(superTypeRef, Variance.OUT_VARIANCE)
        }

        declaration.processAllDeclarations(context.session) { member ->
            if (member is FirCallableSymbol) {
                if (Visibilities.isPrivate(member.resolvedStatus.visibility)) {
                    return@processAllDeclarations
                }
                checkTypeParameters(member.ownTypeParameterSymbols, Variance.IN_VARIANCE)
                checkCallableDeclaration(member)
            }

            if (member is FirClassLikeSymbol) {
                if (Visibilities.isPrivate(member.resolvedStatus.visibility)) {
                    return@processAllDeclarations
                }
                if (member !is FirClassSymbol) {
                    checkTypeParameters(member.ownTypeParameterSymbols, Variance.IN_VARIANCE)
                }
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCallableDeclaration(
        member: FirCallableSymbol<*>,
    ) {
        val memberSource = member.source
        if (memberSource != null && memberSource.kind !is KtFakeSourceElementKind) {
            for (param in member.contextParameterSymbols) {
                checkVarianceConflict(param.resolvedReturnTypeRef, Variance.IN_VARIANCE)
            }

            if (member is FirNamedFunctionSymbol) {
                for (param in member.valueParameterSymbols) {
                    checkVarianceConflict(param.resolvedReturnTypeRef, Variance.IN_VARIANCE)
                }
            }
        }

        val returnTypeVariance =
            if (member is FirPropertySymbol && member.isVar) Variance.INVARIANT else Variance.OUT_VARIANCE

        var returnSource = member.resolvedReturnTypeRef.source
        if (returnSource != null) {
            if (memberSource != null && returnSource.kind is KtFakeSourceElementKind && memberSource.kind !is KtFakeSourceElementKind) {
                returnSource = memberSource
            }
        } else {
            returnSource = memberSource
        }

        checkVarianceConflict(member.resolvedReturnTypeRef, returnTypeVariance, returnSource)

        val receiverTypeRef = member.resolvedReceiverTypeRef
        if (receiverTypeRef != null) {
            checkVarianceConflict(receiverTypeRef, Variance.IN_VARIANCE)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkTypeParameters(
        typeParameters: List<FirTypeParameterSymbol>, variance: Variance,
    ) {
        for (typeParameter in typeParameters) {
            for (bound in typeParameter.resolvedBounds) {
                checkVarianceConflict(bound, variance)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVarianceConflict(
        typeRef: FirTypeRef, variance: Variance,
        source: KtSourceElement? = null
    ) {
        val expandedType = typeRef.coneType.fullyExpandedType()
        checkVarianceConflict(
            type = expandedType,
            variance = variance,
            typeRef = typeRef,
            containingType = expandedType,
            source = source ?: typeRef.source,
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVarianceConflict(
        type: ConeKotlinType,
        variance: Variance,
        typeRef: FirTypeRef?,
        containingType: ConeKotlinType,
        source: KtSourceElement? = null,
        isInAbbreviation: Boolean = false,
    ) {
        if (type is ConeTypeParameterType) {
            val fullyExpandedType = type.fullyExpandedType()
            val typeParameterSymbol = type.lookupTag.typeParameterSymbol
            val resultSource = source ?: typeRef?.source
            if (resultSource != null &&
                !typeParameterSymbol.variance.allowsPosition(variance) &&
                !fullyExpandedType.attributes.contains(CompilerConeAttributes.UnsafeVariance)
            ) {
                val factory =
                    if (isInAbbreviation) FirErrors.TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE else FirErrors.TYPE_VARIANCE_CONFLICT_ERROR
                reporter.reportOn(
                    resultSource,
                    factory,
                    typeParameterSymbol,
                    typeParameterSymbol.variance,
                    variance,
                    containingType
                )
            }
            return
        }

        if (type is ConeClassLikeType) {
            val fullyExpandedType = type.fullyExpandedType()
            val classSymbol = fullyExpandedType.lookupTag.toSymbol(context.session)
            if (classSymbol is FirClassSymbol<*>) {
                val typeRefAndSourcesForArguments = extractArgumentsTypeRefAndSource(typeRef)
                for ((index, typeArgument) in fullyExpandedType.typeArguments.withIndex()) {
                    val paramVariance = classSymbol.typeParameterSymbols.getOrNull(index)?.variance ?: continue

                    val argVariance = when (typeArgument.kind) {
                        ProjectionKind.IN -> Variance.IN_VARIANCE
                        ProjectionKind.OUT -> Variance.OUT_VARIANCE
                        ProjectionKind.INVARIANT -> Variance.INVARIANT
                        else -> continue
                    }

                    val typeArgumentType = typeArgument.type ?: continue

                    val newVariance = when (EnrichedProjectionKind.getEffectiveProjectionKind(paramVariance, argVariance)) {
                        EnrichedProjectionKind.OUT -> variance
                        EnrichedProjectionKind.IN -> variance.opposite()
                        EnrichedProjectionKind.INV -> Variance.INVARIANT
                        EnrichedProjectionKind.STAR -> null // CONFLICTING_PROJECTION error was reported
                    }

                    if (newVariance != null) {
                        val subTypeRefAndSource = typeRefAndSourcesForArguments?.getOrNull(index)

                        checkVarianceConflict(
                            typeArgumentType, newVariance, subTypeRefAndSource?.typeRef, containingType,
                            subTypeRefAndSource?.typeRef?.source ?: source, type.isTypealiasExpansion
                        )
                    }
                }
            }
        }
    }
}
