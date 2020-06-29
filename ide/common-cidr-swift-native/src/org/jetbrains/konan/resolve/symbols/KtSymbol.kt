package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.OCForeignSymbol
import com.jetbrains.cidr.lang.symbols.OCSymbol

interface KtSymbol : OCSymbol, OCForeignSymbol {
    override fun getContainingFile(): VirtualFile
}