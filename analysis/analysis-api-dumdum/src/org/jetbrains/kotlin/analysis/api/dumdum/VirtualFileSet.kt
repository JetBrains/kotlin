package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSet
import com.intellij.util.Processor

class VirtualFileSetImpl(val set: MutableSet<VirtualFile>): VirtualFileSet, MutableSet<VirtualFile> by set {
    override fun freezed(): MutableSet<VirtualFile> {
        return this
    }

    override fun freeze() {
        
    }

    override fun process(processor: Processor<in VirtualFile>): Boolean =
        set.all(processor::process)

}