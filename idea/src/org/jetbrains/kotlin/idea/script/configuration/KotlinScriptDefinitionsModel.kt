/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script.configuration

import com.intellij.ui.BooleanTableCellEditor
import com.intellij.ui.BooleanTableCellRenderer
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.script.StandardIdeScriptDefinition
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate

class KotlinScriptDefinitionsModelDescriptor(val definition: ScriptDefinition, var isEnabled: Boolean)

class KotlinScriptDefinitionsModel private constructor(definitions: MutableList<KotlinScriptDefinitionsModelDescriptor>) :
    ListTableModel<KotlinScriptDefinitionsModelDescriptor>(
        arrayOf(
            ScriptDefinitionName(),
            ScriptDefinitionPattern(),
            ScriptDefinitionIsEnabled()
        ),
        definitions,
        0
    ) {

    fun getDefinitions() = items.map { it.definition }
    fun setDefinitions(definitions: List<ScriptDefinition>, settings: KotlinScriptingSettings) {
        items = definitions.mapTo(arrayListOf()) { KotlinScriptDefinitionsModelDescriptor(it, settings.isScriptDefinitionEnabled(it)) }
    }

    private class ScriptDefinitionName : ColumnInfo<KotlinScriptDefinitionsModelDescriptor, String>(
        KotlinBundle.message("kotlin.script.definitions.model.name.name")
    ) {
        override fun valueOf(item: KotlinScriptDefinitionsModelDescriptor) = item.definition.name
    }

    private class ScriptDefinitionPattern : ColumnInfo<KotlinScriptDefinitionsModelDescriptor, String>(
        KotlinBundle.message("kotlin.script.definitions.model.name.pattern.extension")
    ) {
        override fun valueOf(item: KotlinScriptDefinitionsModelDescriptor): String {
            val definition = item.definition
            return definition.asLegacyOrNull<KotlinScriptDefinitionFromAnnotatedTemplate>()?.scriptFilePattern?.pattern
                ?: (definition as? ScriptDefinition.FromConfigurations)?.filePathPattern
                ?: "." + definition.fileExtension
        }
    }

    private class ScriptDefinitionIsEnabled : ColumnInfo<KotlinScriptDefinitionsModelDescriptor, Boolean>(
        KotlinBundle.message("kotlin.script.definitions.model.name.is.enabled")
    ) {
        override fun valueOf(item: KotlinScriptDefinitionsModelDescriptor): Boolean = item.isEnabled
        override fun setValue(item: KotlinScriptDefinitionsModelDescriptor, value: Boolean) {
            item.isEnabled = value
        }

        override fun getEditor(item: KotlinScriptDefinitionsModelDescriptor?) = BooleanTableCellEditor()
        override fun getRenderer(item: KotlinScriptDefinitionsModelDescriptor?) = BooleanTableCellRenderer()
        override fun isCellEditable(item: KotlinScriptDefinitionsModelDescriptor) =
            item.definition.asLegacyOrNull<StandardIdeScriptDefinition>() == null
    }

    companion object {
        fun createModel(definitions: List<ScriptDefinition>, settings: KotlinScriptingSettings): KotlinScriptDefinitionsModel =
            KotlinScriptDefinitionsModel(definitions.mapTo(arrayListOf()) {
                KotlinScriptDefinitionsModelDescriptor(
                    it,
                    settings.isScriptDefinitionEnabled(it)
                )
            })
    }
}