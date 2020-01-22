/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

class ReformatQuickFix(private val description: String, element: PsiElement? = null) : LocalQuickFix {
    private val elementPoint = element?.createSmartPointer()

    override fun getName(): String = description

    override fun getFamilyName(): String = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        (elementPoint?.element ?: descriptor.psiElement)?.let {
            CodeStyleManager.getInstance(project).reformat(it)
        }
    }
}
