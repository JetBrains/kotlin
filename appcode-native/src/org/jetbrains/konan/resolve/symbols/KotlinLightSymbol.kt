/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.symbols.DeepEqual
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import com.jetbrains.cidr.lang.symbols.OCSymbolOffsetUtil

class KotlinLightSymbol(
    private val psi: PsiElement,
    private val name: String,
    private val kind: OCSymbolKind
) : OCSymbol {
    override fun getKind(): OCSymbolKind = kind

    override fun locateDefinition(project: Project): PsiElement? = psi

    override fun getContainingFile(): VirtualFile? = psi.containingFile.virtualFile

    override fun getName(): String = name

    override fun getComplexOffset(): Long = OCSymbolOffsetUtil.getComplexOffset(psi)

    override fun hashCodeExcludingOffset(): Int {
        var hashCode = psi.hashCode()
        hashCode = hashCode * 31 + name.hashCode()
        hashCode = hashCode * 31 + kind.hashCode()
        return hashCode
    }

    override fun isGlobal(): Boolean {
        //todo implement???
        return true
    }

    override fun deepEqualStep(c: DeepEqual.Comparator, first: Any, second: Any): Boolean = first === second

    override fun isSameSymbol(symbol: OCSymbol?, project: Project): Boolean {
        return symbol === this
                || symbol is KotlinOCWrapperSymbol<*> && symbol.locateDefinition(project) == locateDefinition(project)
                || symbol is KotlinLightSymbol && symbol.locateDefinition(project) == locateDefinition(project)
    }
}