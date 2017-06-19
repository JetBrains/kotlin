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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import kotlin.reflect.KClass
import kotlin.script.dependencies.KotlinScriptExternalDependencies
import kotlin.script.dependencies.ScriptContents

abstract class KotlinScriptExternalImportsProviderBase(private val project: Project): KotlinScriptExternalImportsProvider {
    fun getScriptContents(scriptDefinition: KotlinScriptDefinition, file: VirtualFile)
            = BasicScriptContents(file, getAnnotations = { loadAnnotations(scriptDefinition, file) })

    private fun loadAnnotations(scriptDefinition: KotlinScriptDefinition, file: VirtualFile): List<Annotation> {
        val classLoader = scriptDefinition.template.java.classLoader
        // TODO_R: report error on failure to load annotation class
        return getAnnotationEntries(file, project)
                .mapNotNull { psiAnn ->
                    // TODO: consider advanced matching using semantic similar to actual resolving
                    scriptDefinition.acceptedAnnotations.find { ann ->
                        psiAnn.typeName.let { it == ann.simpleName || it == ann.qualifiedName }
                    }?.let { constructAnnotation(psiAnn, classLoader.loadClass(it.qualifiedName).kotlin as KClass<out Annotation>) }
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
        override val annotations: Iterable<Annotation> by lazy { getAnnotations() }
        override val text: CharSequence? by lazy { virtualFile.inputStream.reader(charset = virtualFile.charset).readText() }
    }

    fun resolveDependencies(
            scriptDef: KotlinScriptDefinition,
            file: VirtualFile,
            oldDependencies: KotlinScriptExternalDependencies? = null
    ): KotlinScriptExternalDependencies? {
        val scriptContents = getScriptContents(scriptDef, file)
        return scriptDef.dependencyResolver.resolve(
                scriptContents,
                (scriptDef as? KotlinScriptDefinitionFromAnnotatedTemplate)?.environment,
                { _, _, _ -> Unit }, // TODO_R:
                oldDependencies
        ).get()
    }
}

