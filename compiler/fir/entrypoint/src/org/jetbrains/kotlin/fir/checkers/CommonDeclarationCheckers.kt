/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers

import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.cfa.FirCallsEffectAnalyzer
import org.jetbrains.kotlin.fir.analysis.cfa.FirPropertyInitializationAnalyzer
import org.jetbrains.kotlin.fir.analysis.cfa.FirReturnsImpliesAnalyzer
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*

object CommonDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker> = setOf(
        FirAnnotationArgumentChecker,
        FirModifierChecker,
        FirConflictsChecker,
        FirConflictingProjectionChecker,
    )

    override val memberDeclarationCheckers: Set<FirMemberDeclarationChecker> = setOf(
        FirInfixFunctionDeclarationChecker,
        FirExposedVisibilityDeclarationChecker,
        FirSealedSupertypeChecker,
    )

    override val functionCheckers: Set<FirFunctionChecker> = setOf(
        FirFunctionNameChecker,
    )

    override val propertyCheckers: Set<FirPropertyChecker> = setOf(
        FirInapplicableLateinitChecker,
        FirDestructuringDeclarationInitializerChecker,
    )

    override val regularClassCheckers: Set<FirRegularClassChecker> = setOf(
        FirAnnotationClassDeclarationChecker,
        FirCommonConstructorDelegationIssuesChecker,
        FirConstructorInInterfaceChecker,
        FirDelegationSuperCallInEnumConstructorChecker,
        FirDelegationInInterfaceChecker,
        FirEnumClassSimpleChecker,
        FirInterfaceWithSuperclassChecker,
        FirLocalEntityNotAllowedChecker,
        FirManyCompanionObjectsChecker,
        FirMethodOfAnyImplementedInInterfaceChecker,
        FirPrimaryConstructorRequiredForDataClassChecker,
        FirSupertypeInitializedInInterfaceChecker,
        FirSupertypeInitializedWithoutPrimaryConstructor,
        FirTypeParametersInObjectChecker,
        FirTypeMismatchOnOverrideChecker,
        FirMemberFunctionChecker,
        FirMemberPropertyChecker,
    )

    override val constructorCheckers: Set<FirConstructorChecker> = setOf(
        FirConstructorAllowedChecker,
    )

    override val controlFlowAnalyserCheckers: Set<FirControlFlowChecker> = setOf(
        FirCallsEffectAnalyzer,
        FirReturnsImpliesAnalyzer,
    )

    override val variableAssignmentCfaBasedCheckers: Set<AbstractFirPropertyInitializationChecker> = setOf(
        FirPropertyInitializationAnalyzer,
    )
}
