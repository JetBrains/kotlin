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

class FilteredTypeCheckers(
    val delegate: TypeCheckers,
    val predicate: (FirTypeChecker<*>) -> Boolean
) : TypeCheckers() {
    override val typeRefCheckers: Set<FirTypeRefChecker> = delegate.typeRefCheckers.filterTo(mutableSetOf(), predicate)
    override val resolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker> = delegate.resolvedTypeRefCheckers.filterTo(mutableSetOf(), predicate)
    override val functionTypeRefCheckers: Set<FirFunctionTypeRefChecker> = delegate.functionTypeRefCheckers.filterTo(mutableSetOf(), predicate)
    override val intersectionTypeRefCheckers: Set<FirIntersectionTypeRefChecker> = delegate.intersectionTypeRefCheckers.filterTo(mutableSetOf(), predicate)
}
