package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.openapi.vfs.VirtualFile

fun interface VirtualFileFactory {
    fun virtualFile(fileId: FileId): VirtualFile
}