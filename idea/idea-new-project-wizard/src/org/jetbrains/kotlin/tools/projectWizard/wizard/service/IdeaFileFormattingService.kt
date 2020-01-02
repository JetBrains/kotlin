/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.service

import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.tools.projectWizard.core.service.FileFormattingService
import java.io.File

class IdeaFileFormattingService(private val project: Project) : FileFormattingService, IdeaWizardService {
    override fun formatFile(text: String, filename: String): String = runReadAction {
        val psiFile = createPsiFile(text, filename) ?: return@runReadAction text
        CodeStyleManager.getInstance(project).reformat(psiFile).text
    }

    private fun createPsiFile(text: String, filename: String) = when (File(filename.removeSuffix(".vm")).extension) {
        "kt" -> KtPsiFactory(project).createFile(text)
        else -> null
    }
}