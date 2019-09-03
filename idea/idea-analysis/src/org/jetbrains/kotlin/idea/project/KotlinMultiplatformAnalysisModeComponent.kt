/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.text.nullize
import org.jetbrains.kotlin.caches.project.cacheByClassInvalidatingOnRootModifications
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.idea.facet.KotlinFacet

object KotlinMultiplatformAnalysisModeComponent {
    private const val resolutionModeOption = "kotlin.multiplatform.analysis.mode"
    private val defaultState = Mode.SEPARATE

    @JvmStatic
    fun setMode(project: Project, mode: Mode) {
        PropertiesComponent.getInstance(project).setValue(resolutionModeOption, mode.name, defaultState.name)
    }

    @JvmStatic
    fun getMode(project: Project): Mode {
        @Suppress("InvalidBundleOrProperty")
        val explicitIdeaSetting = PropertiesComponent.getInstance(project).getValue(resolutionModeOption)
        if (explicitIdeaSetting != null) return Mode.valueOf(explicitIdeaSetting)

        if (project.containsImportedHmppModules()) return Mode.COMPOSITE

        return defaultState
    }

    private fun Project.containsImportedHmppModules(): Boolean =
        cacheByClassInvalidatingOnRootModifications(KotlinMultiplatformAnalysisModeComponent::class.java) {
            ModuleManager.getInstance(this).modules.asSequence()
                .mapNotNull { KotlinFacet.get(it) }
                .any { it.configuration.settings.isHmppEnabled }
        }

    enum class Mode {
        // Analyses each platform in a separate [GlobalFacade]
        SEPARATE,

        // Analyses all platforms in one facade. Uses type refinement and [CompositeResolverForModuleFactory]
        COMPOSITE
    }
}

val Project.useCompositeAnalysis: Boolean
    get() = KotlinMultiplatformAnalysisModeComponent.getMode(this) == KotlinMultiplatformAnalysisModeComponent.Mode.COMPOSITE