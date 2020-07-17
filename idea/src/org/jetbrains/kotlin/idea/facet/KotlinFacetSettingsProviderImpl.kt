/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.facet

import com.intellij.ProjectTopics
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.psi.UserDataProperty

class KotlinFacetSettingsProviderImpl(private val project: Project) : KotlinFacetSettingsProvider {
    companion object {
        private var Module.facetSettingsCache: KotlinFacetSettings? by UserDataProperty(Key.create("FACET_SETTINGS_CACHE"))
    }

    init {
        project.messageBus.connect().subscribe(
            ProjectTopics.PROJECT_ROOTS,
            object : ModuleRootListener {
                override fun rootsChanged(event: ModuleRootEvent) {
                    ModuleManager.getInstance(project).modules.forEach { it.facetSettingsCache = null }
                }
            }
        )
    }

    override fun getSettings(module: Module) = KotlinFacet.get(module)?.configuration?.settings

    override fun getInitializedSettings(module: Module): KotlinFacetSettings {
        getSettings(module)?.let {
            it.initializeIfNeeded(module, null)
            return it
        }

        module.facetSettingsCache?.let { return it }

        return KotlinFacetSettings().apply {
            initializeIfNeeded(module, null)
            module.facetSettingsCache = this
        }
    }
}