/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.configuration.showConfigureKotlinNotificationIfNeeded

class NewKotlinFileAction
    : CreateFileFromTemplateAction(KotlinBundle.message("new.kotlin.file.action"),
                                   "Creates new Kotlin file or class",
                                   KotlinFileType.INSTANCE.icon),
      DumbAware
{
    override fun postProcess(createdElement: PsiFile?, templateName: String?, customProperties: Map<String, String>?) {
        super.postProcess(createdElement, templateName, customProperties)

        val module = ModuleUtilCore.findModuleForPsiElement(createdElement!!)
        if (module != null) {
            showConfigureKotlinNotificationIfNeeded(module)
        }
    }

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle("New Kotlin File/Class")
                .addKind("File", KotlinFileType.INSTANCE.icon, "Kotlin File")
                .addKind("Class", KotlinIcons.CLASS, "Kotlin Class")
                .addKind("Interface", KotlinIcons.INTERFACE, "Kotlin Interface")
                .addKind("Enum class", KotlinIcons.ENUM, "Kotlin Enum")
                .addKind("Object", KotlinIcons.OBJECT, "Kotlin Object")
    }

    override fun getActionName(directory: PsiDirectory, newName: String, templateName: String): String {
        return KotlinBundle.message("new.kotlin.file.action")
    }

    override fun isAvailable(dataContext: DataContext): Boolean {
        if (super.isAvailable(dataContext)) {
            val ideView = LangDataKeys.IDE_VIEW.getData(dataContext)!!
            val project = PlatformDataKeys.PROJECT.getData(dataContext)!!
            val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex
            return ideView.directories.any { projectFileIndex.isInSourceContent(it.virtualFile) }
        }
        return false
    }

    override fun hashCode(): Int {
        return 0
    }

    override fun equals(obj: Any?): Boolean {
        return obj is NewKotlinFileAction
    }
}
