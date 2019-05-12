/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.addOptionTag
import com.intellij.util.attribute
import com.intellij.util.getAttributeBooleanValue
import org.jdom.Element
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition

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

    private var scriptDefinitions = linkedMapOf<KotlinScriptDefinitionKey, KotlinScriptDefinitionValue>()

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
        state.getOptionTag(KotlinScriptingSettings::isAutoReloadEnabled.name)?.let {
            isAutoReloadEnabled = it
        }
        state.getOptionTag(KotlinScriptingSettings::suppressDefinitionsCheck.name)?.let {
            suppressDefinitionsCheck = it
        }

        val scriptDefinitionsList = state.getChildren(SCRIPT_DEFINITION_TAG)
        for (scriptDefinitionElement in scriptDefinitionsList) {
            scriptDefinitions[scriptDefinitionElement.toKey()] = scriptDefinitionElement.toValue()
        }
    }

    fun setOrder(scriptDefinition: ScriptDefinition, order: Int) {
        scriptDefinitions[scriptDefinition.toKey()] = scriptDefinitions[scriptDefinition.toKey()]?.copy(order = order) ?:
                KotlinScriptDefinitionValue(order)
    }


    fun setEnabled(scriptDefinition: ScriptDefinition, isEnabled: Boolean) {
        scriptDefinitions[scriptDefinition.toKey()] = scriptDefinitions[scriptDefinition.toKey()]?.copy(isEnabled = isEnabled) ?:
                KotlinScriptDefinitionValue(scriptDefinitions.size, isEnabled)
    }

    fun getScriptDefinitionOrder(scriptDefinition: ScriptDefinition): Int {
        return scriptDefinitions[scriptDefinition.toKey()]?.order ?: Integer.MAX_VALUE
    }

    fun isScriptDefinitionEnabled(scriptDefinition: ScriptDefinition): Boolean {
        return scriptDefinitions[scriptDefinition.toKey()]?.isEnabled ?: true
    }

    private data class KotlinScriptDefinitionKey(val definitionName: String, val className: String)
    private data class KotlinScriptDefinitionValue(val order: Int, val isEnabled: Boolean = true)

    private fun Element.toKey() = KotlinScriptDefinitionKey(
        getAttributeValue(KotlinScriptDefinitionKey::definitionName.name),
        getAttributeValue(KotlinScriptDefinitionKey::className.name)
    )

    private fun ScriptDefinition.toKey() =
        KotlinScriptDefinitionKey(this.name, this.definitionId)

    private fun Element.addScriptDefinitionContentElement(definition: KotlinScriptDefinitionKey, settings: KotlinScriptDefinitionValue) {
        addElement(SCRIPT_DEFINITION_TAG).apply {
            attribute(KotlinScriptDefinitionKey::className.name, definition.className)
            attribute(KotlinScriptDefinitionKey::definitionName.name, definition.definitionName)

            addElement(KotlinScriptDefinitionValue::order.name).apply {
                text = settings.order.toString()
            }

            if (!settings.isEnabled) {
                addElement(KotlinScriptDefinitionValue::isEnabled.name).apply {
                    text = settings.isEnabled.toString()
                }
            }
        }
    }

    private fun Element.addElement(name: String): Element {
        val element = Element(name)
        addContent(element)
        return element
    }

    private fun Element.toValue(): KotlinScriptDefinitionValue {
        val order = getChildText(KotlinScriptDefinitionValue::order.name)?.toInt() ?: Integer.MAX_VALUE
        val isEnabled = getChildText(KotlinScriptDefinitionValue::isEnabled.name)?.toBoolean() ?: true

        return KotlinScriptDefinitionValue(order, isEnabled)
    }

    private fun Element.getOptionTag(name: String) =
        getChildren("option").firstOrNull { it.getAttribute("name").value == name }?.getAttributeBooleanValue("value")

    companion object {
        fun getInstance(project: Project): KotlinScriptingSettings =
            ServiceManager.getService(project, KotlinScriptingSettings::class.java)

        private const val SCRIPT_DEFINITION_TAG = "scriptDefinition"

    }
}