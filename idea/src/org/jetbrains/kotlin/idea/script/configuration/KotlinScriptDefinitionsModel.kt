/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script.configuration

import com.intellij.ui.BooleanTableCellEditor
import com.intellij.ui.BooleanTableCellRenderer
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import org.jetbrains.kotlin.idea.core.script.StandardIdeScriptDefinition
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.scripting.compiler.plugin.KotlinScriptDefinitionAdapterFromNewAPIBase

class KotlinScriptDefinitionsModelDescriptor(val definition: KotlinScriptDefinition, var isEnabled: Boolean)

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
    fun setDefinitions(definitions: List<KotlinScriptDefinition>, settings: KotlinScriptingSettings) {
        items = definitions.mapTo(arrayListOf()) { KotlinScriptDefinitionsModelDescriptor(it, settings.isScriptDefinitionEnabled(it)) }
    }

    private class ScriptDefinitionName : ColumnInfo<KotlinScriptDefinitionsModelDescriptor, String>("Name") {
        override fun valueOf(item: KotlinScriptDefinitionsModelDescriptor) = item.definition.name
    }

    private class ScriptDefinitionPattern : ColumnInfo<KotlinScriptDefinitionsModelDescriptor, String>("Pattern/Extension") {
        override fun valueOf(item: KotlinScriptDefinitionsModelDescriptor): String {
            val definition = item.definition
            return when (definition) {
                is KotlinScriptDefinitionFromAnnotatedTemplate -> definition.scriptFilePattern.pattern
                is KotlinScriptDefinitionAdapterFromNewAPIBase -> definition.fileExtension
                is StandardIdeScriptDefinition -> KotlinParserDefinition.STD_SCRIPT_EXT
                else -> ""
            }
        }
    }

    private class ScriptDefinitionIsEnabled : ColumnInfo<KotlinScriptDefinitionsModelDescriptor, Boolean>("Is Enabled") {
        override fun valueOf(item: KotlinScriptDefinitionsModelDescriptor): Boolean = item.isEnabled
        override fun setValue(item: KotlinScriptDefinitionsModelDescriptor, value: Boolean) {
            item.isEnabled = value
        }

        override fun getEditor(item: KotlinScriptDefinitionsModelDescriptor?) = BooleanTableCellEditor()
        override fun getRenderer(item: KotlinScriptDefinitionsModelDescriptor?) = BooleanTableCellRenderer()
        override fun isCellEditable(item: KotlinScriptDefinitionsModelDescriptor) = item.definition !is StandardIdeScriptDefinition
    }

    companion object {
        fun createModel(definitions: List<KotlinScriptDefinition>, settings: KotlinScriptingSettings): KotlinScriptDefinitionsModel {
            return KotlinScriptDefinitionsModel(definitions.mapTo(arrayListOf()) {
                KotlinScriptDefinitionsModelDescriptor(
                    it,
                    settings.isScriptDefinitionEnabled(it)
                )
            })
        }
    }
}