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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.psi.PsiFile
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.AbstractCollection
import com.intellij.util.xmlb.annotations.Tag
import org.jdom.Document
import org.jdom.Element
import org.jdom.output.Format
import org.jdom.output.XMLOutputter
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.types.KotlinType
import java.io.File
import java.io.FileWriter
import java.util.*

class KotlinScriptConfigurationManager(project: Project, scriptDefinitionProvider: KotlinScriptDefinitionProvider) : AbstractProjectComponent(project) {
    private val LOG by lazy { Logger.getInstance(KotlinScriptConfigurationManager::class.java) }

    init {
        val scriptDefs = arrayListOf<KotlinScriptDefinition>()
        File(project.basePath ?: ".").listFiles().forEach {
            if (it.name.endsWith(".ktscfg.xml")) {
                val doc = JDOMUtil.loadDocument(it)
                doc.rootElement.children.forEach {
                    val def = XmlSerializer.deserialize(it, KotlinScriptConfig::class.java)
                    if (def != null)
                        scriptDefs.add(KotlinConfigurableScriptDefinition(def))
                }
            }
        }
        if (scriptDefs.isNotEmpty()) {
            scriptDefs.add(StandardScriptDefinition)
            scriptDefinitionProvider.setScriptDefinitions(scriptDefs)
        }
    }
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

class KotlinConfigurableScriptDefinition(val config: KotlinScriptConfig) : KotlinScriptDefinition {
    override val name = config.name
    override fun getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter> =
        config.parameters.map { ScriptParameter(Name.identifier(it.name), getKotlinTypeByFqName(scriptDescriptor, it.type)) }

    override fun getScriptSupertypes(scriptDescriptor: ScriptDescriptor): List<KotlinType> =
        config.supertypes.map { getKotlinTypeByFqName(scriptDescriptor, it) }

    override fun getScriptParametersToPassToSuperclass(scriptDescriptor: ScriptDescriptor): List<Name> =
        config.superclassParamsMapping.map { Name.identifier(it) }

    override fun isScript(file: PsiFile): Boolean =
        Regex(config.fileNameMatch).matches(file.name)

    override fun getScriptName(script: KtScript): Name = Name.identifier(script.name ?: config.name)

    override fun getScriptDependenciesClasspath(): List<String> = config.classpath
}
