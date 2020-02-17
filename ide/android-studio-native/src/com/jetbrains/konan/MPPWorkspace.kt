/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.execution.*
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.mpp.AppleRunConfiguration
import com.jetbrains.mpp.execution.Device
import org.jdom.Element

internal object XCProject {
    const val nodeName = "xcodeproj"
    const val pathAttributeKey = "PATH"
}

private class TargetListener(private val workspace: MPPWorkspace) : ExecutionTargetListener {
    override fun activeTargetChanged(target: ExecutionTarget) {
        val configuration = RunManager.getInstance(workspace.project).selectedConfiguration?.configuration ?: return
        if (configuration !is AppleRunConfiguration) return
        configuration.selectedDevice = target as? Device
    }
}

@State(name = "MPPWorkspace", storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))])
class MPPWorkspace(val project: Project) : PersistentStateComponent<Element>, ProjectComponent {
    var xcproject: String? = null

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(ExecutionTargetManager.TOPIC, TargetListener(this))
    }

    override fun getState(): Element? {
        val stateElement = Element("state")
        val xcElement = Element(XCProject.nodeName)
        xcproject?.let {
            xcElement.setAttribute(XCProject.pathAttributeKey, xcproject)
            stateElement.addContent(xcElement)
        }

        return stateElement
    }

    override fun loadState(stateElement: Element) {
        val xcElement = stateElement.getChildren(XCProject.nodeName).firstOrNull()
        xcElement?.run {
            xcproject = getAttributeValue(XCProject.pathAttributeKey)
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): MPPWorkspace = project.getComponent(MPPWorkspace::class.java)
    }
}


