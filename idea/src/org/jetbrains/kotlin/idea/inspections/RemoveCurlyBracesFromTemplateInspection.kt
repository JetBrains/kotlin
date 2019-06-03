/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.canDropBraces
import org.jetbrains.kotlin.idea.core.dropBraces
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry

class RemoveCurlyBracesFromTemplateInspection :
    AbstractApplicabilityBasedInspection<KtBlockStringTemplateEntry>(KtBlockStringTemplateEntry::class.java) {
    override fun inspectionText(element: KtBlockStringTemplateEntry): String = "Redundant curly braces in string template"

    override val defaultFixText: String = "Remove curly braces"

    override fun isApplicable(element: KtBlockStringTemplateEntry): Boolean = element.canDropBraces()

    override fun applyTo(element: PsiElement, project: Project, editor: Editor?) {
        (element as KtBlockStringTemplateEntry).dropBraces()
    }
}
