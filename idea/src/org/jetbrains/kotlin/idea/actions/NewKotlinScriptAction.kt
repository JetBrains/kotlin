/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

open class NewKotlinScriptAction(
    val actionName: String,
    val description: String,
    val dialogTitle: String,
    val templateName: String
) : CreateFileFromTemplateAction(
    actionName,
    description,
    KotlinIcons.SCRIPT
), DumbAware {

    constructor() : this(
        actionName = "Kotlin Script",
        description = "Creates new Kotlin script",
        dialogTitle = "New Kotlin Script",
        templateName = "Kotlin Script"
    )

    override fun postProcess(createdElement: PsiFile, templateName: String?, customProperties: Map<String, String>?) {
        super.postProcess(createdElement, templateName, customProperties)

        val module = ModuleUtilCore.findModuleForPsiElement(createdElement)

        if (createdElement is KtFile && module != null) {
            NewKotlinFileHook.EP_NAME.extensions.forEach { it.postProcess(createdElement, module) }
        }
    }

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle(dialogTitle)
            .addKind(actionName, KotlinIcons.SCRIPT, templateName)
    }

    override fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory) =
        NewKotlinFileAction.createFileFromTemplateWithStat(name, template, dir)

    override fun getActionName(directory: PsiDirectory, newName: String, templateName: String) = actionName

    override fun startInWriteAction() = false

    override fun hashCode(): Int = 0

    override fun equals(other: Any?): Boolean = this::class == other?.let { it::class }
}