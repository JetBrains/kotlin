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
import org.jetbrains.kotlin.idea.util.DescriptionAware

class KotlinFacetConfiguration : FacetConfiguration, PersistentStateComponent<KotlinFacetConfiguration.Settings> {
    enum class LanguageLevel(override val description: String) : DescriptionAware {
        KOTLIN_1_0("1.0"),
        KOTLIN_1_1("1.1")
    }

    enum class TargetPlatform(override val description: String) : DescriptionAware {
        JVM_1_6("JVM 1.6"),
        JVM_1_8("JVM 1.8"),
        JS("JavaScript")
    }

    class Settings {
        var languageLevel: LanguageLevel? = null
        var targetPlatformKind: TargetPlatform? = null
    }

    private var settings = Settings()

    @Suppress("OverridingDeprecatedMember")
    override fun readExternal(element: Element?) {

    }

    @Suppress("OverridingDeprecatedMember")
    override fun writeExternal(element: Element?) {

    }

    override fun loadState(state: Settings) {
        this.settings = settings
    }

    override fun getState() = settings

    override fun createEditorTabs(
            editorContext: FacetEditorContext,
            validatorsManager: FacetValidatorsManager
    ): Array<FacetEditorTab> = arrayOf(KotlinFacetEditorTab(this, editorContext, validatorsManager))
}
