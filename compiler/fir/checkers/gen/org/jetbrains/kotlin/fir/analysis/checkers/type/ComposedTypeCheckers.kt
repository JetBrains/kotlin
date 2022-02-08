/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class ComposedTypeCheckers : TypeCheckers() {
    override val typeRefCheckers: Set<FirTypeRefChecker>
        get() = _typeRefCheckers

    private val _typeRefCheckers: MutableSet<FirTypeRefChecker> = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: TypeCheckers) {
        _typeRefCheckers += checkers.typeRefCheckers
    }
}
