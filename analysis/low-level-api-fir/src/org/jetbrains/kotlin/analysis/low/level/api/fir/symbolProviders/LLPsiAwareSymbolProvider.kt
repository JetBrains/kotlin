/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * A [FirSymbolProvider][org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider] that is able to provide class-like symbols for
 * specific PSI elements.
 */
interface LLPsiAwareSymbolProvider {
    /**
     * Returns a [FirClassLikeSymbol] for the specific [declaration], or `null` if there is no symbol that matches [declaration], including
     * when [declaration] is outside the scope of this symbol provider. Symbols without associated PSI are not considered as results.
     *
     * In contrast to [getClassLikeSymbolByClassId][org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider.getClassLikeSymbolByClassId],
     * this function is aware of the requested PSI and is able to return a specific symbol for it.
     *
     * ### Implementation notes
     *
     * Care should be taken not to pass [declaration] blindly to functions which accept a *known* declaration, such as the functions defined
     * by [LLKotlinSymbolProvider] for known declarations. In contrast to these functions, [getClassLikeSymbolByPsi] does not require the
     * caller to make sure that the symbol provider can and should provide a symbol for that PSI element.
     *
     * As [getClassLikeSymbolByPsi] can be called with any kind of [PsiElement] from any source, the implementation needs to check its
     * applicability, e.g. with a global search scope check.
     */
    fun getClassLikeSymbolByPsi(classId: ClassId, declaration: PsiElement): FirClassLikeSymbol<*>?
}
