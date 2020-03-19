/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.*
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.kmm.KMMTargetListener
import com.jetbrains.konan.WorkspaceXML
import org.jdom.Element


@State(name = WorkspaceXML.projectComponentName, storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))])
class ProjectWorkspace(project: Project) : WorkspaceBase(project) {
    var xcproject: String? = null

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(ExecutionTargetManager.TOPIC, KMMTargetListener(this))
    }

    override fun getState(): Element {
        val stateElement = super.getState()

        val xcElement = Element(WorkspaceXML.XCProject.nodeName)
        xcproject?.let {
            xcElement.setAttribute(WorkspaceXML.XCProject.attributePath, xcproject)
            stateElement.addContent(xcElement)
        }

        return stateElement
    }

    override fun loadState(stateElement: Element) {
        super.loadState(stateElement)

        val xcElement = stateElement.getChildren(WorkspaceXML.XCProject.nodeName).firstOrNull()
        xcElement?.run {
            xcproject = getAttributeValue(WorkspaceXML.XCProject.attributePath)
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ProjectWorkspace = project.getComponent(ProjectWorkspace::class.java)
    }
}


