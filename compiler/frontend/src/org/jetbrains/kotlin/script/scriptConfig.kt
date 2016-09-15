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
import org.jdom.Document
import org.jdom.Element
import org.jdom.output.Format
import org.jdom.output.XMLOutputter
import java.io.File
import java.io.InputStream
import java.io.StringWriter
import java.util.*

val SCRIPT_CONFIG_FILE_EXTENSION = ".ktscfg.xml"

fun isScriptDefinitionConfigFile(file: java.io.File) = file.isFile && file.name.endsWith(org.jetbrains.kotlin.script.SCRIPT_CONFIG_FILE_EXTENSION)

fun isScriptDefinitionConfigFile(file: com.intellij.openapi.vfs.VirtualFile) = !file.isDirectory && file.name.endsWith(org.jetbrains.kotlin.script.SCRIPT_CONFIG_FILE_EXTENSION)

fun loadScriptConfigsFromProjectRoot(projectRoot: java.io.File): List<org.jetbrains.kotlin.script.KotlinScriptConfig> =
        projectRoot.listFiles { it -> isScriptDefinitionConfigFile(it) }.toList()
            .flatMap { loadScriptConfigs(it) }

fun loadScriptConfigs(configFile: File): List<KotlinScriptConfig> =
        JDOMUtil.loadDocument(configFile).rootElement.children.mapNotNull {
            XmlSerializer.deserialize(it, KotlinScriptConfig::class.java)
        }

@Suppress("unused") // Used externally
fun loadScriptConfigs(configStream: InputStream): List<KotlinScriptConfig> =
        JDOMUtil.loadDocument(configStream).rootElement.children.mapNotNull {
            XmlSerializer.deserialize(it, KotlinScriptConfig::class.java)
        }

@Suppress("unused")
fun generateSampleScriptConfig(): String {

    val doc = Document(Element("KotlinScriptDefinitions"))
    val element = XmlSerializer.serialize(
            KotlinScriptConfig(name = "abc", fileNameMatch = ".*\\.kts", classpath = arrayListOf("aaa", "bbb"),
                               parameters = arrayListOf(KotlinScriptParameterConfig("p1", "t1"))))
    doc.rootElement.addContent(element)

    val sw = StringWriter()
    with (XMLOutputter()) {
        format = Format.getPrettyFormat()
        output(doc, sw)
    }
    return sw.toString()
}

@Tag("scriptParam")
data class KotlinScriptParameterConfig(
        @Tag("name") var name: String = "",
        @Tag("type") var type: String = ""
)

@Tag("script")
data class KotlinScriptConfig(
        @Tag("name")
        var name: String = "KotlinScript",

        @Tag("files")
        var fileNameMatch: String = ".*\\.kts",

        @Tag("template")
        var template: String = "kotlin.script.StandardScriptTemplate",

        @Tag("classpath")
        @AbstractCollection(surroundWithTag = false, elementTag = "path", elementValueAttribute = "")
        var classpath: MutableList<String> = ArrayList(),

        @Tag("parameters")
        @AbstractCollection(surroundWithTag = false, elementValueAttribute = "")
        var parameters: MutableList<KotlinScriptParameterConfig> = ArrayList(),

        @Tag("supertypes")
        @AbstractCollection(surroundWithTag = false, elementTag = "type", elementValueAttribute = "")
        var supertypes: MutableList<String> = ArrayList(),

        @Tag("superclassParameters")
        @AbstractCollection(surroundWithTag = false, elementTag = "name", elementValueAttribute = "")
        var superclassParamsMapping: MutableList<String> = ArrayList()
)

