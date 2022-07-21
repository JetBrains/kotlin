/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

object ExpressionAfterTypeParameterWithoutSpacingChecker : DeclarationChecker {

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.AllowExpressionAfterTypeReferenceWithoutSpacing)) return
        when (declaration) {
            is KtProperty -> {
                if (descriptor is PropertyGetterDescriptor || descriptor is PropertySetterDescriptor) return
                check(declaration.typeReference, declaration.initializer, context)
            }
            is KtFunction -> {
                for (valueParameter in declaration.valueParameters) {
                    check(valueParameter.typeReference, valueParameter.defaultValue, context)
                }

                check(declaration.typeReference, declaration.bodyExpression, context)
            }
        }
    }

    private fun check(typeReference: KtTypeReference?, expression: KtExpression?, context: DeclarationCheckerContext) {
        if (typeReference == null) return
        if (expression == null) return

        val leftSide = typeReference.getRightMostToken()
        if (leftSide.node.elementType != KtTokens.GT) return

        val rightSide = typeReference.nextSibling?.getLeftMostToken() ?: return
        if (rightSide.node.elementType != KtTokens.EQ) return

        context.trace.report(Errors.EXPRESSION_AFTER_TYPE_REFERENCE_WITHOUT_SPACING_NOT_ALLOWED.on(rightSide))
    }

    private fun PsiElement.getLeftMostToken(): PsiElement {
        var current = firstChild ?: return this
        while (true) {
            current = current.firstChild ?: return current
        }
    }

    private fun PsiElement.getRightMostToken(): PsiElement {
        var current = lastChild ?: return this
        while (true) {
            current = current.lastChild ?: return current
        }
    }
}