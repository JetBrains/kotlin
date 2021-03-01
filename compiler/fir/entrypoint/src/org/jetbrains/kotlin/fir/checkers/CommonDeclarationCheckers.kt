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
        FirTypeAliasChecker,
    )

    override val functionCheckers: Set<FirFunctionChecker> = setOf(
        FirContractChecker,
        FirFunctionParameterChecker,
    )

    override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker> = setOf(
        FirFunctionNameChecker
    )

    override val propertyCheckers: Set<FirPropertyChecker> = setOf(
        FirInapplicableLateinitChecker,
        FirDestructuringDeclarationChecker,
        FirConstPropertyChecker,
        FirPropertyAccessorChecker,
        FirInitializerTypeMismatchChecker
    )

    override val classCheckers: Set<FirClassChecker> = setOf(
        FirOverrideChecker,
        FirNotImplementedOverrideChecker,
        FirThrowableSubclassChecker,
        FirOpenMemberChecker,
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
        FirDataClassPrimaryConstructorChecker,
        FirPrimaryConstructorSuperTypeChecker,
        FirTypeParametersInObjectChecker,
        FirMemberFunctionsChecker,
        FirMemberPropertiesChecker,
        FirNestedClassChecker,
        FirInlineClassDeclarationChecker,
    )

    override val constructorCheckers: Set<FirConstructorChecker> = setOf(
        FirConstructorAllowedChecker,
    )

    override val fileCheckers: Set<FirFileChecker> = setOf(
        FirKClassWithIncorrectTypeArgumentChecker,
        FirTopLevelFunctionsChecker,
        FirTopLevelPropertiesChecker,
    )

    override val controlFlowAnalyserCheckers: Set<FirControlFlowChecker> = setOf(
        FirCallsEffectAnalyzer,
        FirReturnsImpliesAnalyzer,
    )

    override val variableAssignmentCfaBasedCheckers: Set<AbstractFirPropertyInitializationChecker> = setOf(
        FirPropertyInitializationAnalyzer,
    )

    override val typeParameterCheckers: Set<FirTypeParameterChecker> = setOf(
        FirTypeParameterBoundsChecker,
    )
}
