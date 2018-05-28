/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script.configuration

import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import org.jetbrains.kotlin.idea.core.script.StandardIdeScriptDefinition
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.scripting.compiler.plugin.KotlinScriptDefinitionAdapterFromNewAPIBase

class KotlinScriptDefinitionsModel(definitions: ArrayList<KotlinScriptDefinition>) :
    ListTableModel<KotlinScriptDefinition>(
        arrayOf(
            ScriptDefinitionName(),
            ScriptDefinitionPattern()
        ),
        definitions,
        0
    ) {

    private class ScriptDefinitionName : ColumnInfo<KotlinScriptDefinition, String>("Name") {
        override fun valueOf(item: KotlinScriptDefinition) = item.name
    }

    private class ScriptDefinitionPattern : ColumnInfo<KotlinScriptDefinition, String>("Pattern/Extension") {
        override fun valueOf(item: KotlinScriptDefinition): String {
            return when (item) {
                is KotlinScriptDefinitionFromAnnotatedTemplate -> item.scriptFilePattern.pattern
                is KotlinScriptDefinitionAdapterFromNewAPIBase -> item.fileExtension
                is StandardIdeScriptDefinition -> KotlinParserDefinition.STD_SCRIPT_EXT
                else -> ""
            }
        }
    }
}