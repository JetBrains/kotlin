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
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.*

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
            FirAmbiguousAnonymousTypeChecker,
            FirExplicitApiDeclarationChecker,
            FirAnnotationChecker,
            FirPublishedApiChecker,
            FirOptInMarkedDeclarationChecker,
        )

    override val callableDeclarationCheckers: Set<FirCallableDeclarationChecker>
        get() = setOf(
            FirKClassWithIncorrectTypeArgumentChecker,
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
            FirTailrecFunctionChecker,
            FirTopLevelFunctionsChecker,
            FirMemberFunctionsChecker,
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
            FirPropertyFieldTypeChecker,
            FirInlinePropertyChecker,
            FirPropertyFromParameterChecker,
            FirLocalVariableTypeParametersSyntaxChecker,
            FirDelegateUsesExtensionPropertyTypeParameterChecker,
            FirTopLevelPropertiesChecker,
        )

    override val backingFieldCheckers: Set<FirBackingFieldChecker>
        get() = setOf(
            FirExplicitBackingFieldForbiddenChecker,
            FirExplicitBackingFieldsUnsupportedChecker,
        )

    override val classCheckers: Set<FirClassChecker>
        get() = setOf(
            FirOverrideChecker,
            FirNotImplementedOverrideChecker,
            FirThrowableSubclassChecker,
            FirOpenMemberChecker,
            FirClassVarianceChecker,
            FirSealedSupertypeChecker,
            FirMemberPropertiesChecker,
            FirImplementationMismatchChecker,
            FirTypeParametersInObjectChecker,
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
            FirImportsChecker,
            FirUnresolvedInMiddleOfImportChecker,
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

    override val enumEntryCheckers: Set<FirEnumEntryChecker>
        get() = setOf(
            FirEnumCompanionInEnumConstructorCallChecker,
        )
}
