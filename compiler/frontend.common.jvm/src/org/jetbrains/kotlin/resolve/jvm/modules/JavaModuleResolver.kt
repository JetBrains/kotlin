/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
