/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil.shortenPathWithEllipsis
import com.intellij.task.ProjectTaskManager
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
        ProjectTaskManager.getInstance(project).buildAllModules()
    }
}

class KonanRebuildProjectAction : KonanProjectBasedBuildAction(message("action.rebuildProject.text")) {
    override fun doBuild(project: Project) {
        ProjectTaskManager.getInstance(project).rebuildAllModules()
    }
}

abstract class KonanConfigurationBasedBuildAction(text: String) : BaseBuildAction(text) {
    final override fun isAvailable(project: Project) = project.isConfigurationBasedActionEnabled

    final override fun update(e: AnActionEvent) {
        super.update(e)
        val project = getEventProject(e)
        e.presentation.isVisible = project.isActionVisible
        if (e.presentation.isVisible) {
            e.presentation.setText(buildText(project.selectedBuildConfiguration), false)
        }
    }

    final override fun doBuild(project: Project) {
        project.selectedBuildConfiguration?.let { configuration ->
            doBuild(project, configuration)
        }
    }

    abstract fun buildText(configuration: GradleKonanConfiguration?): String

    abstract fun doBuild(project: Project, configuration: GradleKonanConfiguration)
}

class KonanBuildConfigurationAction : KonanConfigurationBasedBuildAction(message("action.build.text")) {
    override fun buildText(configuration: GradleKonanConfiguration?) =
        if (configuration != null)
            message("action.buildConfiguration.text", shortenPathWithEllipsis(configuration.name, 23))
        else
            message("action.build.text")

    override fun doBuild(project: Project, configuration: GradleKonanConfiguration) {
        ProjectTaskManager.getInstance(project).build(configuration)
    }
}

class KonanRebuildConfigurationAction : KonanConfigurationBasedBuildAction(message("action.rebuild.text")) {
    override fun buildText(configuration: GradleKonanConfiguration?) =
        if (configuration != null)
            message("action.rebuildConfiguration.text", shortenPathWithEllipsis(configuration.name, 23))
        else
            message("action.rebuild.text")

    override fun doBuild(project: Project, configuration: GradleKonanConfiguration) {
        ProjectTaskManager.getInstance(project).rebuild(configuration)
    }
}

class KonanCleanProjectAction : KonanConfigurationBasedBuildAction(message("action.clean.text")) {
    override fun buildText(configuration: GradleKonanConfiguration?) =
            message("action.clean.text")

    override fun doBuild(project: Project, configuration: GradleKonanConfiguration) {
        ProjectTaskManager.getInstance(project).run(GradleKonanCleanTask(configuration), null)
    }
}

private val Project?.isActionVisible: Boolean
    get() = this != null && GradleKonanWorkspace.getInstance(this).isInitialized

private val Project.isProjectBasedActionEnabled: Boolean
    get() = isActionVisible

private val Project.isConfigurationBasedActionEnabled: Boolean
    get() = isActionVisible && selectedBuildConfiguration != null

private val Project?.selectedBuildConfiguration: GradleKonanConfiguration?
    get() = if (this == null) null
    else GradleKonanWorkspace.getInstance(this).selectedBuildConfiguration
