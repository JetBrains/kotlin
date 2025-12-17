/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.ExhaustivenessStatus
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.isImplicitWhenSubjectVariable
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*

object FirWhenOnSealedChecker : FirWhenExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirWhenExpression) {
        // only important if we had to be exhaustive
        if (!expression.usedAsExpression || expression.subjectVariable == null) return
        // redundant 'else' and problems are not important here
        if (expression.exhaustivenessStatus != ExhaustivenessStatus.ProperlyExhaustive) return

        val types = getSubjectType(context.session, expression)
            ?.minimumBoundIfFlexible(context.session)
            ?.unwrapTypeParameterAndIntersectionTypes(context.session)
        if (!shouldReportType(context.session, types)) return

        val typeToReport = ConeTypeIntersector.intersectTypes(context.session.typeContext, types!!)
        val errorToReport = when {
            expression.branches.none { it.condition is FirElseIfTrueCondition } -> FirErrors.WHEN_ON_SEALED_GEEN_ELSE
            expression.branches.size == 2 -> FirErrors.WHEN_ON_SEALED_EEN_EN_ELSE
            else -> FirErrors.WHEN_ON_SEALED_WEL_ELSE
        }
        reporter.reportOn(expression.source, errorToReport, typeToReport, context)
    }

    fun shouldReportType(session: FirSession, types: Collection<ConeKotlinType>?): Boolean =
        !types.isNullOrEmpty() && types.all {
            val symbol = it.toRegularClassSymbol(session)
            symbol?.classKind == ClassKind.ENUM_CLASS || symbol?.modality == Modality.SEALED
        }

    // copied from FirWhenExhaustivenessComputer
    private fun getSubjectType(session: FirSession, whenExpression: FirWhenExpression): ConeKotlinType? {
        val subjectType = whenExpression.subjectVariable?.takeIf { !it.isImplicitWhenSubjectVariable }?.returnTypeRef?.coneType
            ?: whenExpression.subjectVariable?.initializer?.resolvedType
            ?: return null

        return subjectType.fullyExpandedType(session)
    }

    private fun ConeKotlinType.minimumBoundIfFlexible(session: FirSession): ConeRigidType {
        return when (this) {
            is ConeDynamicType -> when (session.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedExhaustivenessChecksIn21)) {
                true -> upperBound // `dynamic` types must be exhaustive based on the upper bound (`Any?`).
                false -> lowerBound
            }
            is ConeFlexibleType -> lowerBound // All other flexible types may be exhaustive based on the lower bound.
            is ConeRigidType -> this
        }
    }

    private fun ConeKotlinType.unwrapTypeParameterAndIntersectionTypes(session: FirSession): Collection<ConeKotlinType> {
        return when (this) {
            is ConeIntersectionType -> intersectedTypes
            is ConeTypeParameterType if session.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedExhaustivenessChecksIn21)
                -> buildList {
                lookupTag.typeParameterSymbol.resolvedBounds.flatMapTo(this) {
                    it.coneType.unwrapTypeParameterAndIntersectionTypes(session)
                }
                add(this@unwrapTypeParameterAndIntersectionTypes)
            }
            is ConeDefinitelyNotNullType if session.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedExhaustivenessChecksIn21)
                -> original.unwrapTypeParameterAndIntersectionTypes(session)
                .map { it.makeConeTypeDefinitelyNotNullOrNotNull(session.typeContext) }
            else -> listOf(this)
        }
    }
}