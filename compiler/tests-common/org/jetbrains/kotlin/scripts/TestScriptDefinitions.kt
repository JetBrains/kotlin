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

package org.jetbrains.kotlin.scripts

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.script.*
import java.io.File
import java.net.URL
import java.net.URLClassLoader

abstract class BaseScriptDefinition (val extension: String, val cp: List<File>? = null) : KotlinScriptDefinition {
    override val name = "Test Kotlin Script"
    override fun <TF> isScript(file: TF): Boolean = getFileName(file).endsWith(extension)
    override fun getScriptName(script: KtScript): Name = ScriptNameUtil.fileNameWithExtensionStripped(script, extension)
    override fun <TF> getDependenciesFor(file: TF, project: Project, previousDependencies: KotlinScriptExternalDependencies?): KotlinScriptExternalDependencies? =
            object : KotlinScriptExternalDependencies {
                override val classpath: Iterable<File> = cp ?: (classpathFromProperty() + classpathFromClassloader(BaseScriptDefinition::class.java.classLoader)).distinct()
            }
}

open class SimpleParamsWithClasspathTestScriptDefinition(extension: String, val parameters: List<ScriptParameter>, classpath: List<File>? = null)
    : BaseScriptDefinition(extension, classpath)
{
    override fun getScriptParameters(scriptDescriptor: ScriptDescriptor) = parameters
}

open class SimpleParamsTestScriptDefinition(extension: String, parameters: List<ScriptParameter>) : SimpleParamsWithClasspathTestScriptDefinition(extension, parameters)

fun classpathFromProperty(): List<File> =
    System.getProperty("java.class.path")?.let {
        it.split(String.format("\\%s", File.pathSeparatorChar).toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                .map { File(it) }
    } ?: emptyList()

fun URL.toFile() =
    try {
        File(toURI().schemeSpecificPart)
    }
    catch (e: java.net.URISyntaxException) {
        if (protocol != "file") null
        else File(file)
    }

fun classpathFromClassloader(classLoader: ClassLoader): List<File> =
    (classLoader as? URLClassLoader)?.urLs
            ?.mapNotNull { it.toFile() }
            ?: emptyList()

