/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.typeChangeRelatedTo
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableWrongReceiver
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

object FirUpperBoundViolatedQualifiedAccessExpressionChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        // something that contains the type parameters
        // declarations with their declared bounds.
        // it may be the called function declaration
        // or the class declaration
        val calleeSymbol = when (val calleeReference = expression.calleeReference) {
            is FirResolvedErrorReference -> {
                if (calleeReference.diagnostic is ConeInapplicableWrongReceiver) {
                    return
                }
                calleeReference.toResolvedCallableSymbol()
            }
            is FirResolvedNamedReference -> calleeReference.toResolvedCallableSymbol()
            else -> null
        }
        val typeArguments: List<ConeTypeProjection> = expression.typeArguments.toTypeArgumentsWithSourceInfo()
        val typeParameters: List<FirTypeParameterSymbol> = calleeSymbol?.typeParameterSymbols ?: return

        // Neither common calls nor type alias constructor calls may contain projections
        // That should be checked somewhere else
        if (typeArguments.any { it !is ConeKotlinType }) {
            return
        }

        if (typeArguments.size != typeParameters.size) return

        val substitutor = createSubstitutorForUpperBoundViolationCheck(
            typeParameters,
            typeArguments,
            context.session,
        )

        val typeArgumentsAfterArgumentInteractionsFix = when {
            LanguageFeature.ReportUpperBoundViolatedInCallArgumentInteractions.isDisabled() -> typeArguments.map {
                val projectionType = it.type ?: return@map it
                val typeChange = projectionType.typeChangeRelatedTo(LanguageFeature.ReportUpperBoundViolatedInCallArgumentInteractions)
                typeChange?.newType?.withAttributes(projectionType.attributes)?.let(it::replaceType) ?: it
            }
            else -> null
        }
        val substitutorAfterArgumentInteractionsFix = typeArgumentsAfterArgumentInteractionsFix?.let {
            createSubstitutorForUpperBoundViolationCheck(
                typeParameters,
                typeArgumentsAfterArgumentInteractionsFix,
                context.session,
            )
        }

        context(reporter: DiagnosticReporter)
        fun runTheCheck(
            substitutor: ConeSubstitutor,
            typeArguments: List<ConeTypeProjection>,
            mustRelaxDueToArgumentInteractionsBug: Boolean,
        ) {
            val typeAliasConstructorInfo = (calleeSymbol as? FirConstructorSymbol)?.typeAliasConstructorInfo

            if (typeAliasConstructorInfo != null) {
                val typealiasType: ConeClassLikeType = typeAliasConstructorInfo.typeAliasSymbol.defaultType()
                checkUpperBoundViolated(
                    typeRef = null,
                    // Return types of constructors obtained from typealiases (e.g., `TA()`) remain expanded even when
                    // `aliasedTypeExpansionGloballyEnabled == false`, hence the workaround instead of `abbreviatedTypeOrSelf`.`
                    // See: `TypeAliasConstructorsSubstitutingScope.createTypealiasConstructor` and `typeAliasConstructorCrazyProjections.fir.kt`.
                    notExpandedType = substitutor.substituteOrSelf(typealiasType) as ConeClassLikeType,
                    fallbackSource = expression.source,
                    mustRelaxDueToArgumentInteractionsBug = mustRelaxDueToArgumentInteractionsBug,
                )
            } else {
                checkUpperBoundViolated(
                    typeParameters,
                    typeArguments,
                    substitutor,
                    fallbackSource = expression.source,
                    isTypealiasExpansion = false,
                    mustRelaxDueToArgumentInteractionsBug = mustRelaxDueToArgumentInteractionsBug,
                )
            }
        }

        if (substitutorAfterArgumentInteractionsFix == null) {
            runTheCheck(substitutor, typeArguments, mustRelaxDueToArgumentInteractionsBug = false)
            return
        }

        val wereAnyErrors = detectErrorDiagnosticsReported {
            runTheCheck(substitutor, typeArguments, mustRelaxDueToArgumentInteractionsBug = false)
        }

        runTheCheck(
            substitutorAfterArgumentInteractionsFix,
            typeArgumentsAfterArgumentInteractionsFix,
            mustRelaxDueToArgumentInteractionsBug = !wereAnyErrors,
        )
    }

    private fun ConeTypeProjection.withSourceRecursive(expression: FirQualifiedAccessExpression): ConeTypeProjection {
        // Recursively apply source to any arguments of this type.
        val type = when {
            this is ConeClassLikeType && typeArguments.isNotEmpty() -> withArguments { it.withSourceRecursive(expression) }
            else -> this
        }

        // Try to match the expanded type arguments back to the original expression type arguments.
        return when (val argument = expression.typeArguments.find { it.toConeTypeProjection() === this }) {
            // Unable to find a matching argument, fall back to marking the entire expression.
            null -> type.withSource(FirTypeRefSource(null, expression.source))
            // Found the original argument!
            else -> type.withSource(FirTypeRefSource((argument as? FirTypeProjectionWithVariance)?.typeRef, argument.source))
        }
    }
}

private class ErrorDiagnosticDetector : DiagnosticReporter() {
    override var hasErrors = false
        private set

    override var hasWarningsForWError: Boolean = false
        private set

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        val severity = diagnostic?.severity ?: return
        hasErrors = hasErrors || severity.isError
        hasWarningsForWError = hasWarningsForWError || severity.isErrorWhenWError
    }
}

private inline fun detectErrorDiagnosticsReported(block: context(DiagnosticReporter) () -> Unit): Boolean =
    with(ErrorDiagnosticDetector()) {
        block()
        hasErrors
    }
