package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.Stub
import org.jetbrains.kotlin.psi.KtNamedDeclaration

abstract class KtOCBaseSymbol : OCSymbol, OCForeignSymbol, VirtualFileOwner, KtSymbol {
    @Transient private lateinit var file: VirtualFile

    private lateinit var name: String
    private var offset: Long

    constructor(stub: Stub<*>, file: VirtualFile) {
        this.file = file
        name = stub.name
        offset = stub.offset
    }

    constructor() {
        offset = 0
    }

    override fun getName(): String = name

    override fun getComplexOffset(): Long = offset
    override fun setComplexOffset(complexOffset: Long) {
        offset = complexOffset
    }

    override fun getContainingFile(): VirtualFile = file

    override fun deepEqualStep(c: DeepEqual.Comparator, first: Any, second: Any): Boolean {
        val f = first as KtOCBaseSymbol
        val s = second as KtOCBaseSymbol

        if (!Comparing.equal(f.complexOffset, s.complexOffset)) return false
        if (!Comparing.equal(f.name, s.name)) return false
        if (!Comparing.equal(f.file, s.file)) return false

        return true
    }

    override fun hashCodeExcludingOffset(): Int = name.hashCode() * 31 + file.hashCode()

    override fun locateDefinition(project: Project): PsiElement? =
        OCSymbolBase.doLocateDefinition(this, project, KtNamedDeclaration::class.java)?.let { KotlinOCPsiWrapper(it, this) }


    override fun isSameSymbol(symbol: OCSymbol?, project: Project): Boolean {
        return super.isSameSymbol(symbol, project)
               || symbol is KotlinLightSymbol && locateDefinition(project) == symbol.locateDefinition(project)
    }

    override fun init(file: VirtualFile) {
        this.file = file
    }
}