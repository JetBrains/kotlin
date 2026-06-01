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
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.crv.FirReturnValueAnnotationsChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.crv.FirReturnValueOverrideChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.crv.FirUnusedReturnValueChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extra.FirUnusedExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.*

object CommonDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker> = [
        FirModifierChecker,
        FirConflictsDeclarationChecker.Regular,
        FirConflictsDeclarationChecker.ForExpectClass,
        FirTypeConstraintsChecker,
        FirReservedUnderscoreDeclarationChecker,
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
        FirUnusedExpressionChecker,
        FirUnusedReturnValueChecker,
        FirReturnValueAnnotationsChecker,
        FirIllegalCompanionBlockMemberChecker,
    ]

    override val classLikeCheckers: Set<FirClassLikeChecker> = [
        FirExpectActualClassifiersAreInBetaChecker,
    ]

    override val callableDeclarationCheckers: Set<FirCallableDeclarationChecker> = [
        FirKClassWithIncorrectTypeArgumentChecker,
        FirImplicitNothingReturnTypeChecker,
        FirDynamicReceiverChecker,
        FirExtensionShadowedByMemberChecker.Regular,
        FirExtensionShadowedByMemberChecker.ForExpectDeclaration,
        FirReturnValueOverrideChecker,
        FirImplicitReturnTypeAnnotationMissingDependencyChecker,
        FirCoroutineContextAsContextParameterDeclarationChecker,
        FirCompanionExtensionChecker,
        FirCompanionBlockMemberChecker,
    ]

    override val functionCheckers: Set<FirFunctionChecker> = [
        FirContractChecker,
        FirFunctionParameterChecker,
        FirFunctionReturnChecker,
        FirInlineDeclarationChecker,
        FirNonMemberFunctionsChecker,
        FirSuspendLimitationsChecker,
        FirInfixFunctionDeclarationChecker,
        FirOperatorModifierChecker,
        FirTailrecFunctionChecker,
        FirVersionOverloadsChecker,
    ]

    override val namedFunctionCheckers: Set<FirNamedFunctionChecker> = [
        FirFunctionNameChecker,
        FirFunctionTypeParametersSyntaxChecker,
        FirMemberFunctionsChecker,
        FirInlineBodyNamedFunctionChecker,
        FirDataObjectContentChecker,
        ContractSyntaxV2FunctionChecker,
        FirAnyDeprecationChecker,
    ]

    override val propertyCheckers: Set<FirPropertyChecker> = [
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
    ]

    override val backingFieldCheckers: Set<FirBackingFieldChecker> = [
        FirExplicitBackingFieldForbiddenChecker,
        FirExplicitBackingFieldsUnsupportedChecker,
    ]

    override val classCheckers: Set<FirClassChecker> = [
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
        FirAnnotationClassInheritanceChecker,
        FirMultipleDefaultsInheritedFromSupertypesChecker.Regular,
        FirMultipleDefaultsInheritedFromSupertypesChecker.ForExpectClass,
        FirPropertyInitializationChecker,
        FirCompanionBlockChecker,
    ]

    override val regularClassCheckers: Set<FirRegularClassChecker> = [
        FirAnnotationClassDeclarationChecker,
        FirOptInAnnotationClassChecker,
        FirOperatorOfChecker,
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
    ]

    override val constructorCheckers: Set<FirConstructorChecker> = [
        FirConstructorAllowedChecker,
        FirMissingConstructorKeywordSyntaxChecker,
    ]

    override val fileCheckers: Set<FirFileChecker> = [
        FirImportsChecker,
        FirOptInImportsChecker,
        FirUnresolvedInMiddleOfImportChecker,
        FirTooLargeFunctionImportChecker,
        FirTopLevelPropertiesChecker,
        FirPackageConflictsWithClassifierChecker,
        PlatformClassMappedToKotlinImportsChecker,
    ]

    override val scriptCheckers: Set<FirScriptChecker> = [
        FirScriptPropertiesChecker,
    ]

    override val controlFlowAnalyserCheckers: Set<FirControlFlowChecker> = [
        FirCallsEffectAnalyzer,
    ]

    override val variableAssignmentCfaBasedCheckers: Set<AbstractFirPropertyInitializationChecker> = [
        FirPropertyInitializationAnalyzer,
    ]

    override val typeParameterCheckers: Set<FirTypeParameterChecker> = [
        FirTypeParameterBoundsChecker.Regular,
        FirTypeParameterBoundsChecker.ForExpectClass,
        FirTypeParameterVarianceChecker,
        FirReifiedTypeParameterChecker,
        FirTypeParameterSyntaxChecker,
    ]

    override val typeAliasCheckers: Set<FirTypeAliasChecker> = [
        FirAnyTypeAliasChecker,
        FirActualTypeAliasChecker,
    ]

    override val anonymousFunctionCheckers: Set<FirAnonymousFunctionChecker> = [
        FirAnonymousFunctionParametersChecker,
        FirAnonymousFunctionTypeParametersChecker,
        FirInlinedLambdaNonSourceAnnotationsChecker,
        FirSuspendAnonymousFunctionChecker,
        FirMissingDependencyClassForLambdaReceiverChecker,
    ]

    override val anonymousInitializerCheckers: Set<FirAnonymousInitializerChecker> = [
        FirAnonymousInitializerInInterfaceChecker
    ]

    override val valueParameterCheckers: Set<FirValueParameterChecker> = [
        FirValueParameterDefaultValueTypeMismatchChecker,
        FirMissingDependencyClassForParameterChecker,
        FirDestructuringParameterChecker,
    ]

    override val enumEntryCheckers: Set<FirEnumEntryChecker> = [
        FirEnumEntriesRedeclarationChecker,
        FirOptInEnumEntryChecker,
    ]
}
