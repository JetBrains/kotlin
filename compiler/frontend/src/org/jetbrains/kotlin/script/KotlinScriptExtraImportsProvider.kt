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

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class KotlinScriptExtraImportsProvider(val project: Project) {
    private val lock = ReentrantReadWriteLock()
    private val cache = hashMapOf<VirtualFile, List<KotlinScriptExtraImport>>()
    private val envVars: Map<String, List<String>> by lazy { generateKotlinScriptClasspathEnvVarsForIdea(project) }

    fun isExtraImportsConfig(file: VirtualFile): Boolean = file.name.endsWith(IMPORTSFILEEXTENSION)

    fun getExtraImports(file: VirtualFile): List<KotlinScriptExtraImport> = lock.read {
        if (file.isInLocalFileSystem)
            cache.getOrPut(file) {
                file.parent.findFileByRelativePath(file.name + IMPORTSFILEEXTENSION)?.let {
                    loadScriptExtraImportConfigs(it.inputStream).map { KotlinScriptExtraImportFromConfig(it, envVars) }
                }
                ?: emptyList()
            }
        else emptyList()
    }

    fun invalidateExtraImports(importsFile: VirtualFile) {
        importsFile.parent.findFileByRelativePath(importsFile.name.removeSuffix(IMPORTSFILEEXTENSION))?.let {
            lock.write {
                cache.remove(it)
            }
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinScriptExtraImportsProvider? =
                ServiceManager.getService(project, KotlinScriptExtraImportsProvider::class.java)

        val IMPORTSFILEEXTENSION = ".ktsimports.xml"
    }
}