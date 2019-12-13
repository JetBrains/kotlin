/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.facet

import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import org.jdom.Element
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.config.deserializeFacetSettings
import org.jetbrains.kotlin.config.serializeFacetSettings

class KotlinFacetConfigurationImpl : KotlinFacetConfiguration {
    override var settings = KotlinFacetSettings()
        private set

    @Suppress("OverridingDeprecatedMember")
    override fun readExternal(element: Element) {
        settings = deserializeFacetSettings(element)
    }

    @Suppress("OverridingDeprecatedMember")
    override fun writeExternal(element: Element) {
        settings.serializeFacetSettings(element)
    }

    override fun createEditorTabs(
        editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager
    ): Array<FacetEditorTab> {
        settings.initializeIfNeeded(editorContext.module, editorContext.rootModel)

        val tabs = arrayListOf<FacetEditorTab>(KotlinFacetEditorGeneralTab(this, editorContext, validatorsManager))
        if (KotlinFacetCompilerPluginsTab.parsePluginOptions(this).isNotEmpty()) {
            tabs += KotlinFacetCompilerPluginsTab(this, validatorsManager)
        }
        KotlinFacetConfigurationExtension.EP_NAME.extensions.flatMapTo(tabs) { it.createEditorTabs(editorContext, validatorsManager) }
        return tabs.toTypedArray()
    }
}
