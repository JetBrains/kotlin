/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.findContextReceiverListSource
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.*

object FirContextReceiversDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.source?.kind is KtFakeSourceElementKind) return
        val contextReceivers = declaration.getContextReceiver()
        if (contextReceivers.isEmpty()) return
        val source = declaration.source?.findContextReceiverListSource() ?: return

        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) {
            if (checkSubTypes(contextReceivers.map { it.typeRef.coneType }, context)) {
                reporter.reportOn(
                    source,
                    FirErrors.SUBTYPING_BETWEEN_CONTEXT_RECEIVERS,
                    context
                )
            }
            return
        }

        reporter.reportOn(
            source,
            FirErrors.UNSUPPORTED_FEATURE,
            LanguageFeature.ContextReceivers to context.languageVersionSettings,
            context
        )
    }

    private fun FirDeclaration.getContextReceiver(): List<FirContextReceiver> {
        return when (this) {
            is FirCallableDeclaration -> contextReceivers
            is FirRegularClass -> contextReceivers
            is FirScript -> contextReceivers
            else -> emptyList()
        }
    }
}

/**
 * Simplified checking of subtype relation used in context receiver checkers.
 * It converts type parameters to star projections and top level type parameters to its supertypes. Then it checks the relation.
 */
fun checkSubTypes(types: List<ConeKotlinType>, context: CheckerContext): Boolean {
    fun replaceTypeParametersByStarProjections(type: ConeClassLikeType): ConeClassLikeType {
        return type.withArguments(type.typeArguments.map {
            when {
                it.isStarProjection -> it
                it.type!! is ConeTypeParameterType -> ConeStarProjection
                it.type!! is ConeClassLikeType -> replaceTypeParametersByStarProjections(it.type as ConeClassLikeType)
                else -> it
            }
        }.toTypedArray())
    }

    val replacedTypeParameters = types.flatMap { r ->
        when (r) {
            is ConeTypeParameterType -> r.lookupTag.typeParameterSymbol.resolvedBounds.map { it.type }
            is ConeClassLikeType -> listOf(replaceTypeParametersByStarProjections(r))
            else -> listOf(r)
        }
    }

    for (i in replacedTypeParameters.indices)
        for (j in i + 1..<replacedTypeParameters.size) {
            if (replacedTypeParameters[i].isSubtypeOf(replacedTypeParameters[j], context.session)
                || replacedTypeParameters[j].isSubtypeOf(replacedTypeParameters[i], context.session)
            )
                return true
        }

    return false
}
