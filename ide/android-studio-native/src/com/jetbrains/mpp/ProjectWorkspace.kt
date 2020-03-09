/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.*
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.konan.TargetListener
import com.jetbrains.konan.WorkspaceBase
import com.jetbrains.mpp.execution.Device
import org.jdom.Element

internal object XCProject {
    const val nodeName = "xcodeproj"
    const val pathAttributeKey = "PATH"
}

private class DeviceTargetListener(workspace: ProjectWorkspace) : TargetListener(workspace) {
    override fun activeTargetChanged(target: ExecutionTarget) {
        super.activeTargetChanged(target)

        (configuration() as? AppleRunConfiguration)?.let {
            it.selectedDevice = target as? Device
        }
    }
}

@State(name = "KotlinMultiplatform", storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))])
class ProjectWorkspace(project: Project) : WorkspaceBase(project) {
    var xcproject: String? = null

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(ExecutionTargetManager.TOPIC, DeviceTargetListener(this))
    }

    override fun getState(): Element {
        val stateElement = super.getState()

        val xcElement = Element(XCProject.nodeName)
        xcproject?.let {
            xcElement.setAttribute(XCProject.pathAttributeKey, xcproject)
            stateElement.addContent(xcElement)
        }

        return stateElement
    }

    override fun loadState(stateElement: Element) {
        super.loadState(stateElement)

        val xcElement = stateElement.getChildren(XCProject.nodeName).firstOrNull()
        xcElement?.run {
            xcproject = getAttributeValue(XCProject.pathAttributeKey)
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ProjectWorkspace = project.getComponent(ProjectWorkspace::class.java)
    }
}


