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
import org.jetbrains.kotlin.name.FqName

interface JavaModuleResolver : JavaModuleAnnotationsProvider {
    fun checkAccessibility(fileFromOurModule: VirtualFile?, referencedFile: VirtualFile, referencedPackage: FqName?): AccessError?

    /**
     * [checkAccessibility] for a class read from a binary class file: the module of [referencedClassFile] is the module owning
     * the classpath root the file was found in ([BinaryClassFileHandle.rootId]). The default only covers resolvers which know
     * their modules by virtual files.
     */
    fun checkAccessibility(fileFromOurModule: VirtualFile?, referencedClassFile: BinaryClassFileHandle, referencedPackage: FqName?): AccessError? {
        val referencedFile = (referencedClassFile as? VirtualFileBackedClassFileHandle)?.virtualFile ?: return null
        return checkAccessibility(fileFromOurModule, referencedFile, referencedPackage)
    }

    sealed class AccessError {
        object ModuleDoesNotReadUnnamedModule : AccessError()
        data class ModuleDoesNotReadModule(val dependencyModuleName: String) : AccessError()
        data class ModuleDoesNotExportPackage(val dependencyModuleName: String) : AccessError()
    }

    companion object SERVICE {
        fun getInstance(project: Project): JavaModuleResolver = project.getService(JavaModuleResolver::class.java)
    }
}
