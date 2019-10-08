/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile

const val KOTLIN_WORKSHEET_EXTENSION: String = "ws.kts"

class NewKotlinWorksheetAction : NewKotlinScriptAction(
    actionName = "Kotlin Worksheet",
    description = "Creates new Kotlin Worksheet",
    dialogTitle = "New Kotlin Worksheet",
    templateName = "Kotlin Worksheet"
) {

    override fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
        val kotlinWorksheetTemplate = object : FileTemplate by template {
            override fun getExtension(): String = KOTLIN_WORKSHEET_EXTENSION
        }

        return super.createFileFromTemplate(name, kotlinWorksheetTemplate, dir)
    }
}