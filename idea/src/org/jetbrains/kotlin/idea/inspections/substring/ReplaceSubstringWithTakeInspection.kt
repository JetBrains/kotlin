/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.substring

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

class ReplaceSubstringWithTakeInspection : ReplaceSubstringInspection() {
    override fun inspectionText(element: KtDotQualifiedExpression): String = "Replace 'substring' call with 'take' call"

    override val defaultFixText: String = "Replace 'substring' call with 'take' call"

    override fun applyTo(element: KtDotQualifiedExpression, project: Project, editor: Editor?) {
        val argument = element.callExpression?.valueArguments?.elementAtOrNull(1)?.getArgumentExpression() ?: return
        element.replaceWith("$0.take($1)", argument)
    }

    override fun isApplicableInner(element: KtDotQualifiedExpression): Boolean {
        val arguments = element.callExpression?.valueArguments ?: return false
        return arguments.size == 2 && element.isFirstArgumentZero()
    }
}
