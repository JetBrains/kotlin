/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.ExecutionTargetListener
import com.intellij.execution.ExecutionTargetManager
import com.intellij.execution.RunManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import org.jdom.Element
import org.jetbrains.kotlin.gradle.KonanArtifactModel
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.io.File

@State(name = "KonanWorkspace", storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))])
class IdeaKonanWorkspace(val project: Project) : PersistentStateComponent<Element>, ProjectComponent {

    val executables = HashSet<KonanExecutable>()
    private val basePath = File(project.basePath!!)
    var konanHome: String? = null

    var konanVersion: KonanVersion? = null
        set(value) {
            value?.let {
                if (it.major < 1 || it.minor < 3) {
                    KonanLog.MESSAGES.createNotification(
                        "You are using Kotlin/Native version $it. It is obsolete and some plugin functionality may be unavailable.",
                        NotificationType.WARNING
                    ).notify(project)
                }
            }
            field = value
        }

    val isDebugPossible: Boolean
        get() {
            konanVersion?.let {
                return it.major > 1 || (it.major == 1 && it.minor >= 3)
            }

            return false
        }

    init {
        val listener = MyListener(this)
        val connection = project.messageBus.connect()
        connection.subscribe(ExecutionTargetManager.TOPIC, listener)
    }

    override fun getState(): Element? {
        val stateElement = Element("state")
        val executablesElement = Element(XmlKonanWorkspace.nodeAllExecutables)
        executables.toSortedSet().forEach {
            val element = Element(XmlKonanWorkspace.nodeExecutable)
            it.writeToXml(element, basePath)
            executablesElement.addContent(element)
        }
        stateElement.addContent(executablesElement)
        konanHome?.let { stateElement.setAttribute(XmlKonanWorkspace.attributeKonanHome, it) }
        return stateElement
    }

    override fun loadState(stateElement: Element) {
        stateElement.getChild(XmlKonanWorkspace.nodeAllExecutables)?.getChildren(XmlKonanWorkspace.nodeExecutable)?.forEach {
            val executable = KonanExecutable.readFromXml(it, basePath) ?: return@forEach
            executables.add(executable)
        }

        konanHome = stateElement.getAttributeValue(XmlKonanWorkspace.attributeKonanHome)?.also {

        }
    }

    private class MyListener(private val workspace: IdeaKonanWorkspace) : ExecutionTargetListener {
        override fun activeTargetChanged(target: ExecutionTarget) {
            workspace.updateSelectedTarget(target)
        }
    }

    fun updateSelectedTarget(target: ExecutionTarget) {
        val configuration = RunManager.getInstance(project).selectedConfiguration?.configuration ?: return
        if (target is IdeaKonanExecutionTarget && configuration is IdeaKonanRunConfiguration) {
            configuration.selectedTarget = target
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): IdeaKonanWorkspace = project.getComponent(IdeaKonanWorkspace::class.java)
    }

    override fun projectOpened() {
        super.projectOpened()

        val runManager = RunManager.getInstance(project)

        runManager.selectedConfiguration?.apply {
            (configuration as? IdeaKonanRunConfiguration)?.let {
                it.selectedTarget = it.executable?.executionTargets?.firstOrNull()
            }
        }

        updateSelectedTarget(ExecutionTargetManager.getActiveTarget(project))

        konanHome?.let { konanVersion = getKotlinNativeVersion(it) }
    }
}