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
import com.intellij.psi.PsiFile

fun getScriptDefinition(file: VirtualFile, project: Project): KotlinScriptDefinition? =
    KotlinScriptDefinitionProvider.getInstance(project).findScriptDefinition(file)

fun getScriptDefinition(psiFile: PsiFile): KotlinScriptDefinition? =
    KotlinScriptDefinitionProvider.getInstance(psiFile.project).findScriptDefinition(psiFile.originalFile.virtualFile)

fun getScriptExtraImports(file: VirtualFile, project: Project): List<KotlinScriptExtraImport>  =
        KotlinScriptExtraImportsProvider.getInstance(project)?.getExtraImports(file) ?: emptyList()

fun getScriptExtraImports(psiFile: PsiFile): List<KotlinScriptExtraImport>  =
        psiFile.virtualFile?.let { file ->
            KotlinScriptExtraImportsProvider.getInstance(psiFile.project)?.getExtraImports(file)
        } ?: emptyList()

fun getScriptCombinedClasspath(file: VirtualFile, project: Project): List<String> =
        getScriptDefinition(file, project)?.run {
            getScriptDependenciesClasspath() +
            getScriptExtraImports(file, project).flatMap { it.classpath }
        } ?: emptyList()

fun getScriptCombinedClasspath(psiFile: PsiFile): List<String> =
        getScriptDefinition(psiFile)?.run {
            getScriptDependenciesClasspath() +
            getScriptExtraImports(psiFile).flatMap { it.classpath }
        } ?: emptyList()
