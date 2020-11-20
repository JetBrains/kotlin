/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer

interface KtSymbol : ValidityTokenOwner {
    val origin: KtSymbolOrigin
    val psi: PsiElement?

    fun createPointer(): KtSymbolPointer<KtSymbol>
}

enum class KtSymbolOrigin {
    SOURCE,

    /**
     * Declaration which do not have it's PSI source and was generated, they are:
     * For data classes the `copy`, `component{N}`, `toString`, `equals`, `hashCode` functions are generated
     * For enum classes the `valueOf` & `values` functions are generated
     */
    SOURCE_MEMBER_GENERATED,
    LIBRARY,
    JAVA, SAM_CONSTRUCTOR
}