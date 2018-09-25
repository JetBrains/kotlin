/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

object AssignOperatorAmbiguityFactory : KotlinIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val fixes = mutableListOf<IntentionAction>()
        val element = diagnostic.psiElement.parent
        if (element is KtBinaryExpression) {
            val left = element.left
            val right = element.right
            val operationText = when (element.operationToken) {
                KtTokens.PLUSEQ -> "plus"
                KtTokens.MINUSEQ -> "minus"
                else -> null
            }
            if (left != null && right != null && operationText != null) {
                val context = element.analyze(BodyResolveMode.PARTIAL)
                if (left.getType(context).isMutableCollection()) {
                    val property = left.mainReference?.resolve() as? KtProperty
                    val propertyName = property?.name
                    if (property != null && propertyName != null && property.isLocal) {
                        fixes.add(ChangeVariableMutabilityFix(property, false, "Change '$propertyName' to val"))
                    }
                    fixes.add(ReplaceWithAssignFunctionCallFix(element, operationText))
                }
            }
        }
        return fixes
    }
}

private fun KotlinType?.isMutableCollection(): Boolean {
    if (this == null) return false
    return JavaToKotlinClassMap.isMutable(this) || constructor.supertypes.reversed().any { JavaToKotlinClassMap.isMutable(it) }
}

private class ReplaceWithAssignFunctionCallFix(
    element: KtBinaryExpression,
    private val operationText: String
) : KotlinQuickFixAction<KtBinaryExpression>(element) {
    override fun getText() = "Replace with '${operationText}Assign()' call"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val binaryExpression = element ?: return
        val left = binaryExpression.left ?: return
        val right = binaryExpression.right ?: return
        val replaced = binaryExpression.replace(
            KtPsiFactory(binaryExpression).createExpressionByPattern("$0.${operationText}Assign($1)", left, right)
        )
        editor?.caretModel?.moveToOffset(replaced.endOffset)
    }
}




