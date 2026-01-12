/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtPsiSourceElement
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
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtWhenConditionInRange

object FirWhenOnSealedChecker : FirWhenExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirWhenExpression) {
        // only important if we had to be exhaustive
        if (!expression.usedAsExpression || expression.subjectVariable == null) return
        // redundant 'else' and problems are not important here
        if (expression.exhaustivenessStatus is ExhaustivenessStatus.NotExhaustive) return

        val types = getSubjectType(context.session, expression)
            ?.minimumBoundIfFlexible(context.session)
            ?.unwrapTypeParameterAndIntersectionTypes(context.session)
        if (!shouldReportType(context.session, types)) return

        val typeToReport = ConeTypeIntersector.intersectTypes(context.session.typeContext, types!!)
        val diagnosticKind = when {
            expression.branches.none { it.condition is FirElseIfTrueCondition } -> "EXHAUSTIVE"
            expression.exhaustivenessStatus == ExhaustivenessStatus.RedundantlyExhaustive -> "REDUNDANT"
            expression.exhaustivenessStatus == ExhaustivenessStatus.ExhaustiveAsNothing -> "NOTHING"
            expression.isNodeKind<KtIfExpression>(KtNodeTypes.IF) -> "IF"
            expression.branches.any {
                it.condition.isNodeKind<KtWhenConditionInRange>(KtNodeTypes.WHEN_CONDITION_IN_RANGE)
            } -> "RANGE"
            expression.branches.size == 2 -> "SPECIAL_CASE"
            else -> "OTHER"
        }

        val singleElseThing = expression.branches.singleOrNull { it.condition is FirElseIfTrueCondition }?.result?.statements?.singleOrNull()
        val finalDiagnosticKind = when (singleElseThing) {
            is FirThrowExpression -> "$diagnosticKind+THROW"
            is FirReturnExpression -> "$diagnosticKind+RETURN"
            else -> diagnosticKind
        }

        reporter.reportOn(expression.source, FirErrors.WHEN_ON_SEALED, typeToReport, finalDiagnosticKind, context)
    }

    inline fun <reified T : KtElement> FirExpression.isNodeKind(lighterType: IElementType): Boolean =
        when (val s = source) {
            is KtPsiSourceElement -> s.psi is T
            is KtLightSourceElement -> s.lighterASTNode.tokenType == lighterType
            else -> false
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