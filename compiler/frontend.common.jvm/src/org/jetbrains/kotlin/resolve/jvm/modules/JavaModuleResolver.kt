/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.modules

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.java.JavaModuleAnnotationsProvider
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.VirtualFileBackedClassFileHandle
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.VirtualFileBasedApi
import org.jetbrains.kotlin.name.FqName

interface JavaModuleResolver : JavaModuleAnnotationsProvider {
    fun checkAccessibility(fileFromOurModule: VirtualFile?, referencedFile: VirtualFile, referencedPackage: FqName?): AccessError?

    fun checkAccessibility(
        fileFromOurModule: VirtualFile?, referencedClassFile: BinaryClassFileHandle, referencedPackage: FqName?,
    ): AccessError?

    sealed class AccessError {
        object ModuleDoesNotReadUnnamedModule : AccessError()
        data class ModuleDoesNotReadModule(val dependencyModuleName: String) : AccessError()
        data class ModuleDoesNotExportPackage(val dependencyModuleName: String) : AccessError()
    }

    companion object SERVICE {
        fun getInstance(project: Project): JavaModuleResolver = project.getService(JavaModuleResolver::class.java)
    }
}

// TODO(KT-90203): switch [JavaModuleResolver] to class file handles and drop this function.
@VirtualFileBasedApi
fun JavaModuleResolver.checkAccessibilityByVirtualFile(
    fileFromOurModule: VirtualFile?,
    referencedClassFile: BinaryClassFileHandle,
    referencedPackage: FqName?,
): JavaModuleResolver.AccessError? {
    val referencedFile = (referencedClassFile as? VirtualFileBackedClassFileHandle)?.virtualFile
        ?: error("${this::class.java.name} cannot check Java module accessibility of ${referencedClassFile.pathForDiagnostics}")
    return checkAccessibility(fileFromOurModule, referencedFile, referencedPackage)
}
