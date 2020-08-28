/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.rename.KotlinVariableInplaceRenameHandler
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveToDescriptors
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class ReplaceItWithExplicitFunctionLiteralParamIntention : SelfTargetingOffsetIndependentIntention<KtNameReferenceExpression>(
    KtNameReferenceExpression::class.java, KotlinBundle.lazyMessage("replace.it.with.explicit.parameter")
) {
    override fun isApplicableTo(element: KtNameReferenceExpression) = isAutoCreatedItUsage(element)

    override fun applyTo(element: KtNameReferenceExpression, editor: Editor?) {
        if (editor == null) throw IllegalArgumentException("This intention requires an editor")
        val target = element.mainReference.resolveToDescriptors(element.analyze()).single()

        val functionLiteral = DescriptorToSourceUtils.descriptorToDeclaration(target.containingDeclaration ?: return) as KtFunctionLiteral

        val newExpr = KtPsiFactory(element).createExpression("{ it -> }") as KtLambdaExpression
        functionLiteral.addRangeAfter(
            newExpr.functionLiteral.valueParameterList,
            newExpr.functionLiteral.arrow ?: return,
            functionLiteral.lBrace
        )

        PsiDocumentManager.getInstance(element.project).doPostponedOperationsAndUnblockDocument(editor.document)

        val paramToRename = functionLiteral.valueParameters.single()
        editor.caretModel.moveToOffset(element.textOffset)
        KotlinVariableInplaceRenameHandler().doRename(paramToRename, editor, null)
    }
}
