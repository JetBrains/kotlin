/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import java.io.File
import kotlin.script.experimental.dependencies.DependenciesResolver

@Deprecated("Use ScriptDefinitionContributor EP and loadDefinitionsFromTemplates top level function")
interface ScriptTemplatesProvider {

    // for resolving ambiguities
    val id: String

    @Deprecated("Parameter isn't used for resolving priorities anymore. " +
                "com.intellij.openapi.extensions.LoadingOrder constants can be used to order providers when registered from Intellij plugin.",
                ReplaceWith("0"))
    val version: Int
        get() = 0

    val isValid: Boolean

    val templateClassNames: Iterable<String>

    val resolver: DependenciesResolver? get() = null

    val filePattern: String? get() = null

    val templateClasspath: List<File>

    // TODO: need to provide a way to specify this in compiler/repl .. etc
    /*
     * Allows to specify additional jars needed for DependenciesResolver (and not script template).
     * Script template dependencies naturally become (part of) dependencies of the script which is not always desired for resolver dependencies.
     * i.e. gradle resolver may depend on some jars that 'built.gradle.kts' files should not depend on.
     */
    val additionalResolverClasspath: List<File> get() = emptyList()

    val environment: Map<String, Any?>?

    companion object {
        val EP_NAME: ExtensionPointName<ScriptTemplatesProvider> =
                ExtensionPointName.create<ScriptTemplatesProvider>("org.jetbrains.kotlin.scriptTemplatesProvider")
    }
}

class ScriptTemplatesProviderAdapter(private val templatesProvider: ScriptTemplatesProvider) : ScriptDefinitionContributor {
    override val id: String
        get() = templatesProvider.id

    override fun getDefinitions(): List<KotlinScriptDefinition> {
        return loadDefinitionsFromTemplates(
                templatesProvider.templateClassNames.toList(), templatesProvider.templateClasspath,
                templatesProvider.environment.orEmpty(), templatesProvider.additionalResolverClasspath)
    }
}