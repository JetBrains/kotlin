/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.testFramework

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import org.jetbrains.kotlin.idea.test.GradleProcessOutputInterceptor
import java.io.Closeable
import kotlin.test.assertFalse
import kotlin.test.fail

class StatefulTestGradleProjectRefreshCallback(
    private val projectPath: String,
    private val project: Project
) : ExternalProjectRefreshCallback, Closeable {

    private class Error(val message: String, val details: String? = null)

    private var alreadyUsed = false
    private var error: Error? = null

    init {
        GradleProcessOutputInterceptor.getInstance()?.reset()
    }

    override fun onSuccess(externalProject: DataNode<ProjectData>?) {
        checkAlreadyUsed()

        if (externalProject == null) {
            error = Error("Got null external project after Gradle import")
            return
        }
        ServiceManager.getService(ProjectDataManager::class.java).importData(externalProject, project, true)
    }

    override fun onFailure(errorMessage: String, errorDetails: String?) {
        checkAlreadyUsed()

        error = Error(errorMessage, errorDetails)
    }

    override fun close() = assertError()

    fun assertError() {
        val error = error ?: return

        val failure = buildString {
            appendLine("Gradle import failed for ${project.name} at $projectPath")

            project.guessProjectDir()

            append("=".repeat(40)).appendLine(" Error message:")
            appendLine(error.message.trimEnd())

            append("=".repeat(40)).appendLine(" Error details:")
            appendLine(error.details?.trimEnd().orEmpty())

            append("=".repeat(40)).appendLine(" Gradle process output:")
            appendLine(GradleProcessOutputInterceptor.getInstance()?.getOutput()?.trimEnd() ?: "<interceptor not installed>")

            appendLine("=".repeat(40))
        }

        fail(failure)
    }

    private fun checkAlreadyUsed() {
        assertFalse(
            alreadyUsed,
            "${StatefulTestGradleProjectRefreshCallback::class.java} can be used only once." +
                    " Please create a new instance of ${StatefulTestGradleProjectRefreshCallback::class.java} every time you" +
                    " do import from Gradle."
        )

        alreadyUsed = true
    }
}
