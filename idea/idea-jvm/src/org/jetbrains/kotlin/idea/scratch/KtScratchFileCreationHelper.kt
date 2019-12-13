/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import com.intellij.ide.scratch.ScratchFileCreationHelper
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.statistics.FUSEventGroups
import org.jetbrains.kotlin.idea.statistics.KotlinFUSLogger
import org.jetbrains.kotlin.parsing.KotlinParserDefinition

class KtScratchFileCreationHelper : ScratchFileCreationHelper() {

    override fun prepareText(project: Project, context: Context, dataContext: DataContext): Boolean {
        KotlinFUSLogger.log(FUSEventGroups.NewFileTemplate, "Kotlin Scratch")

        context.fileExtension = KotlinParserDefinition.STD_SCRIPT_SUFFIX

        return super.prepareText(project, context, dataContext)
    }
}