/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

interface ScriptTemplatesProvider {

    // for resolving ambiguities
    val id: String
    val version: Int

    val isValid: Boolean

    val templateClassNames: Iterable<String>

    val resolver: ScriptDependenciesResolver? get() = null

    val filePattern: String? get() = null

    val dependenciesClasspath: Iterable<String>

    val environment: Map<String, Any?>?

    companion object {
        val EP_NAME = ExtensionPointName.create<ScriptTemplatesProvider>("org.jetbrains.kotlin.scriptTemplatesProvider")
    }
}

fun makeScriptDefsFromTemplatesProviderExtensions(project: Project,
                                                  errorsHandler: ((ScriptTemplatesProvider, Exception) -> Unit) = { ep, ex -> throw ex }
): List<KotlinScriptDefinitionFromAnnotatedTemplate> =
        makeScriptDefsFromTemplatesProviders(Extensions.getArea(project).getExtensionPoint(ScriptTemplatesProvider.EP_NAME).extensions.asIterable(),
                                             errorsHandler)

fun makeScriptDefsFromTemplatesProviders(providers: Iterable<ScriptTemplatesProvider>,
                                         errorsHandler: ((ScriptTemplatesProvider, Exception) -> Unit) = { ep, ex -> throw ex }
): List<KotlinScriptDefinitionFromAnnotatedTemplate> {
    val idToVersion = hashMapOf<String, Int>()
    return providers.filter { it.isValid }.sortedByDescending { it.version }.flatMap { provider ->
        try {
            idToVersion.get(provider.id)?.let { ver -> errorsHandler(provider, RuntimeException("Conflicting scriptTemplatesProvider ${provider.id}, using one with version $ver")) }
            Logger.getInstance("makeScriptDefsFromTemplatesProviders")
                    .info("[kts] loading script definitions ${provider.templateClassNames} using cp: ${provider.dependenciesClasspath.joinToString(File.pathSeparator)}")
            val loader = URLClassLoader(provider.dependenciesClasspath.map { File(it).toURI().toURL() }.toTypedArray(), ScriptTemplatesProvider::class.java.classLoader)
            provider.templateClassNames.map {
                val cl = loader.loadClass(it)
                idToVersion.put(provider.id, provider.version)
                KotlinScriptDefinitionFromAnnotatedTemplate(cl.kotlin, provider.resolver, provider.filePattern, provider.environment)
            }
        }
        catch (ex: Exception) {
            errorsHandler(provider, ex)
            emptyList<KotlinScriptDefinitionFromAnnotatedTemplate>()
        }
    }
}
