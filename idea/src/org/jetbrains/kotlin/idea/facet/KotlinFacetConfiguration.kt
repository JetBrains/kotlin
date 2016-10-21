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
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.idea.util.DescriptionAware

enum class LanguageLevel(override val description: String) : DescriptionAware {
    KOTLIN_1_0("1.0"),
    KOTLIN_1_1("1.1")
}

sealed class TargetPlatformKind<out Version : DescriptionAware>(
        val version: Version,
        val name: String
) : DescriptionAware {
    override val description = "$name ${version.description}"

    companion object {
        val ALL_PLATFORMS: List<TargetPlatformKind<*>> by lazy { JVMPlatform.JVM_PLATFORMS + JSPlatform }
    }
}

object NoVersion : DescriptionAware {
    override val description = ""
}

enum class JVMVersion(override val description: String) : DescriptionAware {
    JVM_1_6("1.6"),
    JVM_1_8("1.8")
}

class JVMPlatform(version: JVMVersion) : TargetPlatformKind<JVMVersion>(version, "JVM") {
    companion object {
        val JVM_PLATFORMS by lazy { JVMVersion.values().map(::JVMPlatform) }

        operator fun get(version: JVMVersion) = JVM_PLATFORMS[version.ordinal]
    }
}

object JSPlatform : TargetPlatformKind<NoVersion>(NoVersion, "JavaScript")

data class KotlinVersionInfo(
        var languageLevel: LanguageLevel? = null,
        var apiLevel: LanguageLevel? = null,
        var targetPlatformKindKind: TargetPlatformKind<*>? = null
)

class KotlinCompilerInfo {
    var commonCompilerArguments: CommonCompilerArguments? = null
    var k2jsCompilerArguments: K2JSCompilerArguments? = null
    var compilerSettings: CompilerSettings? = null
}

class KotlinFacetConfiguration : FacetConfiguration, PersistentStateComponent<KotlinFacetConfiguration.Settings> {
    class Settings {
        val versionInfo = KotlinVersionInfo()
        val compilerInfo = KotlinCompilerInfo()
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
    ): Array<FacetEditorTab> {
        state.initializeIfNeeded(editorContext.module, editorContext.rootModel)

        val compilerTab = KotlinFacetEditorCompilerTab(state.compilerInfo, editorContext)
        val generalTab = KotlinFacetEditorGeneralTab(this, editorContext, validatorsManager, compilerTab)
        val tabs = arrayListOf(generalTab, compilerTab)
        KotlinFacetConfigurationExtension.EP_NAME.extensions.flatMapTo(tabs) { it.createEditorTabs(editorContext, validatorsManager) }
        return tabs.toTypedArray()
    }
}
