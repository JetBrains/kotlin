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
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.ExhaustivenessStatus
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.isJavaNonAbstractSealed
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.isBooleanOrNullableBoolean
import org.jetbrains.kotlin.name.ClassId

object FirExhaustiveWhenChecker : FirWhenExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirWhenExpression) {
        reportNotExhaustive(expression)
        reportElseMisplaced(expression)
        reportRedundantElse(expression)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportEmptyThenInExpression(
        whenExpression: FirWhenExpression,
    ) {
        val source = whenExpression.source ?: return

        if (source.isIfExpression && whenExpression.usedAsExpression) {
            val thenBranch = whenExpression.branches.firstOrNull()
            if (thenBranch == null || thenBranch.result is FirEmptyExpressionBlock) {
                reporter.reportOn(source, FirErrors.INVALID_IF_AS_EXPRESSION)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportNotExhaustive(whenExpression: FirWhenExpression) {
        val exhaustivenessStatus = whenExpression.exhaustivenessStatus ?: return
        if (exhaustivenessStatus !is ExhaustivenessStatus.NotExhaustive) {
            // whenExpression.isExhaustive is checked as otherwise the constraint is checked below
            reportEmptyThenInExpression(whenExpression)
            return
        }

        val source = whenExpression.source ?: return

        val subjectType = exhaustivenessStatus.subjectType
        val subjectClassSymbol = subjectType?.toRegularClassSymbol(context.session)

        if (whenExpression.usedAsExpression) {
            if (source.isIfExpression) {
                reporter.reportOn(source, FirErrors.INVALID_IF_AS_EXPRESSION)
                return
            } else if (source.isWhenExpression) {
                reportNoElseInWhen(source, whenExpression, subjectClassSymbol)
            }
        } else {
            if (subjectClassSymbol == null) return
            val kind = when {
                subjectClassSymbol.modality == Modality.SEALED -> AlgebraicTypeKind.Sealed
                subjectClassSymbol.classKind == ClassKind.ENUM_CLASS -> AlgebraicTypeKind.Enum
                subjectType.isBooleanOrNullableBoolean -> AlgebraicTypeKind.Boolean
                else -> return
            }

            reportNoElseInWhen(source, whenExpression, subjectClassSymbol)
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun reportNoElseInWhen(
        source: KtSourceElement,
        whenExpression: FirWhenExpression,
        subjectClassSymbol: FirRegularClassSymbol?,
    ) {
        val missingCases = whenExpression.missingCases

        if (missingCases.all { it is WhenMissingCase.IsTypeCheckIsMissing && it.classId.isJavaNonAbstractSealed() }
            && !LanguageFeature.ProperExhaustivenessCheckForJavaOpenSealedClass.isEnabled()
        ) {
            reporter.reportOn(source, FirErrors.MISSING_BRANCH_FOR_NON_ABSTRACT_SEALED_CLASS, missingCases)
            return
        }

        val description = when (subjectClassSymbol?.isExpect) {
            true -> {
                val declarationType = if (subjectClassSymbol.isEnumClass) "enum" else "sealed"
                " ('when' with expect $declarationType subject cannot be exhaustive without else branch)"
            }
            else -> ""
        }
        reporter.reportOn(source, FirErrors.NO_ELSE_IN_WHEN, missingCases, description)
    }

    context(context: CheckerContext)
    private fun ClassId.isJavaNonAbstractSealed(): Boolean {
        val symbol = toSymbol(context.session) as? FirClassLikeSymbol ?: return false
        val fullyExpandedClassSymbol = symbol.fullyExpandedClass(context.session) ?: return false

        @OptIn(SymbolInternals::class)
        return fullyExpandedClassSymbol.fir.isJavaNonAbstractSealed == true
    }

    private val FirWhenExpression.missingCases: List<WhenMissingCase>
        get() = (exhaustivenessStatus as ExhaustivenessStatus.NotExhaustive).reasons

    private enum class AlgebraicTypeKind(val displayName: String) {
        Sealed("sealed class/interface"),
        Enum("enum"),
        Boolean("Boolean")
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun reportElseMisplaced(
        expression: FirWhenExpression,
    ) {
        val branchesCount = expression.branches.size
        for (indexedValue in expression.branches.withIndex()) {
            val branch = indexedValue.value
            if (branch.condition is FirElseIfTrueCondition && indexedValue.index < branchesCount - 1) {
                reporter.reportOn(branch.source, FirErrors.ELSE_MISPLACED_IN_WHEN)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportRedundantElse(whenExpression: FirWhenExpression) {
        if (whenExpression.exhaustivenessStatus == ExhaustivenessStatus.RedundantlyExhaustive) {
            for (branch in whenExpression.branches) {
                if (branch.source == null || branch.condition !is FirElseIfTrueCondition) continue
                reporter.reportOn(branch.source, FirErrors.REDUNDANT_ELSE_IN_WHEN)
            }
        }
    }

    private val KtSourceElement.isIfExpression: Boolean
        get() = elementType == KtNodeTypes.IF

    private val KtSourceElement.isWhenExpression: Boolean
        get() = elementType == KtNodeTypes.WHEN
}
