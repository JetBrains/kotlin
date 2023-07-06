/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.psi.*

internal data class RawFirReplacement(val from: KtElement, val to: KtElement) {
    companion object {
        fun isApplicableForReplacement(element: KtElement) = when (element) {
            is KtFile, is KtScript, is KtClassInitializer, is KtPropertyAccessor, is KtConstructor<*>, is KtClassOrObject, is KtObjectLiteralExpression, is KtTypeAlias,
            is KtNamedFunction, is KtLambdaExpression, is KtAnonymousInitializer, is KtProperty, is KtTypeReference,
            is KtAnnotationEntry, is KtTypeParameter, is KtTypeProjection, is KtParameter, is KtBlockExpression,
            is KtSimpleNameExpression, is KtConstantExpression, is KtStringTemplateExpression, is KtReturnExpression,
            is KtTryExpression, is KtIfExpression, is KtWhenExpression, is KtDoWhileExpression, is KtWhileExpression,
            is KtForExpression, is KtBreakExpression, is KtContinueExpression, is KtBinaryExpression, is KtBinaryExpressionWithTypeRHS,
            is KtIsExpression, is KtUnaryExpression, is KtCallExpression, is KtArrayAccessExpression, is KtQualifiedExpression,
            is KtThisExpression, is KtSuperExpression, is KtParenthesizedExpression, is KtLabeledExpression, is KtAnnotatedExpression,
            is KtThrowExpression, is KtDestructuringDeclaration, is KtClassLiteralExpression, is KtCallableReferenceExpression,
            is KtCollectionLiteralExpression,
            -> true
            else -> false
        }
    }

    inner class Applier {
        private var replacementApplied = false

        private fun ensureReplacementIsValid() {
            require(from == to || isApplicableForReplacement(from)) {
                "Replacement is possible for applicable type but given ${from::class.simpleName}"
            }
            require(from::class == to::class) {
                "Replacement is possible for same type in replacements but given\n${from::class.simpleName} and ${to::class.simpleName}"
            }
        }

        fun tryReplace(element: KtElement): KtElement {
            if (from != element) return element
            ensureReplacementIsValid()
            replacementApplied = true
            return to
        }

        fun ensureApplied() {
            check(from == to || replacementApplied) {
                "Replacement requested but was not applied for ${from::class.simpleName}"
            }
        }
    }
}