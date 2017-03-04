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
import com.intellij.util.xmlb.SkipDefaultsSerializationFilter
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.DataConversionException
import org.jdom.Element
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.*

class KotlinFacetConfiguration : FacetConfiguration {
    var settings = KotlinFacetSettings()
        private set

    private fun Element.getOption(name: String) = getChildren("option").firstOrNull { it.getAttribute("name").value == name }

    private fun Element.getOptionValue(name: String) = getOption(name)?.getAttribute("value")?.value

    private fun Element.getOptionBody(name: String) = getOption(name)?.children?.firstOrNull()

    private fun readV1Config(element: Element) {
        val useProjectSettings = element.getOptionValue("useProjectSettings")?.toBoolean()

        val targetPlatformName = element.getOptionBody("versionInfo")?.getOptionValue("targetPlatformName")
        val targetPlatform = TargetPlatformKind.ALL_PLATFORMS.firstOrNull { it.description == targetPlatformName }
                             ?: TargetPlatformKind.Jvm.get(JvmTarget.DEFAULT)

        val compilerInfoElement = element.getOptionBody("compilerInfo")

        val compilerSettings = CompilerSettings().apply {
            compilerInfoElement?.getOptionBody("compilerSettings")?.let { compilerSettingsElement ->
                XmlSerializer.deserializeInto(this, compilerSettingsElement)
            }
        }

        val commonArgumentsElement = compilerInfoElement?.getOptionBody("_commonCompilerArguments")
        val jvmArgumentsElement = compilerInfoElement?.getOptionBody("k2jvmCompilerArguments")
        val jsArgumentsElement = compilerInfoElement?.getOptionBody("k2jsCompilerArguments")

        val compilerArguments = targetPlatform.createCompilerArguments()

        commonArgumentsElement?.let { XmlSerializer.deserializeInto(compilerArguments, it) }
        when (compilerArguments) {
            is K2JVMCompilerArguments -> jvmArgumentsElement?.let { XmlSerializer.deserializeInto(compilerArguments, it) }
            is K2JSCompilerArguments -> jsArgumentsElement?.let { XmlSerializer.deserializeInto(compilerArguments, it) }
        }

        if (useProjectSettings != null) {
            settings.useProjectSettings = useProjectSettings
        }
        else {
            // Migration problem workaround for pre-1.1-beta releases (mainly 1.0.6) -> 1.1-rc+
            // Problematic cases: 1.1-beta/1.1-beta2 -> 1.1-rc+ (useProjectSettings gets reset to false)
            // This heuristic detects old enough configurations:
            if (jvmArgumentsElement == null) {
                settings.useProjectSettings = false
            }
        }

        settings.compilerSettings = compilerSettings
        settings.compilerArguments = compilerArguments
    }

    private fun readV2Config(element: Element) {
        element.getAttributeValue("useProjectSettings")?.let { settings.useProjectSettings = it.toBoolean() }
        val platformName = element.getAttributeValue("platform")
        val platformKind = TargetPlatformKind.ALL_PLATFORMS.firstOrNull { it.description == platformName } ?: TargetPlatformKind.DEFAULT_PLATFORM
        element.getChild("compilerSettings")?.let {
            settings.compilerSettings = CompilerSettings()
            XmlSerializer.deserializeInto(settings.compilerSettings!!, it)
        }
        element.getChild("compilerArguments")?.let {
            settings.compilerArguments = platformKind.createCompilerArguments()
            XmlSerializer.deserializeInto(settings.compilerArguments!!, it)
        }
    }

    @Suppress("OverridingDeprecatedMember")
    override fun readExternal(element: Element) {
        val version =
                try {
                    element.getAttribute("version")?.intValue
                }
                catch(e: DataConversionException) {
                    null
                } ?: KotlinFacetSettings.DEFAULT_VERSION
        when (version) {
            1 -> readV1Config(element)
            2 -> readV2Config(element)
            else -> settings = KotlinFacetSettings() // Reset facet configuration if versions don't match
        }
    }

    @Suppress("OverridingDeprecatedMember")
    override fun writeExternal(element: Element) {
        val filter = SkipDefaultsSerializationFilter()

        element.setAttribute("version", KotlinFacetSettings.CURRENT_VERSION.toString())
        settings.targetPlatformKind?.let {
            element.setAttribute("platform", it.description)
        }
        if (!settings.useProjectSettings) {
            element.setAttribute("useProjectSettings", settings.useProjectSettings.toString())
        }
        settings.compilerSettings?.let {
            Element("compilerSettings").apply {
                XmlSerializer.serializeInto(it, this, filter)
                element.addContent(this)
            }
        }
        settings.compilerArguments?.let {
            Element("compilerArguments").apply {
                XmlSerializer.serializeInto(it, this, filter)
                element.addContent(this)
            }
        }
    }

    override fun createEditorTabs(
            editorContext: FacetEditorContext,
            validatorsManager: FacetValidatorsManager
    ): Array<FacetEditorTab> {
        settings.initializeIfNeeded(editorContext.module, editorContext.rootModel)

        val tabs = arrayListOf<FacetEditorTab>(KotlinFacetEditorGeneralTab(this, editorContext, validatorsManager))
        KotlinFacetConfigurationExtension.EP_NAME.extensions.flatMapTo(tabs) { it.createEditorTabs(editorContext, validatorsManager) }
        return tabs.toTypedArray()
    }
}
