/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class ConvertArrayParameterToVarargIntention : SelfTargetingIntention<KtParameter>(
    KtParameter::class.java, "Convert to vararg parameter"
) {

    override fun isApplicableTo(element: KtParameter, caretOffset: Int): Boolean {
        val typeReference = element.getChildOfType<KtTypeReference>() ?: return false
        if (element.parent.parent is KtFunctionLiteral) return false
        if (element.isVarArg) return false

        val type = element.descriptor?.type ?: return false
        return when {
            KotlinBuiltIns.isPrimitiveArray(type) ->
                true
            KotlinBuiltIns.isArray(type) -> {
                val typeArguments = typeReference.typeElement?.typeArgumentsAsTypes
                val typeProjection = typeArguments?.firstOrNull()?.parent as? KtTypeProjection
                typeProjection?.hasModifier(KtTokens.IN_KEYWORD) == false
            }
            else ->
                false
        }
    }

    override fun applyTo(element: KtParameter, editor: Editor?) {
        val typeReference = element.getChildOfType<KtTypeReference>() ?: return
        val type = element.descriptor?.type ?: return
        val newType = (if (KotlinBuiltIns.isPrimitiveArray(type)) {
            PrimitiveType.values().firstOrNull { it.arrayTypeName == type.nameIfStandardType }?.typeName?.asString()
        } else {
            typeReference.typeElement?.typeArgumentsAsTypes?.firstOrNull()?.text
        }) ?: return

        typeReference.replace(KtPsiFactory(element).createType(newType))
        element.addModifier(KtTokens.VARARG_KEYWORD)
    }

}