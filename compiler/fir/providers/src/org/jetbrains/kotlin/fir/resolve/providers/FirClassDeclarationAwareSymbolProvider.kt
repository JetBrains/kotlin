/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers

import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId

// TODO (marco): Document.
interface FirClassDeclarationAwareSymbolProvider<DECLARATION> {
    @FirSymbolProviderInternals
    fun getClassLikeSymbolByClassId(classId: ClassId, declaration: DECLARATION): FirClassLikeSymbol<*>?
}
