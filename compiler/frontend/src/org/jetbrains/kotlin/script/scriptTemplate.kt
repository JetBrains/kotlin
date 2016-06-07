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

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.types.KotlinType
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScriptFilePattern(val pattern: String)

interface ScriptDependencies {
    val classpath: List<String>
    val implicitImports: List<String>
}

interface GetScriptDependencies {
    operator fun invoke(annotations: Iterable<Annotation>, context: Any?): ScriptDependencies?
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScriptDependencyExtractor(val extractor: KClass<out GetScriptDependencies>)

data class KotlinScriptDefinitionFromTemplate(val template: KClass<out Any>, val context: Any?) : KotlinScriptDefinition {
    override val name = template.simpleName!!

    override fun getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter> =
            template.constructors.first().parameters.map { ScriptParameter(Name.identifier(it.name!!), getKotlinTypeByFqName(scriptDescriptor, it.type.toString())) }

    override fun getScriptSupertypes(scriptDescriptor: ScriptDescriptor): List<KotlinType> =
            listOf(getKotlinTypeByFqName(scriptDescriptor, template.qualifiedName!!))

    override fun getScriptParametersToPassToSuperclass(scriptDescriptor: ScriptDescriptor): List<Name> =
            getScriptParameters(scriptDescriptor).map { it.name }

    override fun <TF> isScript(file: TF): Boolean =
            template.annotations.any { (it as? ScriptFilePattern)?.let { Regex(it.pattern).matches(getFileName(file)) } ?: false }

    // TODO: implement other strategy - e.g. try to extract something from match with ScriptFilePattern
    override fun getScriptName(script: KtScript): Name = ScriptNameUtil.fileNameWithExtensionStripped(script, KotlinParserDefinition.STD_SCRIPT_EXT)

    private val dependenciesExtractors by lazy {
        template.annotations.mapNotNull { it as? ScriptDependencyExtractor }.map { it.extractor.constructors.first().call() }
    }

    private val dependencies by lazy {
        dependenciesExtractors.mapNotNull { it(template.annotations, context) }
    }

    override fun getScriptDependenciesClasspath(): List<String> = dependencies.flatMap { it.classpath }
}

