package org.jetbrains.konan

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.StubTree
import org.jetbrains.konan.analyser.index.KonanDescriptorManager
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText

class KonanDecompiledFile(provider: KotlinDecompiledFileViewProvider,
                          text: (VirtualFile) -> DecompiledText) : KtDecompiledFile(provider, text) {

  override fun getStubTree(): StubTree? {
    val vFile = virtualFile
    val cache = KonanDescriptorManager.INSTANCE

    val cached = cache.getStub(vFile)
    if (cached != null) {
      return cached
    }

    val stubTree = super.getStubTree()
    if (stubTree != null) {
      cache.cacheStub(vFile, stubTree)
    }

    return stubTree
  }
}