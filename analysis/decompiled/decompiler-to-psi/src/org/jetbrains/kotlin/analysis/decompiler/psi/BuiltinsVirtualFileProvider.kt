/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
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
        StandardClassIds.builtInsPackages.mapNotNullTo(mutableSetOf()) { builtInPackageFqName ->
            val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(builtInPackageFqName)
            classLoader.getResource(resourcePath)
        }
    }

    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        val builtInFiles = getBuiltinVirtualFiles()
        return GlobalSearchScope.filesScope(project, builtInFiles)
    }

    protected abstract fun findVirtualFile(url: URL): VirtualFile?

    override fun getBuiltinVirtualFiles(): Set<VirtualFile> = builtInUrls.mapNotNullTo(mutableSetOf()) { url ->
        val file = findVirtualFile(url)
        if (file == null) {
            logger<BuiltinsVirtualFileProvider>().warn("VirtualFile not found for builtin $url")
        }
        file
    }
}

class BuiltinsVirtualFileProviderCliImpl : BuiltinsVirtualFileProviderBaseImpl() {
    override fun findVirtualFile(url: URL): VirtualFile? {
        val split = URLUtil.splitJarUrl(url.path)
            ?: errorWithAttachment("URL for builtins does not contain jar separator") {
                withEntry("url", url) { url.toString() }
            }
        val jarPath = split.first
        val builtinFile = split.second
        val pathToQuery = jarPath + URLUtil.JAR_SEPARATOR + builtinFile
        val jarFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JAR_PROTOCOL)
        return jarFileSystem.findFileByPath(pathToQuery)
    }
}