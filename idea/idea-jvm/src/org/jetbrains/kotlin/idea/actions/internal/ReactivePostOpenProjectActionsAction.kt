/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.kotlin.idea.configuration.ui.KotlinConfigurationCheckerService

class ReactivePostOpenProjectActionsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val configurationChecker = KotlinConfigurationCheckerService.getInstanceIfNotDisposed(project) ?: return
        configurationChecker.performProjectPostOpenActions()
    }
}
