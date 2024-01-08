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

class ComposedTypeCheckers(val predicate: (FirCheckerWithMppKind) -> Boolean) : TypeCheckers() {
    constructor(mppKind: MppCheckerKind) : this({ it.mppKind == mppKind })

    override val typeRefCheckers: Set<FirTypeRefChecker>
        get() = _typeRefCheckers

    private val _typeRefCheckers: MutableSet<FirTypeRefChecker> = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: TypeCheckers) {
        checkers.typeRefCheckers.filterTo(_typeRefCheckers, predicate)
    }
}
