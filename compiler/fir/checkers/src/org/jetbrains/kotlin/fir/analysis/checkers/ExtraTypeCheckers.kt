/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.extra.PlatformClassMappedToKotlinTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extra.RedundantNullableChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers

object ExtraTypeCheckers : TypeCheckers() {
    override val resolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker> = setOf(
        RedundantNullableChecker,
        PlatformClassMappedToKotlinTypeRefChecker,
    )
}
