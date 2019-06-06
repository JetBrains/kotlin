/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.intentions.isToString
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReferenceExpression

class ReplaceToStringWithStringTemplateInspection : AbstractApplicabilityBasedInspection<KtDotQualifiedExpression>(
    KtDotQualifiedExpression::class.java
) {
    override fun isApplicable(element: KtDotQualifiedExpression): Boolean {
        if (element.receiverExpression !is KtReferenceExpression) return false
        if (element.parent is KtBlockStringTemplateEntry) return false
        return element.isToString()
    }

    override fun applyTo(element: KtDotQualifiedExpression, project: Project, editor: Editor?) {
        val variable = element.receiverExpression.text
        element.replace(KtPsiFactory(element).createExpression("\"$$variable\""))
    }

    override fun inspectionText(element: KtDotQualifiedExpression) = "Call of 'toString' could be replaced with string template"

    override val defaultFixText = "Replace 'toString' with string template"
}