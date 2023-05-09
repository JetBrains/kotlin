/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.type.FirUnsupportedDefaultValueInFunctionTypeParameterChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.*

object CommonTypeCheckers : TypeCheckers() {
    override val typeRefCheckers: Set<FirTypeRefChecker> = setOf(
        FirTypeAnnotationChecker,
        FirSuspendModifierChecker,
        FirDeprecatedTypeChecker,
        FirOptInUsageTypeRefChecker,
        FirDefinitelyNotNullableChecker,
        FirUnsupportedDefaultValueInFunctionTypeParameterChecker,
        FirUnsupportedModifiersInFunctionTypeParameterChecker,
        FirStarProjectionModifierChecker,
    )
}
