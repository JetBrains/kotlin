/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.SingleRootFileViewProvider
import java.io.File

/**
 * Returns a [VirtualFile] for the given [path] without IDEA's built-in file size limits.
 */
fun getVirtualFileNoSizeLimit(file: File) =
    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)?.also { SingleRootFileViewProvider.doNotCheckFileSizeLimit(it) }
