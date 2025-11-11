/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.typeChangeRelatedTo
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableWrongReceiver
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
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

        val typeArguments: List<ConeTypeProjection>
        val typeParameters: List<FirTypeParameterSymbol>

        if (calleeSymbol is FirConstructorSymbol && calleeSymbol.typeAliasConstructorInfo?.originalConstructor != null) {
            val constructedType = expression.resolvedType.fullyExpandedType()
            // Updating arguments with source information after expanding the type seems extremely brittle as it relies on identity equality
            // of the expression type arguments and the expanded type arguments. This cannot be applied before expanding the type because it
            // seems like the type is already expended.
            typeArguments = constructedType.typeArguments.map {
                it.withSourceRecursive(expression)
            }

            typeParameters = constructedType.toRegularClassSymbol()?.typeParameterSymbols ?: return
        } else {
            typeArguments = expression.typeArguments.toTypeArgumentsWithSourceInfo()
            typeParameters = calleeSymbol?.typeParameterSymbols ?: return
        }

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
            checkUpperBoundViolated(
                typeParameters,
                typeArguments,
                substitutor,
                fallbackSource = expression.source,
                mustRelaxDueToArgumentInteractionsBug = mustRelaxDueToArgumentInteractionsBug,
            )
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

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        if (diagnostic?.severity == Severity.ERROR) {
            hasErrors = true
        }
    }
}

private inline fun detectErrorDiagnosticsReported(block: context(DiagnosticReporter) () -> Unit): Boolean =
    with(ErrorDiagnosticDetector()) {
        block()
        hasErrors
    }
