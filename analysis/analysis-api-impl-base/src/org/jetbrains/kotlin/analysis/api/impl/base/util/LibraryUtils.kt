/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import java.nio.file.Path

object LibraryUtils {
    /**
     * Get all [VirtualFile]s inside the given [jar] (of [Path])
     *
     * Note that, if [CoreJarFileSystem] is not given, a fresh instance will be used, which will create fresh instances of [VirtualFile],
     *   resulting in potential hash mismatch (e.g., if used in scope membership check).
     */
    fun getAllVirtualFilesFromJar(
        jar: Path,
        jarFileSystem: CoreJarFileSystem = CoreJarFileSystem(),
    ): Collection<VirtualFile> {
        return jarFileSystem.refreshAndFindFileByPath(jar.toAbsolutePath().toString() + JAR_SEPARATOR)
            ?.let { getAllVirtualFilesFromRoot(it) } ?: emptySet()
    }

    /**
     * Get all [VirtualFile]s inside the given [dir] (of [Path])
     */
    fun getAllVirtualFilesFromDirectory(
        dir: Path,
    ): Collection<VirtualFile> {
        val fs = StandardFileSystems.local()
        return fs.findFileByPath(dir.toAbsolutePath().toString())?.let { getAllVirtualFilesFromRoot(it) } ?: return emptySet()
    }

    private fun getAllVirtualFilesFromRoot(
        root: VirtualFile
    ): Collection<VirtualFile> {
        val files = mutableSetOf<VirtualFile>()
        VfsUtilCore.iterateChildrenRecursively(
            root,
            /*filter=*/{ true },
            /*iterator=*/{ virtualFile ->
                if (!virtualFile.isDirectory) {
                    files.add(virtualFile)
                }
                true
            }
        )
        return files
    }
}
