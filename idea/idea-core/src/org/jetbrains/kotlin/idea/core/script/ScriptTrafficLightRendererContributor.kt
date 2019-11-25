/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.codeInsight.daemon.impl.SeverityRegistrar
import com.intellij.codeInsight.daemon.impl.TrafficLightRenderer
import com.intellij.codeInsight.daemon.impl.TrafficLightRendererContributor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile

class ScriptTrafficLightRendererContributor : TrafficLightRendererContributor {
    override fun createRenderer(editor: Editor, file: PsiFile?): TrafficLightRenderer? {
        if ((file as? KtFile)?.isScript() == true) {
            return ScriptTrafficLightRenderer(file.project, editor.document, file)
        }
        return null
    }

    class ScriptTrafficLightRenderer(project: Project, document: Document, private val file: KtFile) :
        TrafficLightRenderer(project, document, file) {
        override fun getDaemonCodeAnalyzerStatus(severityRegistrar: SeverityRegistrar): DaemonCodeAnalyzerStatus {
            val status = super.getDaemonCodeAnalyzerStatus(severityRegistrar)
            if (!ScriptDefinitionsManager.getInstance(file.project).isReady()) {
                status.reasonWhySuspended = "Loading kotlin script definitions"
                status.errorAnalyzingFinished = false
            } else if (!ScriptConfigurationManager.getInstance(project).hasConfiguration(file)) {
                status.reasonWhySuspended = "Loading kotlin script dependencies"
                status.errorAnalyzingFinished = false
            }
            return status
        }
    }
}