/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import java.nio.file.Path

object LibraryUtils {
    fun getAllVirtualFilesFromJar(jar: Path): Collection<VirtualFile> {
        val jarFileSystem = CoreJarFileSystem()
        val root = jarFileSystem.refreshAndFindFileByPath(jar.toAbsolutePath().toString() + JAR_SEPARATOR)!!

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
