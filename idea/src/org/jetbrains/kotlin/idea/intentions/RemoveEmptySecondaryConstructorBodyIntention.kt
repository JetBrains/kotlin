/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

@Suppress("DEPRECATION")
class RemoveEmptySecondaryConstructorBodyInspection : IntentionBasedInspection<KtBlockExpression>(
    RemoveEmptySecondaryConstructorBodyIntention::class
), CleanupLocalInspectionTool {
    override fun problemHighlightType(element: KtBlockExpression): ProblemHighlightType =
        ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

class RemoveEmptySecondaryConstructorBodyIntention :
    SelfTargetingOffsetIndependentIntention<KtBlockExpression>(KtBlockExpression::class.java, "Remove empty constructor body") {

    override fun applyTo(element: KtBlockExpression, editor: Editor?) = element.delete()

    override fun isApplicableTo(element: KtBlockExpression): Boolean {
        if (element.parent !is KtSecondaryConstructor) return false
        if (element.statements.isNotEmpty()) return false

        return element.text.replace("{", "").replace("}", "").isBlank()
    }

}