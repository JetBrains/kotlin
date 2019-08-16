/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.idea.scratch.getScratchFileFromSelectedEditor

class RunScratchFromHereAction : ScratchAction(
    KotlinBundle.message("scratch.run.from.here.button"),
    AllIcons.Diff.ArrowRight
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val scratchFile = getScratchFileFromSelectedEditor(project) ?: return

        doAction(scratchFile)
    }

    companion object {
        fun doAction(scratchFile: ScratchFile) {
            val executor = scratchFile.replScratchExecutor ?: return

            try {
                executor.executeNew()
            } catch (ex: Throwable) {
                executor.errorOccurs("Exception occurs during Run Scratch Action", ex, true)
            }
        }
    }
}