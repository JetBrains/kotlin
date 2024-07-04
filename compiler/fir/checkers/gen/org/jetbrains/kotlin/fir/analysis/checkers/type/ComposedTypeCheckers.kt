/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.FirCheckerWithMppKind
import org.jetbrains.kotlin.fir.analysis.checkers.CheckerSessionKind

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class ComposedTypeCheckers(val predicate: (FirCheckerWithMppKind) -> Boolean) : TypeCheckers() {
    constructor(mppKind: CheckerSessionKind) : this({ it.mppKind == mppKind })

    override val typeRefCheckers: Set<FirTypeRefChecker>
        get() = _typeRefCheckers
    override val resolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker>
        get() = _resolvedTypeRefCheckers
    override val functionTypeRefCheckers: Set<FirFunctionTypeRefChecker>
        get() = _functionTypeRefCheckers
    override val intersectionTypeRefCheckers: Set<FirIntersectionTypeRefChecker>
        get() = _intersectionTypeRefCheckers

    private val _typeRefCheckers: MutableSet<FirTypeRefChecker> = mutableSetOf()
    private val _resolvedTypeRefCheckers: MutableSet<FirResolvedTypeRefChecker> = mutableSetOf()
    private val _functionTypeRefCheckers: MutableSet<FirFunctionTypeRefChecker> = mutableSetOf()
    private val _intersectionTypeRefCheckers: MutableSet<FirIntersectionTypeRefChecker> = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: TypeCheckers) {
        checkers.typeRefCheckers.filterTo(_typeRefCheckers, predicate)
        checkers.resolvedTypeRefCheckers.filterTo(_resolvedTypeRefCheckers, predicate)
        checkers.functionTypeRefCheckers.filterTo(_functionTypeRefCheckers, predicate)
        checkers.intersectionTypeRefCheckers.filterTo(_intersectionTypeRefCheckers, predicate)
    }
}
