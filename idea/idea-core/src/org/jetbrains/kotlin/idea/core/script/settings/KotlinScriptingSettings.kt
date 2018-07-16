/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.addOptionTag
import com.intellij.util.attribute
import com.intellij.util.element
import com.intellij.util.getAttributeBooleanValue
import org.jdom.Element
import org.jetbrains.kotlin.script.KotlinScriptDefinition

@State(
    name = "KotlinScriptingSettings",
    storages = [Storage("kotlinScripting.xml")]
)
class KotlinScriptingSettings : PersistentStateComponent<Element> {
    var isAutoReloadEnabled = false

    /**
     * true if notification about multiple script definition applicable for one script file is suppressed
     */
    var suppressDefinitionsCheck = false

    private var scriptDefinitions = linkedMapOf<KotlinScriptDefinitionKey, Int>()

    override fun getState(): Element {
        val definitionsRootElement = Element("KotlinScriptingSettings")

        if (isAutoReloadEnabled) {
            definitionsRootElement.addOptionTag(
                KotlinScriptingSettings::isAutoReloadEnabled.name,
                isAutoReloadEnabled.toString()
            )
        }

        if (suppressDefinitionsCheck) {
            definitionsRootElement.addOptionTag(
                KotlinScriptingSettings::suppressDefinitionsCheck.name,
                suppressDefinitionsCheck.toString()
            )
        }

        if (scriptDefinitions.isEmpty()) {
            return definitionsRootElement
        }

        for (scriptDefinition in scriptDefinitions) {
            definitionsRootElement.addScriptDefinitionContentElement(scriptDefinition.key, scriptDefinition.value)
        }

        return definitionsRootElement
    }

    override fun loadState(state: Element) {
        isAutoReloadEnabled =
                state.getAttributeBooleanValue(KotlinScriptingSettings::isAutoReloadEnabled.name)
        suppressDefinitionsCheck =
                state.getAttributeBooleanValue(KotlinScriptingSettings::suppressDefinitionsCheck.name)

        val scriptDefinitionsList = state.getChildren(SCRIPT_DEFINITION_TAG)
        for (scriptDefinitionElement in scriptDefinitionsList) {
            scriptDefinitions[scriptDefinitionElement.toKey()] = scriptDefinitionElement.getOrderTag()
        }
    }

    fun saveScriptDefinitionsOrder(newScriptDefinitions: List<KotlinScriptDefinition>) {
        for ((index, scriptDefinition) in newScriptDefinitions.withIndex()) {
            scriptDefinitions[scriptDefinition.toKey()] = index
        }
    }

    fun getScriptDefinitionOrder(scriptDefinition: KotlinScriptDefinition): Int {
        return scriptDefinitions[scriptDefinition.toKey()] ?: Integer.MAX_VALUE
    }

    private data class KotlinScriptDefinitionKey(val definitionName: String, val className: String)

    private fun Element.toKey() = KotlinScriptDefinitionKey(
        getAttributeValue(KotlinScriptDefinitionKey::definitionName.name),
        getAttributeValue(KotlinScriptDefinitionKey::className.name)
    )

    private fun KotlinScriptDefinition.toKey() =
        KotlinScriptDefinitionKey(this.name, this::class.qualifiedName ?: "unknown")

    private fun Element.addScriptDefinitionContentElement(definition: KotlinScriptDefinitionKey, order: Int) {
        element(SCRIPT_DEFINITION_TAG).apply {
            attribute(KotlinScriptDefinitionKey::className.name, definition.className)
            attribute(KotlinScriptDefinitionKey::definitionName.name, definition.definitionName)

            element(SCRIPT_DEFINITION_ORDER_TAG).apply {
                text = order.toString()
            }
        }
    }

    private fun Element.getOrderTag(): Int {
        return getChildText(SCRIPT_DEFINITION_ORDER_TAG)?.toInt() ?: Integer.MAX_VALUE
    }

    companion object {
        fun getInstance(project: Project): KotlinScriptingSettings =
            ServiceManager.getService(project, KotlinScriptingSettings::class.java)

        private const val SCRIPT_DEFINITION_TAG = "scriptDefinition"
        private const val SCRIPT_DEFINITION_ORDER_TAG = "order"

    }
}