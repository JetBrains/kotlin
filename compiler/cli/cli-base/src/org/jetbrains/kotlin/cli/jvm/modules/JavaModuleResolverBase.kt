/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.modules

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

abstract class JavaModuleResolverBase protected constructor(
    private val moduleGraph: JavaModuleGraph,
    private val systemModules: List<JavaModule.Explicit>,
    project: Project
) : JavaModuleResolver {
    private val virtualFileFinder by lazy { VirtualFileFinder.getInstance(project) }

    override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? {
        val virtualFile = virtualFileFinder.findSourceOrBinaryVirtualFile(classId) ?: return null

        return (findJavaModule(virtualFile) as? JavaModule.Explicit)?.moduleInfo?.annotations
    }

    protected abstract fun findUserModule(file: VirtualFile): JavaModule?

    private fun findJavaModule(file: VirtualFile): JavaModule? {
        if (file.fileSystem.protocol == StandardFileSystems.JRT_PROTOCOL || file.extension == "sig") {
            return systemModules.firstOrNull { module -> file in module }
        }

        return findUserModule(file)
    }

    protected operator fun JavaModule.contains(file: VirtualFile): Boolean =
        moduleRoots.any { (val root = file, val isBinary) -> isBinary && VfsUtilCore.isAncestor(root, file, false) }

    override fun checkAccessibility(
        fileFromOurModule: VirtualFile?, referencedFile: VirtualFile, referencedPackage: FqName?
    ): JavaModuleResolver.AccessError? {
        val ourModule = fileFromOurModule?.let(this::findJavaModule)
        val theirModule = this.findJavaModule(referencedFile)

        if (ourModule?.name == theirModule?.name) return null

        if (theirModule == null) {
            return JavaModuleResolver.AccessError.ModuleDoesNotReadUnnamedModule
        }

        if (ourModule != null && !moduleGraph.reads(ourModule.name, theirModule.name)) {
            return JavaModuleResolver.AccessError.ModuleDoesNotReadModule(theirModule.name)
        }

        val fqName = referencedPackage ?: return null
        if (!theirModule.exports(fqName) && (ourModule == null || !theirModule.exportsTo(fqName, ourModule.name))) {
            return JavaModuleResolver.AccessError.ModuleDoesNotExportPackage(theirModule.name)
        }

        return null
    }
}
