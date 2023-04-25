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

package org.jetbrains.kotlin.resolve.jvm.modules

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.java.JavaModuleAnnotationsProvider
import org.jetbrains.kotlin.name.FqName

interface JavaModuleResolver : JavaModuleAnnotationsProvider {
    fun checkAccessibility(fileFromOurModule: VirtualFile?, referencedFile: VirtualFile, referencedPackage: FqName?): AccessError?

    sealed class AccessError {
        object ModuleDoesNotReadUnnamedModule : AccessError()
        data class ModuleDoesNotReadModule(val dependencyModuleName: String) : AccessError()
        data class ModuleDoesNotExportPackage(val dependencyModuleName: String) : AccessError()
    }

    companion object SERVICE {
        fun getInstance(project: Project): JavaModuleResolver = project.getService(JavaModuleResolver::class.java)
    }
}
