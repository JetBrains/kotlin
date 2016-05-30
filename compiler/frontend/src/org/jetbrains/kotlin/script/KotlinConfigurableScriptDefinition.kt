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

import com.intellij.openapi.project.Project
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

    private val evaluatedClasspath by lazy { config.classpath.evalWithVars(environmentVars).distinct() }

    override fun getScriptDependenciesClasspath(): List<String> = evaluatedClasspath
}


// return all combination of replacements of vars in the strings
// if corresponding list of replacements is empty, all strings containing the reference to the var are removed
// TODO: fix and tests
// TODO: move to some utils
internal fun List<String>.evalWithVars(varsMap: Map<String, List<String>>?): List<String> =
        if (varsMap == null || varsMap.isEmpty()) this
        else this.flatMap { cpentry ->
            varsMap.entries.fold(listOf(cpentry)) { p, v ->
                if (cpentry.contains("\${${v.key}}")) {
                    if (v.value.isEmpty()) emptyList()
                    else v.value.flatMap { valListElement ->
                        p.map { it.replace("\${${v.key}}", valListElement) }
                    }
                }
                else p
            }
        }

fun generateKotlinScriptClasspathEnvVarsForCompiler(project: Project): Map<String, List<String>> {
    val paths = PathUtil.getKotlinPathsForCompiler()
    return mapOf("kotlin-runtime" to listOf(paths.runtimePath.canonicalPath),
                 "project-root" to listOf(project.basePath ?: "."),
                 "jdk" to PathUtil.getJdkClassesRoots().map { it.canonicalPath })
}

fun generateKotlinScriptClasspathEnvVarsForIdea(project: Project): Map<String, List<String>> {
    val paths = PathUtil.getKotlinPathsForIdeaPlugin()
    return mapOf("kotlin-runtime" to listOf(paths.runtimePath.canonicalPath),
                 "project-root" to listOf(project.basePath ?: "."),
                 "jdk" to PathUtil.getJdkClassesRoots().map { it.canonicalPath })
}
