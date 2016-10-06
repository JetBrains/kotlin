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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.memberFunctions
import kotlin.reflect.primaryConstructor

open class KotlinScriptDefinitionFromAnnotatedTemplate(
        template: KClass<out Any>,
        providedResolver: ScriptDependenciesResolver? = null,
        providedScriptFilePattern: String? = null,
        val environment: Map<String, Any?>? = null
) : KotlinScriptDefinition(template) {

    val scriptFilePattern by lazy {
        providedScriptFilePattern ?: template.annotations.firstIsInstanceOrNull<ScriptTemplateDefinition>()?.scriptFilePattern ?: DEFAULT_SCRIPT_FILE_PATTERN
    }

    val resolver: ScriptDependenciesResolver? by lazy {
        val defAnn by lazy { template.annotations.firstIsInstanceOrNull<ScriptTemplateDefinition>() }
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
            else -> BasicScriptDependenciesResolver()
        }
    }

    private val acceptedAnnotations: List<KClass<out Annotation>> by lazy {
        val resolveMethod = ScriptDependenciesResolver::resolve
        val resolverMethodAnnotations =
                resolver::class.memberFunctions.find {
                    it.name == resolveMethod.name &&
                    sameSignature(it, resolveMethod)
                }
                ?.annotations
                ?.filterIsInstance<AcceptedAnnotations>()
        resolverMethodAnnotations?.flatMap {
            val v = it.supportedAnnotationClasses
            v.toList() // TODO: inline after KT-9453 is resolved (now it fails with "java.lang.Class cannot be cast to kotlin.reflect.KClass")
        }
        ?: emptyList()
    }

    override val name = template.simpleName!!

    override fun <TF> isScript(file: TF): Boolean =
            scriptFilePattern.let { Regex(it).matches(getFileName(file)) }

    // TODO: implement other strategy - e.g. try to extract something from match with ScriptFilePattern
    override fun getScriptName(script: KtScript): Name = ScriptNameUtil.fileNameWithExtensionStripped(script, KotlinParserDefinition.STD_SCRIPT_EXT)

    override fun <TF> getDependenciesFor(file: TF, project: Project, previousDependencies: KotlinScriptExternalDependencies?): KotlinScriptExternalDependencies? {

        val script = BasicScriptContents(file, getAnnotations = {
            val classLoader = (template as Any).javaClass.classLoader
            getAnnotationEntries(file, project)
                    .mapNotNull { psiAnn ->
                        // TODO: consider advanced matching using semantic similar to actual resolving
                        acceptedAnnotations.find { ann ->
                            psiAnn.typeName.let { it == ann.simpleName || it == ann.qualifiedName }
                        }?.let { KtAnnotationWrapper(psiAnn, classLoader.loadClass(it.qualifiedName).kotlin as KClass<out Annotation>) }
                    }
                    .map { it.getProxy(classLoader) }
        })

        fun logClassloadingError(ex: Throwable) {
            logScriptDefMessage(ScriptDependenciesResolver.ReportSeverity.WARNING, ex.message ?: "Invalid script template: ${template.qualifiedName}", null)
        }

        try {
            val fileDeps = resolver?.resolve(script, environment, ::logScriptDefMessage, previousDependencies)
            // TODO: use it as a Future
            return fileDeps?.get()
        }
        catch (ex: ClassNotFoundException) {
            logClassloadingError(ex)
        }
        catch (ex: NoClassDefFoundError) {
            logClassloadingError(ex)
        }
        catch (ex: ClassCastException) {
            logClassloadingError(ex)
        }
        return null
    }

    private fun <TF> getAnnotationEntries(file: TF, project: Project): Iterable<KtAnnotationEntry> = when (file) {
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
            if (file is KtFile) file.annotationEntries
            else throw IllegalArgumentException("Unable to extract kotlin annotations from ${file.name} (${file.fileType})")

    private fun getAnnotationEntriesFromVirtualFile(file: VirtualFile, project: Project): Iterable<KtAnnotationEntry> {
        val psiFile: PsiFile = PsiManager.getInstance(project).findFile(file)
                               ?: throw IllegalArgumentException("Unable to load PSI from ${file.canonicalPath}")
        return getAnnotationEntriesFromPsiFile(psiFile)
    }

    class BasicScriptContents<out TF>(myFile: TF, getAnnotations: () -> Iterable<Annotation>) : ScriptContents {
        override val file: File? by lazy { getFile(myFile) }
        override val annotations: Iterable<Annotation> by lazy { getAnnotations() }
        override val text: CharSequence? by lazy { getFileContents(myFile) }
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
