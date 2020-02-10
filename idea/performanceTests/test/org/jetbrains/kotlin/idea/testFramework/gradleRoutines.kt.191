/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.testFramework

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.openapi.project.ex.ProjectManagerEx
import org.jetbrains.plugins.gradle.service.project.GradleProjectOpenProcessor
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.nio.file.Paths

fun refreshGradleProject(projectPath: String, project: Project) {
    GradleProjectOpenProcessor.openGradleProject(project, null, Paths.get(projectPath))

    val gradleArguments = System.getProperty("kotlin.test.gradle.import.arguments")
    ExternalSystemUtil.refreshProjects(
        ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
            .forceWhenUptodate()
            .useDefaultCallback()
            .use(ProgressExecutionMode.MODAL_SYNC)
            .also {
                gradleArguments?.run(it::withArguments)
            }
    )

    dispatchAllInvocationEvents()
}

fun openGradleProject(projectPath: String, project: Project) {
        dispatchAllInvocationEvents()

        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(projectPath)!!

        FileDocumentManager.getInstance().saveAllDocuments()

        val path = Paths.get(virtualFile.path)
        GradleProjectOpenProcessor.openGradleProject(project, null, path)

        dispatchAllInvocationEvents()
        runInEdtAndWait {
            PlatformTestUtil.saveProject(project)
        }
    }
