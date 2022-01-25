/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration.*

object JvmDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = setOf(
            FirJvmExternalDeclarationChecker,
            FirJvmNameChecker,
            FirJvmDefaultChecker,
            FirJvmFunctionDelegateMemberNameClashChecker,
            FirJvmStaticChecker,
            FirRepeatableAnnotationChecker,
            FirInvalidAndDangerousCharactersJvmChecker,
        )

    override val classCheckers: Set<FirClassChecker>
        get() = setOf(
            FirStrictfpApplicabilityChecker
        )

    override val regularClassCheckers: Set<FirRegularClassChecker>
        get() = setOf(
            FirJvmRecordChecker,
            FirJvmInlineApplicabilityChecker
        )

    override val propertyCheckers: Set<FirPropertyChecker>
        get() = setOf(
            FirVolatileAnnotationChecker,
            FirJvmFieldApplicabilityChecker,
            FirJvmSyntheticApplicabilityChecker,
            FirJvmInlinePropertyChecker,
        )

    override val functionCheckers: Set<FirFunctionChecker>
        get() = setOf(
            FirSynchronizedAnnotationChecker,
            FirOverloadsChecker,
            FirJvmInlineDeclarationChecker,
        )

    override val typeParameterCheckers: Set<FirTypeParameterChecker>
        get() = setOf(
            FirUpperBoundsChecker,
        )
}
