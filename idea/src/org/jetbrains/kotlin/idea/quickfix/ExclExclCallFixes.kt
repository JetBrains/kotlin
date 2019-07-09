/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.UNSAFE_CALL
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.isValidOperator


abstract class ExclExclCallFix(psiElement: PsiElement) : KotlinQuickFixAction<PsiElement>(psiElement) {
    override fun getFamilyName(): String = text

    override fun startInWriteAction(): Boolean = true
}

class RemoveExclExclCallFix(psiElement: PsiElement) : ExclExclCallFix(psiElement), CleanupFix, HighPriorityAction {
    override fun getText(): String = KotlinBundle.message("remove.unnecessary.non.null.assertion")

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean =
        getExclExclPostfixExpression() != null

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return

        val postfixExpression = getExclExclPostfixExpression() ?: return
        val expression = KtPsiFactory(project).createExpression(postfixExpression.baseExpression!!.text)
        postfixExpression.replace(expression)
    }

    private fun getExclExclPostfixExpression(): KtPostfixExpression? {
        val operationParent = element?.parent
        if (operationParent is KtPostfixExpression && operationParent.baseExpression != null) {
            return operationParent
        }
        return null
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction = RemoveExclExclCallFix(diagnostic.psiElement)
    }
}

class AddExclExclCallFix(psiElement: PsiElement, val checkImplicitReceivers: Boolean) : ExclExclCallFix(psiElement), LowPriorityAction {
    constructor(psiElement: PsiElement) : this(psiElement, true)

    override fun getText() = KotlinBundle.message("introduce.non.null.assertion")

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean =
        getExpressionForIntroduceCall() != null

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return

        val expr = getExpressionForIntroduceCall() ?: return
        val modifiedExpression = expr.expression
        val exclExclExpression = if (expr.implicitReceiver) {
            KtPsiFactory(project).createExpressionByPattern("this!!.$0", modifiedExpression)
        } else {
            KtPsiFactory(project).createExpressionByPattern("$0!!", modifiedExpression)
        }
        modifiedExpression.replace(exclExclExpression)
    }

    private class ExpressionForCall(val expression: KtExpression, val implicitReceiver: Boolean)

    private fun KtExpression?.expressionForCall(implicitReceiver: Boolean = false) = this?.let { ExpressionForCall(it, implicitReceiver) }

    private fun getExpressionForIntroduceCall(): ExpressionForCall? {
        val psiElement = element ?: return null
        if ((psiElement as? KtExpression).isNullExpression()) {
            return null
        }
        if (psiElement is LeafPsiElement && psiElement.elementType == KtTokens.DOT) {
            return (psiElement.prevSibling as? KtExpression).expressionForCall()
        }
        return when (psiElement) {
            is KtArrayAccessExpression -> psiElement.expressionForCall()
            is KtOperationReferenceExpression -> {
                when (val parent = psiElement.parent) {
                    is KtUnaryExpression -> parent.baseExpression.expressionForCall()
                    is KtBinaryExpression -> {
                        val receiver = if (KtPsiUtil.isInOrNotInOperation(parent)) parent.right else parent.left
                        receiver.expressionForCall()
                    }
                    else -> null
                }
            }
            is KtExpression -> {
                val context = psiElement.analyze()
                if (checkImplicitReceivers && psiElement.getResolvedCall(context)?.getImplicitReceiverValue() != null) {
                    val expressionToReplace = psiElement.parent as? KtCallExpression ?: psiElement
                    expressionToReplace.expressionForCall(implicitReceiver = true)
                } else {
                    context[BindingContext.EXPRESSION_TYPE_INFO, psiElement]?.let {
                        val type = it.type

                        val dataFlowValueFactory = psiElement.getResolutionFacade().frontendService<DataFlowValueFactory>()

                        if (type != null) {
                            val nullability = it.dataFlowInfo.getStableNullability(
                                dataFlowValueFactory.createDataFlowValue(psiElement, type, context, psiElement.findModuleDescriptor())
                            )
                            if (!nullability.canBeNonNull()) return null
                        }
                    }
                    psiElement.expressionForCall()
                }
            }
            else -> null
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction {
            val psiElement = diagnostic.psiElement
            if (diagnostic.factory == UNSAFE_CALL && psiElement is KtArrayAccessExpression) {
                psiElement.arrayExpression?.let { return AddExclExclCallFix(it) }
            }
            return AddExclExclCallFix(psiElement)
        }
    }
}

object SmartCastImpossibleExclExclFixFactory : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        if (diagnostic.factory !== Errors.SMARTCAST_IMPOSSIBLE) return null
        val element = diagnostic.psiElement as? KtExpression ?: return null

        val analyze = element.analyze(BodyResolveMode.PARTIAL)
        val type = analyze.getType(element)
        if (type == null || !TypeUtils.isNullableType(type)) return null

        val diagnosticWithParameters = Errors.SMARTCAST_IMPOSSIBLE.cast(diagnostic)
        val expectedType = diagnosticWithParameters.a
        if (TypeUtils.isNullableType(expectedType)) return null
        val nullableExpectedType = TypeUtils.makeNullable(expectedType)
        if (!type.isSubtypeOf(nullableExpectedType)) return null

        return AddExclExclCallFix(element, checkImplicitReceivers = false)
    }
}

object MissingIteratorExclExclFixFactory : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val element = diagnostic.psiElement as? KtExpression ?: return null

        val analyze = element.analyze(BodyResolveMode.PARTIAL)
        val type = analyze.getType(element)
        if (type == null || !TypeUtils.isNullableType(type)) return null

        val descriptor = type.constructor.declarationDescriptor

        fun hasIteratorFunction(classifierDescriptor: ClassifierDescriptor?): Boolean {
            if (classifierDescriptor !is ClassDescriptor) return false

            val memberScope = classifierDescriptor.unsubstitutedMemberScope
            val functions = memberScope.getContributedFunctions(OperatorNameConventions.ITERATOR, NoLookupLocation.FROM_IDE)

            return functions.any { it.isValidOperator() }
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