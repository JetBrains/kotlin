/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        private var Module.facetSettingsCache : KotlinFacetSettings? by UserDataProperty(Key.create("FACET_SETTINGS_CACHE"))
    }

    init {
        project.messageBus.connect(project).subscribe(
                ProjectTopics.PROJECT_ROOTS,
                object : ModuleRootListener {
                    override fun rootsChanged(event: ModuleRootEvent?) {
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