/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.symbols.DeepEqual
import com.jetbrains.cidr.lang.symbols.OCForeignSymbol
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.OCSymbolOffsetUtil
import org.jetbrains.kotlin.backend.konan.objcexport.Stub
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.resolve.source.getPsi

abstract class KotlinOCWrapperSymbol<T : Stub<*>>(
    protected val stub: T,
    private val project: Project
) : OCSymbol, OCForeignSymbol {

    override fun getName(): String = stub.name

    override fun getComplexOffset(): Long = psi()?.let { OCSymbolOffsetUtil.getComplexOffset(it) } ?: 0

    protected fun psi(): PsiElement? = (stub.descriptor as? DeclarationDescriptorWithSource)?.source?.getPsi()

    override fun getContainingFile(): VirtualFile? = psi()?.containingFile?.virtualFile

    override fun deepEqualStep(c: DeepEqual.Comparator, first: Any, second: Any): Boolean {
        val f = first as KotlinOCWrapperSymbol<*>
        val s = second as KotlinOCWrapperSymbol<*>

        if (!Comparing.equal(f.stub, s.stub)) return false
        if (!Comparing.equal(f.project, s.project)) return false

        return true
    }

    override fun hashCodeExcludingOffset(): Int = stub.hashCode() * 31 + project.hashCode()

    override fun locateDefinition(project: Project): PsiElement? {
        return psi()?.let { KotlinOCPsiWrapper(it, this) }
    }

    override fun isSameSymbol(symbol: OCSymbol?, project: Project): Boolean {
        return super.isSameSymbol(symbol, project)
               || symbol is KotlinLightSymbol && psi() == symbol.locateDefinition(project)
    }
}