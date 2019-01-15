/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.source.getPsi

object ScopeCheckerForEvaluator {
    fun checkScopes(bindingContext: BindingContext, codeFragment: KtCodeFragment): Set<String> {
        val result = hashSetOf<String>()

        codeFragment.accept(object : KtTreeVisitor<Unit>() {
            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: Unit?): Void? {
                val target = bindingContext[BindingContext.REFERENCE_TARGET, expression]
                if (target is DeclarationDescriptorWithVisibility && target.visibility == Visibilities.LOCAL) {
                    val declarationPsiElement = target.toSourceElement.getPsi()
                    if (declarationPsiElement != null) {
                        runReadAction {
                            if (doesCrossInlineBounds(bindingContext, expression, declarationPsiElement)) {
                                result.add(expression.getReferencedName())
                            }
                        }
                    }
                }

                return null
            }
        }, Unit)

        return result
    }

    private fun doesCrossInlineBounds(bindingContext: BindingContext, reference: KtSimpleNameExpression, declaration: PsiElement): Boolean {
        val declarationParent = declaration.parent ?: return false
        var currentParent: PsiElement? = reference.parent?.takeIf { it.isInside(declarationParent) } ?: return false

        while (currentParent != null && currentParent != declarationParent) {
            if (currentParent is KtFunctionLiteral) {
                val functionDescriptor = bindingContext[BindingContext.FUNCTION, currentParent]
                if (functionDescriptor != null && !functionDescriptor.isInline) {
                    return true
                }
            }

            currentParent = when (currentParent) {
                is KtCodeFragment -> currentParent.context
                else -> currentParent.parent
            }
        }

        return false
    }

    private tailrec fun PsiElement.isInside(parent: PsiElement): Boolean {
        if (parent.isAncestor(this)) {
            return true
        }

        val context = (this.containingFile as? KtCodeFragment)?.context ?: return false
        return context.isInside(parent)
    }
}