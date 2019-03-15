/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.psi.KtFile

class NewKotlinScriptAction : CreateFileFromTemplateAction(
    "Kotlin Script",
    "Creates new Kotlin script",
    KotlinIcons.SCRIPT
), DumbAware {

    override fun postProcess(createdElement: PsiFile, templateName: String?, customProperties: Map<String, String>?) {
        super.postProcess(createdElement, templateName, customProperties)

        val module = ModuleUtilCore.findModuleForPsiElement(createdElement)

        if (createdElement is KtFile && module != null) {
            NewKotlinFileHook.EP_NAME.extensions.forEach { it.postProcess(createdElement, module) }
        }
    }

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle("New Kotlin Script")
            .addKind("Kotlin Script", KotlinIcons.SCRIPT, "Kotlin Script")
    }

    override fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory) =
        NewKotlinFileAction.createFileFromTemplateWithStat(name, template, dir)

    override fun getActionName(directory: PsiDirectory, newName: String, templateName: String) = "Kotlin Script"

    override fun startInWriteAction() = false

    override fun hashCode(): Int = 0

    override fun equals(other: Any?): Boolean = other is NewKotlinScriptAction
}