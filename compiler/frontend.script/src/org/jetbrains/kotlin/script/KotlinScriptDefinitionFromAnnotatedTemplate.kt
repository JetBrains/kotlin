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
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.script.dependencies.BasicScriptDependenciesResolver
import kotlin.script.dependencies.KotlinScriptExternalDependencies
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.templates.AcceptedAnnotations

open class KotlinScriptDefinitionFromAnnotatedTemplate(
        template: KClass<out Any>,
        providedResolver: ScriptDependenciesResolver? = null,
        providedScriptFilePattern: String? = null,
        val environment: Map<String, Any?>? = null
) : KotlinScriptDefinition(template) {

    val scriptFilePattern by lazy {
        providedScriptFilePattern
        ?: takeUnlessError { template.annotations.firstIsInstanceOrNull<kotlin.script.templates.ScriptTemplateDefinition>()?.scriptFilePattern }
        ?: takeUnlessError { template.annotations.firstIsInstanceOrNull<org.jetbrains.kotlin.script.ScriptTemplateDefinition>()?.scriptFilePattern }
        ?: DEFAULT_SCRIPT_FILE_PATTERN
    }

    val resolver: ScriptDependenciesResolver? by lazy {
        val defAnn by lazy { takeUnlessError { template.annotations.firstIsInstanceOrNull<kotlin.script.templates.ScriptTemplateDefinition>() } }
        val legacyDefAnn by lazy { takeUnlessError { template.annotations.firstIsInstanceOrNull<org.jetbrains.kotlin.script.ScriptTemplateDefinition>() } }
        when {
            providedResolver != null -> providedResolver
            // TODO: logScriptDefMessage missing or invalid constructor
            defAnn != null ->
                try {
                    defAnn.resolver.primaryConstructor?.call() ?: null.apply {
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
            else -> BasicScriptDependenciesResolver()
        }
    }

    val samWithReceiverAnnotations: List<String>? by lazy {
        takeUnlessError { template.annotations.firstIsInstanceOrNull<kotlin.script.extensions.SamWithReceiverAnnotations>()?.annotations?.toList() }
        ?: takeUnlessError { template.annotations.firstIsInstanceOrNull<org.jetbrains.kotlin.script.SamWithReceiverAnnotations>()?.annotations?.toList() }
    }

    private val acceptedAnnotations: List<KClass<out Annotation>> by lazy {

        fun sameSignature(left: KFunction<*>, right: KFunction<*>): Boolean =
                left.parameters.size == right.parameters.size &&
                left.parameters.zip(right.parameters).all {
                    it.first.kind == KParameter.Kind.INSTANCE ||
                    it.first.type == it.second.type
                }

        val resolveMethod = ScriptDependenciesResolver::resolve
        val resolverMethodAnnotations =
                resolver?.let { it::class }?.memberFunctions?.find { function ->
                    function.name == resolveMethod.name &&
                    sameSignature(function, resolveMethod)
                }?.annotations?.filterIsInstance<AcceptedAnnotations>()
        resolverMethodAnnotations?.flatMap {
            it.supportedAnnotationClasses.toList()
        }
        ?: emptyList()
    }

    override val name = template.simpleName!!

    override fun <TF: Any> isScript(file: TF): Boolean =
            scriptFilePattern.let { Regex(it).matches(getFileName(file)) }

    // TODO: implement other strategy - e.g. try to extract something from match with ScriptFilePattern
    override fun getScriptName(script: KtScript): Name = NameUtils.getScriptNameForFile(script.containingKtFile.name)

    override fun <TF: Any> getDependenciesFor(file: TF, project: Project, previousDependencies: KotlinScriptExternalDependencies?): KotlinScriptExternalDependencies? {
        return takeUnlessError(reportError = false) {
            val fileDeps = resolver?.resolve(makeScriptContents(file, project), environment, ::logScriptDefMessage, previousDependencies)
            // TODO: use it as a Future
            fileDeps?.get()
        }
    }

    fun <TF: Any>  makeScriptContents(file: TF, project: Project) = BasicScriptContents(file, getAnnotations = {
        val classLoader = template.java.classLoader
        takeUnlessError(reportError = false) {
            getAnnotationEntries(file, project)
                    .mapNotNull { psiAnn ->
                        // TODO: consider advanced matching using semantic similar to actual resolving
                        acceptedAnnotations.find { ann ->
                            psiAnn.typeName.let { it == ann.simpleName || it == ann.qualifiedName }
                        }?.let { constructAnnotation(psiAnn, classLoader.loadClass(it.qualifiedName).kotlin as KClass<out Annotation>) }
                    }
        }
        ?: emptyList()
    })


    fun getDependenciesFor(previousDependencies: KotlinScriptExternalDependencies?, scriptContents: ScriptContents): KotlinScriptExternalDependencies? {
        return takeUnlessError(reportError = false) {
            // TODO: use it as a Future
            resolver?.resolve(scriptContents, environment, ::logScriptDefMessage, previousDependencies)?.get()
        }
    }

    private fun <TF: Any> getAnnotationEntries(file: TF, project: Project): Iterable<KtAnnotationEntry> = when (file) {
        is PsiFile -> getAnnotationEntriesFromPsiFile(file)
        is VirtualFile -> getAnnotationEntriesFromVirtualFile(file, project)
        is File -> {
            val virtualFile = (StandardFileSystems.local().findFileByPath(file.canonicalPath)
                               ?: throw IllegalArgumentException("Unable to find file ${file.canonicalPath}"))
            getAnnotationEntriesFromVirtualFile(virtualFile, project)
        }
        else -> throw IllegalArgumentException("Unsupported file type $file")
    }

    private fun getAnnotationEntriesFromPsiFile(file: PsiFile) =
            (file as? KtFile)?.annotationEntries
            ?: throw IllegalArgumentException("Unable to extract kotlin annotations from ${file.name} (${file.fileType})")

    private fun getAnnotationEntriesFromVirtualFile(file: VirtualFile, project: Project): Iterable<KtAnnotationEntry> {
        val psiFile: PsiFile = PsiManager.getInstance(project).findFile(file)
                               ?: throw IllegalArgumentException("Unable to load PSI from ${file.canonicalPath}")
        return getAnnotationEntriesFromPsiFile(psiFile)
    }

    class BasicScriptContents<out TF: Any>(myFile: TF, getAnnotations: () -> Iterable<Annotation>) : ScriptContents {
        override val file: File? by lazy { getFile(myFile) }
        override val annotations: Iterable<Annotation> by lazy { getAnnotations() }
        override val text: CharSequence? by lazy { getFileContents(myFile) }
    }

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

internal fun logScriptDefMessage(reportSeverity: ScriptDependenciesResolver.ReportSeverity, s: String, position: ScriptContents.Position?): Unit {
    val msg = (position?.run { "[at $line:$col]" } ?: "") + s
    when (reportSeverity) {
        ScriptDependenciesResolver.ReportSeverity.ERROR -> KotlinScriptDefinitionFromAnnotatedTemplate.log.error(msg)
        ScriptDependenciesResolver.ReportSeverity.WARNING -> KotlinScriptDefinitionFromAnnotatedTemplate.log.warn(msg)
        ScriptDependenciesResolver.ReportSeverity.INFO -> KotlinScriptDefinitionFromAnnotatedTemplate.log.info(msg)
        ScriptDependenciesResolver.ReportSeverity.DEBUG -> KotlinScriptDefinitionFromAnnotatedTemplate.log.debug(msg)
    }
}

internal fun sameSignature(left: KFunction<*>, right: KFunction<*>): Boolean =
        left.parameters.size == right.parameters.size &&
        left.parameters.zip(right.parameters).all {
            it.first.kind == KParameter.Kind.INSTANCE ||
            it.first.type == it.second.type
        }
