package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.jetbrains.cidr.lang.symbols.DeepEqual
import com.jetbrains.cidr.lang.symbols.OCSymbolBase
import org.jetbrains.kotlin.backend.konan.objcexport.Stub
import org.jetbrains.kotlin.psi.KtNamedDeclaration

abstract class KtImmediateSymbol : KtSymbol {
    private lateinit var name: String
    private var offset: Long

    constructor(stub: Stub<*>, name: String) {
        this.name = name
        this.offset = stub.offset
    }

    constructor() {
        this.offset = 0
    }

    override fun getName(): String = name

    override fun getComplexOffset(): Long = offset
    override fun setComplexOffset(complexOffset: Long) {
        offset = complexOffset
    }

    override fun deepEqualStep(c: DeepEqual.Comparator, first: Any, second: Any): Boolean {
        val f = first as KtImmediateSymbol
        val s = second as KtImmediateSymbol

        if (!Comparing.equal(f.complexOffset, s.complexOffset)) return false
        if (!Comparing.equal(f.name, s.name)) return false

        return true
    }

    override fun hashCodeExcludingOffset(): Int = name.hashCode() * 31

    protected fun doLocateDefinition(project: Project): KtNamedDeclaration? =
        OCSymbolBase.doLocateDefinition(this, project, KtNamedDeclaration::class.java)
}
