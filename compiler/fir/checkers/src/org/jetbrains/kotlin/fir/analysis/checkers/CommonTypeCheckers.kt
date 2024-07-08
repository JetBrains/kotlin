/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.type.*

object CommonTypeCheckers : TypeCheckers() {
    override val typeRefCheckers: Set<FirTypeRefChecker> = setOf(
        FirSuspendModifierChecker,
    )

    override val resolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker> = setOf(
        FirTypeAnnotationChecker,
        FirSuspendModifierChecker,
        FirDeprecatedTypeChecker,
        FirOptInUsageTypeRefChecker,
        FirStarProjectionModifierChecker,
        FirInOutProjectionModifierChecker,
        FirDuplicateParameterNameInFunctionTypeChecker,
        FirOptionalExpectationTypeChecker,
        FirIncompatibleClassTypeChecker,
        FirContextReceiversTypeChecker,
        FirContextReceiversDeprecatedTypeChecker,
        FirProjectionRelationChecker,
        FirArrayOfNothingTypeChecker,
    )

    override val intersectionTypeRefCheckers: Set<FirIntersectionTypeRefChecker> = setOf(
        FirDefinitelyNotNullableChecker,
    )

    override val functionTypeRefCheckers: Set<FirFunctionTypeRefChecker> = setOf(
        FirUnsupportedDefaultValueInFunctionTypeParameterChecker,
        FirUnsupportedModifiersInFunctionTypeParameterChecker,
    )
}
