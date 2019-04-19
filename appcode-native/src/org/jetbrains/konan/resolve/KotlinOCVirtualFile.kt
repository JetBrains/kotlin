package org.jetbrains.konan.resolve

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

data class KotlinOCVirtualFile(private val file: VirtualFile): LightVirtualFile(file.name) {
    override fun isWritable(): Boolean = false
}