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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import kotlin.reflect.KClass
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver.ResolveResult.Failure
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.ScriptReport

class ScriptContentLoader(private val project: Project) {
    fun getScriptContents(scriptDefinition: KotlinScriptDefinition, file: VirtualFile)
            = BasicScriptContents(file, getAnnotations = { loadAnnotations(scriptDefinition, file) })

    private fun loadAnnotations(scriptDefinition: KotlinScriptDefinition, file: VirtualFile): List<Annotation> {
        val classLoader = scriptDefinition.template.java.classLoader
        // TODO_R: report error on failure to load annotation class
        return ApplicationManager.getApplication().runReadAction<List<Annotation>> {
            getAnnotationEntries(file, project)
                .mapNotNull { psiAnn ->
                    // TODO: consider advanced matching using semantic similar to actual resolving
                    scriptDefinition.acceptedAnnotations.find { ann ->
                        psiAnn.typeName.let { it == ann.simpleName || it == ann.qualifiedName }
                    }?.let {
                        constructAnnotation(psiAnn, classLoader.loadClass(it.qualifiedName).kotlin as KClass<out Annotation>, project)
                    }
                }
        }
    }

    private fun getAnnotationEntries(file: VirtualFile, project: Project): Iterable<KtAnnotationEntry> {
        val psiFile: PsiFile = PsiManager.getInstance(project).findFile(file)
                               ?: throw IllegalArgumentException("Unable to load PSI from ${file.canonicalPath}")
        return (psiFile as? KtFile)?.annotationEntries
               ?: throw IllegalArgumentException("Unable to extract kotlin annotations from ${file.name} (${file.fileType})")
    }

    class BasicScriptContents(virtualFile: VirtualFile, getAnnotations: () -> Iterable<Annotation>) : ScriptContents {
        override val file: File = File(virtualFile.path)
        override val annotations: Iterable<Annotation> by lazy(LazyThreadSafetyMode.PUBLICATION) { getAnnotations() }
        override val text: CharSequence? by lazy(LazyThreadSafetyMode.PUBLICATION) { virtualFile.inputStream.reader(charset = virtualFile.charset).readText() }
    }

    fun loadContentsAndResolveDependencies(
            scriptDef: KotlinScriptDefinition,
            file: VirtualFile
    ): DependenciesResolver.ResolveResult {
        val scriptContents = getScriptContents(scriptDef, file)
        val environment = getEnvironment(scriptDef)
        val result = try {
            scriptDef.dependencyResolver.resolve(
                    scriptContents,
                    environment
            )
        }
        catch (e: Throwable) {
            e.asResolveFailure(scriptDef)
        }
        return result
    }

    fun getEnvironment(scriptDef: KotlinScriptDefinition) =
            (scriptDef as? KotlinScriptDefinitionFromAnnotatedTemplate)?.environment.orEmpty()
}

fun ScriptDependencies.adjustByDefinition(
        scriptDef: KotlinScriptDefinition
): ScriptDependencies {
    val additionalClasspath = (scriptDef as? KotlinScriptDefinitionFromAnnotatedTemplate)?.templateClasspath ?: return this
    if (additionalClasspath.isEmpty()) return this
    return copy(classpath = additionalClasspath + classpath)
}

fun Throwable.asResolveFailure(scriptDef: KotlinScriptDefinition): Failure {
    val prefix = "${scriptDef.dependencyResolver::class.simpleName} threw exception ${this::class.simpleName}:\n "
    return Failure(ScriptReport(prefix + (message ?: "<no message>"), ScriptReport.Severity.FATAL))
}