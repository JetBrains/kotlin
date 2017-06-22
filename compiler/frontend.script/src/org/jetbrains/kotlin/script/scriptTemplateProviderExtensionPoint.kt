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

package org.jetbrains.kotlin.script

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import java.io.File
import java.net.URLClassLoader
import kotlin.script.dependencies.ScriptDependenciesResolver

interface ScriptTemplatesProvider {

    // for resolving ambiguities
    val id: String

    @Deprecated("Parameter isn't used for resolving priorities anymore. " +
                "com.intellij.openapi.extensions.LoadingOrder constants can be used to order providers when registered from Intellij plugin.",
                ReplaceWith("0"))
    val version: Int get() = 0

    val isValid: Boolean

    val templateClassNames: Iterable<String>

    val resolver: ScriptDependenciesResolver? get() = null

    val filePattern: String? get() = null

    val dependenciesClasspath: Iterable<String>

    val environment: Map<String, Any?>?

    // for caching already loaded definitions, when needed
    val scriptDefinitions: List<KotlinScriptDefinition>? get() = null

    companion object {
        val EP_NAME: ExtensionPointName<ScriptTemplatesProvider> =
                ExtensionPointName.create<ScriptTemplatesProvider>("org.jetbrains.kotlin.scriptTemplatesProvider")
    }
}

fun makeScriptDefsFromTemplatesProviderExtensions(project: Project,
                                                  errorsHandler: ((ScriptTemplatesProvider, Exception) -> Unit) = { _, ex -> throw ex }
): List<KotlinScriptDefinition> =
        makeScriptDefsFromTemplatesProviders(Extensions.getArea(project).getExtensionPoint(ScriptTemplatesProvider.EP_NAME).extensions.asIterable(),
                                             errorsHandler)

fun makeScriptDefsFromTemplatesProviders(providers: Iterable<ScriptTemplatesProvider>,
                                         errorsHandler: ((ScriptTemplatesProvider, Exception) -> Unit) = { _, ex -> throw ex }
): List<KotlinScriptDefinition> = providers.flatMap { provider ->
    try {
        LOG.info("[kts] loading script definitions ${provider.templateClassNames} using cp: ${provider.dependenciesClasspath.joinToString(File.pathSeparator)}")
        provider.scriptDefinitions ?: run {
            val loader = URLClassLoader(provider.dependenciesClasspath.map { File(it).toURI().toURL() }.toTypedArray(), ScriptTemplatesProvider::class.java.classLoader)
            provider.templateClassNames.map {
                KotlinScriptDefinitionFromAnnotatedTemplate(loader.loadClass(it).kotlin, provider.resolver, provider.filePattern, provider.environment)
            }
        }
    }
    catch (ex: Exception) {
        LOG.info("Templates provider ${provider.id} is invalid: ${ex.message}")
        errorsHandler(provider, ex)
        emptyList<KotlinScriptDefinition>()
    }
}

private val LOG = Logger.getInstance("ScriptTemplatesProviders")
