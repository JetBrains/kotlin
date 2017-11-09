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

import com.intellij.facet.Facet
import com.intellij.facet.FacetType
import com.intellij.facet.FacetTypeId
import com.intellij.facet.FacetTypeRegistry
import com.intellij.facet.ui.FacetEditor
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinIcons
import javax.swing.Icon

class KotlinFacetType : FacetType<KotlinFacet, KotlinFacetConfiguration>(TYPE_ID, ID, NAME) {
    companion object {
        val TYPE_ID = FacetTypeId<KotlinFacet>("kotlin-language")
        val ID = "kotlin-language"
        val NAME = "Kotlin"

        val INSTANCE: KotlinFacetType
            get() = FacetTypeRegistry.getInstance().findFacetType(TYPE_ID) as KotlinFacetType
    }

    override fun isSuitableModuleType(moduleType: ModuleType<*>) = moduleType is JavaModuleType

    override fun getIcon(): Icon = KotlinIcons.SMALL_LOGO

    override fun createDefaultConfiguration() = KotlinFacetConfiguration()

    override fun createFacet(
            module: Module,
            name: String,
            configuration: KotlinFacetConfiguration,
            underlyingFacet: Facet<*>?
    ) = KotlinFacet(module, name, configuration)

    override fun createMultipleConfigurationsEditor(project: Project, editors: Array<out FacetEditor>) =
            MultipleKotlinFacetEditor(project, editors)
}