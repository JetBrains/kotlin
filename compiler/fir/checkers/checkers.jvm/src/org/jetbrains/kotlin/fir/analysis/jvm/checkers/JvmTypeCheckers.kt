/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.type.FirDynamicUnsupportedChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirFunctionTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.type.FirFunctionalTypeParameterNameChecker
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.type.FirJvmModuleAccessibilityTypeChecker

object JvmTypeCheckers : TypeCheckers() {
    override val functionTypeRefCheckers: Set<FirFunctionTypeRefChecker> = setOf(
        FirFunctionalTypeParameterNameChecker,
    )

    override val resolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker> = setOf(
        FirDynamicUnsupportedChecker,
        FirJvmModuleAccessibilityTypeChecker,
    )
}
