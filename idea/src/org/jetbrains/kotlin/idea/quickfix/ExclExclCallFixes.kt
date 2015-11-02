/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.util.OperatorChecks
import org.jetbrains.kotlin.util.OperatorNameConventions

public abstract class ExclExclCallFix : IntentionAction {
    override fun getFamilyName(): String = getText()

    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile) = file is KtFile
}

public class RemoveExclExclCallFix(val psiElement: PsiElement) : ExclExclCallFix(), CleanupFix {
    override fun getText(): String = KotlinBundle.message("remove.unnecessary.non.null.assertion")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean
        = super.isAvailable(project, editor, file) && getExclExclPostfixExpression() != null

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return

        val postfixExpression = getExclExclPostfixExpression() ?: return
        val expression = KtPsiFactory(project).createExpression(postfixExpression.getBaseExpression()!!.getText())
        postfixExpression.replace(expression)
    }

    private fun getExclExclPostfixExpression(): KtPostfixExpression? {
        val operationParent = psiElement.getParent()
        if (operationParent is KtPostfixExpression && operationParent.getBaseExpression() != null) {
            return operationParent
        }
        return null
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction
            = RemoveExclExclCallFix(diagnostic.getPsiElement())
    }
}

public class AddExclExclCallFix(val psiElement: PsiElement) : ExclExclCallFix() {
    override fun getText() = KotlinBundle.message("introduce.non.null.assertion")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean
            = super.isAvailable(project, editor, file) &&
              getExpressionForIntroduceCall() != null

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val modifiedExpression = getExpressionForIntroduceCall() ?: return
        val exclExclExpression = KtPsiFactory(project).createExpression(modifiedExpression.getText() + "!!")
        modifiedExpression.replace(exclExclExpression)
    }

    protected fun getExpressionForIntroduceCall(): KtExpression? {
        if (psiElement is LeafPsiElement && psiElement.getElementType() == KtTokens.DOT) {
            val sibling = psiElement.getPrevSibling()
            if (sibling is KtExpression) {
                return sibling
            }
        }
        else if (psiElement is KtExpression) {
            return psiElement
        }

        return null
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction
                = AddExclExclCallFix(diagnostic.getPsiElement())
    }
}

object MissingIteratorExclExclFixFactory : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val element = diagnostic.psiElement
        if (element !is KtExpression) return null
        
        val analyze = element.analyze(BodyResolveMode.PARTIAL)
        val type = analyze.getType(element)
        if (type == null || !TypeUtils.isNullableType(type)) return null
        
        val descriptor = type.constructor.declarationDescriptor

        fun hasIteratorFunction(descriptor: ClassifierDescriptor?) : Boolean {
            if (descriptor !is ClassDescriptor) return false

            val memberScope = descriptor.unsubstitutedMemberScope
            val functions = memberScope.getFunctions(OperatorNameConventions.ITERATOR, NoLookupLocation.FROM_IDE)

            return functions.any { it.isOperator() && OperatorChecks.canBeOperator(it) }
        }

        when (descriptor) {
            is TypeParameterDescriptor -> {
                if (descriptor.upperBounds.none { hasIteratorFunction(it.constructor.declarationDescriptor) }) return null
            }
            is ClassifierDescriptor -> {
                if (!hasIteratorFunction(descriptor)) return null
            }
            else -> return null
        }

        return AddExclExclCallFix(element)
    }
}