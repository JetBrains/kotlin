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
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirDelegationInInterfaceSyntaxChecker
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirFunctionTypeParametersSyntaxChecker
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirTypeParameterSyntaxChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirReservedUnderscoreDeclarationChecker

object CommonDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = setOf(
            FirAnnotationArgumentChecker,
            FirModifierChecker,
            FirConflictsChecker,
            FirConflictingProjectionChecker,
            FirTypeConstraintsChecker,
            FirReservedUnderscoreDeclarationChecker
        )

    override val memberDeclarationCheckers: Set<FirMemberDeclarationChecker>
        get() = setOf(
            FirInfixFunctionDeclarationChecker,
            FirExposedVisibilityDeclarationChecker,
            FirSealedSupertypeChecker,
            FirTypeAliasChecker,
            FirCyclicTypeBoundsChecker,
            FirInlineDeclarationChecker,
        )

    override val functionCheckers: Set<FirFunctionChecker>
        get() = setOf(
            FirContractChecker,
            FirFunctionParameterChecker,
        )

    override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker>
        get() = setOf(
            FirFunctionNameChecker,
            FirFunctionTypeParametersSyntaxChecker,
        )

    override val propertyCheckers: Set<FirPropertyChecker>
        get() = setOf(
            FirInapplicableLateinitChecker,
            FirDestructuringDeclarationChecker,
            FirConstPropertyChecker,
            FirPropertyAccessorChecker,
            FirPropertyTypeParametersChecker,
            FirPropertyAccessorChecker,
            FirInitializerTypeMismatchChecker,
            FirDelegatedPropertyChecker,
        )

    override val classCheckers: Set<FirClassChecker>
        get() = setOf(
            FirOverrideChecker,
            FirNotImplementedOverrideChecker,
            FirThrowableSubclassChecker,
            FirOpenMemberChecker,
        )

    override val regularClassCheckers: Set<FirRegularClassChecker>
        get() = setOf(
            FirAnnotationClassDeclarationChecker,
            FirCommonConstructorDelegationIssuesChecker,
            FirConstructorInInterfaceChecker,
            FirDelegationSuperCallInEnumConstructorChecker,
            FirDelegationInInterfaceSyntaxChecker,
            FirEnumClassSimpleChecker,
            FirInterfaceWithSuperclassChecker,
            FirLocalEntityNotAllowedChecker,
            FirManyCompanionObjectsChecker,
            FirMethodOfAnyImplementedInInterfaceChecker,
            FirDataClassPrimaryConstructorChecker,
            FirPrimaryConstructorSuperTypeChecker,
            FirTypeParametersInObjectChecker,
            FirFunInterfaceDeclarationChecker,
            FirMemberFunctionsChecker,
            FirMemberPropertiesChecker,
            FirNestedClassChecker,
            FirInlineClassDeclarationChecker,
        )

    override val constructorCheckers: Set<FirConstructorChecker>
        get() = setOf(
            FirConstructorAllowedChecker,
        )

    override val fileCheckers: Set<FirFileChecker>
        get() = setOf(
            FirKClassWithIncorrectTypeArgumentChecker,
            FirTopLevelFunctionsChecker,
            FirTopLevelPropertiesChecker,
        )

    override val controlFlowAnalyserCheckers: Set<FirControlFlowChecker>
        get() = setOf(
            FirCallsEffectAnalyzer,
            FirReturnsImpliesAnalyzer,
        )

    override val variableAssignmentCfaBasedCheckers: Set<AbstractFirPropertyInitializationChecker>
        get() = setOf(
            FirPropertyInitializationAnalyzer,
        )

    override val typeParameterCheckers: Set<FirTypeParameterChecker>
        get() = setOf(
            FirTypeParameterBoundsChecker,
            FirTypeParameterVarianceChecker,
            FirReifiedTypeParameterChecker,
            FirTypeParameterSyntaxChecker,
        )
}
