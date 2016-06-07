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

import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.AbstractCollection
import com.intellij.util.xmlb.annotations.Tag
import java.io.File
import java.net.URLClassLoader
import java.util.*

val SCRIPT_CONFIG_DEF_FILE_EXTENSION = ".ktsdef.xml"

fun isScriptDefConfigFile(file: File) = file.isFile && file.name.endsWith(SCRIPT_CONFIG_DEF_FILE_EXTENSION)

fun isScriptDefConfigFile(file: com.intellij.openapi.vfs.VirtualFile) = !file.isDirectory && file.name.endsWith(SCRIPT_CONFIG_DEF_FILE_EXTENSION)

fun loadScriptDefConfigsFromProjectRoot(projectRoot: java.io.File): List<KotlinScripDeftConfig> =
        projectRoot.walk().filter(::isScriptDefinitionConfigFile).toList()
                .flatMap { loadScriptDefConfigs(it) }

fun loadScriptDefConfigs(configFile: File): List<KotlinScripDeftConfig> =
        JDOMUtil.loadDocument(configFile).rootElement.children.mapNotNull {
            XmlSerializer.deserialize(it, KotlinScripDeftConfig::class.java)
        }

fun makeScriptDefsFromConfigs(configs: List<KotlinScripDeftConfig>): List<KotlinScriptDefinitionFromTemplate> =
        configs.map {
            val loader = URLClassLoader(it.classpath.map { File(it).toURI().toURL() }.toTypedArray())
            val cl = loader.loadClass(it.def)
            KotlinScriptDefinitionFromTemplate(cl.kotlin, null)
        }

@Tag("scriptDef")
data class KotlinScripDeftConfig(
        @Tag("def")
        var def: String = "",

        @Tag("classpath")
        @AbstractCollection(surroundWithTag = false, elementTag = "path", elementValueAttribute = "")
        var classpath: MutableList<String> = ArrayList()
)
