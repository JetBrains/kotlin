/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.console.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import org.jetbrains.kotlin.console.KotlinConsoleKeeper

class ConsoleModuleDialog(private val project: Project) {
    private val TITLE = "Choose context module..."

    fun showIfNeeded(dataContext: DataContext) {
        val module = getModule(dataContext)
        if (module != null) return runConsole(module)

        val modules = ModuleManager.getInstance(project).modules

        if (modules.isEmpty()) return errorNotification(project, "No modules were found")
        if (modules.size == 1) return runConsole(modules.first())

        val moduleActions = modules.sortedBy { it.name }.map { createRunAction(it) }
        val moduleGroup = DefaultActionGroup(moduleActions)

        val modulePopup = JBPopupFactory.getInstance().createActionGroupPopup(
            TITLE, moduleGroup, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true, ActionPlaces.UNKNOWN
        )

        modulePopup.showCenteredInCurrentWindow(project)
    }

    private fun getModule(dataContext: DataContext): Module? {
        val file = CommonDataKeys.VIRTUAL_FILE.getData(dataContext) ?: return null

        val moduleForFile = ModuleUtilCore.findModuleForFile(file, project)
        if (moduleForFile != null) return moduleForFile

        return null
    }

    private fun runConsole(module: Module) {
        KotlinConsoleKeeper.getInstance(project).run(module)
    }

    private fun createRunAction(module: Module) = object : AnAction(module.name) {
        override fun actionPerformed(e: AnActionEvent) = runConsole(module)
    }
}