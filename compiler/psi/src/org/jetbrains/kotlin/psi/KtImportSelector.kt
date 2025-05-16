/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.stubs.KotlinImportSelectorStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtImportSelector : KtElementImplStub<KotlinImportSelectorStub> {
    @Suppress("unused")
    constructor(node: ASTNode) : super(node)

    @Suppress("unused")
    constructor(stub: KotlinImportSelectorStub) : super(stub, KtStubElementTypes.IMPORT_SELECTOR)

    override fun <R : Any?, D : Any?> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitImportSelector(this, data)
    }

    val importDirective: KtImportDirective?
        get() = parent as? KtImportDirective

    // we only have [extension] selectors for now
    fun getSelector(): KtImportInfo.ImportSelector? = greenStub?.getSelector() ?: KtImportInfo.ImportSelector.Extension
    fun getSelectorIdentifier(): PsiElement? = findChildByType(KtTokens.EXTENSION_KEYWORD)

    override fun getTextOffset() = getSelectorIdentifier()?.textOffset ?: startOffset

    override fun getUseScope() = LocalSearchScope(containingFile)
}