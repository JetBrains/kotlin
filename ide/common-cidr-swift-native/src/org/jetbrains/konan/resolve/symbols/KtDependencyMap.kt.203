package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.vfs.VirtualFile
import gnu.trove.TObjectLongHashMap

class KtDependencyMap : TObjectLongHashMap<VirtualFile> {
    constructor() : super()
    constructor(initialCapacity: Int) : super(initialCapacity)

    override fun get(key: VirtualFile): Long = index(key).let { if (it < 0) -1L else _values[it] }
}