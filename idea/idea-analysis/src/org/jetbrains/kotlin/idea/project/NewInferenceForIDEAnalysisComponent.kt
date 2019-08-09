/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.idea.util.isDev
import org.jetbrains.kotlin.idea.util.isEap
import org.jetbrains.kotlin.idea.util.isSnapshot

object NewInferenceForIDEAnalysisComponent {
    private const val inferenceOptionV1 = "kotlin.use.new.inference.for.ide.analysis"
    private const val inferenceOptionV2 = "kotlin.use.new.inference.for.ide.analysis.v2"
    val defaultState: Boolean
        get() {
            val bundledVersion = KotlinCompilerVersion.VERSION
            return isEap(bundledVersion) || isDev(bundledVersion) || isSnapshot(bundledVersion)
        }

    @JvmStatic
    fun setEnabled(project: Project, state: Boolean) {
        PropertiesComponent.getInstance(project).setValue(inferenceOptionV2, state, defaultState)
    }

    @JvmStatic
    fun isEnabled(project: Project): Boolean {
        return PropertiesComponent.getInstance(project).getBoolean(inferenceOptionV2, defaultState)
    }

    // This method is preserved only for FUS collector and it shouldn't be used in other contexts
    @Deprecated("Use isEnabled method instead", replaceWith = ReplaceWith("this.isEnabled(project)"))
    fun isEnabledForV1(project: Project): Boolean {
        return PropertiesComponent.getInstance(project).getBoolean(inferenceOptionV1, true)
    }
}