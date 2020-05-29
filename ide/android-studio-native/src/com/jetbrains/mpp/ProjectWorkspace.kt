/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.ExecutionTargetManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.kmm.KMMTargetListener
import com.jetbrains.kmm.XcProjectFile
import com.jetbrains.konan.WorkspaceXML
import org.jdom.Element
import java.io.File

sealed class XcProjectStatus {
    object Found : XcProjectStatus()
    data class Misconfiguration(val reason: String) : XcProjectStatus()
    data class NotFound(val reason: String) : XcProjectStatus()
    object NotLocated : XcProjectStatus()
}

@State(name = WorkspaceXML.projectComponentName, storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))])
class ProjectWorkspace(project: Project) : WorkspaceBase(project) {

    var xcProjectStatus: XcProjectStatus = XcProjectStatus.NotLocated
    var xcProjectFile: XcProjectFile? = null

    fun locateXCProject(path: String) {
        if (project.basePath == null) {
            xcProjectStatus = XcProjectStatus.Misconfiguration("project base path is absent")
            return
        }

        val xcProjectDir = if (FileUtil.isAbsolute(path))
            File(path)
        else
            File(project.basePath).resolve(path)

        if (!xcProjectDir.exists()) {
            xcProjectStatus = XcProjectStatus.NotFound("directory $xcProjectDir doesn't exist")
            return
        }

        xcProjectFile = XcProjectFile.findXcProjectFile(xcProjectDir)

        xcProjectStatus = if (xcProjectFile != null) {
            XcProjectStatus.Found
        } else {
            XcProjectStatus.NotFound("can't find Xcode projects located at $xcProjectDir")
        }
    }

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(ExecutionTargetManager.TOPIC, KMMTargetListener(this))
    }

    override fun getState(): Element {
        val stateElement = super.getState()

        if (xcProjectFile != null) {
            val xcElement = Element(WorkspaceXML.XCProject.nodeName)
            val relativePath = File(xcProjectFile!!.absolutePath).relativeTo(File(project.basePath))
            xcElement.setAttribute(WorkspaceXML.XCProject.attributePath, relativePath.path)
            stateElement.addContent(xcElement)
        }

        return stateElement
    }

    override fun loadState(stateElement: Element) {
        super.loadState(stateElement)

        val xcElement = stateElement.getChildren(WorkspaceXML.XCProject.nodeName).firstOrNull()
        xcElement?.run {
            val relativePath = getAttributeValue(WorkspaceXML.XCProject.attributePath) ?: return@run
            locateXCProject(relativePath)
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ProjectWorkspace = project.getComponent(ProjectWorkspace::class.java)
    }
}


