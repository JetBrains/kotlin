/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project

object KotlinLibraryToSourceAnalysisComponent {
    private const val libraryToSourceAnalysisOption = "kotlin.library.to.source.analysis"

    @JvmStatic
    fun setState(project: Project, isEnabled: Boolean) {
        PropertiesComponent.getInstance(project).setValue(libraryToSourceAnalysisOption, isEnabled, false)
    }

    @JvmStatic
    fun isEnabled(project: Project): Boolean =
        PropertiesComponent.getInstance(project).getBoolean(libraryToSourceAnalysisOption)
}

val Project.libraryToSourceAnalysisEnabled: Boolean
    get() = KotlinLibraryToSourceAnalysisComponent.isEnabled(this)