/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirErrorNamedReference : FirNamedReference {
    override val psi: PsiElement?
    override val name: Name
    override val candidateSymbol: AbstractFirBasedSymbol<*>?
    val errorReason: String

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitErrorNamedReference(this, data)
}
