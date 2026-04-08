/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.secondToLastContainer
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeTypeArgumentsForOuterClass
import org.jetbrains.kotlin.psi.psiUtil.UNWRAPPABLE_TOKEN_TYPES
import org.jetbrains.kotlin.psi.psiUtil.getExplicitReceiverOfDotQualified
import org.jetbrains.kotlin.resolve.source.getExplicitReceiverOfDotQualified
import org.jetbrains.kotlin.resolve.source.hasUnwrappableAsExplicitReceiver
import org.jetbrains.kotlin.toKtLightSourceElement
import org.jetbrains.kotlin.toKtPsiSourceElement

object FirParenthesizedPackageQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirResolvedQualifier) {
        val source = expression.source?.takeIf { it.kind !is KtFakeSourceElementKind } ?: return
        if (LanguageFeature.ForbidAnnotationsTypeArgumentsAndParenthesesForPackageQualifier.isEnabled()) {
            ImplementationForError.check(expression, source)
        } else {
            ImplementationForDeprecationWarning.check(expression, source,)
        }
    }

    /**
     * In `part1.part2.part3.foo()` checks `part1.part2.part3`.
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOutermostPackageParenthesized(expression: FirResolvedQualifier): Boolean {
        val containingElement = context.secondToLastContainer ?: return false
        if (expression.isExplicitReceiverOf(containingElement) && containingElement.source.hasUnwrappableAsExplicitReceiver()) {
            reportParenthesizedPackageQualifier(containingElement.source, onReceiver = true)
            return true
        }
        return false
    }

    private fun FirResolvedQualifier.isExplicitReceiverOf(other: FirElement): Boolean {
        return when (other) {
            is FirResolvedQualifier -> this == other.explicitParent
            is FirQualifiedAccessExpression -> this == other.explicitReceiver
            else -> false
        }
    }

    private object ImplementationForError {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        fun check(expression: FirResolvedQualifier, source: KtSourceElement) {
            if (expression.symbol != null || source.kind is KtFakeSourceElementKind) return

            if (checkOutermostPackageParenthesized(expression)) return
            checkNestedPackagesParenthesized(source)
        }

        /**
         * In `part1.part2.part3.foo()` checks `part1.part2` and `part1`.
         */
        context(context: CheckerContext, reporter: DiagnosticReporter)
        private fun checkNestedPackagesParenthesized(source: KtSourceElement) {
            val sourceOfParenthesized = source.getChild(UNWRAPPABLE_TOKEN_TYPES)
            if (sourceOfParenthesized != null) {
                reportParenthesizedPackageQualifier(sourceOfParenthesized)
            }
        }
    }

    private object ImplementationForDeprecationWarning {
        /**
         * When [LanguageFeature.ForbidAnnotationsTypeArgumentsAndParenthesesForPackageQualifier] is disabled, we must also
         * report deprecation warnings for type arguments. This property allows us to find parenthesized expression or type arguments
         * via single check.
         */
        private val UNWRAPPABLE_OR_TYPE_ARGUMENT_LIST: Set<IElementType> = UNWRAPPABLE_TOKEN_TYPES + KtNodeTypes.TYPE_ARGUMENT_LIST

        context(context: CheckerContext, reporter: DiagnosticReporter)
        fun check(
            expression: FirResolvedQualifier,
            source: KtSourceElement,
        ) {
            when {
                // Qualifier corresponds to package itself
                expression.symbol == null -> {
                    // Already reported in other checkers
                    if (expression.typeArguments.isNotEmpty() || expression.hasConeTypeArgumentsForOuterClassDiagnostic()) {
                        return
                    }
                    if (checkOutermostPackageParenthesized(expression)) return
                    checkNestedPackages(source)
                }
                expression.explicitParent == null -> {
                    // qualifier corresponds to class: `Class` or `part1.part2.part3.Class` where we don't have
                    // dedicated qualifier for `part1.part2.part3`
                    if (expression.hasConeTypeArgumentsForOuterClassDiagnostic()) return
                    if (source.hasUnwrappableAsExplicitReceiver()) {
                        reportParenthesizedPackageQualifier(source, onReceiver = true)
                        return
                    }
                    val packageReceiverSource = when (source) {
                        is KtLightSourceElement -> {
                            source.lighterASTNode
                                .getExplicitReceiverOfDotQualified(source.treeStructure)
                                ?.toKtLightSourceElement(source.treeStructure)
                        }
                        is KtPsiSourceElement -> {
                            source.psi
                                .getExplicitReceiverOfDotQualified()
                                ?.toKtPsiSourceElement()
                        }
                    }
                    packageReceiverSource?.let {
                        checkNestedPackages(it)
                    }
                }
            }
        }

        /**
         * For `part1.part2.part3.foo()`:
         *  - checks type arguments on `part1`, `part1.part2`, and `part1.part2.part3`;
         *  - checks whether `part1` and `part2` are parenthesized.
         */
        context(context: CheckerContext, reporter: DiagnosticReporter)
        private fun checkNestedPackages(source: KtSourceElement) {
            val parenthesizedOrTypeArgumentList = source.getChild(UNWRAPPABLE_OR_TYPE_ARGUMENT_LIST) ?: return
            if (parenthesizedOrTypeArgumentList.elementType == KtNodeTypes.TYPE_ARGUMENT_LIST) {
                reporter.reportOn(
                    parenthesizedOrTypeArgumentList,
                    FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED_IN_PACKAGE_QUALIFIER_WARNING
                )
            } else {
                reportParenthesizedPackageQualifier(parenthesizedOrTypeArgumentList)
            }
        }

        private fun FirResolvedQualifier.hasConeTypeArgumentsForOuterClassDiagnostic(): Boolean {
            return nonFatalDiagnostics.any { it is ConeTypeArgumentsForOuterClass }
        }
    }

    context(checker: CheckerContext, reporter: DiagnosticReporter)
    private fun reportParenthesizedPackageQualifier(source: KtSourceElement?, onReceiver: Boolean = false) {
        reporter.reportOn(
            source,
            FirErrors.PARENTHESIZED_PACKAGE_QUALIFIER,
            positioningStrategy =
                if (onReceiver) SourceElementPositioningStrategies.RECEIVER_OF_DOT_QUALIFIED
                else SourceElementPositioningStrategies.DEFAULT,
        )
    }
}
