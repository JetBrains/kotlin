/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.ExecutionTargetProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import org.jdom.Element
import org.jetbrains.kotlin.gradle.KonanArtifactModel
import java.io.File
import javax.swing.Icon

class IdeaKonanExecutionTargetProvider : ExecutionTargetProvider() {
    override fun getTargets(project: Project, ideConfiguration: RunConfiguration): List<ExecutionTarget> {
        val konanConfiguration = ideConfiguration as? IdeaKonanRunConfiguration ?: return emptyList()
        return konanConfiguration.executable?.executionTargets ?: emptyList()
    }
}

class IdeaKonanExecutionTarget(
    private val executableName: String,
    val isDebug: Boolean,
    val productFile: File,
    val gradleTask: String) : ExecutionTarget() {
    override fun getId() = "KonanExecutionTarget:$executableName:$displayName"
    override fun getDisplayName() = if (isDebug) "Debug" else "Release"
    override fun getIcon(): Icon? = null

    override fun canRun(configuration: RunConfiguration) = configuration is IdeaKonanRunConfiguration

    fun toXml(projectDir: File): Element {
        val element = Element(XmlExecutionTarget.nodeName)
        element.setAttribute(XmlExecutionTarget.attributeIsDebug, isDebug.toString())
        element.setAttribute(XmlExecutionTarget.attributeFileName, productFile.toRelativeString(projectDir))
        element.setAttribute(XmlExecutionTarget.attributeGradleTask, gradleTask)
        return element
    }

    companion object {
        fun constructFrom(artifact: KonanArtifactModel, executableName: String): IdeaKonanExecutionTarget? {
            with(artifact) {
                return IdeaKonanExecutionTarget(
                    executableName,
                    buildTaskPath.contains("debug", ignoreCase = true),
                    file,
                    buildTaskPath)
            }
        }

        fun fromXml(parentElement: Element, executableName: String, projectDir: File): List<IdeaKonanExecutionTarget> {
            val result = ArrayList<IdeaKonanExecutionTarget>()
            parentElement.getChildren(XmlExecutionTarget.nodeName).forEach { element ->
                val isDebug = element.getAttribute(XmlExecutionTarget.attributeIsDebug)?.value?.toBoolean() ?: return@forEach
                val relativeFilePath = element.getAttribute(XmlExecutionTarget.attributeFileName)?.value ?: return@forEach
                val gradleTask = element.getAttribute(XmlExecutionTarget.attributeGradleTask)?.value ?: return@forEach

                result.add(
                    IdeaKonanExecutionTarget(
                        executableName,
                        isDebug,
                        projectDir.resolve(relativeFilePath),
                        gradleTask)
                )
            }

            return result
        }
    }
}