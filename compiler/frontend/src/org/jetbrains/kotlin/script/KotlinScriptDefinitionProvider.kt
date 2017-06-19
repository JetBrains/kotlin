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

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import kotlin.concurrent.read
import kotlin.concurrent.write

class KotlinScriptDefinitionProvider {

    private val definitions: MutableList<KotlinScriptDefinition> = arrayListOf(StandardScriptDefinition)
    private val definitionsLock = java.util.concurrent.locks.ReentrantReadWriteLock()

    fun setScriptDefinitions(newDefinitions: List<KotlinScriptDefinition>): Boolean {
        var changed = false
        definitionsLock.read {
            if (newDefinitions != definitions) {
                definitionsLock.write {
                    definitions.clear()
                    definitions.addAll(newDefinitions)
                }
                changed = true
            }
        }
        return changed
    }

    fun findScriptDefinition(file: VirtualFile): KotlinScriptDefinition? = findScriptDefinition(file.name)

    fun findScriptDefinition(fileName: String): KotlinScriptDefinition? = definitionsLock.read {
        definitions.firstOrNull { it.isScript(fileName) }
    }

    fun isScript(fileName: String): Boolean = definitionsLock.read {
        definitions.any { it.isScript(fileName) }
    }

    fun addScriptDefinition(scriptDefinition: KotlinScriptDefinition) {
        definitionsLock.write {
            definitions.add(0, scriptDefinition)
        }
    }

    fun removeScriptDefinition(scriptDefinition: KotlinScriptDefinition) {
        definitionsLock.write {
            definitions.remove(scriptDefinition)
        }
    }

    fun getAllKnownFileTypes(): Iterable<LanguageFileType> = definitionsLock.read {
        definitions.map { it.fileType }.distinct()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinScriptDefinitionProvider? =
                ServiceManager.getService(project, KotlinScriptDefinitionProvider::class.java)
    }
}

fun getScriptDefinition(file: VirtualFile, project: Project): KotlinScriptDefinition? =
        KotlinScriptDefinitionProvider.getInstance(project)?.findScriptDefinition(file)

fun getScriptDefinition(psiFile: PsiFile): KotlinScriptDefinition? =
        KotlinScriptDefinitionProvider.getInstance(psiFile.project)?.findScriptDefinition(psiFile.name)
