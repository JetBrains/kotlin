/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class ConvertArrayParameterToVarargIntention : SelfTargetingIntention<KtParameter>(
    KtParameter::class.java, DEFAULT_TEXT
) {

    override fun isApplicableTo(element: KtParameter, caretOffset: Int): Boolean {
        val typeReference = element.getChildOfType<KtTypeReference>() ?: return false
        if (element.parent.parent is KtFunctionLiteral) return false
        if (element.isVarArg) return false

        val type = element.descriptor?.type ?: return false
        return when {
            KotlinBuiltIns.isPrimitiveArray(type) -> {
                text = DEFAULT_TEXT
                true
            }
            KotlinBuiltIns.isArray(type) -> {
                val typeArgument = typeReference.typeElement?.typeArgumentsAsTypes?.firstOrNull()
                val typeProjection = typeArgument?.parent as? KtTypeProjection
                if (typeProjection?.hasModifier(KtTokens.IN_KEYWORD) == false) {
                    text = if (!typeProjection.hasModifier(KtTokens.OUT_KEYWORD)
                        && !KotlinBuiltIns.isPrimitiveType(element.builtIns.getArrayElementType(type))
                    ) {
                        BREAKING_TEXT
                    } else {
                        DEFAULT_TEXT
                    }
                    true
                } else {
                    false
                }
            }
            else ->
                false
        }
    }

    override fun applyTo(element: KtParameter, editor: Editor?) {
        val typeReference = element.getChildOfType<KtTypeReference>() ?: return
        val type = element.descriptor?.type ?: return
        val newType = KotlinBuiltIns.getPrimitiveArrayElementType(type)?.typeName?.asString()
                ?: typeReference.typeElement?.typeArgumentsAsTypes?.firstOrNull()?.text
                ?: return
        typeReference.replace(KtPsiFactory(element).createType(newType))
        element.addModifier(KtTokens.VARARG_KEYWORD)
    }

    companion object {
        private const val DEFAULT_TEXT = "Convert to vararg parameter"

        private const val BREAKING_TEXT = "$DEFAULT_TEXT (may break code)"
    }

}