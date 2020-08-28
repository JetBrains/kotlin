/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.project.KotlinLibraryToSourceAnalysisComponent
import org.jetbrains.kotlin.idea.project.libraryToSourceAnalysisEnabled

class LibraryToSourceDependencySupportToggleAction : ToggleAction(
    KotlinBundle.message("toggle.library.to.source.dependency.support"),
    KotlinBundle.message("enable.components.for.library.to.source.analysis.in.kotlin"),
    null
) {
    override fun isSelected(e: AnActionEvent): Boolean =
        e.project?.libraryToSourceAnalysisEnabled == true

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        e.project?.let {
            KotlinLibraryToSourceAnalysisComponent.setState(it, state)
        }
    }

}