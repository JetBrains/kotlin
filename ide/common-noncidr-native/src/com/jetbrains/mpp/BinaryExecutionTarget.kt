/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.configurations.RunConfiguration
import com.jetbrains.konan.WorkspaceXML
import com.jetbrains.mpp.runconfig.BinaryRunConfiguration
import org.jdom.Element
import org.jetbrains.kotlin.gradle.KonanArtifactModel
import java.io.File
import javax.swing.Icon

class BinaryExecutionTarget(
    private val executableName: String,
    val isDebug: Boolean,
    val productFile: File,
    val gradleTask: String) : ExecutionTarget() {
    override fun getId() = "KonanExecutionTarget:$executableName:$displayName"
    override fun getDisplayName() = if (isDebug) "Debug" else "Release"
    override fun getIcon(): Icon? = null

    override fun canRun(configuration: RunConfiguration) = configuration is BinaryRunConfiguration

    fun toXml(projectDir: File): Element {
        val element = Element(WorkspaceXML.Target.nodeName)
        element.setAttribute(WorkspaceXML.Target.attributeIsDebug, isDebug.toString())
        element.setAttribute(WorkspaceXML.Target.attributeFileName, productFile.toRelativeString(projectDir))
        element.setAttribute(WorkspaceXML.Target.attributeGradleTask, gradleTask)
        return element
    }

    companion object {
        fun constructFrom(artifact: KonanArtifactModel, executableName: String): BinaryExecutionTarget? {
            with(artifact) {
                return BinaryExecutionTarget(
                    executableName,
                    buildTaskPath.contains("debug", ignoreCase = true),
                    file,
                    buildTaskPath
                )
            }
        }

        fun fromXml(parentElement: Element, executableName: String, projectDir: File): List<BinaryExecutionTarget> {
            val result = ArrayList<BinaryExecutionTarget>()
            parentElement.getChildren(WorkspaceXML.Target.nodeName).forEach { element ->
                val isDebug = element.getAttribute(WorkspaceXML.Target.attributeIsDebug)?.value?.toBoolean() ?: return@forEach
                val relativeFilePath = element.getAttribute(WorkspaceXML.Target.attributeFileName)?.value ?: return@forEach
                val gradleTask = element.getAttribute(WorkspaceXML.Target.attributeGradleTask)?.value ?: return@forEach

                result.add(
                    BinaryExecutionTarget(
                        executableName,
                        isDebug,
                        projectDir.resolve(relativeFilePath),
                        gradleTask
                    )
                )
            }

            return result
        }
    }
}