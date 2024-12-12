package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.openapi.vfs.VirtualFile

fun interface FilePathExtractor {
    fun filePath(virtualFile: VirtualFile): FileId
}