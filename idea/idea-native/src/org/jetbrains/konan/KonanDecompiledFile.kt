package org.jetbrains.konan

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText

class KonanDecompiledFile(provider: KotlinDecompiledFileViewProvider, text: (VirtualFile) -> DecompiledText) :
    KtDecompiledFile(provider, text)
