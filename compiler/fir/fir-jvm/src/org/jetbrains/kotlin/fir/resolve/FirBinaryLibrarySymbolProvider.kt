/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * Since all symbols of binary libraries are already resolved, theoretically we can access them.
 * This is an API to support the binary library symbol search without the module dependencies.
 */
class FirBinaryLibrarySymbolProvider(private val findSymbol: (ClassId) -> FirClassLikeSymbol<*>?) : FirSessionComponent {
    fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? = findSymbol(classId)
}

val FirSession.binaryLibrarySymbolProvider: FirBinaryLibrarySymbolProvider? by FirSession.nullableSessionComponentAccessor()