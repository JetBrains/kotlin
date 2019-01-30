/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import com.intellij.ide.scratch.ScratchFileCreationHelper
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.idea.statistics.KotlinEventTrigger
import org.jetbrains.kotlin.idea.statistics.KotlinStatisticsTrigger

class KtScratchFileCreationHelper: ScratchFileCreationHelper() {

    override fun prepareText(project: Project, context: Context, dataContext: DataContext): Boolean {
        KotlinStatisticsTrigger.trigger(KotlinEventTrigger.KotlinIdeNewFileTemplateTrigger, "Kotlin Scratch")

        context.fileExtension = KotlinParserDefinition.STD_SCRIPT_SUFFIX

        return super.prepareText(project, context, dataContext)
    }
}