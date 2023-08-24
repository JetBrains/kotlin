/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.cfa.FirCallsEffectAnalyzer
import org.jetbrains.kotlin.fir.analysis.cfa.FirPropertyInitializationAnalyzer
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnonymousFunctionParametersChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFiniteBoundRestrictionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirInlinedLambdaNonSourceAnnotationsChecker
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.*

object CommonDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = setOf(
            FirModifierChecker,
            FirConflictsDeclarationChecker,
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
            FirExpectConsistencyChecker,
            FirOptionalExpectationDeclarationChecker
        )

    override val callableDeclarationCheckers: Set<FirCallableDeclarationChecker>
        get() = setOf(
            FirKClassWithIncorrectTypeArgumentChecker,
            FirImplicitNothingReturnTypeChecker,
            FirDynamicReceiverChecker,
            FirActualCallableDeclarationChecker,
        )

    override val functionCheckers: Set<FirFunctionChecker>
        get() = setOf(
            FirContractChecker,
            FirFunctionParameterChecker,
            FirFunctionReturnChecker,
            FirInlineDeclarationChecker,
            FirNonMemberFunctionsChecker,
            FirSuspendLimitationsChecker,
        )

    override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker>
        get() = setOf(
            FirFunctionNameChecker,
            FirFunctionTypeParametersSyntaxChecker,
            FirOperatorModifierChecker,
            FirTailrecFunctionChecker,
            FirMemberFunctionsChecker,
            FirDataObjectContentChecker,
            ContractSyntaxV2FunctionChecker,
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
            FirPropertyFromParameterChecker,
            FirLocalVariableTypeParametersSyntaxChecker,
            FirDelegateUsesExtensionPropertyTypeParameterChecker,
            FirTopLevelPropertiesChecker,
            FirLocalExtensionPropertyChecker,
            ContractSyntaxV2PropertyChecker,
            FirVolatileAnnotationChecker,
            FirInlinePropertyChecker,
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
            FirNotImplementedOverrideSimpleEnumEntryChecker,
            FirThrowableSubclassChecker,
            FirOpenMemberChecker,
            FirClassVarianceChecker,
            FirSealedSupertypeChecker,
            FirMemberPropertiesChecker,
            FirImplementationMismatchChecker,
            FirTypeParametersInObjectChecker,
            FirSupertypesChecker,
            FirPrimaryConstructorSuperTypeChecker,
            FirDynamicSupertypeChecker,
            FirEnumCompanionInEnumConstructorCallChecker,
            FirBadInheritedJavaSignaturesChecker,
            FirSealedInterfaceAllowedChecker,
            FirMixedFunctionalTypesInSupertypesChecker,
        )

    override val regularClassCheckers: Set<FirRegularClassChecker>
        get() = setOf(
            FirAnnotationClassDeclarationChecker,
            FirOptInAnnotationClassChecker,
            FirCommonConstructorDelegationIssuesChecker,
            FirDelegationSuperCallInEnumConstructorChecker,
            FirDelegationInInterfaceSyntaxChecker,
            FirEnumClassSimpleChecker,
            FirLocalEntityNotAllowedChecker,
            FirManyCompanionObjectsChecker,
            FirMethodOfAnyImplementedInInterfaceChecker,
            FirDataClassPrimaryConstructorChecker,
            FirFunInterfaceDeclarationChecker,
            FirNestedClassChecker,
            FirValueClassDeclarationChecker,
            FirOuterClassArgumentsRequiredChecker,
            FirPropertyInitializationChecker,
            FirDelegateFieldTypeMismatchChecker,
            FirFiniteBoundRestrictionChecker,
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
            FirActualTypeAliasChecker,
            FirActualTypealiasToSpecialAnnotationChecker,
            FirTypeAliasExpandsToArrayOfNothingsChecker,
        )

    override val anonymousFunctionCheckers: Set<FirAnonymousFunctionChecker>
        get() = setOf(
            FirAnonymousFunctionParametersChecker,
            FirInlinedLambdaNonSourceAnnotationsChecker,
            FirAnonymousFunctionSyntaxChecker,
        )

    override val anonymousInitializerCheckers: Set<FirAnonymousInitializerChecker>
        get() = setOf(
            FirAnonymousInitializerInInterfaceChecker
        )

    override val valueParameterCheckers: Set<FirValueParameterChecker>
        get() = setOf(
            FirValueParameterDefaultValueTypeMismatchChecker,
        )
}
