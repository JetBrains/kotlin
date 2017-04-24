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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit
import java.lang.RuntimeException

class ConvertToBlockBodyIntention : SelfTargetingIntention<KtDeclarationWithBody>(
        KtDeclarationWithBody::class.java, "Convert to block body"
), LowPriorityAction {
    override fun isApplicableTo(element: KtDeclarationWithBody, caretOffset: Int): Boolean {
        if (element is KtFunctionLiteral || element.hasBlockBody() || !element.hasBody()) return false

        when (element) {
            is KtNamedFunction -> {
                val returnType = element.returnType() ?: return false
                if (!element.hasDeclaredReturnType() && returnType.isError) return false// do not convert when type is implicit and unknown
                return true
            }

            is KtPropertyAccessor -> return true

            else -> error("Unknown declaration type: $element")
        }
    }

    override fun allowCaretInsideElement(element: PsiElement) = element !is KtDeclaration && super.allowCaretInsideElement(element)

    override fun applyTo(element: KtDeclarationWithBody, editor: Editor?) {
        convert(element)
    }

    companion object {

        fun convert(declaration: KtDeclarationWithBody): KtDeclarationWithBody {
            val body = declaration.bodyExpression!!

            fun generateBody(returnsValue: Boolean): KtExpression {
                val bodyType = body.analyze().getType(body)
                val factory = KtPsiFactory(declaration)
                if (bodyType != null && bodyType.isUnit() && body is KtNameReferenceExpression) return factory.createEmptyBody()
                val unitWhenAsResult = (bodyType == null || bodyType.isUnit()) && body.resultingWhens().isNotEmpty()
                val needReturn = returnsValue &&
                                 (bodyType == null || (!bodyType.isUnit() && !bodyType.isNothing()))
                val statement = if (needReturn || unitWhenAsResult) factory.createExpressionByPattern("return $0", body) else body
                return factory.createSingleStatementBlock(statement)
            }

            val newBody = when (declaration) {
                is KtNamedFunction -> {
                    val returnType = declaration.returnType()!!
                    if (!declaration.hasDeclaredReturnType() && !returnType.isUnit()) {
                        declaration.setType(returnType)
                    }
                    generateBody(!returnType.isUnit() && !returnType.isNothing())
                }

                is KtPropertyAccessor -> generateBody(declaration.isGetter)

                else -> throw RuntimeException("Unknown declaration type: $declaration")
            }

            declaration.equalsToken!!.delete()
            body.replace(newBody)
            return declaration
        }

        private fun KtNamedFunction.returnType(): KotlinType? {
            val descriptor = analyze(BodyResolveMode.PARTIAL)[BindingContext.DECLARATION_TO_DESCRIPTOR, this] ?: return null
            return (descriptor as FunctionDescriptor).returnType
        }
    }
}
