/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class ConvertVarargParameterToArrayIntention : SelfTargetingIntention<KtParameter>(
    KtParameter::class.java, "Convert to array parameter"
) {

    override fun isApplicableTo(element: KtParameter, caretOffset: Int): Boolean {
        if (element.getChildOfType<KtTypeReference>() == null) return false
        return element.isVarArg
    }

    override fun applyTo(element: KtParameter, editor: Editor?) {
        val typeReference = element.getChildOfType<KtTypeReference>() ?: return
        val type = element.descriptor?.type ?: return
        val newType = if (KotlinBuiltIns.isPrimitiveArray(type)) type.toString() else "Array<${typeReference.text}>"

        typeReference.replace(KtPsiFactory(element).createType(newType))
        element.removeModifier(KtTokens.VARARG_KEYWORD)
    }

}