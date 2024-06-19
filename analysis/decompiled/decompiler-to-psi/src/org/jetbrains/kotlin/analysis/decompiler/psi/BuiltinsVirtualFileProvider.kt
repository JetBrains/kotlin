/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import java.net.URL

abstract class BuiltinsVirtualFileProvider {
    abstract fun getBuiltinVirtualFiles(): Set<VirtualFile>

    abstract fun createBuiltinsScope(project: Project): GlobalSearchScope

    companion object {
        fun getInstance(): BuiltinsVirtualFileProvider =
            ApplicationManager.getApplication().getService(BuiltinsVirtualFileProvider::class.java)
    }
}

abstract class BuiltinsVirtualFileProviderBaseImpl : BuiltinsVirtualFileProvider() {
    private val builtInUrls: Set<URL> by lazy {
        val classLoader = this::class.java.classLoader
        StandardClassIds.builtInsPackages.mapTo(mutableSetOf()) { builtInPackageFqName ->
            val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(builtInPackageFqName)
            classLoader.getResource(resourcePath)
                ?: errorWithAttachment("Resource for builtin $builtInPackageFqName not found") {
                    withEntry("resourcePath", resourcePath)
                }
        }
    }

    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        val builtInFiles = getBuiltinVirtualFiles()
        return GlobalSearchScope.filesScope(project, builtInFiles)
    }

    protected abstract fun findVirtualFile(url: URL): VirtualFile?

    override fun getBuiltinVirtualFiles(): Set<VirtualFile> = builtInUrls.mapTo(mutableSetOf()) { url ->
        findVirtualFile(url)
            ?: errorWithAttachment("Virtual file for builtin is not found") {
                withEntry("resourceUrl", url) { it.toString() }
            }
    }
}

class BuiltinsVirtualFileProviderCliImpl(
    private val jarFileSystem: CoreJarFileSystem,
) : BuiltinsVirtualFileProviderBaseImpl() {
    override fun findVirtualFile(url: URL): VirtualFile? {
        val split = URLUtil.splitJarUrl(url.path)
            ?: errorWithAttachment("URL for builtins does not contain jar separator") {
                withEntry("url", url) { url.toString() }
            }
        val jarPath = split.first
        val builtinFile = split.second
        val pathToQuery = jarPath + URLUtil.JAR_SEPARATOR + builtinFile
        return jarFileSystem.findFileByPath(pathToQuery)
    }
}