/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.util.ui.EmptyClipboardOwner
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.idea.util.buildNumber
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class CopyKotlinProjectOverviewAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT) ?: return

        try {
            val result = """
                ${getIDEVersion()}
                ${KotlinPluginUtil.getPluginVersion()}
                Kotlin Presence: ${prepareInfo(getKotlinStateInModules(project), "Absent")}
                Build: ${prepareInfo(getModuleBuildSystems(project))}
                Build Versions: ${prepareInfo(getModuleBuildVersions(project), "Unknown")}
            """.trimIndent()

            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(result), EmptyClipboardOwner.INSTANCE)
        } catch (_: IndexNotReadyException) {
            DumbService.getInstance(project).showDumbModeNotification("Can't finish while indexing is in progress")
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = ApplicationManager.getApplication().isInternal

        val project = e.getData(CommonDataKeys.PROJECT)
        e.presentation.isEnabled = project != null
    }

    private fun getIDEVersion(): String {
        val appInfo = ApplicationInfoEx.getInstanceEx() as ApplicationInfoImpl
        val appName = appInfo.fullApplicationName
        val edition = ApplicationNamesInfo.getInstance().editionName

        return if (edition != null) "$appName ($edition)" else appName
    }

    private fun prepareInfo(infoSequence: Sequence<String?>, noValue: String = "NO VALUE"): String {
        val size = 10
        val infos = infoSequence.take(size + 1).toList()
        val infosSet = infos.subList(0, minOf(infos.size, size)).toSet().filterNotNull()

        val values = if (infosSet.isEmpty()) noValue else infosSet.joinToString(separator = ", ")

        return values + if (infos.size > size) ", ..." else ""
    }

    private fun getKotlinStateInModules(project: Project): Sequence<String?> {
        val modules = ModuleManager.getInstance(project).modules

        return sequence {
            for (module in modules) {
                yield(
                    when {
                        hasKotlinFilesInSources(module) -> "In Sources"
                        hasKotlinFilesOnlyInTests(module) -> "In Tests"
                        else -> null
                    }
                )
            }
        }
    }

    private fun getModuleBuildSystems(project: Project): Sequence<String> {
        val modules = ModuleManager.getInstance(project).modules

        return sequence {
            for (module in modules) {
                yield(module.getBuildSystemType().javaClass.simpleName)
            }
        }
    }

    private fun getModuleBuildVersions(project: Project): Sequence<String?> {
        val modules = ModuleManager.getInstance(project).modules

        return sequence {
            for (module in modules) {
                val compilerVersion = if (module.getBuildSystemType() == BuildSystemType.JPS) {
                    buildNumber
                } else {
                    module.externalCompilerVersion
                }

                yield(compilerVersion)
            }
        }
    }
}
