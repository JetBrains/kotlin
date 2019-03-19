/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.imports.canBeAddedToImport
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceImportAlias.KotlinIntroduceImportAliasHandler
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.psi.KtInstanceExpressionWithLabel
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class IntroduceImportAliasIntention : SelfTargetingRangeIntention<KtNameReferenceExpression>(
    KtNameReferenceExpression::class.java,
    "Introduce import alias"
) {
    override fun applicabilityRange(element: KtNameReferenceExpression): TextRange? {
        if (element.parent is KtInstanceExpressionWithLabel) return null
        if (element.mainReference.getImportAlias() != null) return null

        val targets = element.resolveMainReferenceToDescriptors()
        if (targets.isEmpty() || targets.any { !it.canBeAddedToImport() }) return null
        return element.textRange
    }

    override fun applyTo(element: KtNameReferenceExpression, editor: Editor?) {
        if (editor == null) return
        KotlinIntroduceImportAliasHandler.doRefactoring(element.project, editor, element)
    }
}