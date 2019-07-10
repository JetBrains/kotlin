package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.DeepEqual
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.VirtualFileOwner
import org.jetbrains.kotlin.backend.konan.objcexport.Stub

abstract class KtOCImmediateSymbol : KtImmediateSymbol, VirtualFileOwner {
    @Transient
    private lateinit var file: VirtualFile

    constructor(stub: Stub<*>, file: VirtualFile) : super(stub) {
        this.file = file
    }

    constructor() : super()

    override fun getContainingFile(): VirtualFile = file

    override fun deepEqualStep(c: DeepEqual.Comparator, first: Any, second: Any): Boolean {
        if (!super.deepEqualStep(c, first, second)) return false

        val f = first as KtOCImmediateSymbol
        val s = second as KtOCImmediateSymbol

        if (!Comparing.equal(f.file, s.file)) return false

        return true
    }

    override fun hashCodeExcludingOffset(): Int = name.hashCode() * 31 + file.hashCode()

    override fun isSameSymbol(symbol: OCSymbol?, project: Project): Boolean {
        return super.isSameSymbol(symbol, project)
               || symbol is KtOCLightSymbol && locateDefinition(project) == symbol.locateDefinition(project)
    }

    override fun init(file: VirtualFile) {
        this.file = file
    }
}