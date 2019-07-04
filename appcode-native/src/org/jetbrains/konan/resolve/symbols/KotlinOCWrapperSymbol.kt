/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.Stub
import org.jetbrains.kotlin.psi.KtNamedDeclaration

abstract class KotlinOCWrapperSymbol<T : Stub<*>>(
    stub: T,
    @Transient val project: Project,
    @Transient private val file: VirtualFile?
) : OCSymbol, OCForeignSymbol {

    @Transient private val myStub: T? = stub

    private val myName: String = stub.name
    private var myComplexOffset: Long = -1 //todo by stub { myStub.psi?.let { OCSymbolOffsetUtil.getComplexOffset(it.textRange.startOffset, 0) } ?: 0 }

    override fun getName(): String = myName

    override fun getComplexOffset(): Long = myComplexOffset

    protected fun psi(): PsiElement? {
        return when {
            myStub != null -> myStub.psi
            else -> OCSymbolBase.doLocateDefinition(this, project, KtNamedDeclaration::class.java)
        }
    }

    override fun getContainingFile(): VirtualFile? = file

    override fun deepEqualStep(c: DeepEqual.Comparator, first: Any, second: Any): Boolean {
        val f = first as KotlinOCWrapperSymbol<*>
        val s = second as KotlinOCWrapperSymbol<*>

        if (!Comparing.equal(f.myStub, s.myStub)) return false
        if (!Comparing.equal(f.project, s.project)) return false

        if (f.myStub == null) {
            if (f.myComplexOffset != s.myComplexOffset) return false
            if (f.myName != s.myName) return false
        }

        return true
    }

    override fun hashCodeExcludingOffset(): Int {
        return when {
            myStub != null -> myStub.hashCode() * 31 + project.hashCode()
            else -> (project.hashCode() * 31 + myName.hashCode()) * 31 + file.hashCode()
        }
    }

    override fun locateDefinition(project: Project): PsiElement? {
        return psi()?.let { KotlinOCPsiWrapper(it, this) }
    }

    override fun isSameSymbol(symbol: OCSymbol?, project: Project): Boolean {
        return super.isSameSymbol(symbol, project)
               || symbol is KotlinLightSymbol && psi() == symbol.locateDefinition(project)
    }

    override fun setComplexOffset(complexOffset: Long) {
        myComplexOffset = complexOffset
    }

    override fun updateOffset(start: Int, lengthShift: Int) {
        if (myStub == null) {
            super.updateOffset(start, lengthShift)
        }
    }

    protected fun <R> stub(initializer: T.() -> R): Lazy<R> = lazy {
        myStub!!.initializer()
    }
}