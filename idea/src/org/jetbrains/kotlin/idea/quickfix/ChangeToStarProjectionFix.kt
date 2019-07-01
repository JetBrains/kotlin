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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypes
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

class ChangeToStarProjectionFix(element: KtTypeElement) : KotlinQuickFixAction<KtTypeElement>(element) {
    override fun getFamilyName() = "Change to star projection"

    override fun getText() = element?.let { "Change type arguments to <${it.typeArgumentsAsTypes.joinToString { "*" }}>" } ?: ""

    public override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val star = KtPsiFactory(file).createStar()
        element.typeArgumentsAsTypes.forEach { it?.replace(star) }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val binaryExpr = diagnostic.psiElement.getNonStrictParentOfType<KtBinaryExpressionWithTypeRHS>()
            val typeReference = binaryExpr?.right ?: diagnostic.psiElement.getNonStrictParentOfType<KtTypeReference>()
            val typeElement = typeReference?.typeElement ?: return null
            if (typeElement is KtFunctionType) return null

            if (binaryExpr?.operationReference?.isAsKeyword() == true) {
                val parent = binaryExpr.getParentOfTypes(true, KtValueArgument::class.java, KtQualifiedExpression::class.java)
                val type = when (parent) {
                    is KtValueArgument -> {
                        val callExpr = parent.getStrictParentOfType<KtCallExpression>()
                        (callExpr?.resolveToCall()?.getArgumentMapping(parent) as? ArgumentMatch)?.valueParameter?.original?.type
                    }
                    is KtQualifiedExpression ->
                        if (KtPsiUtil.safeDeparenthesize(parent.receiverExpression) == binaryExpr)
                            parent.resolveToCall()?.resultingDescriptor?.extensionReceiverParameter?.value?.original?.type
                        else
                            null
                    else ->
                        null
                }
                if (type?.arguments?.any { !it.isStarProjection && !it.type.isTypeParameter() } == true) return null
            }

            if (typeElement.typeArgumentsAsTypes.isNotEmpty()) {
                return ChangeToStarProjectionFix(typeElement)
            }
            return null
        }

        private fun KtSimpleNameExpression.isAsKeyword(): Boolean {
            val elementType = getReferencedNameElementType()
            return elementType == KtTokens.AS_KEYWORD || elementType == KtTokens.AS_SAFE
        }
    }
}
