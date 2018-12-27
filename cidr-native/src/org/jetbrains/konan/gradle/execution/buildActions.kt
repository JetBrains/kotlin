/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.execution.RunManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil.shortenPathWithEllipsis
import com.jetbrains.cidr.execution.build.BaseBuildAction
import org.jetbrains.konan.KonanBundle.message
import org.jetbrains.konan.gradle.GradleKonanWorkspace

abstract class KonanProjectBasedBuildAction(text: String) : BaseBuildAction(text) {
    final override fun isAvailable(project: Project) = project.isProjectBasedActionEnabled

    final override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isVisible = getEventProject(e).isActionVisible
    }
}

class KonanBuildProjectAction : KonanProjectBasedBuildAction(message("action.buildProject.text")) {
    override fun doBuild(project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            GradleKonanBuild.buildProject(project, cleanupNeeded = false, compileNeeded = true)
        }
        //ProjectTaskManager.getInstance(project).build(*modules)
    }
}

class KonanRebuildProjectAction : KonanProjectBasedBuildAction(message("action.rebuildProject.text")) {
    override fun doBuild(project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            GradleKonanBuild.buildProject(project, cleanupNeeded = true, compileNeeded = true)
        }
        //ProjectTaskManager.getInstance(project).rebuild(*modules)
    }
}

class KonanCleanProjectAction : KonanProjectBasedBuildAction(message("action.clean.text")) {
    override fun doBuild(project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            GradleKonanBuild.buildProject(project, cleanupNeeded = true, compileNeeded = false)
        }
    }
}

abstract class KonanConfigurationBasedBuildAction(text: String) : BaseBuildAction(text) {
    final override fun isAvailable(project: Project) = project.isConfigurationBasedActionEnabled

    final override fun update(e: AnActionEvent) {
        super.update(e)
        val project = getEventProject(e)
        e.presentation.isVisible = project.isActionVisible
        if (e.presentation.isVisible) {
            e.presentation.setText(buildText(project.selectedConfiguration), false)
        }
    }

    final override fun doBuild(project: Project) {
        project.selectedConfiguration?.let { configuration ->
            doBuild(project, configuration)
        }
    }

    abstract fun buildText(configuration: GradleKonanAppRunConfiguration?): String

    abstract fun doBuild(project: Project, configuration: GradleKonanAppRunConfiguration)
}

class KonanBuildConfigurationAction : KonanConfigurationBasedBuildAction(message("action.build.text")) {
    override fun buildText(configuration: GradleKonanAppRunConfiguration?) =
        if (configuration != null)
            message("action.buildConfiguration.text", shortenPathWithEllipsis(configuration.name, 23))
        else
            message("action.build.text")

    override fun doBuild(project: Project, configuration: GradleKonanAppRunConfiguration) {
        ApplicationManager.getApplication().executeOnPooledThread {
            GradleKonanBuild.buildConfiguration(project, cleanupNeeded = false, configuration = configuration)
        }
    }
}

class KonanRebuildConfigurationAction : KonanConfigurationBasedBuildAction(message("action.rebuild.text")) {
    override fun buildText(configuration: GradleKonanAppRunConfiguration?) =
        if (configuration != null)
            message("action.rebuildConfiguration.text", shortenPathWithEllipsis(configuration.name, 23))
        else
            message("action.rebuild.text")

    override fun doBuild(project: Project, configuration: GradleKonanAppRunConfiguration) {
        ApplicationManager.getApplication().executeOnPooledThread {
            GradleKonanBuild.buildConfiguration(project, cleanupNeeded = true, configuration = configuration)
        }
    }
}

private val Project?.isActionVisible: Boolean
    get() = this != null && GradleKonanWorkspace.getInstance(this).isInitialized

private val Project.isProjectBasedActionEnabled: Boolean
    get() = GradleKonanWorkspace.getInstance(this).buildModules.isNotEmpty()

private val Project.isConfigurationBasedActionEnabled: Boolean
    get() = selectedConfiguration != null

private val Project?.selectedConfiguration: GradleKonanAppRunConfiguration?
    get() = if (this == null) null
    else RunManager.getInstance(this).selectedConfiguration?.configuration as? GradleKonanAppRunConfiguration
