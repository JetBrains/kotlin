/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.testFramework

import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

enum class ProjectOpenAction {
    SIMPLE_JAVA_MODULE {
        override fun openProject(projectPath: String, projectName: String, jdk: Sdk): Project {
            val project = ProjectManagerEx.getInstanceEx().loadAndOpenProject(projectPath)!!

            val modulePath = "$projectPath/$name${ModuleFileType.DOT_DEFAULT_EXTENSION}"
            val projectFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(projectPath))!!
            val srcFile = projectFile.findChild("src")!!

            val module = runWriteAction {
                ProjectRootManager.getInstance(project).projectSdk = jdk

                val module = ModuleManager.getInstance(project).newModule(modulePath, ModuleTypeId.JAVA_MODULE)
                PsiTestUtil.addSourceRoot(module, srcFile)

                module
            }

            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(module, jdk)

            return project
        }
    },

    EXISTING_IDEA_PROJECT {
        override fun openProject(projectPath: String, projectName: String, jdk: Sdk): Project {
            val projectManagerEx = ProjectManagerEx.getInstanceEx()

            val project = loadProjectWithName(projectPath, projectName)
            assertNotNull(project, "project $projectName at $projectPath is not loaded")

            runWriteAction {
                ProjectRootManager.getInstance(project).projectSdk = jdk
            }

            assertTrue(projectManagerEx.openProject(project), "project $projectName at $projectPath is not opened")

            return project
        }
    },

    GRADLE_PROJECT {
        override fun openProject(projectPath: String, projectName: String, jdk: Sdk): Project {
            val project = ProjectManagerEx.getInstanceEx().loadAndOpenProject(projectPath)!!

            runWriteAction {
                ProjectRootManager.getInstance(project).projectSdk = jdk
            }

            refreshGradleProject(projectPath, project)

            return project
        }
    };

    abstract fun openProject(projectPath: String, projectName: String, jdk: Sdk): Project
}