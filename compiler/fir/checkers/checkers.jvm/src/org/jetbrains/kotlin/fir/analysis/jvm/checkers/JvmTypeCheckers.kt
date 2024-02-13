/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.type.*
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.type.FirFunctionalTypeParameterNameChecker

object JvmTypeCheckers : TypeCheckers() {
    override val typeRefCheckers: Set<FirTypeRefChecker> = setOf(
        FirDynamicUnsupportedChecker,
        FirFunctionalTypeParameterNameChecker,
    )
}
