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

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.AbstractCollection
import com.intellij.util.xmlb.annotations.Tag
import org.jdom.Document
import org.jdom.Element
import org.jdom.output.Format
import org.jdom.output.XMLOutputter
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.InputStream
import java.io.StringWriter
import java.util.*


val SCRIPT_CONFIG_FILE_EXTENSION = ".ktscfg.xml"

class KotlinScriptConfigurationManager(project: Project, scriptDefinitionProvider: KotlinScriptDefinitionProvider) : AbstractProjectComponent(project) {

    val kotlinEnvVars: Map<String, List<String>> by lazy {
        val paths = PathUtil.getKotlinPathsForIdeaPlugin()
        mapOf("kotlin-runtime" to listOf(paths.runtimePath.canonicalPath),
              "project-root" to listOf(project.basePath ?: "."),
              "jdk" to PathUtil.getJdkClassesRoots().map { it.canonicalPath })
    }

    init {
        loadScriptDefinitionsFromDirectoryWithConfigs(File(project.basePath ?: "."), kotlinEnvVars).let {
            if (it.isNotEmpty()) {
                scriptDefinitionProvider.setScriptDefinitions(it + StandardScriptDefinition)
            }
        }
    }
}

fun loadScriptDefinitionsFromDirectoryWithConfigs(dir: File, kotlinEnvVars: Map<String, List<String>>? = null): List<KotlinConfigurableScriptDefinition> =
    dir.walk().filter { it.isFile && it.name.endsWith(SCRIPT_CONFIG_FILE_EXTENSION, ignoreCase = true) }.toList()
            .flatMap { loadScriptDefinitionsFromConfig(it, kotlinEnvVars) }

fun loadScriptDefinitionsFromConfig(configFile: File, kotlinEnvVars: Map<String, List<String>>? = null): List<KotlinConfigurableScriptDefinition> =
        JDOMUtil.loadDocument(configFile).rootElement.children.mapNotNull {
            XmlSerializer.deserialize(it, KotlinScriptConfig::class.java)?.let {
                KotlinConfigurableScriptDefinition(it, kotlinEnvVars)
            }
        }

@Suppress("unused") // Used externally
fun loadScriptDefinitionsFromConfig(configStream: InputStream, kotlinEnvVars: Map<String, List<String>>? = null): List<KotlinConfigurableScriptDefinition> =
        JDOMUtil.loadDocument(configStream).rootElement.children.mapNotNull {
            XmlSerializer.deserialize(it, KotlinScriptConfig::class.java)?.let {
                KotlinConfigurableScriptDefinition(it, kotlinEnvVars)
            }
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
class KotlinScriptParameterConfig(@Tag("name") var name: String = "",
                                  @Tag("type") var type: String = "")

@Tag("script")
class KotlinScriptConfig(
        @Tag("name")
        var name: String = "KotlinScript",

        @Tag("files")
        var fileNameMatch: String = ".*\\.kts",

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

class KotlinConfigurableScriptDefinition(val config: KotlinScriptConfig, val environmentVars: Map<String, List<String>>?) : KotlinScriptDefinition {
    override val name = config.name
    override fun getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter> =
        config.parameters.map { ScriptParameter(Name.identifier(it.name), getKotlinTypeByFqName(scriptDescriptor, it.type)) }

    override fun getScriptSupertypes(scriptDescriptor: ScriptDescriptor): List<KotlinType> =
        config.supertypes.map { getKotlinTypeByFqName(scriptDescriptor, it) }

    override fun getScriptParametersToPassToSuperclass(scriptDescriptor: ScriptDescriptor): List<Name> =
        config.superclassParamsMapping.map { Name.identifier(it) }

    override fun isScript(file: VirtualFile): Boolean =
            Regex(config.fileNameMatch).matches(file.name)

    override fun getScriptName(script: KtScript): Name = ScriptNameUtil.fileNameWithExtensionStripped(script, KotlinParserDefinition.STD_SCRIPT_EXT)

    // return all combination of replacements of env vars in the classpath entries
    // if corresponding list of replacements is empty, all classpath entries containing the reference to the var are removed
    // TODO: tests
    override fun getScriptDependenciesClasspath(): List<String> =
            if (environmentVars == null || environmentVars.isEmpty()) config.classpath
            else config.classpath.flatMap { cpentry ->
                    environmentVars.entries.fold(listOf(cpentry)) { p, v ->
                        if (v.value.isEmpty() && cpentry.contains("\${${v.key}}")) emptyList()
                        else v.value.flatMap { valListElement ->
                            p.map { it.replace("\${${v.key}}", valListElement) }
                        }
                    }
                }
}
