/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltInsVirtualFileProviderBaseImpl
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import java.net.URL

internal class BuiltInsVirtualFileProviderImp(
    private val jarFileSystem: CoreJarFileSystem,
) : BuiltInsVirtualFileProviderBaseImpl() {
    override fun findVirtualFile(url: URL): VirtualFile? {
        val split = URLUtil.splitJarUrl(url.path)
            ?: errorWithAttachment("URL for builtins does not contain jar separator") {
                withEntry("url", url) { url.toString() }
            }
        val jarPath = split.first
        val builtInFile = split.second
        val pathToQuery = jarPath + URLUtil.JAR_SEPARATOR + builtInFile
        return jarFileSystem.findFileByPath(pathToQuery)
    }
}