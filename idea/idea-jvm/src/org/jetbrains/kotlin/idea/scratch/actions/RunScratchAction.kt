/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.scratch.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.compiler.CompilerManager
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.idea.scratch.ScratchFileLanguageProvider
import org.jetbrains.kotlin.idea.scratch.getScratchPanelFromSelectedEditor
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutputHandlerAdapter

class RunScratchAction : AnAction(
    KotlinBundle.message("scratch.run.button"),
    KotlinBundle.message("scratch.run.button"),
    AllIcons.Actions.Execute
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val scratchTopPanel = getScratchPanelFromSelectedEditor(project) ?: return
        val scratchFile = scratchTopPanel.scratchFile

        val isMakeBeforeRun = false // todo use property from panel
        val isRepl = true //todo use property from panel

        val provider = ScratchFileLanguageProvider.get(scratchFile.psiFile.language) ?: return

        val handler = provider.getOutputHandler()

        val module = scratchTopPanel.getModule()
        if (module == null) {
            handler.error(scratchFile, "Module should be selected")
            handler.onFinish(scratchFile)
            return
        }

        val runnable = r@ {
            val executor = provider.createReplExecutor(scratchFile)
            if (executor == null) {
                handler.error(scratchFile, "Couldn't run file using REPL")
                handler.onFinish(scratchFile)
                return@r
            }

            e.presentation.isEnabled = false

            executor.addOutputHandler(handler)

            executor.addOutputHandler(object : ScratchOutputHandlerAdapter() {
                override fun onFinish(file: ScratchFile) {
                    e.presentation.isEnabled = true
                }
            })

            executor.execute()
        }

        if (isMakeBeforeRun) {
            CompilerManager.getInstance(project)
                .make(module) { aborted, errors, _, _ -> if (!aborted && errors == 0) runnable() }
        } else {
            runnable()
        }
    }
}
