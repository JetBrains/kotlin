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
        isAutoReloadEnabled =
                state.getAttributeBooleanValue(KotlinScriptingSettings::isAutoReloadEnabled.name)
        suppressDefinitionsCheck =
                state.getAttributeBooleanValue(KotlinScriptingSettings::suppressDefinitionsCheck.name)

        val scriptDefinitionsList = state.getChildren(SCRIPT_DEFINITION_TAG)
        for (scriptDefinitionElement in scriptDefinitionsList) {
            scriptDefinitions[scriptDefinitionElement.toKey()] = scriptDefinitionElement.toValue()
        }
    }

    fun setOrder(scriptDefinition: KotlinScriptDefinition, order: Int) {
        scriptDefinitions[scriptDefinition.toKey()] = scriptDefinitions[scriptDefinition.toKey()]?.copy(order = order) ?:
                KotlinScriptDefinitionValue(order)
    }


    fun setEnabled(scriptDefinition: KotlinScriptDefinition, isEnabled: Boolean) {
        scriptDefinitions[scriptDefinition.toKey()] = scriptDefinitions[scriptDefinition.toKey()]?.copy(isEnabled = isEnabled) ?:
                KotlinScriptDefinitionValue(scriptDefinitions.size, isEnabled)
    }

    fun getScriptDefinitionOrder(scriptDefinition: KotlinScriptDefinition): Int {
        return scriptDefinitions[scriptDefinition.toKey()]?.order ?: Integer.MAX_VALUE
    }

    fun isScriptDefinitionEnabled(scriptDefinition: KotlinScriptDefinition): Boolean {
        return scriptDefinitions[scriptDefinition.toKey()]?.isEnabled ?: true
    }

    private data class KotlinScriptDefinitionKey(val definitionName: String, val className: String)
    private data class KotlinScriptDefinitionValue(val order: Int, val isEnabled: Boolean = true)

    private fun Element.toKey() = KotlinScriptDefinitionKey(
        getAttributeValue(KotlinScriptDefinitionKey::definitionName.name),
        getAttributeValue(KotlinScriptDefinitionKey::className.name)
    )

    private fun KotlinScriptDefinition.toKey() =
        KotlinScriptDefinitionKey(this.name, this::class.qualifiedName ?: "unknown")

    private fun Element.addScriptDefinitionContentElement(definition: KotlinScriptDefinitionKey, settings: KotlinScriptDefinitionValue) {
        element(SCRIPT_DEFINITION_TAG).apply {
            attribute(KotlinScriptDefinitionKey::className.name, definition.className)
            attribute(KotlinScriptDefinitionKey::definitionName.name, definition.definitionName)

            element(KotlinScriptDefinitionValue::order.name).apply {
                text = settings.order.toString()
            }

            if (!settings.isEnabled) {
                element(KotlinScriptDefinitionValue::isEnabled.name).apply {
                    text = settings.isEnabled.toString()
                }
            }
        }
    }

    private fun Element.toValue(): KotlinScriptDefinitionValue {
        val order = getChildText(KotlinScriptDefinitionValue::order.name)?.toInt() ?: Integer.MAX_VALUE
        val isEnabled = getChildText(KotlinScriptDefinitionValue::isEnabled.name)?.toBoolean() ?: true

        return KotlinScriptDefinitionValue(order, isEnabled)
    }

    companion object {
        fun getInstance(project: Project): KotlinScriptingSettings =
            ServiceManager.getService(project, KotlinScriptingSettings::class.java)

        private const val SCRIPT_DEFINITION_TAG = "scriptDefinition"

    }
}