/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.cfa.FirCallsEffectAnalyzer
import org.jetbrains.kotlin.fir.analysis.cfa.FirPropertyInitializationAnalyzer
import org.jetbrains.kotlin.fir.analysis.cfa.FirReturnsImpliesAnalyzer
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnonymousFunctionParametersChecker
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirAnonymousFunctionSyntaxChecker
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirDelegationInInterfaceSyntaxChecker
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirFunctionTypeParametersSyntaxChecker
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirTypeParameterSyntaxChecker

object CommonDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = setOf(
            FirModifierChecker,
            FirConflictsChecker,
            FirProjectionRelationChecker,
            FirTypeConstraintsChecker,
            FirReservedUnderscoreDeclarationChecker,
            FirUpperBoundViolatedDeclarationChecker,
            FirInfixFunctionDeclarationChecker,
            FirExposedVisibilityDeclarationChecker,
            FirCyclicTypeBoundsChecker,
            FirExpectActualDeclarationChecker,
            FirInvalidAndDangerousCharactersChecker
        )

    override val functionCheckers: Set<FirFunctionChecker>
        get() = setOf(
            FirContractChecker,
            FirFunctionParameterChecker,
            FirInlineDeclarationChecker,
            FirFunctionReturnChecker,
        )

    override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker>
        get() = setOf(
            FirFunctionNameChecker,
            FirFunctionTypeParametersSyntaxChecker,
            FirOperatorModifierChecker,
        )

    override val propertyCheckers: Set<FirPropertyChecker>
        get() = setOf(
            FirInapplicableLateinitChecker,
            FirDestructuringDeclarationChecker,
            FirConstPropertyChecker,
            FirPropertyAccessorsTypesChecker,
            FirPropertyTypeParametersChecker,
            FirInitializerTypeMismatchChecker,
            FirDelegatedPropertyChecker,
            FirInlinePropertyChecker,
            FirPropertyFromParameterChecker,
        )

    override val classCheckers: Set<FirClassChecker>
        get() = setOf(
            FirOverrideChecker,
            FirNotImplementedOverrideChecker,
            FirThrowableSubclassChecker,
            FirOpenMemberChecker,
            FirClassVarianceChecker,
            FirSealedSupertypeChecker,
            FirMemberFunctionsChecker,
            FirMemberPropertiesChecker,
            FirImplementationMismatchChecker,
        )

    override val regularClassCheckers: Set<FirRegularClassChecker>
        get() = setOf(
            FirAnnotationClassDeclarationChecker,
            FirOptInAnnotationClassChecker,
            FirCommonConstructorDelegationIssuesChecker,
            FirConstructorInInterfaceChecker,
            FirDelegationSuperCallInEnumConstructorChecker,
            FirDelegationInInterfaceSyntaxChecker,
            FirEnumClassSimpleChecker,
            FirSupertypesChecker,
            FirLocalEntityNotAllowedChecker,
            FirManyCompanionObjectsChecker,
            FirMethodOfAnyImplementedInInterfaceChecker,
            FirDataClassPrimaryConstructorChecker,
            FirPrimaryConstructorSuperTypeChecker,
            FirTypeParametersInObjectChecker,
            FirFunInterfaceDeclarationChecker,
            FirNestedClassChecker,
            FirInlineClassDeclarationChecker,
            FirOuterClassArgumentsRequiredChecker,
            FirPropertyInitializationChecker,
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
            FirImportsChecker,
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

    override val annotatedDeclarationCheckers: Set<FirAnnotatedDeclarationChecker>
        get() = setOf(
            FirAnnotationChecker,
            FirPublishedApiChecker,
        )

    override val typeAliasCheckers: Set<FirTypeAliasChecker>
        get() = setOf(
            FirTopLevelTypeAliasChecker,
        )

    override val anonymousFunctionCheckers: Set<FirAnonymousFunctionChecker>
        get() = setOf(
            FirAnonymousFunctionParametersChecker,
            FirAnonymousFunctionSyntaxChecker,
        )

    override val anonymousInitializerCheckers: Set<FirAnonymousInitializerChecker>
        get() = setOf(
            FirAnonymousInitializerInInterfaceChecker
        )
}
