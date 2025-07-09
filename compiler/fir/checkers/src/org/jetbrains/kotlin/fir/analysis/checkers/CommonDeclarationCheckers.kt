/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.cfa.FirCallsEffectAnalyzer
import org.jetbrains.kotlin.fir.analysis.cfa.FirPropertyInitializationAnalyzer
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.*

object CommonDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker> = setOf(
        FirModifierChecker,
        FirConflictsDeclarationChecker,
        FirTypeConstraintsChecker,
        FirReservedUnderscoreDeclarationChecker,
        FirUpperBoundViolatedDeclarationChecker,
        FirExposedVisibilityDeclarationChecker,
        FirCyclicTypeBoundsChecker,
        FirExpectActualDeclarationChecker,
        FirExpectRefinementChecker,
        FirRequiresOptInOnExpectChecker,
        FirAmbiguousAnonymousTypeChecker,
        FirExplicitApiDeclarationChecker,
        FirAnnotationChecker,
        FirPublishedApiChecker,
        FirContextReceiversDeprecatedDeclarationChecker,
        FirOptInMarkedDeclarationChecker,
        FirExpectConsistencyChecker,
        FirOptionalExpectationDeclarationChecker,
        FirMissingDependencySupertypeInDeclarationsChecker,
        FirContextParametersDeclarationChecker,
        FirUnusedReturnValueChecker,
        FirReturnValueAnnotationsChecker,
    )

    override val classLikeCheckers: Set<FirClassLikeChecker> = setOf(
        FirExpectActualClassifiersAreInBetaChecker,
    )

    override val callableDeclarationCheckers: Set<FirCallableDeclarationChecker> = setOf(
        FirKClassWithIncorrectTypeArgumentChecker,
        FirImplicitNothingReturnTypeChecker,
        FirDynamicReceiverChecker,
        FirExtensionShadowedByMemberChecker.Regular,
        FirExtensionShadowedByMemberChecker.ForExpectDeclaration,
    )

    override val functionCheckers: Set<FirFunctionChecker> = setOf(
        FirContractChecker,
        FirFunctionParameterChecker,
        FirFunctionReturnChecker,
        FirInlineDeclarationChecker,
        FirNonMemberFunctionsChecker,
        FirSuspendLimitationsChecker,
        FirInfixFunctionDeclarationChecker,
        FirOperatorModifierChecker,
        FirTailrecFunctionChecker,
    )

    override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker> = setOf(
        FirFunctionNameChecker,
        FirFunctionTypeParametersSyntaxChecker,
        FirMemberFunctionsChecker,
        FirInlineBodySimpleFunctionChecker,
        FirDataObjectContentChecker,
        ContractSyntaxV2FunctionChecker,
        FirAnyDeprecationChecker,
    )

    override val propertyCheckers: Set<FirPropertyChecker> = setOf(
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
        FirLocalExtensionPropertyChecker,
        ContractSyntaxV2PropertyChecker,
        FirVolatileAnnotationChecker,
        FirInlinePropertyChecker,
        FirUnnamedPropertyChecker,
        FirContextualPropertyWithBackingFieldChecker,
    )

    override val backingFieldCheckers: Set<FirBackingFieldChecker> = setOf(
        FirExplicitBackingFieldForbiddenChecker,
        FirExplicitBackingFieldsUnsupportedChecker,
    )

    override val classCheckers: Set<FirClassChecker> = setOf(
        FirOverrideChecker.Regular,
        FirOverrideChecker.ForExpectClass,
        FirNotImplementedOverrideChecker,
        FirNotImplementedOverrideSimpleEnumEntryChecker.Regular,
        FirNotImplementedOverrideSimpleEnumEntryChecker.ForExpectClass,
        FirThrowableSubclassChecker,
        FirOpenMemberChecker,
        FirClassVarianceChecker,
        FirSealedSupertypeChecker,
        FirMemberPropertiesChecker,
        FirImplementationMismatchChecker.Regular,
        FirImplementationMismatchChecker.ForExpectClass,
        FirTypeParametersInObjectChecker,
        FirSupertypesChecker,
        FirPrimaryConstructorSuperTypeChecker,
        FirDynamicSupertypeChecker,
        FirDataClassConsistentDataCopyAnnotationChecker,
        FirEnumCompanionInEnumConstructorCallChecker,
        FirBadInheritedJavaSignaturesChecker,
        FirSealedInterfaceAllowedChecker,
        FirMixedFunctionalTypesInSupertypesChecker.Regular,
        FirMixedFunctionalTypesInSupertypesChecker.ForExpectClass,
        FirDelegateFieldTypeMismatchChecker,
        FirMultipleDefaultsInheritedFromSupertypesChecker.Regular,
        FirMultipleDefaultsInheritedFromSupertypesChecker.ForExpectClass,
        FirPropertyInitializationChecker,
    )

    override val regularClassCheckers: Set<FirRegularClassChecker> = setOf(
        FirAnnotationClassDeclarationChecker,
        FirOptInAnnotationClassChecker,
        FirCommonConstructorDelegationIssuesChecker,
        FirDelegationSuperCallInEnumConstructorChecker,
        FirDelegationInExpectClassSyntaxChecker,
        FirDelegationInInterfaceSyntaxChecker,
        FirEnumClassSimpleChecker,
        FirLocalEntityNotAllowedChecker,
        FirInlineBodyRegularClassChecker,
        FirManyCompanionObjectsChecker,
        FirMethodOfAnyImplementedInInterfaceChecker,
        FirDataClassPrimaryConstructorChecker,
        FirDataClassNonPublicConstructorChecker,
        FirFunInterfaceDeclarationChecker.Regular,
        FirFunInterfaceDeclarationChecker.ForExpectClass,
        FirNestedClassChecker,
        FirValueClassDeclarationChecker.Regular,
        FirValueClassDeclarationChecker.ForExpectClass,
        FirOuterClassArgumentsRequiredChecker,
        FirFiniteBoundRestrictionChecker,
        FirNonExpansiveInheritanceRestrictionChecker,
        FirObjectConstructorChecker,
        FirInlineClassDeclarationChecker,
        FirEnumEntryInitializationChecker,
    )

    override val constructorCheckers: Set<FirConstructorChecker> = setOf(
        FirConstructorAllowedChecker,
        FirMissingConstructorKeywordSyntaxChecker,
    )

    override val fileCheckers: Set<FirFileChecker> = setOf(
        FirImportsChecker,
        FirOptInImportsChecker,
        FirUnresolvedInMiddleOfImportChecker,
        FirTopLevelPropertiesChecker,
        FirPackageConflictsWithClassifierChecker,
    )

    override val scriptCheckers: Set<FirScriptChecker> = setOf(
        FirScriptPropertiesChecker,
    )

    override val controlFlowAnalyserCheckers: Set<FirControlFlowChecker> = setOf(
        FirCallsEffectAnalyzer,
    )

    override val variableAssignmentCfaBasedCheckers: Set<AbstractFirPropertyInitializationChecker> = setOf(
        FirPropertyInitializationAnalyzer,
    )

    override val typeParameterCheckers: Set<FirTypeParameterChecker> = setOf(
        FirTypeParameterBoundsChecker.Regular,
        FirTypeParameterBoundsChecker.ForExpectClass,
        FirTypeParameterVarianceChecker,
        FirReifiedTypeParameterChecker,
        FirTypeParameterSyntaxChecker,
    )

    override val typeAliasCheckers: Set<FirTypeAliasChecker> = setOf(
        FirAnyTypeAliasChecker,
        FirActualTypeAliasChecker,
    )

    override val anonymousFunctionCheckers: Set<FirAnonymousFunctionChecker> = setOf(
        FirAnonymousFunctionParametersChecker,
        FirAnonymousFunctionTypeParametersChecker,
        FirInlinedLambdaNonSourceAnnotationsChecker,
        FirSuspendAnonymousFunctionChecker,
        FirMissingDependencyClassForLambdaReceiverChecker,
    )

    override val anonymousInitializerCheckers: Set<FirAnonymousInitializerChecker> = setOf(
        FirAnonymousInitializerInInterfaceChecker
    )

    override val valueParameterCheckers: Set<FirValueParameterChecker> = setOf(
        FirValueParameterDefaultValueTypeMismatchChecker,
        FirMissingDependencyClassForParameterChecker,
    )

    override val enumEntryCheckers: Set<FirEnumEntryChecker> = setOf(
        FirEnumEntriesRedeclarationChecker,
        FirOptInEnumEntryChecker,
    )
}
