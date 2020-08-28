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
    SOURCE, LIBRARY, JAVA, SAM_CONSTRUCTOR
}