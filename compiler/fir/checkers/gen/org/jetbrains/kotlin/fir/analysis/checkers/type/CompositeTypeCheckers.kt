/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.FirCheckerWithMppKind
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class CompositeTypeCheckers(val predicate: (FirCheckerWithMppKind) -> Boolean) : TypeCheckers() {
    constructor(mppKind: MppCheckerKind) : this({ it.mppKind == mppKind })

    override val typeRefCheckers: Set<FirTypeRefChecker>
        field: MutableSet<FirTypeRefChecker> = []
    override val resolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker>
        field: MutableSet<FirResolvedTypeRefChecker> = []
    override val functionTypeRefCheckers: Set<FirFunctionTypeRefChecker>
        field: MutableSet<FirFunctionTypeRefChecker> = []
    override val intersectionTypeRefCheckers: Set<FirIntersectionTypeRefChecker>
        field: MutableSet<FirIntersectionTypeRefChecker> = []

    @CheckersComponentInternal
    fun register(checkers: TypeCheckers) {
        checkers.typeRefCheckers.filterTo(typeRefCheckers, predicate)
        checkers.resolvedTypeRefCheckers.filterTo(resolvedTypeRefCheckers, predicate)
        checkers.functionTypeRefCheckers.filterTo(functionTypeRefCheckers, predicate)
        checkers.intersectionTypeRefCheckers.filterTo(intersectionTypeRefCheckers, predicate)
    }
}
