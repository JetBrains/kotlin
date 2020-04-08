/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.formatter.adjustLineIndent
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

class ConvertToBlockBodyIntention : SelfTargetingIntention<KtDeclarationWithBody>(
    KtDeclarationWithBody::class.java,
    KotlinBundle.lazyMessage("convert.to.block.body")
) {
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

                is KtPropertyAccessor -> {
                    val parent = declaration.parent
                    if (parent is KtProperty && parent.typeReference == null) {
                        val descriptor = parent.resolveToDescriptorIfAny()
                        (descriptor as? CallableDescriptor)?.returnType?.let { parent.setType(it) }
                    }

                    generateBody(declaration.isGetter)
                }

                else -> throw RuntimeException("Unknown declaration type: $declaration")
            }

            declaration.equalsToken!!.delete()
            val replaced = body.replace(newBody)
            declaration.containingKtFile.adjustLineIndent(replaced.startOffset, replaced.endOffset)
            return declaration
        }

        private fun KtNamedFunction.returnType(): KotlinType? {
            val descriptor = resolveToDescriptorIfAny() ?: return null
            return descriptor.returnType
        }
    }
}
