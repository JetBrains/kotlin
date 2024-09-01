/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class TypeCheckers {
    companion object {
        val EMPTY: TypeCheckers = object : TypeCheckers() {}
    }

    open val typeRefCheckers: Set<FirTypeRefChecker> = emptySet()
    open val resolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker> = emptySet()
    open val functionTypeRefCheckers: Set<FirFunctionTypeRefChecker> = emptySet()
    open val intersectionTypeRefCheckers: Set<FirIntersectionTypeRefChecker> = emptySet()

    @CheckersComponentInternal internal val allTypeRefCheckers: Set<FirTypeRefChecker> by lazy { typeRefCheckers }
    @CheckersComponentInternal internal val allResolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker> by lazy { resolvedTypeRefCheckers + typeRefCheckers }
    @CheckersComponentInternal internal val allFunctionTypeRefCheckers: Set<FirFunctionTypeRefChecker> by lazy { functionTypeRefCheckers + typeRefCheckers }
    @CheckersComponentInternal internal val allIntersectionTypeRefCheckers: Set<FirIntersectionTypeRefChecker> by lazy { intersectionTypeRefCheckers + typeRefCheckers }
}
