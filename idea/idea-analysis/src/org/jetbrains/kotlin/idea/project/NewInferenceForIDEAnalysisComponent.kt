/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project

object NewInferenceForIDEAnalysisComponent {
    private const val inferenceOption = "kotlin.use.new.inference.for.ide.analysis"
    private const val defaultState = true

    @JvmStatic
    fun setEnabled(project: Project, state: Boolean) {
        PropertiesComponent.getInstance(project).setValue(inferenceOption, state, defaultState)
    }

    @JvmStatic
    fun isEnabled(project: Project): Boolean {
        return PropertiesComponent.getInstance(project).getBoolean(inferenceOption, defaultState)
    }
}