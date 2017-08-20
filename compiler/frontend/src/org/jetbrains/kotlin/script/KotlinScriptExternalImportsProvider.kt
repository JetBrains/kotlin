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
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlin.script.dependencies.KotlinScriptExternalDependencies

interface KotlinScriptExternalImportsProvider {
    fun <TF: Any> getExternalImports(file: TF): KotlinScriptExternalDependencies?

    companion object {
        fun getInstance(project: Project): KotlinScriptExternalImportsProvider =
                ServiceManager.getService(project, KotlinScriptExternalImportsProvider::class.java)
    }
}

fun getScriptExternalDependencies(file: VirtualFile, project: Project): KotlinScriptExternalDependencies?  =
        KotlinScriptExternalImportsProvider.getInstance(project).getExternalImports(file)
