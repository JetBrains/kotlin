/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class RemoveExplicitLambdaParameterTypesIntention :
    SelfTargetingIntention<KtLambdaExpression>(KtLambdaExpression::class.java, "Remove explicit lambda parameter types (may break code)") {
    override fun isApplicableTo(element: KtLambdaExpression, caretOffset: Int): Boolean {
        if (element.valueParameters.none { it.typeReference != null }) return false
        val arrow = element.functionLiteral.arrow ?: return false
        return caretOffset <= arrow.endOffset
    }

    override fun applyTo(element: KtLambdaExpression, editor: Editor?) {
        val oldParameterList = element.functionLiteral.valueParameterList!!

        val parameterString = oldParameterList.parameters.asSequence().map {
            it.destructuringDeclaration?.text ?: it.name
        }.joinToString(", ")
        val newParameterList = KtPsiFactory(element).createLambdaParameterList(parameterString)
        oldParameterList.replace(newParameterList)
    }
}
