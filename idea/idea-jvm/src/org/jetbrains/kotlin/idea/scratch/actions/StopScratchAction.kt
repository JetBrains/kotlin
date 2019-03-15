/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.scratch.getScratchPanelFromSelectedEditor
import org.jetbrains.kotlin.idea.scratch.LOG as log

class StopScratchAction : ScratchAction(
    KotlinBundle.message("scratch.stop.button"),
    AllIcons.Actions.Suspend
) {

    override fun actionPerformed(e: AnActionEvent) {
        ScratchCompilationSupport.forceStop()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        val project = e.project ?: return
        val panel = getScratchPanelFromSelectedEditor(project) ?: return

        e.presentation.isEnabledAndVisible = ScratchCompilationSupport.isInProgress(panel.scratchFile)
    }
}
