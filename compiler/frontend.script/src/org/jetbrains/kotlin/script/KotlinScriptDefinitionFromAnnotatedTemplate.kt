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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.script.dependencies.DependenciesResolver
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.templates.AcceptedAnnotations

open class KotlinScriptDefinitionFromAnnotatedTemplate(
        template: KClass<out Any>,
        providedResolver: DependenciesResolver? = null,
        providedScriptFilePattern: String? = null,
        val environment: Map<String, Any?>? = null
) : KotlinScriptDefinition(template) {

    val scriptFilePattern by lazy {
        providedScriptFilePattern
        ?: takeUnlessError { template.annotations.firstIsInstanceOrNull<kotlin.script.templates.ScriptTemplateDefinition>()?.scriptFilePattern }
        ?: takeUnlessError { template.annotations.firstIsInstanceOrNull<org.jetbrains.kotlin.script.ScriptTemplateDefinition>()?.scriptFilePattern }
        ?: DEFAULT_SCRIPT_FILE_PATTERN
    }

    override val dependencyResolver: DependenciesResolver by lazy {
        computeResolver(template, providedResolver)
    }

    private fun computeResolver(
            template: KClass<out Any>,
            providedResolver: DependenciesResolver?
    ): @Suppress("DEPRECATION") DependenciesResolver {
        val defAnn by lazy { takeUnlessError { template.annotations.firstIsInstanceOrNull<kotlin.script.templates.ScriptTemplateDefinition>() } }
        val legacyDefAnn by lazy { takeUnlessError { template.annotations.firstIsInstanceOrNull<ScriptTemplateDefinition>() } }
        return when {
                   providedResolver != null -> providedResolver
               // TODO: logScriptDefMessage missing or invalid constructor
                   defAnn != null ->
                       try {
                           defAnn.resolver.primaryConstructor?.call() as? DependenciesResolver ?: null.apply {
                               log.warn("[kts] No default constructor found for ${defAnn.resolver.qualifiedName}")
                           }
                       }
                       catch (ex: ClassCastException) {
                           log.warn("[kts] Script def error ${ex.message}")
                           null
                       }
                   legacyDefAnn != null ->
                       try {
                           log.warn("[kts] Deprecated annotations on the script template are used, please update the provider")
                           legacyDefAnn.resolver.primaryConstructor?.call()?.let {
                               LegacyScriptDependenciesResolverWrapper(it)
                           }
                           ?: null.apply {
                               log.warn("[kts] No default constructor found for ${legacyDefAnn.resolver.qualifiedName}")
                           }
                       }
                       catch (ex: ClassCastException) {
                           log.warn("[kts] Script def error ${ex.message}")
                           null
                       }
                   else -> null
               } ?: DependenciesResolver.NoDependencies
    }

    val samWithReceiverAnnotations: List<String>? by lazy {
        takeUnlessError { template.annotations.firstIsInstanceOrNull<kotlin.script.extensions.SamWithReceiverAnnotations>()?.annotations?.toList() }
        ?: takeUnlessError { template.annotations.firstIsInstanceOrNull<org.jetbrains.kotlin.script.SamWithReceiverAnnotations>()?.annotations?.toList() }
    }

    override val acceptedAnnotations: List<KClass<out Annotation>> by lazy {

        fun sameSignature(left: KFunction<*>, right: KFunction<*>): Boolean =
                left.parameters.size == right.parameters.size &&
                left.parameters.zip(right.parameters).all {
                    it.first.kind == KParameter.Kind.INSTANCE ||
                    it.first.type == it.second.type
                }

        val resolveMethod = ScriptDependenciesResolver::resolve
        val resolverMethodAnnotations =
                dependencyResolver::class.memberFunctions.find { function ->
                    function.name == resolveMethod.name &&
                    sameSignature(function, resolveMethod)
                }?.annotations?.filterIsInstance<AcceptedAnnotations>()
        resolverMethodAnnotations?.flatMap {
            it.supportedAnnotationClasses.toList()
        }
        ?: emptyList()
    }

    override val name = template.simpleName!!

    override fun isScript(fileName: String): Boolean =
            scriptFilePattern.let { Regex(it).matches(fileName) }

    // TODO: implement other strategy - e.g. try to extract something from match with ScriptFilePattern
    override fun getScriptName(script: KtScript): Name = NameUtils.getScriptNameForFile(script.containingKtFile.name)

    override fun toString(): String = "KotlinScriptDefinitionFromAnnotatedTemplate - ${template.simpleName}"

    override val annotationsForSamWithReceivers: List<String>
        get() = samWithReceiverAnnotations ?: super.annotationsForSamWithReceivers

    private inline fun<T> takeUnlessError(reportError: Boolean = true, body: () -> T?): T? =
            try {
                body()
            }
            catch (ex: Throwable) {
                if (reportError) {
                    log.error("Invalid script template: " + template.qualifiedName, ex)
                }
                else {
                    log.warn("Invalid script template: " + template.qualifiedName, ex)
                }
                null
            }

    companion object {
        internal val log = Logger.getInstance(KotlinScriptDefinitionFromAnnotatedTemplate::class.java)
    }
}