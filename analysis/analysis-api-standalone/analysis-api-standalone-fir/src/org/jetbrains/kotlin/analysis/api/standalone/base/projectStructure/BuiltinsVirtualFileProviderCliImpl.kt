/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProviderBaseImpl
import org.jetbrains.kotlin.psi.KtPlatformInterface
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import java.net.URL

@OptIn(KtPlatformInterface::class)
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
