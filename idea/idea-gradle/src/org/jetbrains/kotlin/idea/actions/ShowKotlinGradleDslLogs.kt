/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiFile
import com.intellij.ui.BrowserHyperlinkListener
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import java.io.File

class ShowKotlinGradleDslLogs : IntentionAction, AnAction(), DumbAware {
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        openLogsDirIfPresent(project)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        openLogsDirIfPresent(project)
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = RevealFileAction.isSupported()

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        presentation.isEnabledAndVisible = e.project != null && RevealFileAction.isSupported()
        presentation.text = NAME
    }

    private fun openLogsDirIfPresent(project: Project) {
        val logsDir = findLogsDir()
        if (logsDir != null) {
            RevealFileAction.openDirectory(logsDir)
        } else {
            val parent = WindowManager.getInstance().getStatusBar(project)?.component
                ?: WindowManager.getInstance().findVisibleFrame()?.rootPane
            JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(
                    KotlinIdeaGradleBundle.message(
                        "text.gradle.dsl.logs.cannot.be.found.automatically.see.how.to.find.logs",
                        gradleTroubleshootingLink
                    ),
                    MessageType.ERROR,
                    BrowserHyperlinkListener.INSTANCE
                )
                .setFadeoutTime(5000)
                .createBalloon()
                .showInCenterOf(parent)
        }
    }

    /** The way how to find Gradle logs is described here
     * @see org.jetbrains.kotlin.idea.actions.ShowKotlinGradleDslLogs.gradleTroubleshootingLink
     */
    private fun findLogsDir(): File? {
        val userHome = System.getProperty("user.home")
        return when {
            SystemInfo.isMac -> File("$userHome/Library/Logs/gradle-kotlin-dsl")
            SystemInfo.isLinux -> File("$userHome/.gradle-kotlin-dsl/logs")
            SystemInfo.isWindows -> File("$userHome/AppData/Local/gradle-kotlin-dsl/log")
            else -> null
        }.takeIf { it?.exists() == true }
    }

    override fun startInWriteAction() = false

    override fun getText() = NAME

    override fun getFamilyName() = NAME

    companion object {
        private const val gradleTroubleshootingLink = "https://docs.gradle.org/current/userguide/kotlin_dsl.html#troubleshooting"

        val NAME = KotlinIdeaGradleBundle.message("action.text.show.kotlin.gradle.dsl.logs.in", RevealFileAction.getFileManagerName())
    }
}