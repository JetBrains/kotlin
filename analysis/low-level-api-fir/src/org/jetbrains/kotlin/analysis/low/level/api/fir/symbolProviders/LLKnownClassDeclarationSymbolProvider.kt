/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration

/**
 * A [FirSymbolProvider] which is able to provide a class-like symbol for a [ClassId] with an already known class-like declaration [E]. The
 * main purpose is optimization to avoid searching for a PSI declaration which is already known.
 */
internal interface LLKnownClassDeclarationSymbolProvider<E : PsiElement> {
    /**
     * Returns the [FirClassLikeSymbol] with the given [classId] for a known [classLikeDeclaration].
     *
     * As [classLikeDeclaration] is already known, this function is optimized to avoid a search for the corresponding PSI declaration.
     * As per the contract of [LLModuleSpecificSymbolProviderAccess], the declaration must be in the scope of the symbol provider's module.
     *
     * Furthermore, the function does not guarantee that a symbol for exactly [classLikeDeclaration] will be returned, as this parameter is
     * only used for optimization. This is in line with the contracts of [FirSymbolProvider.getClassLikeSymbolByClassId], which only
     * considers the [ClassId] itself and operates on a first-come, first-serve basis. The first [KtClassLikeDeclaration] passed to this
     * function or fetched by the symbol provider itself becomes the basis of the class-like symbol for that class ID.
     *
     * To find a symbol for a specific PSI declaration, use [LLPsiAwareSymbolProvider.getClassLikeSymbolByPsi].
     */
    @LLModuleSpecificSymbolProviderAccess
    fun getClassLikeSymbolByClassId(classId: ClassId, classLikeDeclaration: E): FirClassLikeSymbol<*>?
}
