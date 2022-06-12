/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.children

object TypeArgumentListLikeExpressionsChecker : DeclarationChecker {

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.AllowTypeArgumentListLikeExpressions)) return
        if (declaration.parent !is KtFile) return

        val visitor = object : KtTreeVisitorVoid() {
            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                super.visitBinaryExpression(expression)

                if (wasTypeArgumentListLikeExpression(expression)) {
                    context.trace.report(Errors.TYPE_ARGUMENT_LIST_LIKE_EXPRESSION.on(expression.operationReference))
                }
            }

            override fun visitUserType(type: KtUserType) {
                super.visitUserType(type)

                if (wasTypeArgumentListLikeExpression(type)) {
                    var parent = type.parent ?: return
                    while (parent !is KtBinaryExpression) {
                        parent = parent.parent ?: return
                    }

                    context.trace.report(Errors.TYPE_ARGUMENT_LIST_LIKE_EXPRESSION.on(parent.operationReference))
                }
            }

            private fun wasTypeArgumentListLikeExpression(element: KtElement): Boolean {
                return element.node.children().any { it.elementType == KtNodeTypes.TYPE_ARGUMENT_LIST_LIKE_EXPRESSION }
            }
        }

        declaration.accept(visitor)
    }
}