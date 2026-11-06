/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.impl.VirtualFileEnumeration
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtPlatformInterface
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import java.net.URL

/**
 * A [BuiltinsVirtualFileProvider] that discovers built-in files as resources of its own class loader.
 *
 * Implementors only have to map a resource [java.net.URL] to a [VirtualFile] of the file system they operate on; locating the
 * built-in resources and building the search scope are handled here.
 */
@KtPlatformInterface
abstract class BuiltinsVirtualFileProviderBaseImpl : BuiltinsVirtualFileProvider() {
    private val builtInUrls: Set<URL> by lazy {
        val classLoader = this::class.java.classLoader
        StandardClassIds.builtInsPackages.mapNotNullTo(mutableSetOf()) { builtInPackageFqName ->
            val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(builtInPackageFqName)
            classLoader.getResource(resourcePath)
        }
    }

    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        val builtInFiles = getBuiltinVirtualFiles()

        return KotlinBuiltinsFileScope(project, builtInFiles)
    }

    /**
     * Scope representing Kotlin builtin files
     */
    private class KotlinBuiltinsFileScope(project: Project, builtinFiles: Set<VirtualFile>) :
        DelegatingGlobalSearchScope(project, filesScope(project, builtinFiles)), VirtualFileEnumeration {
        /**
         * Bare [GlobalSearchScope.FilesScope] could not be used here,
         * as it has a misleading constant `true` value for [GlobalSearchScope.isSearchInModuleContent].
         * Furthermore, these builtin files are not supposed to cover any module content.
         * Some Kotlin logic relies on the correctness of [GlobalSearchScope.isSearchInModuleContent] return value,
         * so it has to be explicitly set to `false`.
         */
        override fun isSearchInModuleContent(aModule: Module): Boolean = false

        override fun isSearchInLibraries(): Boolean = true

        override fun getDisplayName(): String = "Kotlin builtin files scope"

        override fun contains(fileId: Int): Boolean {
            return (delegate as VirtualFileEnumeration).contains(fileId)
        }

        override fun asArray(): IntArray {
            return (delegate as VirtualFileEnumeration).asArray()
        }
    }

    /**
     * Resolves the [url] of a built-in resource to a [VirtualFile], or returns `null` if it cannot be found.
     */
    protected abstract fun findVirtualFile(url: URL): VirtualFile?

    override fun getBuiltinVirtualFiles(): Set<VirtualFile> = builtInUrls.mapNotNullTo(mutableSetOf()) { url ->
        val file = findVirtualFile(url)
        if (file == null) {
            logger<BuiltinsVirtualFileProvider>().warn("VirtualFile not found for builtin $url")
        }
        file
    }
}
