/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration.*

object JvmDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker> = setOf(
        FirJvmExternalDeclarationChecker,
        FirJvmNameChecker,
        FirJvmExposeBoxedChecker,
        FirJvmDefaultChecker,
        FirJvmStaticChecker,
        FirRepeatableAnnotationChecker,
        FirJvmInvalidAndDangerousCharactersChecker,
        FirJvmRedundantRepeatableChecker,
    )

    override val constructorCheckers: Set<FirConstructorChecker> = setOf(
        FirJavaClassInheritsKtPrivateClassDeclarationChecker,
    )

    override val classCheckers: Set<FirClassChecker> = setOf(
        FirStrictfpApplicabilityChecker,
        FirOverrideJavaNullabilityWarningChecker.Regular,
        FirOverrideJavaNullabilityWarningChecker.ForExpectClass,
        FirImplementationByDelegationWithDifferentGenericSignatureChecker,
        FirPropertyHidesJavaFieldChecker,
        FirIncompatibleAnnotationsChecker,
    )

    override val regularClassCheckers: Set<FirRegularClassChecker> = setOf(
        FirJvmRecordChecker,
        FirJvmInlineApplicabilityChecker,
        FirInterfaceJvmFieldApplicabilityChecker,
    )

    override val classLikeCheckers: Set<FirClassLikeChecker> = setOf(
        FirJvmConflictsChecker,
    )

    override val callableDeclarationCheckers: Set<FirCallableDeclarationChecker> = setOf(
        FirJvmFunctionDelegateMemberNameClashChecker,
    )

    override val propertyCheckers: Set<FirPropertyChecker> = setOf(
        FirJvmFieldApplicabilityChecker,
        FirJvmSyntheticApplicabilityChecker,
        FirJvmAnnotationsTargetNonExistentAccessorChecker,
        FirPropertyJavaNullabilityWarningChecker,
    )

    override val functionCheckers: Set<FirFunctionChecker> = setOf(
        FirSynchronizedAnnotationChecker,
        FirOverloadsChecker,
        FirJvmThrowsChecker,
        FirJvmVersionOverloadsChecker,
    )

    override val typeParameterCheckers: Set<FirTypeParameterChecker> = setOf(
        FirUpperBoundsChecker,
    )

    override val namedFunctionCheckers: Set<FirNamedFunctionChecker> = setOf(
        FirAccidentalOverrideClashChecker,
    )

    override val valueParameterCheckers: Set<FirValueParameterChecker> = setOf(
        FirValueParameterJavaNullabilityWarningChecker
    )

    override val anonymousFunctionCheckers: Set<FirAnonymousFunctionChecker> = setOf(
        FirJvmIndyAnnotatedLambdaChecker,
    )
}
