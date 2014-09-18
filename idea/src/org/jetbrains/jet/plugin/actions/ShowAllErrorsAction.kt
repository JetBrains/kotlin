/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.jet.plugin.project.PluginJetFilesProvider
import com.intellij.openapi.actionSystem.CommonDataKeys
import org.jetbrains.jet.plugin.caches.resolve.getAnalysisResults
import org.jetbrains.jet.lang.diagnostics.Severity
import org.jetbrains.jet.plugin.highlighter.IdeErrorMessages

class ShowAllErrors : AnAction("Show all errors") {

    override fun actionPerformed(e: AnActionEvent?) {

        val project = CommonDataKeys.PROJECT.getData(e!!.getDataContext())!!

        PluginJetFilesProvider.allFilesInProject(project).forEach { file ->
            //println("\nFILE: " +  + "\n")
            val bindingContext = file.getAnalysisResults().getBindingContext()
            val errors = bindingContext.getDiagnostics().filter {
                it.getSeverity() == Severity.ERROR
            }
            if (errors.isNotEmpty()) {
                val message = errors.filter { it.getPsiFile() == file }.map {
                    file.getVirtualFile()!!.getCanonicalPath() + " : " + IdeErrorMessages.RENDERER.render(it)
                }.joinToString("\n")
                println(message)
            }
        }
    }


    override fun update(e: AnActionEvent?) {
        e!!.getPresentation().setVisible(true)
        e.getPresentation().setEnabled(true)
    }
}
