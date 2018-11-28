/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.ScriptTemplatesProvider

class ScriptTemplatesProviderAdapter(private val templatesProvider: ScriptTemplatesProvider) :
    ScriptDefinitionContributor {
    override val id: String
        get() = templatesProvider.id

    override fun getDefinitions(): List<KotlinScriptDefinition> {
        return loadDefinitionsFromTemplates(
            templatesProvider.templateClassNames.toList(), templatesProvider.templateClasspath,
            templatesProvider.environment.orEmpty(), templatesProvider.additionalResolverClasspath
        )
    }
}