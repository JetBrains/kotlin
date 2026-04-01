/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.LocalSearchScope
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.stubs.KotlinImportAliasStub

/**
 * Represents an import alias that provides an alternative name for an imported declaration.
 *
 * ### Example:
 *
 * ```kotlin
 * import java.util.Date as JavaDate
 * //                    ^^^^^^^^^^^
 * ```
 */
class KtImportAlias : KtElementImplStub<KotlinImportAliasStub>, PsiNameIdentifierOwner {
    @Suppress("unused")
    constructor(node: ASTNode) : super(node)

    @Suppress("unused")
    constructor(stub: KotlinImportAliasStub) : super(stub, KtStubBasedElementTypes.IMPORT_ALIAS)

    override fun <R : Any?, D : Any?> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitImportAlias(this, data)
    }

    val importDirective: KtImportDirective?
        get() = parent as? KtImportDirective

    override fun getName(): String? = greenStub?.getName() ?: nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        nameIdentifier?.replace(KtPsiFactory(project).createNameIdentifier(name))
        return this
    }

    override fun getNameIdentifier(): PsiElement? = findChildByType(KtTokens.IDENTIFIER)

    override fun getTextOffset() = nameIdentifier?.textOffset ?: startOffset

    override fun getUseScope() = LocalSearchScope(containingFile)
}