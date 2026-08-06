/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * A symbol provider which hides certain classes from the regular
 * [getClassLikeSymbolByClassId][org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider.getClassLikeSymbolByClassId]
 * surface, which is used by dependent sessions and cross-module resolution, while still serving them for module-internal
 * accesses via [getClassLikeSymbolByClassIdIncludingHidden].
 *
 * This is required for builtin classes with SDK-dependent supertypes
 * (see `FirJvmDeserializationExtension.CLASSES_WITH_SDK_DEPENDENT_SUPERTYPES`, KT-29858): use-site modules must resolve
 * them through the SDK-keyed builtins session (see
 * [LLFirBuiltinsSessionFactory.getBuiltinsSession][org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.LLFirBuiltinsSessionFactory.getBuiltinsSession]),
 * but PSI-anchored and other module-internal accesses within a library session still work with the library's own copies.
 */
internal interface LLPartiallyHidingSymbolProvider {
    /**
     * Returns the class-like symbol for [classId], including the classes hidden from the regular lookup.
     */
    fun getClassLikeSymbolByClassIdIncludingHidden(classId: ClassId): FirClassLikeSymbol<*>?
}
