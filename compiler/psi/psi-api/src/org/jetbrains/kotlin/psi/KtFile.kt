/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets

/**
 * The root of the PSI tree for a Kotlin source or script file.
 *
 * A [KtFile] holds the file's package directive, imports, and top-level declarations (see [KtCommonFile] for that shared structure). On top
 * of that, it implements the platform's [PsiClassOwner], so it can expose the file's Kotlin declarations to Java-facing tooling as light
 * [PsiClass]es (for example, the file facade class and top-level class declarations).
 *
 * Obtain the containing file of any [KtElement] via [KtPureElement.getContainingKtFile].
 */
open class KtFile(viewProvider: FileViewProvider, isCompiled: Boolean) : @Suppress("DEPRECATION") KtCommonFile(viewProvider, isCompiled),
    PsiClassOwner {
    /**
     * Returns the Java light classes that this file contributes, such as the file facade class and any top-level class declarations, or an
     * empty array if none are available.
     */
    override fun getClasses(): Array<PsiClass> {
        val fileClassProvider = project.getService(KtFileClassProvider::class.java)
        return fileClassProvider?.getFileClasses(this) ?: PsiClass.EMPTY_ARRAY
    }

    override fun setPackageName(packageName: String) {}

    @Deprecated(
        message = "getPackageFqName should be used instead",
        replaceWith = ReplaceWith("packageFqName.asString()"),
    )
    override fun getPackageName(): String {
        return packageFqName.asString()
    }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitKtFile(this, data)
    }

    companion object {
        /**
         * The stub element types that may appear as top-level members of a Kotlin file: all declaration types plus scripts.
         */
        val FILE_DECLARATION_TYPES = TokenSet.orSet(KtTokenSets.DECLARATION_TYPES, TokenSet.create(KtNodeTypes.SCRIPT))
    }
}
