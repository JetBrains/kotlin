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

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile

interface ScriptDefinitionProvider {
    fun findScriptDefinition(fileName: String): KotlinScriptDefinition?
    fun isScript(fileName: String): Boolean
    fun getDefaultScriptDefinition(): KotlinScriptDefinition

    fun getKnownFilenameExtensions(): Sequence<String>

    companion object {
        fun getInstance(project: Project): ScriptDefinitionProvider? =
            ServiceManager.getService(project, ScriptDefinitionProvider::class.java)
    }
}

fun findScriptDefinition(psiFile: PsiFile): KotlinScriptDefinition? {
    val virtualFile = psiFile.virtualFile ?: psiFile.originalFile.virtualFile ?: return null
    return findScriptDefinition(virtualFile, psiFile.project)
}

fun findScriptDefinition(file: VirtualFile, project: Project): KotlinScriptDefinition? {
    if (file.isDirectory ||
        file.extension == KotlinFileType.EXTENSION ||
        file.extension == JavaClassFileType.INSTANCE.defaultExtension ||
        !isKotlinFileType(file)
    ) {
        return null
    }

    val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(project) ?: return null
    val psiFile = PsiManager.getInstance(project).findFile(file)
    if (psiFile != null) {
        if (psiFile !is KtFile) return null

        // Do not use psiFile.script here because this method can be called during indexes access
        // and accessing stubs may cause deadlock
        // If script definition cannot be find, default script definition is used
        // because all KtFile-s with KotlinFileType and non-kts extensions are parsed as scripts
        val definition = scriptDefinitionProvider.findScriptDefinition(file.name)
        return definition ?: scriptDefinitionProvider.getDefaultScriptDefinition()
    }

    return scriptDefinitionProvider.findScriptDefinition(file.name)
}

private fun isKotlinFileType(file: VirtualFile): Boolean {
    val typeRegistry = FileTypeRegistry.getInstance()
    return typeRegistry.getFileTypeByFile(file) == KotlinFileType.INSTANCE ||
            typeRegistry.getFileTypeByFileName(file.name) == KotlinFileType.INSTANCE
}

