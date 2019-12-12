package org.jetbrains.konan.resolve.symbols.objc

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.symbols.DeepEqual
import com.jetbrains.cidr.lang.symbols.VirtualFileOwner
import org.jetbrains.konan.resolve.symbols.KtImmediateSymbol
import org.jetbrains.konan.resolve.symbols.KtOCSymbolPsiWrapper
import org.jetbrains.kotlin.backend.konan.objcexport.Stub

abstract class KtOCImmediateSymbol : KtImmediateSymbol, VirtualFileOwner {
    @Transient
    private lateinit var file: VirtualFile

    constructor(stub: Stub<*>, name: String, file: VirtualFile) : super(stub, name) {
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

    override fun init(file: VirtualFile) {
        this.file = file
    }

    override fun locateDefinition(project: Project): PsiElement? = doLocateDefinition(project)?.let { KtOCSymbolPsiWrapper(it, this) }
}