/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.util.application.getServiceSafe

class DaemonCodeAnalyzerStatusService(project: Project) : Disposable {
    companion object {
        fun getInstance(project: Project): DaemonCodeAnalyzerStatusService = project.getServiceSafe()
    }

    @Volatile
    var daemonRunning: Boolean = false
        private set

    init {
        val messageBusConnection = project.messageBus.connect(this)
        messageBusConnection.subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, object : DaemonCodeAnalyzer.DaemonListener {
            override fun daemonStarting(fileEditors: MutableCollection<out FileEditor>) {
                daemonRunning = true
            }

            override fun daemonFinished(fileEditors: MutableCollection<out FileEditor>) {
                daemonRunning = false
            }

            override fun daemonCancelEventOccurred(reason: String) {
                daemonRunning = false
            }
        })
    }

    override fun dispose() {

    }
}