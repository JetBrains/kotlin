/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import org.jetbrains.kotlin.script.ScriptTemplatesProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.getEnvironment
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class ScriptTemplatesProviderAdapter(private val templatesProvider: ScriptTemplatesProvider) : ScriptDefinitionSourceAsContributor {

    override val id: String
        get() = templatesProvider.id

    override val definitions: Sequence<ScriptDefinition>
        get() =
            loadDefinitionsFromTemplates(
                templatesProvider.templateClassNames.toList(), templatesProvider.templateClasspath,
                ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
                    getEnvironment {
                        templatesProvider.environment
                    }
                },
                templatesProvider.additionalResolverClasspath
            ).asSequence()
}