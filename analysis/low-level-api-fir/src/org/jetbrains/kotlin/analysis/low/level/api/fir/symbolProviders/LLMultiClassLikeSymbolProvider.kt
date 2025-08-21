/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * An [LLMultiClassLikeSymbolProvider] is able to provide all class-like symbols for a single [ClassId].
 *
 * Its behavior is in contrast to [getClassLikeSymbolByClassId][org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider.getClassLikeSymbolByClassId],
 * which only returns the first class-like symbol with the class ID, whereas [getAllClassLikeSymbolsByClassId] returns *all* such symbols.
 */
internal interface LLMultiClassLikeSymbolProvider : LLPsiAwareSymbolProvider {
    /**
     * Returns all [FirClassLikeSymbol]s with the given [classId].
     */
    fun getAllClassLikeSymbolsByClassId(classId: ClassId): List<FirClassLikeSymbol<*>>
}
