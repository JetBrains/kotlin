/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.ExhaustivenessStatus
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.isExhaustive
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.isBooleanOrNullableBoolean
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

object FirExhaustiveWhenChecker : FirWhenExpressionChecker() {
    override fun check(expression: FirWhenExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        reportNotExhaustive(expression, context, reporter)
        reportElseMisplaced(expression, reporter, context)
    }

    private fun reportNotExhaustive(whenExpression: FirWhenExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (whenExpression.isExhaustive) return

        val source = whenExpression.source ?: return

        val subjectType = whenExpression.subject?.resolvedType?.lowerBoundIfFlexible()
        val subjectClassSymbol = subjectType?.fullyExpandedType(context.session)?.toRegularClassSymbol(context.session)

        if (whenExpression.usedAsExpression) {
            if (source.isIfExpression) {
                reporter.reportOn(source, FirErrors.INVALID_IF_AS_EXPRESSION, context)
                return
            } else if (source.isWhenExpression) {
                reportNoElseInWhen(reporter, source, whenExpression, subjectClassSymbol, context)
            }
        } else {
            if (subjectClassSymbol == null) return
            val kind = when {
                subjectClassSymbol.modality == Modality.SEALED -> AlgebraicTypeKind.Sealed
                subjectClassSymbol.classKind == ClassKind.ENUM_CLASS -> AlgebraicTypeKind.Enum
                subjectType.isBooleanOrNullableBoolean -> AlgebraicTypeKind.Boolean
                else -> return
            }

            if (context.session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitNonExhaustiveWhenOnAlgebraicTypes)) {
                reportNoElseInWhen(reporter, source, whenExpression, subjectClassSymbol, context)
            } else {
                reporter.reportOn(source, FirErrors.NON_EXHAUSTIVE_WHEN_STATEMENT, kind.displayName, whenExpression.missingCases, context)
            }
        }
    }

    private fun reportNoElseInWhen(
        reporter: DiagnosticReporter,
        source: KtSourceElement,
        whenExpression: FirWhenExpression,
        subjectClassSymbol: FirRegularClassSymbol?,
        context: CheckerContext,
    ) {
        val description = when (subjectClassSymbol?.isExpect) {
            true -> {
                val declarationType = if (subjectClassSymbol.isEnumClass) "enum" else "sealed"
                " ('when' with expect $declarationType subject cannot be exhaustive without else branch)"
            }
            else -> ""
        }
        reporter.reportOn(source, FirErrors.NO_ELSE_IN_WHEN, whenExpression.missingCases, description, context)
    }

    private val FirWhenExpression.missingCases: List<WhenMissingCase>
        get() = (exhaustivenessStatus as ExhaustivenessStatus.NotExhaustive).reasons

    private enum class AlgebraicTypeKind(val displayName: String) {
        Sealed("sealed class/interface"),
        Enum("enum"),
        Boolean("Boolean")
    }

    private fun reportElseMisplaced(
        expression: FirWhenExpression,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        val branchesCount = expression.branches.size
        for (indexedValue in expression.branches.withIndex()) {
            val branch = indexedValue.value
            if (branch.condition is FirElseIfTrueCondition && indexedValue.index < branchesCount - 1) {
                reporter.reportOn(branch.source, FirErrors.ELSE_MISPLACED_IN_WHEN, context)
            }
        }
    }

    private val KtSourceElement.isIfExpression: Boolean
        get() = elementType == KtNodeTypes.IF

    private val KtSourceElement.isWhenExpression: Boolean
        get() = elementType == KtNodeTypes.WHEN
}
