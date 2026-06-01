/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration.*

object JvmDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = [
            FirJvmExternalDeclarationChecker,
            FirJvmNameChecker,
            FirJvmExposeBoxedChecker,
            FirJvmDefaultChecker,
            FirJvmStaticChecker,
            FirRepeatableAnnotationChecker,
            FirJvmInvalidAndDangerousCharactersChecker,
            FirJvmRedundantRepeatableChecker,
        ]

    override val constructorCheckers: Set<FirConstructorChecker>
        get() = setOf(
            FirJavaClassInheritsKtPrivateClassDeclarationChecker,
        )

    override val classCheckers: Set<FirClassChecker>
        get() = [
            FirStrictfpApplicabilityChecker,
            FirOverrideJavaNullabilityWarningChecker.Regular,
            FirOverrideJavaNullabilityWarningChecker.ForExpectClass,
            FirImplementationByDelegationWithDifferentGenericSignatureChecker,
            FirPropertyHidesJavaFieldChecker,
            FirIncompatibleAnnotationsChecker,
        ]

    override val regularClassCheckers: Set<FirRegularClassChecker>
        get() = [
            FirJvmRecordChecker,
            FirJvmInlineApplicabilityChecker,
        ]

    override val classLikeCheckers: Set<FirClassLikeChecker>
        get() = setOf(
            FirJvmConflictsChecker,
        )

    override val callableDeclarationCheckers: Set<FirCallableDeclarationChecker>
        get() = setOf(
            FirJvmFunctionDelegateMemberNameClashChecker,
        )

    override val propertyCheckers: Set<FirPropertyChecker>
        get() = [
            FirJvmFieldApplicabilityChecker,
            FirJvmSyntheticApplicabilityChecker,
            FirPropertyJavaNullabilityWarningChecker,
        ]

    override val functionCheckers: Set<FirFunctionChecker>
        get() = [
            FirSynchronizedAnnotationChecker,
            FirOverloadsChecker,
            FirJvmThrowsChecker,
            FirJvmVersionOverloadsChecker,
        ]

    override val typeParameterCheckers: Set<FirTypeParameterChecker>
        get() = setOf(
            FirUpperBoundsChecker,
        )

    override val namedFunctionCheckers: Set<FirNamedFunctionChecker>
        get() = setOf(
            FirAccidentalOverrideClashChecker,
        )

    override val valueParameterCheckers: Set<FirValueParameterChecker>
        get() = setOf(
            FirValueParameterJavaNullabilityWarningChecker
        )
}
