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
import kotlin.script.dependencies.DependenciesResolver

interface ScriptTemplatesProvider {

    // for resolving ambiguities
    val id: String

    @Deprecated("Parameter isn't used for resolving priorities anymore. " +
                "com.intellij.openapi.extensions.LoadingOrder constants can be used to order providers when registered from Intellij plugin.",
                ReplaceWith("0"))
    val version: Int get() = 0

    val isValid: Boolean

    val templateClassNames: Iterable<String>

    val resolver: DependenciesResolver? get() = null

    val filePattern: String? get() = null

    val dependenciesClasspath: List<File>

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

fun makeScriptDefsFromTemplatesProviderExtensions(project: Project,
                                                  errorsHandler: ((ScriptTemplatesProvider, Throwable) -> Unit) = { _, ex -> throw ex }
): List<KotlinScriptDefinition> =
        makeScriptDefsFromTemplatesProviders(Extensions.getArea(project).getExtensionPoint(ScriptTemplatesProvider.EP_NAME).extensions.asIterable(),
                                             errorsHandler)

fun makeScriptDefsFromTemplatesProviders(providers: Iterable<ScriptTemplatesProvider>,
                                         errorsHandler: ((ScriptTemplatesProvider, Throwable) -> Unit) = { _, ex -> throw ex }
): List<KotlinScriptDefinition> = providers.flatMap { provider ->
    try {
        val loader = createClassLoader(provider)
        provider.templateClassNames.map {
            KotlinScriptDefinitionFromAnnotatedTemplate(loader.loadClass(it).kotlin, provider.resolver, provider.filePattern, provider.environment)
        }
    }
    catch (ex: Throwable) {
        LOG.info("Templates provider ${provider.id} is invalid: ${ex.message}")
        errorsHandler(provider, ex)
        emptyList<KotlinScriptDefinition>()
    }
}

private fun createClassLoader(provider: ScriptTemplatesProvider): ClassLoader {
    val classpath = provider.dependenciesClasspath + provider.additionalResolverClasspath
    LOG.info("[kts] loading script definitions ${provider.templateClassNames} using cp: ${classpath.joinToString(File.pathSeparator)}")
    val baseLoader = ScriptTemplatesProvider::class.java.classLoader
    return if (classpath.isEmpty()) baseLoader else URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), baseLoader)
}

private val LOG = Logger.getInstance("ScriptTemplatesProviders")
