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

import com.intellij.facet.FacetConfiguration
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.openapi.components.PersistentStateComponent
import org.jdom.Element
import org.jetbrains.kotlin.config.KotlinFacetSettings

class KotlinFacetConfiguration : FacetConfiguration, PersistentStateComponent<KotlinFacetSettings> {
    private var settings = KotlinFacetSettings()

    @Suppress("OverridingDeprecatedMember")
    override fun readExternal(element: Element?) {

    }

    @Suppress("OverridingDeprecatedMember")
    override fun writeExternal(element: Element?) {

    }

    override fun loadState(state: KotlinFacetSettings) {
        this.settings = state
    }

    override fun getState() = settings

    override fun createEditorTabs(
            editorContext: FacetEditorContext,
            validatorsManager: FacetValidatorsManager
    ): Array<FacetEditorTab> {
        state.initializeIfNeeded(editorContext.module, editorContext.rootModel)

        val compilerTab = KotlinFacetEditorCompilerTab(state.compilerInfo, editorContext)
        val generalTab = KotlinFacetEditorGeneralTab(this, editorContext, validatorsManager, compilerTab)
        val tabs = arrayListOf(generalTab, compilerTab)
        KotlinFacetConfigurationExtension.EP_NAME.extensions.flatMapTo(tabs) { it.createEditorTabs(editorContext, validatorsManager) }
        return tabs.toTypedArray()
    }
}
