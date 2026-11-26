/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.modules

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

/**
 * Handles system modules and common accessibility checks for Java module resolvers.
 *
 * [findSourceModule] and [findBinaryModule] resolve non-system files. JRT and `.sig` files are handled before these hooks
 * are called.
 */
abstract class AbstractJavaModuleResolver(
    private val moduleGraph: JavaModuleGraph,
    private val systemModules: List<JavaModule.Explicit>
) : JavaModuleResolver {
    protected abstract fun findSourceModule(file: VirtualFile): JavaModule?

    protected abstract fun findBinaryModule(file: VirtualFile): JavaModule?

    protected fun findJavaModule(file: VirtualFile): JavaModule? {
        findSystemModule(file)?.let { return it }

        return when (file.fileType) {
            KotlinFileType.INSTANCE, JavaFileType.INSTANCE -> findSourceModule(file)
            JavaClassFileType.INSTANCE -> findBinaryModule(file)
            else -> null
        }
    }

    protected fun findSystemModule(file: VirtualFile): JavaModule.Explicit? {
        if (!isSystemModuleFile(file)) return null
        return systemModules.firstOrNull { module -> file in module }
    }

    protected fun findSystemModule(rootId: JvmClasspathRootId): JavaModule.Explicit? =
        systemModules.firstOrNull { module -> module.moduleRoots.any { root -> root.isBinary && root.id == rootId } }

    protected fun isSystemModuleFile(file: VirtualFile): Boolean =
        file.fileSystem.protocol == StandardFileSystems.JRT_PROTOCOL || file.extension == "sig"

    protected operator fun JavaModule.contains(file: VirtualFile): Boolean =
        moduleRoots.any { (val root = file, val isBinary) -> isBinary && VfsUtilCore.isAncestor(root, file, false) }

    override fun checkAccessibility(
        fileFromOurModule: VirtualFile?, referencedFile: VirtualFile, referencedPackage: FqName?,
    ): JavaModuleResolver.AccessError? = checkAccessibility(
        fileFromOurModule?.let(this::findJavaModule),
        findJavaModule(referencedFile),
        referencedPackage,
    )

    protected fun checkAccessibility(
        ourModule: JavaModule?,
        theirModule: JavaModule?,
        referencedPackage: FqName?,
    ): JavaModuleResolver.AccessError? {
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
