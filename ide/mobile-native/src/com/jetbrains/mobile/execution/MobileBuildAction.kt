package com.jetbrains.mobile.execution

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectModelBuildableElement
import com.intellij.task.ProjectTaskContext
import com.jetbrains.cidr.execution.build.CidrBuildTargetAction
import com.jetbrains.mobile.MobileBundle

class MobileBuildAction : CidrBuildTargetAction(true, MobileBundle.message("build"), null, AllIcons.Actions.Compile) {
    private fun selectedRunConfiguration(project: Project) =
        RunManager.getInstance(project).selectedConfiguration?.configuration as? MobileRunConfigurationBase

    override fun isEnabled(project: Project): Boolean =
        selectedRunConfiguration(project) != null

    override fun createContext(project: Project, dataContext: DataContext): ProjectTaskContext =
        ProjectTaskContext(dataContext, selectedRunConfiguration(project)!!)

    override fun getBuildableElements(project: Project): List<ProjectModelBuildableElement> {
        if (selectedRunConfiguration(project) == null) return emptyList()
        return listOf(MobileProjectTaskRunner.BuildableElement())
    }

    override fun buildText(configuration: RunConfiguration?): String =
        if (configuration != null)
            MobileBundle.message("build.something", configuration.name)
        else
            MobileBundle.message("build")
}