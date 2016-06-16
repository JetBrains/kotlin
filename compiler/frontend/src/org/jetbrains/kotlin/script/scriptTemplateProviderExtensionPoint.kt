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

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import java.io.File
import java.net.URLClassLoader

interface ScriptTemplateProvider {

    // for resolving ambiguities
    val id: String
    val version: Int

    val isValid: Boolean

    val templateClassName: String

    val dependenciesClasspath: Iterable<String>

    val context: Any?

    companion object {
        val EP_NAME = ExtensionPointName.create<ScriptTemplateProvider>("org.jetbrains.kotlin.scriptTemplateProvider")
    }
}

fun makeScriptDefsFromTemplateProviderExtensions(project: Project,
                                                 errorsHandler: ((ScriptTemplateProvider, Exception) -> Unit) = fun (ep, ex) = println(ex)): List<KotlinScriptDefinitionFromTemplate> =
        makeScriptDefsFromTemplateProviders(Extensions.getArea(project).getExtensionPoint(ScriptTemplateProvider.EP_NAME).extensions.asIterable(),
                                            errorsHandler)

fun makeScriptDefsFromTemplateProviders(providers: Iterable<ScriptTemplateProvider>,
                                        errorsHandler: ((ScriptTemplateProvider, Exception) -> Unit) = fun (ep, ex) = println(ex)
): List<KotlinScriptDefinitionFromTemplate> {
    val idToVersion = hashMapOf<String, Int>()
    return providers.filter { it.isValid }.sortedByDescending { it.version }.mapNotNull {
        try {
            idToVersion.get(it.id)?.let { ver -> errorsHandler(it, RuntimeException("Conflicting scriptTemplateProvider ${it.id}, using one with version $ver")) }
            val loader = URLClassLoader(it.dependenciesClasspath.map { File(it).toURI().toURL() }.toTypedArray(), ScriptTemplateProvider::class.java.classLoader)
            val cl = loader.loadClass(it.templateClassName)
            idToVersion.put(it.id, it.version)
            KotlinScriptDefinitionFromTemplate(cl.kotlin, it.context)
        }
        catch (ex: Exception) {
            errorsHandler(it, ex)
            null
        }
    }
}
