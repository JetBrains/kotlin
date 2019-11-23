/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.quickfix.ChangeVariableTypeFix
import org.jetbrains.kotlin.idea.quickfix.moveCaretToEnd
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.TypeUtils

class UnsafeNotNullAssertionOnReallyNullableInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return dotQualifiedExpressionVisitor(fun(expression) {
            val postfixExpression = expression.receiverExpression as? KtPostfixExpression ?: return
            if (postfixExpression.operationToken != KtTokens.EXCLEXCL) return

            val context = postfixExpression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
            val base = postfixExpression.baseExpression
// experiments woth local var begins
            val myRef = postfixExpression.baseExpression as KtNameReferenceExpression
//            myRef.mainReference.resolve() //TODO:!!!
            val propertyDescriptor = context[BindingContext.REFERENCE_TARGET, myRef] as LocalVariableDescriptor
            val psiProperty = propertyDescriptor.source.getPsi()
            val declaration = psiProperty as? KtProperty as KtVariableDeclaration
            val changeValFix = ChangeVariableTypeFix.OnType(
                declaration,
                propertyDescriptor.returnType.constructor.declarationDescriptor?.builtIns?.stringType ?: return
            )
            holder.registerProblem(
                expression, "Unsafe using of '!!' operator",
                ProblemHighlightType.WEAK_WARNING, IntentionWrapper(changeValFix, declaration.containingFile)
            )
// experiments woth local var ends
            val baseType = base?.getType(context) ?: return
            if (context.diagnostics.forElement(postfixExpression.operationReference)
                    .any { it.factory == Errors.UNNECESSARY_NOT_NULL_ASSERTION }
            ) return
            if (!TypeUtils.isNullableType(baseType)) return
            holder.registerProblem(
                expression,
                "Unsafe using of '!!' operator",
                ProblemHighlightType.WEAK_WARNING,
                AddSaveCallAndElvisFix()
            )
        })
    }

}


private class AddSaveCallAndElvisFix : LocalQuickFix {
    override fun getName() = "Replace unsafe assertion with safe call and elvis"

    override fun getFamilyName(): String = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        applyFix(project, descriptor.psiElement as? KtDotQualifiedExpression ?: return)
    }

    private fun applyFix(project: Project, expression: KtDotQualifiedExpression) {
        if (!FileModificationService.getInstance().preparePsiElementForWrite(expression)) return
        val postfix = expression.receiverExpression as? KtPostfixExpression ?: return
        val editor = expression.findExistingEditor()
        expression.replaced(KtPsiFactory(expression).buildExpression {
            appendExpression(postfix.baseExpression)
            appendFixedText("?.")
            appendExpression(expression.selectorExpression)
            appendFixedText("?:")
        }).moveCaretToEnd(editor, project)
    }
}