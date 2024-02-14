/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageFeature.ForbidExposingTypesInPrimaryConstructorProperties
import org.jetbrains.kotlin.config.LanguageFeature.ForbidInferringTypeVariablesIntoEmptyIntersection
import org.jetbrains.kotlin.config.LanguageFeature.ForbidUsingExtensionPropertyTypeParameterInDelegate
import org.jetbrains.kotlin.config.LanguageFeature.ModifierNonBuiltinSuspendFunError
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitAllMultipleDefaultsInheritedFromSupertypes
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitAssigningSingleElementsToVarargsInNamedForm
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitConfusingSyntaxInWhenBranches
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitConstructorAndSupertypeOnTypealiasWithTypeProjection
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitCyclesInAnnotations
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitImplementingVarByInheritedVal
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitInlineModifierOnPrimaryConstructorParameters
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitInvisibleAbstractMethodsInSuperclasses
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitNonReifiedArraysAsReifiedTypeArguments
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitScriptTopLevelInnerClasses
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitSingleNamedFunctionAsExpression
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitTypealiasAsCallableQualifierInImport
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitUseSiteTargetAnnotationsOnSuperTypes
import org.jetbrains.kotlin.config.LanguageFeature.RestrictRetentionForExpressionAnnotations
import org.jetbrains.kotlin.config.LanguageFeature.RestrictionOfValReassignmentViaBackingField
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory4
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation3
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation4
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement.Version
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBackingField
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtExpressionWithLabel
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.psi.KtWhenCondition
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.MismatchOrIncompatible
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.types.Variance

/**
 * Generated from: [org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DIAGNOSTICS_LIST]
 */
object FirErrors {
    // Meta-errors
    val UNSUPPORTED: KtDiagnosticFactory1<String> by error1<PsiElement, String>()
    val UNSUPPORTED_FEATURE: KtDiagnosticFactory1<Pair<LanguageFeature, LanguageVersionSettings>> by error1<PsiElement, Pair<LanguageFeature, LanguageVersionSettings>>()
    val UNSUPPORTED_SUSPEND_TEST: KtDiagnosticFactory0 by error0<PsiElement>()
    val NEW_INFERENCE_ERROR: KtDiagnosticFactory1<String> by error1<PsiElement, String>()

    // Miscellaneous
    val OTHER_ERROR: KtDiagnosticFactory0 by error0<PsiElement>()

    // General syntax
    val ILLEGAL_CONST_EXPRESSION: KtDiagnosticFactory0 by error0<PsiElement>()
    val ILLEGAL_UNDERSCORE: KtDiagnosticFactory0 by error0<PsiElement>()
    val EXPRESSION_EXPECTED: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)
    val ASSIGNMENT_IN_EXPRESSION_CONTEXT: KtDiagnosticFactory0 by error0<KtBinaryExpression>()
    val BREAK_OR_CONTINUE_OUTSIDE_A_LOOP: KtDiagnosticFactory0 by error0<PsiElement>()
    val NOT_A_LOOP_LABEL: KtDiagnosticFactory0 by error0<PsiElement>()
    val BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY: KtDiagnosticFactory0 by error0<KtExpressionWithLabel>()
    val VARIABLE_EXPECTED: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)
    val DELEGATION_IN_INTERFACE: KtDiagnosticFactory0 by error0<PsiElement>()
    val DELEGATION_NOT_TO_INTERFACE: KtDiagnosticFactory0 by error0<PsiElement>()
    val NESTED_CLASS_NOT_ALLOWED: KtDiagnosticFactory1<String> by error1<KtNamedDeclaration, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val INCORRECT_CHARACTER_LITERAL: KtDiagnosticFactory0 by error0<PsiElement>()
    val EMPTY_CHARACTER_LITERAL: KtDiagnosticFactory0 by error0<PsiElement>()
    val TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL: KtDiagnosticFactory0 by error0<PsiElement>()
    val ILLEGAL_ESCAPE: KtDiagnosticFactory0 by error0<PsiElement>()
    val INT_LITERAL_OUT_OF_RANGE: KtDiagnosticFactory0 by error0<PsiElement>()
    val FLOAT_LITERAL_OUT_OF_RANGE: KtDiagnosticFactory0 by error0<PsiElement>()
    val WRONG_LONG_SUFFIX: KtDiagnosticFactory0 by error0<KtElement>(SourceElementPositioningStrategies.LONG_LITERAL_SUFFIX)
    val UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH: KtDiagnosticFactory0 by error0<KtElement>()
    val DIVISION_BY_ZERO: KtDiagnosticFactory0 by warning0<KtExpression>()
    val VAL_OR_VAR_ON_LOOP_PARAMETER: KtDiagnosticFactory1<KtKeywordToken> by error1<KtParameter, KtKeywordToken>(SourceElementPositioningStrategies.VAL_OR_VAR_NODE)
    val VAL_OR_VAR_ON_FUN_PARAMETER: KtDiagnosticFactory1<KtKeywordToken> by error1<KtParameter, KtKeywordToken>(SourceElementPositioningStrategies.VAL_OR_VAR_NODE)
    val VAL_OR_VAR_ON_CATCH_PARAMETER: KtDiagnosticFactory1<KtKeywordToken> by error1<KtParameter, KtKeywordToken>(SourceElementPositioningStrategies.VAL_OR_VAR_NODE)
    val VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER: KtDiagnosticFactory1<KtKeywordToken> by error1<KtParameter, KtKeywordToken>(SourceElementPositioningStrategies.VAL_OR_VAR_NODE)
    val INVISIBLE_SETTER: KtDiagnosticFactory3<FirPropertySymbol, Visibility, CallableId> by error3<PsiElement, FirPropertySymbol, Visibility, CallableId>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)
    val INNER_ON_TOP_LEVEL_SCRIPT_CLASS: KtDiagnosticFactoryForDeprecation0 by deprecationError0<PsiElement>(ProhibitScriptTopLevelInnerClasses)
    val ERROR_SUPPRESSION: KtDiagnosticFactory1<String> by warning1<PsiElement, String>()
    val MISSING_CONSTRUCTOR_KEYWORD: KtDiagnosticFactory0 by error0<PsiElement>()

    // Unresolved
    val INVISIBLE_REFERENCE: KtDiagnosticFactory3<FirBasedSymbol<*>, Visibility, ClassId?> by error3<PsiElement, FirBasedSymbol<*>, Visibility, ClassId?>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val UNRESOLVED_REFERENCE: KtDiagnosticFactory2<String, String?> by error2<PsiElement, String, String?>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val UNRESOLVED_LABEL: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.LABEL)
    val DESERIALIZATION_ERROR: KtDiagnosticFactory0 by error0<PsiElement>()
    val ERROR_FROM_JAVA_RESOLUTION: KtDiagnosticFactory0 by error0<PsiElement>()
    val MISSING_STDLIB_CLASS: KtDiagnosticFactory0 by error0<PsiElement>()
    val NO_THIS: KtDiagnosticFactory0 by error0<PsiElement>()
    val DEPRECATION_ERROR: KtDiagnosticFactory2<FirBasedSymbol<*>, String> by error2<PsiElement, FirBasedSymbol<*>, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DEPRECATION: KtDiagnosticFactory2<FirBasedSymbol<*>, String> by warning2<PsiElement, FirBasedSymbol<*>, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val VERSION_REQUIREMENT_DEPRECATION_ERROR: KtDiagnosticFactory4<FirBasedSymbol<*>, Version, String, String> by error4<PsiElement, FirBasedSymbol<*>, Version, String, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val VERSION_REQUIREMENT_DEPRECATION: KtDiagnosticFactory4<FirBasedSymbol<*>, Version, String, String> by warning4<PsiElement, FirBasedSymbol<*>, Version, String, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val TYPEALIAS_EXPANSION_DEPRECATION_ERROR: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> by error3<PsiElement, FirBasedSymbol<*>, FirBasedSymbol<*>, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val TYPEALIAS_EXPANSION_DEPRECATION: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> by warning3<PsiElement, FirBasedSymbol<*>, FirBasedSymbol<*>, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val API_NOT_AVAILABLE: KtDiagnosticFactory2<ApiVersion, ApiVersion> by error2<PsiElement, ApiVersion, ApiVersion>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)
    val UNRESOLVED_REFERENCE_WRONG_RECEIVER: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> by error1<PsiElement, Collection<FirBasedSymbol<*>>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val UNRESOLVED_IMPORT: KtDiagnosticFactory1<String> by error1<PsiElement, String>(SourceElementPositioningStrategies.IMPORT_LAST_NAME)
    val DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE: KtDiagnosticFactory0 by error0<PsiElement>()
    val MISSING_DEPENDENCY_CLASS: KtDiagnosticFactory1<ConeKotlinType> by error1<PsiElement, ConeKotlinType>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val MISSING_DEPENDENCY_SUPERCLASS: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by error2<PsiElement, ConeKotlinType, ConeKotlinType>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER: KtDiagnosticFactory1<ConeKotlinType> by warning1<PsiElement, ConeKotlinType>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)

    // Call resolution
    val CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS: KtDiagnosticFactory0 by error0<KtExpression>()
    val NO_CONSTRUCTOR: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.VALUE_ARGUMENTS_LIST)
    val FUNCTION_CALL_EXPECTED: KtDiagnosticFactory2<String, Boolean> by error2<PsiElement, String, Boolean>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val ILLEGAL_SELECTOR: KtDiagnosticFactory0 by error0<PsiElement>()
    val NO_RECEIVER_ALLOWED: KtDiagnosticFactory0 by error0<PsiElement>()
    val FUNCTION_EXPECTED: KtDiagnosticFactory2<String, ConeKotlinType> by error2<PsiElement, String, ConeKotlinType>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val INTERFACE_AS_FUNCTION: KtDiagnosticFactory1<FirRegularClassSymbol> by error1<PsiElement, FirRegularClassSymbol>()
    val EXPECT_CLASS_AS_FUNCTION: KtDiagnosticFactory1<FirRegularClassSymbol> by error1<PsiElement, FirRegularClassSymbol>()
    val INNER_CLASS_CONSTRUCTOR_NO_RECEIVER: KtDiagnosticFactory1<FirRegularClassSymbol> by error1<PsiElement, FirRegularClassSymbol>()
    val RESOLUTION_TO_CLASSIFIER: KtDiagnosticFactory1<FirRegularClassSymbol> by error1<PsiElement, FirRegularClassSymbol>()
    val AMBIGUOUS_ALTERED_ASSIGN: KtDiagnosticFactory1<List<String?>> by error1<PsiElement, List<String?>>()
    val FORBIDDEN_BINARY_MOD: KtDiagnosticFactory2<FirBasedSymbol<*>, String> by error2<PsiElement, FirBasedSymbol<*>, String>(SourceElementPositioningStrategies.OPERATOR_MODIFIER)
    val DEPRECATED_BINARY_MOD: KtDiagnosticFactory2<FirBasedSymbol<*>, String> by error2<PsiElement, FirBasedSymbol<*>, String>(SourceElementPositioningStrategies.OPERATOR_MODIFIER)

    // Super
    val SUPER_IS_NOT_AN_EXPRESSION: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val SUPER_NOT_AVAILABLE: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val ABSTRACT_SUPER_CALL: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val ABSTRACT_SUPER_CALL_WARNING: KtDiagnosticFactory0 by warning0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val INSTANCE_ACCESS_BEFORE_SUPER_CALL: KtDiagnosticFactory1<String> by error1<PsiElement, String>()
    val SUPER_CALL_WITH_DEFAULT_PARAMETERS: KtDiagnosticFactory1<String> by error1<PsiElement, String>()

    // Supertypes
    val NOT_A_SUPERTYPE: KtDiagnosticFactory0 by error0<PsiElement>()
    val TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER: KtDiagnosticFactory0 by warning0<KtElement>()
    val SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE: KtDiagnosticFactory0 by error0<PsiElement>()
    val QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE: KtDiagnosticFactory1<FirBasedSymbol<*>> by error1<KtTypeReference, FirBasedSymbol<*>>()
    val SUPERTYPE_INITIALIZED_IN_INTERFACE: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val INTERFACE_WITH_SUPERCLASS: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val FINAL_SUPERTYPE: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val CLASS_CANNOT_BE_EXTENDED_DIRECTLY: KtDiagnosticFactory1<FirRegularClassSymbol> by error1<KtTypeReference, FirRegularClassSymbol>()
    val SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val SINGLETON_IN_SUPERTYPE: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val NULLABLE_SUPERTYPE: KtDiagnosticFactory0 by error0<KtTypeReference>(SourceElementPositioningStrategies.QUESTION_MARK_BY_TYPE)
    val MANY_CLASSES_IN_SUPERTYPE_LIST: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val SUPERTYPE_APPEARS_TWICE: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val CLASS_IN_SUPERTYPE_FOR_ENUM: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val SEALED_SUPERTYPE: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val SEALED_SUPERTYPE_IN_LOCAL_CLASS: KtDiagnosticFactory2<String, ClassKind> by error2<KtTypeReference, String, ClassKind>()
    val SEALED_INHERITOR_IN_DIFFERENT_PACKAGE: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val SEALED_INHERITOR_IN_DIFFERENT_MODULE: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val CLASS_INHERITS_JAVA_SEALED_CLASS: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val UNSUPPORTED_SEALED_FUN_INTERFACE: KtDiagnosticFactory0 by error0<PsiElement>()
    val SUPERTYPE_NOT_A_CLASS_OR_INTERFACE: KtDiagnosticFactory1<String> by error1<KtElement, String>()
    val UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION: KtDiagnosticFactory1<FirBasedSymbol<*>> by error1<PsiElement, FirBasedSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val CYCLIC_INHERITANCE_HIERARCHY: KtDiagnosticFactory0 by error0<PsiElement>()
    val EXPANDED_TYPE_CANNOT_BE_INHERITED: KtDiagnosticFactory1<ConeKotlinType> by error1<KtTypeReference, ConeKotlinType>()
    val PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE: KtDiagnosticFactory0 by error0<KtModifierListOwner>(SourceElementPositioningStrategies.VARIANCE_MODIFIER)
    val INCONSISTENT_TYPE_PARAMETER_VALUES: KtDiagnosticFactory3<FirTypeParameterSymbol, FirRegularClassSymbol, Collection<ConeKotlinType>> by error3<KtClass, FirTypeParameterSymbol, FirRegularClassSymbol, Collection<ConeKotlinType>>(SourceElementPositioningStrategies.SUPERTYPES_LIST)
    val INCONSISTENT_TYPE_PARAMETER_BOUNDS: KtDiagnosticFactory3<FirTypeParameterSymbol, FirRegularClassSymbol, Collection<ConeKotlinType>> by error3<PsiElement, FirTypeParameterSymbol, FirRegularClassSymbol, Collection<ConeKotlinType>>()
    val AMBIGUOUS_SUPER: KtDiagnosticFactory1<List<ConeKotlinType>> by error1<KtSuperExpression, List<ConeKotlinType>>()

    // Constructor problems
    val CONSTRUCTOR_IN_OBJECT: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val CONSTRUCTOR_IN_INTERFACE: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val NON_PRIVATE_CONSTRUCTOR_IN_ENUM: KtDiagnosticFactory0 by error0<PsiElement>()
    val NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED: KtDiagnosticFactory0 by error0<PsiElement>()
    val CYCLIC_CONSTRUCTOR_DELEGATION_CALL: KtDiagnosticFactory0 by error0<PsiElement>()
    val PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)
    val PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL: KtDiagnosticFactory1<FirBasedSymbol<*>> by error1<KtExpression, FirBasedSymbol<*>>()
    val SUPERTYPE_NOT_INITIALIZED: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR: KtDiagnosticFactory0 by error0<PsiElement>()
    val DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR: KtDiagnosticFactory0 by error0<PsiElement>()
    val EXPLICIT_DELEGATION_CALL_REQUIRED: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)
    val SEALED_CLASS_CONSTRUCTOR_CALL: KtDiagnosticFactory0 by error0<PsiElement>()
    val DATA_CLASS_WITHOUT_PARAMETERS: KtDiagnosticFactory0 by error0<KtNamedDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val DATA_CLASS_VARARG_PARAMETER: KtDiagnosticFactory0 by error0<KtParameter>()
    val DATA_CLASS_NOT_PROPERTY_PARAMETER: KtDiagnosticFactory0 by error0<KtParameter>()

    // Annotations
    val ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR: KtDiagnosticFactory0 by error0<KtExpression>()
    val ANNOTATION_ARGUMENT_MUST_BE_CONST: KtDiagnosticFactory0 by error0<KtExpression>()
    val ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST: KtDiagnosticFactory0 by error0<KtExpression>()
    val ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL: KtDiagnosticFactory0 by error0<KtExpression>()
    val ANNOTATION_CLASS_MEMBER: KtDiagnosticFactory0 by error0<PsiElement>()
    val ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT: KtDiagnosticFactory0 by error0<KtExpression>()
    val INVALID_TYPE_OF_ANNOTATION_MEMBER: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val LOCAL_ANNOTATION_CLASS_ERROR: KtDiagnosticFactory0 by error0<KtClassOrObject>()
    val MISSING_VAL_ON_ANNOTATION_PARAMETER: KtDiagnosticFactory0 by error0<KtParameter>()
    val NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION: KtDiagnosticFactory0 by error0<KtExpression>()
    val CYCLE_IN_ANNOTATION_PARAMETER: KtDiagnosticFactoryForDeprecation0 by deprecationError0<KtParameter>(ProhibitCyclesInAnnotations)
    val ANNOTATION_CLASS_CONSTRUCTOR_CALL: KtDiagnosticFactory0 by error0<KtCallExpression>()
    val ENUM_CLASS_CONSTRUCTOR_CALL: KtDiagnosticFactory0 by error0<KtCallExpression>()
    val NOT_AN_ANNOTATION_CLASS: KtDiagnosticFactory1<String> by error1<PsiElement, String>()
    val NULLABLE_TYPE_OF_ANNOTATION_MEMBER: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val VAR_ANNOTATION_PARAMETER: KtDiagnosticFactory0 by error0<KtParameter>(SourceElementPositioningStrategies.VAL_OR_VAR_NODE)
    val SUPERTYPES_FOR_ANNOTATION_CLASS: KtDiagnosticFactory0 by error0<KtClass>(SourceElementPositioningStrategies.SUPERTYPES_LIST)
    val ANNOTATION_USED_AS_ANNOTATION_ARGUMENT: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val ILLEGAL_KOTLIN_VERSION_STRING_VALUE: KtDiagnosticFactory0 by error0<KtExpression>()
    val NEWER_VERSION_IN_SINCE_KOTLIN: KtDiagnosticFactory1<String> by warning1<KtExpression, String>()
    val DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS: KtDiagnosticFactory0 by error0<PsiElement>()
    val DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS: KtDiagnosticFactory0 by error0<PsiElement>()
    val DEPRECATED_SINCE_KOTLIN_WITHOUT_DEPRECATED: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val OVERRIDE_DEPRECATION: KtDiagnosticFactory2<FirBasedSymbol<*>, DeprecationInfo> by warning2<KtNamedDeclaration, FirBasedSymbol<*>, DeprecationInfo>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val ANNOTATION_ON_SUPERCLASS: KtDiagnosticFactoryForDeprecation0 by deprecationError0<KtAnnotationEntry>(ProhibitUseSiteTargetAnnotationsOnSuperTypes)
    val RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION: KtDiagnosticFactoryForDeprecation0 by deprecationError0<PsiElement>(RestrictRetentionForExpressionAnnotations)
    val WRONG_ANNOTATION_TARGET: KtDiagnosticFactory1<String> by error1<KtAnnotationEntry, String>()
    val WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET: KtDiagnosticFactory2<String, String> by error2<KtAnnotationEntry, String, String>()
    val INAPPLICABLE_TARGET_ON_PROPERTY: KtDiagnosticFactory1<String> by error1<KtAnnotationEntry, String>()
    val INAPPLICABLE_TARGET_ON_PROPERTY_WARNING: KtDiagnosticFactory1<String> by warning1<KtAnnotationEntry, String>()
    val INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE: KtDiagnosticFactory1<String> by error1<KtAnnotationEntry, String>()
    val INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val INAPPLICABLE_PARAM_TARGET: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val REDUNDANT_ANNOTATION_TARGET: KtDiagnosticFactory1<String> by warning1<KtAnnotationEntry, String>()
    val INAPPLICABLE_FILE_TARGET: KtDiagnosticFactory0 by error0<KtAnnotationEntry>(SourceElementPositioningStrategies.ANNOTATION_USE_SITE)
    val REPEATED_ANNOTATION: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val REPEATED_ANNOTATION_WARNING: KtDiagnosticFactory0 by warning0<KtAnnotationEntry>()
    val NOT_A_CLASS: KtDiagnosticFactory0 by error0<PsiElement>()
    val WRONG_EXTENSION_FUNCTION_TYPE: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val WRONG_EXTENSION_FUNCTION_TYPE_WARNING: KtDiagnosticFactory0 by warning0<KtAnnotationEntry>()
    val ANNOTATION_IN_WHERE_CLAUSE_ERROR: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val COMPILER_REQUIRED_ANNOTATION_AMBIGUITY: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by error2<PsiElement, ConeKotlinType, ConeKotlinType>()
    val AMBIGUOUS_ANNOTATION_ARGUMENT: KtDiagnosticFactory1<List<FirBasedSymbol<*>>> by error1<PsiElement, List<FirBasedSymbol<*>>>()
    val VOLATILE_ON_VALUE: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val VOLATILE_ON_DELEGATE: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val POTENTIALLY_NON_REPORTED_ANNOTATION: KtDiagnosticFactory0 by warning0<KtAnnotationEntry>()

    // OptIn
    val OPT_IN_USAGE: KtDiagnosticFactory2<ClassId, String> by warning2<PsiElement, ClassId, String>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val OPT_IN_USAGE_ERROR: KtDiagnosticFactory2<ClassId, String> by error2<PsiElement, ClassId, String>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val OPT_IN_OVERRIDE: KtDiagnosticFactory2<ClassId, String> by warning2<PsiElement, ClassId, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val OPT_IN_OVERRIDE_ERROR: KtDiagnosticFactory2<ClassId, String> by error2<PsiElement, ClassId, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val OPT_IN_IS_NOT_ENABLED: KtDiagnosticFactory0 by warning0<KtAnnotationEntry>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION: KtDiagnosticFactory0 by error0<PsiElement>()
    val OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN: KtDiagnosticFactory0 by error0<PsiElement>()
    val OPT_IN_WITHOUT_ARGUMENTS: KtDiagnosticFactory0 by warning0<KtAnnotationEntry>()
    val OPT_IN_ARGUMENT_IS_NOT_MARKER: KtDiagnosticFactory1<ClassId> by warning1<KtAnnotationEntry, ClassId>()
    val OPT_IN_MARKER_WITH_WRONG_TARGET: KtDiagnosticFactory1<String> by error1<KtAnnotationEntry, String>()
    val OPT_IN_MARKER_WITH_WRONG_RETENTION: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val OPT_IN_MARKER_ON_WRONG_TARGET: KtDiagnosticFactory1<String> by error1<KtAnnotationEntry, String>()
    val OPT_IN_MARKER_ON_OVERRIDE: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val OPT_IN_MARKER_ON_OVERRIDE_WARNING: KtDiagnosticFactory0 by warning0<KtAnnotationEntry>()
    val SUBCLASS_OPT_IN_INAPPLICABLE: KtDiagnosticFactory1<String> by error1<KtAnnotationEntry, String>()

    // Exposed visibility
    val EXPOSED_TYPEALIAS_EXPANDED_TYPE: KtDiagnosticFactory3<EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility> by error3<KtNamedDeclaration, EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val EXPOSED_FUNCTION_RETURN_TYPE: KtDiagnosticFactory3<EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility> by error3<KtNamedDeclaration, EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val EXPOSED_RECEIVER_TYPE: KtDiagnosticFactory3<EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility> by error3<KtTypeReference, EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility>()
    val EXPOSED_PROPERTY_TYPE: KtDiagnosticFactory3<EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility> by error3<KtNamedDeclaration, EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR: KtDiagnosticFactoryForDeprecation3<EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility> by deprecationError3<KtNamedDeclaration, EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility>(ForbidExposingTypesInPrimaryConstructorProperties, SourceElementPositioningStrategies.DECLARATION_NAME)
    val EXPOSED_PARAMETER_TYPE: KtDiagnosticFactory3<EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility> by error3<KtParameter, EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility>()
    val EXPOSED_SUPER_INTERFACE: KtDiagnosticFactory3<EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility> by error3<KtTypeReference, EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility>()
    val EXPOSED_SUPER_CLASS: KtDiagnosticFactory3<EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility> by error3<KtTypeReference, EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility>()
    val EXPOSED_TYPE_PARAMETER_BOUND: KtDiagnosticFactory3<EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility> by error3<KtTypeReference, EffectiveVisibility, FirBasedSymbol<*>, EffectiveVisibility>()

    // Modifiers
    val INAPPLICABLE_INFIX_MODIFIER: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.INFIX_MODIFIER)
    val REPEATED_MODIFIER: KtDiagnosticFactory1<KtModifierKeywordToken> by error1<PsiElement, KtModifierKeywordToken>()
    val REDUNDANT_MODIFIER: KtDiagnosticFactory2<KtModifierKeywordToken, KtModifierKeywordToken> by warning2<PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()
    val DEPRECATED_MODIFIER: KtDiagnosticFactory2<KtModifierKeywordToken, KtModifierKeywordToken> by error2<PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()
    val DEPRECATED_MODIFIER_PAIR: KtDiagnosticFactory2<KtModifierKeywordToken, KtModifierKeywordToken> by warning2<PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()
    val DEPRECATED_MODIFIER_FOR_TARGET: KtDiagnosticFactory2<KtModifierKeywordToken, String> by warning2<PsiElement, KtModifierKeywordToken, String>()
    val REDUNDANT_MODIFIER_FOR_TARGET: KtDiagnosticFactory2<KtModifierKeywordToken, String> by warning2<PsiElement, KtModifierKeywordToken, String>()
    val INCOMPATIBLE_MODIFIERS: KtDiagnosticFactory2<KtModifierKeywordToken, KtModifierKeywordToken> by error2<PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()
    val REDUNDANT_OPEN_IN_INTERFACE: KtDiagnosticFactory0 by warning0<KtModifierListOwner>(SourceElementPositioningStrategies.OPEN_MODIFIER)
    val WRONG_MODIFIER_TARGET: KtDiagnosticFactory2<KtModifierKeywordToken, String> by error2<PsiElement, KtModifierKeywordToken, String>()
    val OPERATOR_MODIFIER_REQUIRED: KtDiagnosticFactory2<FirNamedFunctionSymbol, String> by error2<PsiElement, FirNamedFunctionSymbol, String>()
    val OPERATOR_CALL_ON_CONSTRUCTOR: KtDiagnosticFactory1<String> by error1<PsiElement, String>()
    val INFIX_MODIFIER_REQUIRED: KtDiagnosticFactory1<FirNamedFunctionSymbol> by error1<PsiElement, FirNamedFunctionSymbol>()
    val WRONG_MODIFIER_CONTAINING_DECLARATION: KtDiagnosticFactory2<KtModifierKeywordToken, String> by error2<PsiElement, KtModifierKeywordToken, String>()
    val DEPRECATED_MODIFIER_CONTAINING_DECLARATION: KtDiagnosticFactory2<KtModifierKeywordToken, String> by warning2<PsiElement, KtModifierKeywordToken, String>()
    val INAPPLICABLE_OPERATOR_MODIFIER: KtDiagnosticFactory1<String> by error1<PsiElement, String>(SourceElementPositioningStrategies.OPERATOR_MODIFIER)
    val NO_EXPLICIT_VISIBILITY_IN_API_MODE: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_START_TO_NAME)
    val NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING: KtDiagnosticFactory0 by warning0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_START_TO_NAME)
    val NO_EXPLICIT_RETURN_TYPE_IN_API_MODE: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING: KtDiagnosticFactory0 by warning0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val ANONYMOUS_SUSPEND_FUNCTION: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.SUSPEND_MODIFIER)

    // Value classes
    val VALUE_CLASS_NOT_TOP_LEVEL: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER)
    val VALUE_CLASS_NOT_FINAL: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.MODALITY_MODIFIER)
    val ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER)
    val INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE: KtDiagnosticFactory0 by error0<KtElement>()
    val VALUE_CLASS_EMPTY_CONSTRUCTOR: KtDiagnosticFactory0 by error0<KtElement>()
    val VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER: KtDiagnosticFactory0 by error0<KtParameter>()
    val PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS: KtDiagnosticFactory0 by error0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val DELEGATED_PROPERTY_INSIDE_VALUE_CLASS: KtDiagnosticFactory0 by error0<PsiElement>()
    val VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE: KtDiagnosticFactory1<ConeKotlinType> by error1<KtTypeReference, ConeKotlinType>()
    val VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION: KtDiagnosticFactory0 by error0<PsiElement>()
    val VALUE_CLASS_CANNOT_EXTEND_CLASSES: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val VALUE_CLASS_CANNOT_BE_RECURSIVE: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val MULTI_FIELD_VALUE_CLASS_PRIMARY_CONSTRUCTOR_DEFAULT_PARAMETER: KtDiagnosticFactory0 by error0<KtExpression>()
    val SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS: KtDiagnosticFactory0 by error0<PsiElement>()
    val RESERVED_MEMBER_INSIDE_VALUE_CLASS: KtDiagnosticFactory1<String> by error1<KtFunction, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val TYPE_ARGUMENT_ON_TYPED_VALUE_CLASS_EQUALS: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val INNER_CLASS_INSIDE_VALUE_CLASS: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.INNER_MODIFIER)
    val VALUE_CLASS_CANNOT_BE_CLONEABLE: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER)
    val VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS: KtDiagnosticFactory0 by error0<KtDeclaration>()
    val ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET: KtDiagnosticFactory1<String> by error1<KtAnnotationEntry, String>()

    // Applicability
    val NONE_APPLICABLE: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> by error1<PsiElement, Collection<FirBasedSymbol<*>>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val INAPPLICABLE_CANDIDATE: KtDiagnosticFactory1<FirBasedSymbol<*>> by error1<PsiElement, FirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val TYPE_MISMATCH: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, Boolean> by error3<PsiElement, ConeKotlinType, ConeKotlinType, Boolean>()
    val TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR: KtDiagnosticFactory1<FirTypeParameterSymbol> by error1<PsiElement, FirTypeParameterSymbol>()
    val THROWABLE_TYPE_MISMATCH: KtDiagnosticFactory2<ConeKotlinType, Boolean> by error2<PsiElement, ConeKotlinType, Boolean>()
    val CONDITION_TYPE_MISMATCH: KtDiagnosticFactory2<ConeKotlinType, Boolean> by error2<PsiElement, ConeKotlinType, Boolean>()
    val ARGUMENT_TYPE_MISMATCH: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, Boolean> by error3<PsiElement, ConeKotlinType, ConeKotlinType, Boolean>()
    val NULL_FOR_NONNULL_TYPE: KtDiagnosticFactory1<ConeKotlinType> by error1<PsiElement, ConeKotlinType>()
    val INAPPLICABLE_LATEINIT_MODIFIER: KtDiagnosticFactory1<String> by error1<KtModifierListOwner, String>(SourceElementPositioningStrategies.LATEINIT_MODIFIER)
    val VARARG_OUTSIDE_PARENTHESES: KtDiagnosticFactory0 by error0<KtElement>()
    val NAMED_ARGUMENTS_NOT_ALLOWED: KtDiagnosticFactory1<ForbiddenNamedArgumentsTarget> by error1<KtValueArgument, ForbiddenNamedArgumentsTarget>(SourceElementPositioningStrategies.NAME_OF_NAMED_ARGUMENT)
    val NON_VARARG_SPREAD: KtDiagnosticFactory0 by error0<LeafPsiElement>()
    val ARGUMENT_PASSED_TWICE: KtDiagnosticFactory0 by error0<KtValueArgument>(SourceElementPositioningStrategies.NAME_OF_NAMED_ARGUMENT)
    val TOO_MANY_ARGUMENTS: KtDiagnosticFactory1<FirCallableSymbol<*>> by error1<PsiElement, FirCallableSymbol<*>>()
    val NO_VALUE_FOR_PARAMETER: KtDiagnosticFactory1<FirValueParameterSymbol> by error1<KtElement, FirValueParameterSymbol>(SourceElementPositioningStrategies.VALUE_ARGUMENTS)
    val NAMED_PARAMETER_NOT_FOUND: KtDiagnosticFactory1<String> by error1<KtValueArgument, String>(SourceElementPositioningStrategies.NAME_OF_NAMED_ARGUMENT)
    val NAME_FOR_AMBIGUOUS_PARAMETER: KtDiagnosticFactory0 by error0<KtValueArgument>(SourceElementPositioningStrategies.NAME_OF_NAMED_ARGUMENT)
    val ASSIGNMENT_TYPE_MISMATCH: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, Boolean> by error3<KtExpression, ConeKotlinType, ConeKotlinType, Boolean>()
    val RESULT_TYPE_MISMATCH: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by error2<KtExpression, ConeKotlinType, ConeKotlinType>()
    val MANY_LAMBDA_EXPRESSION_ARGUMENTS: KtDiagnosticFactory0 by error0<KtValueArgument>()
    val NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER: KtDiagnosticFactory1<String> by error1<KtElement, String>()
    val SPREAD_OF_NULLABLE: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.SPREAD_OPERATOR)
    val ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION: KtDiagnosticFactoryForDeprecation1<ConeKotlinType> by deprecationError1<KtExpression, ConeKotlinType>(ProhibitAssigningSingleElementsToVarargsInNamedForm)
    val ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION: KtDiagnosticFactoryForDeprecation0 by deprecationError0<KtExpression>(ProhibitAssigningSingleElementsToVarargsInNamedForm)
    val REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION: KtDiagnosticFactory0 by warning0<KtExpression>()
    val REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION: KtDiagnosticFactory0 by warning0<KtExpression>()
    val INFERENCE_UNSUCCESSFUL_FORK: KtDiagnosticFactory1<String> by error1<PsiElement, String>()
    val NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE: KtDiagnosticFactory1<FirClassLikeSymbol<*>> by error1<PsiElement, FirClassLikeSymbol<*>>()

    // Ambiguity
    val OVERLOAD_RESOLUTION_AMBIGUITY: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> by error1<PsiElement, Collection<FirBasedSymbol<*>>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val ASSIGN_OPERATOR_AMBIGUITY: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> by error1<PsiElement, Collection<FirBasedSymbol<*>>>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val ITERATOR_AMBIGUITY: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> by error1<PsiElement, Collection<FirBasedSymbol<*>>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val HAS_NEXT_FUNCTION_AMBIGUITY: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> by error1<PsiElement, Collection<FirBasedSymbol<*>>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val NEXT_AMBIGUITY: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> by error1<PsiElement, Collection<FirBasedSymbol<*>>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val AMBIGUOUS_FUNCTION_TYPE_KIND: KtDiagnosticFactory1<Collection<FunctionTypeKind>> by error1<PsiElement, Collection<FunctionTypeKind>>()

    // Context receivers resolution
    val NO_CONTEXT_RECEIVER: KtDiagnosticFactory1<ConeKotlinType> by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val MULTIPLE_ARGUMENTS_APPLICABLE_FOR_CONTEXT_RECEIVER: KtDiagnosticFactory1<ConeKotlinType> by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val AMBIGUOUS_CALL_WITH_IMPLICIT_CONTEXT_RECEIVER: KtDiagnosticFactory0 by error0<KtElement>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL: KtDiagnosticFactory0 by error0<KtElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val SUBTYPING_BETWEEN_CONTEXT_RECEIVERS: KtDiagnosticFactory0 by error0<KtElement>()
    val CONTEXT_RECEIVERS_WITH_BACKING_FIELD: KtDiagnosticFactory0 by error0<KtElement>()

    // Types & type parameters
    val RECURSION_IN_IMPLICIT_TYPES: KtDiagnosticFactory0 by error0<PsiElement>()
    val INFERENCE_ERROR: KtDiagnosticFactory0 by error0<PsiElement>()
    val PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT: KtDiagnosticFactory0 by error0<PsiElement>()
    val UPPER_BOUND_VIOLATED: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, String> by error3<PsiElement, ConeKotlinType, ConeKotlinType, String>()
    val UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by error2<PsiElement, ConeKotlinType, ConeKotlinType>()
    val TYPE_ARGUMENTS_NOT_ALLOWED: KtDiagnosticFactory1<String> by error1<PsiElement, String>()
    val TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED: KtDiagnosticFactory0 by error0<PsiElement>()
    val WRONG_NUMBER_OF_TYPE_ARGUMENTS: KtDiagnosticFactory2<Int, FirClassLikeSymbol<*>> by error2<PsiElement, Int, FirClassLikeSymbol<*>>()
    val NO_TYPE_ARGUMENTS_ON_RHS: KtDiagnosticFactory2<Int, FirClassLikeSymbol<*>> by error2<PsiElement, Int, FirClassLikeSymbol<*>>()
    val OUTER_CLASS_ARGUMENTS_REQUIRED: KtDiagnosticFactory1<FirClassLikeSymbol<*>> by error1<PsiElement, FirClassLikeSymbol<*>>()
    val TYPE_PARAMETERS_IN_OBJECT: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.TYPE_PARAMETERS_LIST)
    val TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.TYPE_PARAMETERS_LIST)
    val ILLEGAL_PROJECTION_USAGE: KtDiagnosticFactory0 by error0<PsiElement>()
    val TYPE_PARAMETERS_IN_ENUM: KtDiagnosticFactory0 by error0<PsiElement>()
    val CONFLICTING_PROJECTION: KtDiagnosticFactory1<ConeKotlinType> by error1<KtTypeProjection, ConeKotlinType>(SourceElementPositioningStrategies.VARIANCE_MODIFIER)
    val CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION: KtDiagnosticFactory1<ConeKotlinType> by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.VARIANCE_MODIFIER)
    val REDUNDANT_PROJECTION: KtDiagnosticFactory1<ConeKotlinType> by warning1<KtTypeProjection, ConeKotlinType>(SourceElementPositioningStrategies.VARIANCE_MODIFIER)
    val VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED: KtDiagnosticFactory0 by error0<KtTypeParameter>(SourceElementPositioningStrategies.VARIANCE_MODIFIER)
    val CATCH_PARAMETER_WITH_DEFAULT_VALUE: KtDiagnosticFactory0 by error0<PsiElement>()
    val REIFIED_TYPE_IN_CATCH_CLAUSE: KtDiagnosticFactory0 by error0<PsiElement>()
    val TYPE_PARAMETER_IN_CATCH_CLAUSE: KtDiagnosticFactory0 by error0<PsiElement>()
    val GENERIC_THROWABLE_SUBCLASS: KtDiagnosticFactory0 by error0<KtTypeParameter>()
    val INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS: KtDiagnosticFactory0 by error0<KtClassOrObject>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE: KtDiagnosticFactory1<FirTypeParameterSymbol> by error1<KtNamedDeclaration, FirTypeParameterSymbol>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val TYPE_PARAMETER_AS_REIFIED: KtDiagnosticFactory1<FirTypeParameterSymbol> by error1<PsiElement, FirTypeParameterSymbol>()
    val TYPE_PARAMETER_AS_REIFIED_ARRAY: KtDiagnosticFactoryForDeprecation1<FirTypeParameterSymbol> by deprecationError1<PsiElement, FirTypeParameterSymbol>(ProhibitNonReifiedArraysAsReifiedTypeArguments)
    val REIFIED_TYPE_FORBIDDEN_SUBSTITUTION: KtDiagnosticFactory1<ConeKotlinType> by error1<PsiElement, ConeKotlinType>()
    val DEFINITELY_NON_NULLABLE_AS_REIFIED: KtDiagnosticFactory0 by error0<PsiElement>()
    val FINAL_UPPER_BOUND: KtDiagnosticFactory1<ConeKotlinType> by warning1<KtTypeReference, ConeKotlinType>()
    val UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER: KtDiagnosticFactory0 by error0<KtElement>()
    val ONLY_ONE_CLASS_BOUND_ALLOWED: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val REPEATED_BOUND: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val CONFLICTING_UPPER_BOUNDS: KtDiagnosticFactory1<FirTypeParameterSymbol> by error1<KtNamedDeclaration, FirTypeParameterSymbol>()
    val NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER: KtDiagnosticFactory2<Name, FirBasedSymbol<*>> by error2<KtSimpleNameExpression, Name, FirBasedSymbol<*>>()
    val BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val REIFIED_TYPE_PARAMETER_NO_INLINE: KtDiagnosticFactory0 by error0<KtTypeParameter>(SourceElementPositioningStrategies.REIFIED_MODIFIER)
    val TYPE_PARAMETERS_NOT_ALLOWED: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.TYPE_PARAMETERS_LIST)
    val TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER: KtDiagnosticFactory0 by error0<KtTypeParameter>()
    val RETURN_TYPE_MISMATCH: KtDiagnosticFactory4<ConeKotlinType, ConeKotlinType, FirFunction, Boolean> by error4<KtExpression, ConeKotlinType, ConeKotlinType, FirFunction, Boolean>(SourceElementPositioningStrategies.WHOLE_ELEMENT)
    val IMPLICIT_NOTHING_RETURN_TYPE: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val IMPLICIT_NOTHING_PROPERTY_TYPE: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val ABBREVIATED_NOTHING_RETURN_TYPE: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val ABBREVIATED_NOTHING_PROPERTY_TYPE: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val CYCLIC_GENERIC_UPPER_BOUND: KtDiagnosticFactory0 by error0<PsiElement>()
    val FINITE_BOUNDS_VIOLATION: KtDiagnosticFactory0 by error0<PsiElement>()
    val FINITE_BOUNDS_VIOLATION_IN_JAVA: KtDiagnosticFactory1<List<FirBasedSymbol<*>>> by warning1<PsiElement, List<FirBasedSymbol<*>>>()
    val EXPANSIVE_INHERITANCE: KtDiagnosticFactory0 by error0<PsiElement>()
    val EXPANSIVE_INHERITANCE_IN_JAVA: KtDiagnosticFactory1<List<FirBasedSymbol<*>>> by warning1<PsiElement, List<FirBasedSymbol<*>>>()
    val DEPRECATED_TYPE_PARAMETER_SYNTAX: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.TYPE_PARAMETERS_LIST)
    val MISPLACED_TYPE_PARAMETER_CONSTRAINTS: KtDiagnosticFactory0 by warning0<KtTypeParameter>()
    val DYNAMIC_SUPERTYPE: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val DYNAMIC_UPPER_BOUND: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val DYNAMIC_RECEIVER_NOT_ALLOWED: KtDiagnosticFactory0 by error0<KtElement>()
    val DYNAMIC_RECEIVER_EXPECTED_BUT_WAS_NON_DYNAMIC: KtDiagnosticFactory1<ConeKotlinType> by error1<PsiElement, ConeKotlinType>()
    val INCOMPATIBLE_TYPES: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by error2<KtElement, ConeKotlinType, ConeKotlinType>()
    val INCOMPATIBLE_TYPES_WARNING: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by warning2<KtElement, ConeKotlinType, ConeKotlinType>()
    val TYPE_VARIANCE_CONFLICT_ERROR: KtDiagnosticFactory4<FirTypeParameterSymbol, Variance, Variance, ConeKotlinType> by error4<PsiElement, FirTypeParameterSymbol, Variance, Variance, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE: KtDiagnosticFactory4<FirTypeParameterSymbol, Variance, Variance, ConeKotlinType> by error4<PsiElement, FirTypeParameterSymbol, Variance, Variance, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val SMARTCAST_IMPOSSIBLE: KtDiagnosticFactory4<ConeKotlinType, FirExpression, String, Boolean> by error4<KtExpression, ConeKotlinType, FirExpression, String, Boolean>()
    val REDUNDANT_NULLABLE: KtDiagnosticFactory0 by warning0<KtTypeReference>(SourceElementPositioningStrategies.REDUNDANT_NULLABLE)
    val PLATFORM_CLASS_MAPPED_TO_KOTLIN: KtDiagnosticFactory1<ClassId> by warning1<PsiElement, ClassId>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION: KtDiagnosticFactoryForDeprecation4<String, Collection<ConeKotlinType>, String, String> by deprecationError4<PsiElement, String, Collection<ConeKotlinType>, String, String>(ForbidInferringTypeVariablesIntoEmptyIntersection)
    val INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION: KtDiagnosticFactory4<String, Collection<ConeKotlinType>, String, String> by warning4<PsiElement, String, Collection<ConeKotlinType>, String, String>()
    val INCORRECT_LEFT_COMPONENT_OF_INTERSECTION: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val INCORRECT_RIGHT_COMPONENT_OF_INTERSECTION: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val NULLABLE_ON_DEFINITELY_NOT_NULLABLE: KtDiagnosticFactory0 by error0<KtTypeReference>()

    // Reflection
    val EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED: KtDiagnosticFactory1<FirCallableSymbol<*>> by error1<KtExpression, FirCallableSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val CALLABLE_REFERENCE_LHS_NOT_A_CLASS: KtDiagnosticFactory0 by error0<KtExpression>()
    val CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR: KtDiagnosticFactory0 by error0<KtExpression>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE: KtDiagnosticFactory0 by error0<KtExpression>()
    val CLASS_LITERAL_LHS_NOT_A_CLASS: KtDiagnosticFactory0 by error0<KtExpression>()
    val NULLABLE_TYPE_IN_CLASS_LITERAL_LHS: KtDiagnosticFactory0 by error0<KtExpression>()
    val EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS: KtDiagnosticFactory1<ConeKotlinType> by error1<PsiElement, ConeKotlinType>()
    val UNSUPPORTED_CLASS_LITERALS_WITH_EMPTY_LHS: KtDiagnosticFactory0 by error0<KtElement>()
    val MUTABLE_PROPERTY_WITH_CAPTURED_TYPE: KtDiagnosticFactory0 by warning0<PsiElement>()

    // overrides
    val NOTHING_TO_OVERRIDE: KtDiagnosticFactory1<FirCallableSymbol<*>> by error1<KtModifierListOwner, FirCallableSymbol<*>>(SourceElementPositioningStrategies.OVERRIDE_MODIFIER)
    val CANNOT_OVERRIDE_INVISIBLE_MEMBER: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by error2<KtNamedDeclaration, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.OVERRIDE_MODIFIER)
    val DATA_CLASS_OVERRIDE_CONFLICT: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by error2<KtClassOrObject, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DATA_MODIFIER)
    val DATA_CLASS_OVERRIDE_DEFAULT_VALUES: KtDiagnosticFactory2<FirCallableSymbol<*>, FirClassSymbol<*>> by error2<KtElement, FirCallableSymbol<*>, FirClassSymbol<*>>(SourceElementPositioningStrategies.DATA_MODIFIER)
    val CANNOT_WEAKEN_ACCESS_PRIVILEGE: KtDiagnosticFactory3<Visibility, FirCallableSymbol<*>, Name> by error3<KtModifierListOwner, Visibility, FirCallableSymbol<*>, Name>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val CANNOT_CHANGE_ACCESS_PRIVILEGE: KtDiagnosticFactory3<Visibility, FirCallableSymbol<*>, Name> by error3<KtModifierListOwner, Visibility, FirCallableSymbol<*>, Name>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val CANNOT_INFER_VISIBILITY: KtDiagnosticFactory1<FirCallableSymbol<*>> by error1<KtDeclaration, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES: KtDiagnosticFactory3<Name, FirValueParameterSymbol, List<FirCallableSymbol<*>>> by error3<KtElement, Name, FirValueParameterSymbol, List<FirCallableSymbol<*>>>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE: KtDiagnosticFactory3<Name, FirValueParameterSymbol, List<FirCallableSymbol<*>>> by error3<KtElement, Name, FirValueParameterSymbol, List<FirCallableSymbol<*>>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_DEPRECATION: KtDiagnosticFactoryForDeprecation3<Name, FirValueParameterSymbol, List<FirCallableSymbol<*>>> by deprecationError3<KtElement, Name, FirValueParameterSymbol, List<FirCallableSymbol<*>>>(ProhibitAllMultipleDefaultsInheritedFromSupertypes, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE_DEPRECATION: KtDiagnosticFactoryForDeprecation3<Name, FirValueParameterSymbol, List<FirCallableSymbol<*>>> by deprecationError3<KtElement, Name, FirValueParameterSymbol, List<FirCallableSymbol<*>>>(ProhibitAllMultipleDefaultsInheritedFromSupertypes, SourceElementPositioningStrategies.DECLARATION_NAME)
    val TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS: KtDiagnosticFactory1<ConeKotlinType> by error1<KtElement, ConeKotlinType>()
    val OVERRIDING_FINAL_MEMBER: KtDiagnosticFactory2<FirCallableSymbol<*>, Name> by error2<KtNamedDeclaration, FirCallableSymbol<*>, Name>(SourceElementPositioningStrategies.OVERRIDE_MODIFIER)
    val RETURN_TYPE_MISMATCH_ON_INHERITANCE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by error2<KtClassOrObject, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val PROPERTY_TYPE_MISMATCH_ON_INHERITANCE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by error2<KtClassOrObject, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val VAR_TYPE_MISMATCH_ON_INHERITANCE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by error2<KtClassOrObject, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val RETURN_TYPE_MISMATCH_BY_DELEGATION: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by error2<KtClassOrObject, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val PROPERTY_TYPE_MISMATCH_BY_DELEGATION: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by error2<KtClassOrObject, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by error2<KtClassOrObject, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val CONFLICTING_INHERITED_MEMBERS: KtDiagnosticFactory2<FirClassSymbol<*>, List<FirCallableSymbol<*>>> by error2<KtClassOrObject, FirClassSymbol<*>, List<FirCallableSymbol<*>>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val ABSTRACT_MEMBER_NOT_IMPLEMENTED: KtDiagnosticFactory2<FirClassSymbol<*>, FirCallableSymbol<*>> by error2<KtClassOrObject, FirClassSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val ABSTRACT_MEMBER_NOT_IMPLEMENTED_BY_ENUM_ENTRY: KtDiagnosticFactory2<FirEnumEntrySymbol, List<FirCallableSymbol<*>>> by error2<KtEnumEntry, FirEnumEntrySymbol, List<FirCallableSymbol<*>>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED: KtDiagnosticFactory2<FirClassSymbol<*>, FirCallableSymbol<*>> by error2<KtClassOrObject, FirClassSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER: KtDiagnosticFactoryForDeprecation2<FirClassSymbol<*>, FirCallableSymbol<*>> by deprecationError2<KtClassOrObject, FirClassSymbol<*>, FirCallableSymbol<*>>(ProhibitInvisibleAbstractMethodsInSuperclasses, SourceElementPositioningStrategies.DECLARATION_NAME)
    val AMBIGUOUS_ANONYMOUS_TYPE_INFERRED: KtDiagnosticFactory1<Collection<ConeKotlinType>> by error1<KtDeclaration, Collection<ConeKotlinType>>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val MANY_IMPL_MEMBER_NOT_IMPLEMENTED: KtDiagnosticFactory2<FirClassSymbol<*>, FirCallableSymbol<*>> by error2<KtClassOrObject, FirClassSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED: KtDiagnosticFactory2<FirClassSymbol<*>, FirCallableSymbol<*>> by error2<KtClassOrObject, FirClassSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val OVERRIDING_FINAL_MEMBER_BY_DELEGATION: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by error2<KtClassOrObject, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by warning2<KtClassOrObject, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val RETURN_TYPE_MISMATCH_ON_OVERRIDE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by error2<KtNamedDeclaration, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_RETURN_TYPE)
    val PROPERTY_TYPE_MISMATCH_ON_OVERRIDE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by error2<KtNamedDeclaration, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_RETURN_TYPE)
    val VAR_TYPE_MISMATCH_ON_OVERRIDE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by error2<KtNamedDeclaration, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_RETURN_TYPE)
    val VAR_OVERRIDDEN_BY_VAL: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by error2<KtNamedDeclaration, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.VAL_OR_VAR_NODE)
    val VAR_IMPLEMENTED_BY_INHERITED_VAL: KtDiagnosticFactoryForDeprecation3<FirClassSymbol<*>, FirCallableSymbol<*>, FirCallableSymbol<*>> by deprecationError3<KtNamedDeclaration, FirClassSymbol<*>, FirCallableSymbol<*>, FirCallableSymbol<*>>(ProhibitImplementingVarByInheritedVal, SourceElementPositioningStrategies.DECLARATION_NAME)
    val NON_FINAL_MEMBER_IN_FINAL_CLASS: KtDiagnosticFactory0 by warning0<KtNamedDeclaration>(SourceElementPositioningStrategies.OPEN_MODIFIER)
    val NON_FINAL_MEMBER_IN_OBJECT: KtDiagnosticFactory0 by warning0<KtNamedDeclaration>(SourceElementPositioningStrategies.OPEN_MODIFIER)
    val VIRTUAL_MEMBER_HIDDEN: KtDiagnosticFactory2<FirCallableSymbol<*>, FirRegularClassSymbol> by error2<KtNamedDeclaration, FirCallableSymbol<*>, FirRegularClassSymbol>(SourceElementPositioningStrategies.DECLARATION_NAME)

    // Redeclarations
    val MANY_COMPANION_OBJECTS: KtDiagnosticFactory0 by error0<KtObjectDeclaration>(SourceElementPositioningStrategies.COMPANION_OBJECT)
    val CONFLICTING_OVERLOADS: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> by error1<PsiElement, Collection<FirBasedSymbol<*>>>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val REDECLARATION: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> by error1<KtNamedDeclaration, Collection<FirBasedSymbol<*>>>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val PACKAGE_OR_CLASSIFIER_REDECLARATION: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> by error1<KtNamedDeclaration, Collection<FirBasedSymbol<*>>>(SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME)
    val EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE: KtDiagnosticFactory1<FirBasedSymbol<*>> by error1<KtNamedDeclaration, FirBasedSymbol<*>>(SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME)
    val METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE: KtDiagnosticFactory0 by error0<PsiElement>()

    // Invalid local declarations
    val LOCAL_OBJECT_NOT_ALLOWED: KtDiagnosticFactory1<Name> by error1<KtNamedDeclaration, Name>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val LOCAL_INTERFACE_NOT_ALLOWED: KtDiagnosticFactory1<Name> by error1<KtNamedDeclaration, Name>(SourceElementPositioningStrategies.DECLARATION_NAME)

    // Functions
    val ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS: KtDiagnosticFactory2<FirCallableSymbol<*>, FirClassSymbol<*>> by error2<KtFunction, FirCallableSymbol<*>, FirClassSymbol<*>>(SourceElementPositioningStrategies.MODALITY_MODIFIER)
    val ABSTRACT_FUNCTION_WITH_BODY: KtDiagnosticFactory1<FirCallableSymbol<*>> by error1<KtFunction, FirCallableSymbol<*>>(SourceElementPositioningStrategies.MODALITY_MODIFIER)
    val NON_ABSTRACT_FUNCTION_WITH_NO_BODY: KtDiagnosticFactory1<FirCallableSymbol<*>> by error1<KtFunction, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val PRIVATE_FUNCTION_WITH_NO_BODY: KtDiagnosticFactory1<FirCallableSymbol<*>> by error1<KtFunction, FirCallableSymbol<*>>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val NON_MEMBER_FUNCTION_NO_BODY: KtDiagnosticFactory1<FirCallableSymbol<*>> by error1<KtFunction, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val FUNCTION_DECLARATION_WITH_NO_NAME: KtDiagnosticFactory0 by error0<KtFunction>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val ANONYMOUS_FUNCTION_WITH_NAME: KtDiagnosticFactory0 by error0<KtFunction>()
    val SINGLE_ANONYMOUS_FUNCTION_WITH_NAME: KtDiagnosticFactoryForDeprecation0 by deprecationError0<KtFunction>(ProhibitSingleNamedFunctionAsExpression)
    val ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE: KtDiagnosticFactory0 by error0<KtParameter>(SourceElementPositioningStrategies.PARAMETER_DEFAULT_VALUE)
    val USELESS_VARARG_ON_PARAMETER: KtDiagnosticFactory0 by warning0<KtParameter>()
    val MULTIPLE_VARARG_PARAMETERS: KtDiagnosticFactory0 by error0<KtParameter>(SourceElementPositioningStrategies.PARAMETER_VARARG_MODIFIER)
    val FORBIDDEN_VARARG_PARAMETER_TYPE: KtDiagnosticFactory1<ConeKotlinType> by error1<KtParameter, ConeKotlinType>(SourceElementPositioningStrategies.PARAMETER_VARARG_MODIFIER)
    val VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION: KtDiagnosticFactory0 by error0<KtParameter>()
    val CANNOT_INFER_PARAMETER_TYPE: KtDiagnosticFactory0 by error0<KtElement>()
    val NO_TAIL_CALLS_FOUND: KtDiagnosticFactory0 by warning0<KtNamedFunction>(SourceElementPositioningStrategies.TAILREC_MODIFIER)
    val TAILREC_ON_VIRTUAL_MEMBER_ERROR: KtDiagnosticFactory0 by error0<KtNamedFunction>(SourceElementPositioningStrategies.TAILREC_MODIFIER)
    val NON_TAIL_RECURSIVE_CALL: KtDiagnosticFactory0 by warning0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED: KtDiagnosticFactory0 by warning0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DATA_OBJECT_CUSTOM_EQUALS_OR_HASH_CODE: KtDiagnosticFactory0 by error0<KtNamedFunction>(SourceElementPositioningStrategies.OVERRIDE_MODIFIER)

    // Parameter default values
    val DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE: KtDiagnosticFactory0 by error0<KtElement>()

    // Fun interfaces
    val FUN_INTERFACE_CONSTRUCTOR_REFERENCE: KtDiagnosticFactory0 by error0<KtExpression>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS: KtDiagnosticFactory0 by error0<KtClass>(SourceElementPositioningStrategies.FUN_MODIFIER)
    val FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.FUN_INTERFACE)
    val FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.FUN_INTERFACE)
    val FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.FUN_INTERFACE)
    val FUN_INTERFACE_WITH_SUSPEND_FUNCTION: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.FUN_INTERFACE)

    // Properties & accessors
    val ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS: KtDiagnosticFactory2<FirCallableSymbol<*>, FirClassSymbol<*>> by error2<KtModifierListOwner, FirCallableSymbol<*>, FirClassSymbol<*>>(SourceElementPositioningStrategies.MODALITY_MODIFIER)
    val PRIVATE_PROPERTY_IN_INTERFACE: KtDiagnosticFactory0 by error0<KtProperty>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val ABSTRACT_PROPERTY_WITH_INITIALIZER: KtDiagnosticFactory0 by error0<KtExpression>()
    val PROPERTY_INITIALIZER_IN_INTERFACE: KtDiagnosticFactory0 by error0<KtExpression>()
    val PROPERTY_WITH_NO_TYPE_NO_INITIALIZER: KtDiagnosticFactory0 by error0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val MUST_BE_INITIALIZED: KtDiagnosticFactory0 by error0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val MUST_BE_INITIALIZED_WARNING: KtDiagnosticFactory0 by warning0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val MUST_BE_INITIALIZED_OR_BE_FINAL: KtDiagnosticFactory0 by error0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val MUST_BE_INITIALIZED_OR_BE_FINAL_WARNING: KtDiagnosticFactory0 by warning0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val MUST_BE_INITIALIZED_OR_BE_ABSTRACT: KtDiagnosticFactory0 by error0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val MUST_BE_INITIALIZED_OR_BE_ABSTRACT_WARNING: KtDiagnosticFactory0 by warning0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT: KtDiagnosticFactory0 by error0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT_WARNING: KtDiagnosticFactory0 by warning0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT: KtDiagnosticFactory0 by error0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val UNNECESSARY_LATEINIT: KtDiagnosticFactory0 by warning0<KtProperty>(SourceElementPositioningStrategies.LATEINIT_MODIFIER)
    val BACKING_FIELD_IN_INTERFACE: KtDiagnosticFactory0 by error0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val EXTENSION_PROPERTY_WITH_BACKING_FIELD: KtDiagnosticFactory0 by error0<KtExpression>()
    val PROPERTY_INITIALIZER_NO_BACKING_FIELD: KtDiagnosticFactory0 by error0<KtExpression>()
    val ABSTRACT_DELEGATED_PROPERTY: KtDiagnosticFactory0 by error0<KtExpression>()
    val DELEGATED_PROPERTY_IN_INTERFACE: KtDiagnosticFactory0 by error0<KtExpression>()
    val ABSTRACT_PROPERTY_WITH_GETTER: KtDiagnosticFactory0 by error0<KtPropertyAccessor>()
    val ABSTRACT_PROPERTY_WITH_SETTER: KtDiagnosticFactory0 by error0<KtPropertyAccessor>()
    val PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY: KtDiagnosticFactory0 by error0<KtModifierListOwner>(SourceElementPositioningStrategies.PRIVATE_MODIFIER)
    val PRIVATE_SETTER_FOR_OPEN_PROPERTY: KtDiagnosticFactory0 by error0<KtModifierListOwner>(SourceElementPositioningStrategies.PRIVATE_MODIFIER)
    val VAL_WITH_SETTER: KtDiagnosticFactory0 by error0<KtPropertyAccessor>()
    val CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT: KtDiagnosticFactory0 by error0<KtElement>(SourceElementPositioningStrategies.CONST_MODIFIER)
    val CONST_VAL_WITH_GETTER: KtDiagnosticFactory0 by error0<KtElement>()
    val CONST_VAL_WITH_DELEGATE: KtDiagnosticFactory0 by error0<KtExpression>()
    val TYPE_CANT_BE_USED_FOR_CONST_VAL: KtDiagnosticFactory1<ConeKotlinType> by error1<KtProperty, ConeKotlinType>(SourceElementPositioningStrategies.CONST_MODIFIER)
    val CONST_VAL_WITHOUT_INITIALIZER: KtDiagnosticFactory0 by error0<KtProperty>(SourceElementPositioningStrategies.CONST_MODIFIER)
    val CONST_VAL_WITH_NON_CONST_INITIALIZER: KtDiagnosticFactory0 by error0<KtExpression>()
    val WRONG_SETTER_PARAMETER_TYPE: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by error2<KtTypeReference, ConeKotlinType, ConeKotlinType>()
    val DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER: KtDiagnosticFactoryForDeprecation1<FirTypeParameterSymbol> by deprecationError1<KtProperty, FirTypeParameterSymbol>(ForbidUsingExtensionPropertyTypeParameterInDelegate, SourceElementPositioningStrategies.PROPERTY_DELEGATE)
    val INITIALIZER_TYPE_MISMATCH: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, Boolean> by error3<KtNamedDeclaration, ConeKotlinType, ConeKotlinType, Boolean>(SourceElementPositioningStrategies.PROPERTY_INITIALIZER)
    val GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY: KtDiagnosticFactory0 by error0<KtModifierListOwner>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY: KtDiagnosticFactory0 by error0<KtModifierListOwner>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val WRONG_SETTER_RETURN_TYPE: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val WRONG_GETTER_RETURN_TYPE: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by error2<KtTypeReference, ConeKotlinType, ConeKotlinType>()
    val ACCESSOR_FOR_DELEGATED_PROPERTY: KtDiagnosticFactory0 by error0<KtPropertyAccessor>()
    val PROPERTY_INITIALIZER_WITH_EXPLICIT_FIELD_DECLARATION: KtDiagnosticFactory0 by error0<KtExpression>()
    val PROPERTY_FIELD_DECLARATION_MISSING_INITIALIZER: KtDiagnosticFactory0 by error0<KtBackingField>()
    val LATEINIT_PROPERTY_FIELD_DECLARATION_WITH_INITIALIZER: KtDiagnosticFactory0 by error0<KtBackingField>(SourceElementPositioningStrategies.LATEINIT_MODIFIER)
    val LATEINIT_FIELD_IN_VAL_PROPERTY: KtDiagnosticFactory0 by error0<KtBackingField>(SourceElementPositioningStrategies.LATEINIT_MODIFIER)
    val LATEINIT_NULLABLE_BACKING_FIELD: KtDiagnosticFactory0 by error0<KtBackingField>(SourceElementPositioningStrategies.LATEINIT_MODIFIER)
    val BACKING_FIELD_FOR_DELEGATED_PROPERTY: KtDiagnosticFactory0 by error0<KtBackingField>(SourceElementPositioningStrategies.FIELD_KEYWORD)
    val PROPERTY_MUST_HAVE_GETTER: KtDiagnosticFactory0 by error0<KtProperty>()
    val PROPERTY_MUST_HAVE_SETTER: KtDiagnosticFactory0 by error0<KtProperty>()
    val EXPLICIT_BACKING_FIELD_IN_INTERFACE: KtDiagnosticFactory0 by error0<KtBackingField>(SourceElementPositioningStrategies.FIELD_KEYWORD)
    val EXPLICIT_BACKING_FIELD_IN_ABSTRACT_PROPERTY: KtDiagnosticFactory0 by error0<KtBackingField>(SourceElementPositioningStrategies.FIELD_KEYWORD)
    val EXPLICIT_BACKING_FIELD_IN_EXTENSION: KtDiagnosticFactory0 by error0<KtBackingField>(SourceElementPositioningStrategies.FIELD_KEYWORD)
    val REDUNDANT_EXPLICIT_BACKING_FIELD: KtDiagnosticFactory0 by warning0<KtBackingField>(SourceElementPositioningStrategies.FIELD_KEYWORD)
    val ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS: KtDiagnosticFactory0 by error0<KtModifierListOwner>(SourceElementPositioningStrategies.ABSTRACT_MODIFIER)
    val LOCAL_VARIABLE_WITH_TYPE_PARAMETERS_WARNING: KtDiagnosticFactory0 by warning0<KtProperty>(SourceElementPositioningStrategies.TYPE_PARAMETERS_LIST)
    val LOCAL_VARIABLE_WITH_TYPE_PARAMETERS: KtDiagnosticFactory0 by error0<KtProperty>(SourceElementPositioningStrategies.TYPE_PARAMETERS_LIST)
    val EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS: KtDiagnosticFactory1<String> by error1<KtExpression, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val SAFE_CALLABLE_REFERENCE_CALL: KtDiagnosticFactory0 by error0<KtExpression>()
    val LATEINIT_INTRINSIC_CALL_ON_NON_LITERAL: KtDiagnosticFactory0 by error0<PsiElement>()
    val LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT: KtDiagnosticFactory0 by error0<PsiElement>()
    val LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION: KtDiagnosticFactory0 by error0<PsiElement>()
    val LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY: KtDiagnosticFactory1<FirBasedSymbol<*>> by error1<PsiElement, FirBasedSymbol<*>>()
    val LOCAL_EXTENSION_PROPERTY: KtDiagnosticFactory0 by error0<PsiElement>()

    // Multi-platform projects
    val EXPECTED_DECLARATION_WITH_BODY: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL: KtDiagnosticFactory0 by error0<KtConstructorDelegationCall>()
    val EXPECTED_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER: KtDiagnosticFactory0 by error0<KtParameter>()
    val EXPECTED_ENUM_CONSTRUCTOR: KtDiagnosticFactory0 by error0<KtConstructor<*>>()
    val EXPECTED_ENUM_ENTRY_WITH_BODY: KtDiagnosticFactory0 by error0<KtEnumEntry>()
    val EXPECTED_PROPERTY_INITIALIZER: KtDiagnosticFactory0 by error0<KtExpression>()
    val EXPECTED_DELEGATED_PROPERTY: KtDiagnosticFactory0 by error0<KtExpression>()
    val EXPECTED_LATEINIT_PROPERTY: KtDiagnosticFactory0 by error0<KtModifierListOwner>(SourceElementPositioningStrategies.LATEINIT_MODIFIER)
    val SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS: KtDiagnosticFactory0 by error0<KtElement>(SourceElementPositioningStrategies.SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS_DIAGNOSTIC)
    val EXPECTED_PRIVATE_DECLARATION: KtDiagnosticFactory0 by error0<KtModifierListOwner>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val EXPECTED_EXTERNAL_DECLARATION: KtDiagnosticFactory0 by error0<KtModifierListOwner>(SourceElementPositioningStrategies.EXTERNAL_MODIFIER)
    val EXPECTED_TAILREC_FUNCTION: KtDiagnosticFactory0 by error0<KtModifierListOwner>(SourceElementPositioningStrategies.TAILREC_MODIFIER)
    val IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS: KtDiagnosticFactory0 by error0<KtDelegatedSuperTypeEntry>()
    val ACTUAL_TYPE_ALIAS_NOT_TO_CLASS: KtDiagnosticFactory0 by error0<KtTypeAlias>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE: KtDiagnosticFactory0 by error0<KtTypeAlias>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE: KtDiagnosticFactory0 by error0<KtTypeAlias>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION: KtDiagnosticFactory0 by error0<KtTypeAlias>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val ACTUAL_TYPE_ALIAS_TO_NULLABLE_TYPE: KtDiagnosticFactory0 by error0<KtTypeAlias>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val ACTUAL_TYPE_ALIAS_TO_NOTHING: KtDiagnosticFactory0 by error0<KtTypeAlias>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS: KtDiagnosticFactory0 by error0<KtFunction>(SourceElementPositioningStrategies.PARAMETERS_WITH_DEFAULT_VALUE)
    val DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS: KtDiagnosticFactory2<FirClassSymbol<*>, Collection<FirCallableSymbol<*>>> by error2<KtTypeAlias, FirClassSymbol<*>, Collection<FirCallableSymbol<*>>>()
    val DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE: KtDiagnosticFactory2<FirRegularClassSymbol, Collection<FirNamedFunctionSymbol>> by error2<KtClass, FirRegularClassSymbol, Collection<FirNamedFunctionSymbol>>(SourceElementPositioningStrategies.SUPERTYPES_LIST)
    val EXPECTED_FUNCTION_SOURCE_WITH_DEFAULT_ARGUMENTS_NOT_FOUND: KtDiagnosticFactory0 by error0<PsiElement>()
    val NO_ACTUAL_FOR_EXPECT: KtDiagnosticFactory3<FirBasedSymbol<*>, FirModuleData, Map<ExpectActualCompatibility<FirBasedSymbol<*>>, Collection<FirBasedSymbol<*>>>> by error3<KtNamedDeclaration, FirBasedSymbol<*>, FirModuleData, Map<ExpectActualCompatibility<FirBasedSymbol<*>>, Collection<FirBasedSymbol<*>>>>(SourceElementPositioningStrategies.INCOMPATIBLE_DECLARATION)
    val ACTUAL_WITHOUT_EXPECT: KtDiagnosticFactory2<FirBasedSymbol<*>, Map<out ExpectActualCompatibility<FirBasedSymbol<*>>, Collection<FirBasedSymbol<*>>>> by error2<KtNamedDeclaration, FirBasedSymbol<*>, Map<out ExpectActualCompatibility<FirBasedSymbol<*>>, Collection<FirBasedSymbol<*>>>>(SourceElementPositioningStrategies.DECLARATION_NAME_ONLY)
    val AMBIGUOUS_EXPECTS: KtDiagnosticFactory2<FirBasedSymbol<*>, Collection<FirModuleData>> by error2<KtNamedDeclaration, FirBasedSymbol<*>, Collection<FirModuleData>>(SourceElementPositioningStrategies.INCOMPATIBLE_DECLARATION)
    val NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS: KtDiagnosticFactory2<FirBasedSymbol<*>, List<Pair<FirBasedSymbol<*>, Map<out MismatchOrIncompatible<FirBasedSymbol<*>>, Collection<FirBasedSymbol<*>>>>>> by error2<KtNamedDeclaration, FirBasedSymbol<*>, List<Pair<FirBasedSymbol<*>, Map<out MismatchOrIncompatible<FirBasedSymbol<*>>, Collection<FirBasedSymbol<*>>>>>>(SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME)
    val ACTUAL_MISSING: KtDiagnosticFactory0 by error0<KtNamedDeclaration>(SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME)
    val EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING: KtDiagnosticFactory0 by warning0<KtClassLikeDeclaration>(SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER)
    val NOT_A_MULTIPLATFORM_COMPILATION: KtDiagnosticFactory0 by error0<PsiElement>()
    val EXPECT_ACTUAL_OPT_IN_ANNOTATION: KtDiagnosticFactory0 by error0<KtNamedDeclaration>(SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER)
    val ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION: KtDiagnosticFactory1<ClassId> by error1<KtTypeAlias, ClassId>(SourceElementPositioningStrategies.TYPEALIAS_TYPE_REFERENCE)
    val ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT: KtDiagnosticFactory4<FirBasedSymbol<*>, FirBasedSymbol<*>, KtSourceElement?, ExpectActualAnnotationsIncompatibilityType<FirAnnotation>> by warning4<KtElement, FirBasedSymbol<*>, FirBasedSymbol<*>, KtSourceElement?, ExpectActualAnnotationsIncompatibilityType<FirAnnotation>>(SourceElementPositioningStrategies.DECLARATION_NAME_ONLY)
    val OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY: KtDiagnosticFactory0 by error0<PsiElement>()
    val OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE: KtDiagnosticFactory0 by error0<PsiElement>()
    val OPTIONAL_EXPECTATION_NOT_ON_EXPECTED: KtDiagnosticFactory0 by error0<PsiElement>()

    // Destructuring declaration
    val INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION: KtDiagnosticFactory0 by error0<KtDestructuringDeclaration>()
    val COMPONENT_FUNCTION_MISSING: KtDiagnosticFactory2<Name, ConeKotlinType> by error2<PsiElement, Name, ConeKotlinType>()
    val COMPONENT_FUNCTION_AMBIGUITY: KtDiagnosticFactory2<Name, Collection<FirBasedSymbol<*>>> by error2<PsiElement, Name, Collection<FirBasedSymbol<*>>>()
    val COMPONENT_FUNCTION_ON_NULLABLE: KtDiagnosticFactory1<Name> by error1<KtExpression, Name>()
    val COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH: KtDiagnosticFactory3<Name, ConeKotlinType, ConeKotlinType> by error3<KtExpression, Name, ConeKotlinType, ConeKotlinType>()

    // Control flow diagnostics
    val UNINITIALIZED_VARIABLE: KtDiagnosticFactory1<FirPropertySymbol> by error1<KtExpression, FirPropertySymbol>()
    val UNINITIALIZED_PARAMETER: KtDiagnosticFactory1<FirValueParameterSymbol> by error1<KtSimpleNameExpression, FirValueParameterSymbol>()
    val UNINITIALIZED_ENUM_ENTRY: KtDiagnosticFactory1<FirEnumEntrySymbol> by error1<KtExpression, FirEnumEntrySymbol>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val UNINITIALIZED_ENUM_COMPANION: KtDiagnosticFactory1<FirRegularClassSymbol> by error1<KtExpression, FirRegularClassSymbol>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val VAL_REASSIGNMENT: KtDiagnosticFactory1<FirVariableSymbol<*>> by error1<KtExpression, FirVariableSymbol<*>>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)
    val VAL_REASSIGNMENT_VIA_BACKING_FIELD: KtDiagnosticFactoryForDeprecation1<FirBackingFieldSymbol> by deprecationError1<KtExpression, FirBackingFieldSymbol>(RestrictionOfValReassignmentViaBackingField, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)
    val CAPTURED_VAL_INITIALIZATION: KtDiagnosticFactory1<FirPropertySymbol> by error1<KtExpression, FirPropertySymbol>()
    val CAPTURED_MEMBER_VAL_INITIALIZATION: KtDiagnosticFactory1<FirPropertySymbol> by error1<KtExpression, FirPropertySymbol>()
    val SETTER_PROJECTED_OUT: KtDiagnosticFactory1<FirPropertySymbol> by error1<KtBinaryExpression, FirPropertySymbol>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)
    val WRONG_INVOCATION_KIND: KtDiagnosticFactory3<FirBasedSymbol<*>, EventOccurrencesRange, EventOccurrencesRange> by warning3<PsiElement, FirBasedSymbol<*>, EventOccurrencesRange, EventOccurrencesRange>()
    val LEAKED_IN_PLACE_LAMBDA: KtDiagnosticFactory1<FirBasedSymbol<*>> by warning1<PsiElement, FirBasedSymbol<*>>()
    val WRONG_IMPLIES_CONDITION: KtDiagnosticFactory0 by warning0<PsiElement>()
    val VARIABLE_WITH_NO_TYPE_NO_INITIALIZER: KtDiagnosticFactory0 by error0<KtVariableDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val INITIALIZATION_BEFORE_DECLARATION: KtDiagnosticFactory1<FirBasedSymbol<*>> by error1<KtExpression, FirBasedSymbol<*>>()
    val UNREACHABLE_CODE: KtDiagnosticFactory2<Set<KtSourceElement>, Set<KtSourceElement>> by warning2<KtElement, Set<KtSourceElement>, Set<KtSourceElement>>(SourceElementPositioningStrategies.UNREACHABLE_CODE)
    val SENSELESS_COMPARISON: KtDiagnosticFactory1<Boolean> by warning1<KtExpression, Boolean>()
    val SENSELESS_NULL_IN_WHEN: KtDiagnosticFactory0 by warning0<KtElement>()
    val TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM: KtDiagnosticFactory0 by error0<KtExpression>()

    // Nullability
    val UNSAFE_CALL: KtDiagnosticFactory2<ConeKotlinType, FirExpression?> by error2<PsiElement, ConeKotlinType, FirExpression?>(SourceElementPositioningStrategies.DOT_BY_QUALIFIED)
    val UNSAFE_IMPLICIT_INVOKE_CALL: KtDiagnosticFactory1<ConeKotlinType> by error1<PsiElement, ConeKotlinType>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val UNSAFE_INFIX_CALL: KtDiagnosticFactory4<ConeKotlinType, FirExpression, String, FirExpression?> by error4<KtExpression, ConeKotlinType, FirExpression, String, FirExpression?>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val UNSAFE_OPERATOR_CALL: KtDiagnosticFactory4<ConeKotlinType, FirExpression, String, FirExpression?> by error4<KtExpression, ConeKotlinType, FirExpression, String, FirExpression?>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val ITERATOR_ON_NULLABLE: KtDiagnosticFactory0 by error0<KtExpression>()
    val UNNECESSARY_SAFE_CALL: KtDiagnosticFactory1<ConeKotlinType> by warning1<PsiElement, ConeKotlinType>(SourceElementPositioningStrategies.SAFE_ACCESS)
    val SAFE_CALL_WILL_CHANGE_NULLABILITY: KtDiagnosticFactory0 by warning0<KtSafeQualifiedExpression>(SourceElementPositioningStrategies.CALL_ELEMENT_WITH_DOT)
    val UNEXPECTED_SAFE_CALL: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.SAFE_ACCESS)
    val UNNECESSARY_NOT_NULL_ASSERTION: KtDiagnosticFactory1<ConeKotlinType> by warning1<KtExpression, ConeKotlinType>(SourceElementPositioningStrategies.OPERATOR)
    val NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION: KtDiagnosticFactory0 by warning0<KtExpression>(SourceElementPositioningStrategies.OPERATOR)
    val NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE: KtDiagnosticFactory0 by warning0<KtExpression>(SourceElementPositioningStrategies.OPERATOR)
    val USELESS_ELVIS: KtDiagnosticFactory1<ConeKotlinType> by warning1<KtBinaryExpression, ConeKotlinType>(SourceElementPositioningStrategies.USELESS_ELVIS)
    val USELESS_ELVIS_RIGHT_IS_NULL: KtDiagnosticFactory0 by warning0<KtBinaryExpression>(SourceElementPositioningStrategies.USELESS_ELVIS)

    // Casts and is-checks
    val CANNOT_CHECK_FOR_ERASED: KtDiagnosticFactory1<ConeKotlinType> by error1<PsiElement, ConeKotlinType>()
    val CAST_NEVER_SUCCEEDS: KtDiagnosticFactory0 by warning0<KtBinaryExpressionWithTypeRHS>(SourceElementPositioningStrategies.OPERATOR)
    val USELESS_CAST: KtDiagnosticFactory0 by warning0<KtBinaryExpressionWithTypeRHS>(SourceElementPositioningStrategies.AS_TYPE)
    val UNCHECKED_CAST: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by warning2<KtBinaryExpressionWithTypeRHS, ConeKotlinType, ConeKotlinType>(SourceElementPositioningStrategies.AS_TYPE)
    val USELESS_IS_CHECK: KtDiagnosticFactory1<Boolean> by warning1<KtElement, Boolean>()
    val IS_ENUM_ENTRY: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val DYNAMIC_NOT_ALLOWED: KtDiagnosticFactory0 by error0<KtTypeReference>()
    val ENUM_ENTRY_AS_TYPE: KtDiagnosticFactory0 by error0<KtTypeReference>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)

    // When expressions
    val EXPECTED_CONDITION: KtDiagnosticFactory0 by error0<KtWhenCondition>()
    val NO_ELSE_IN_WHEN: KtDiagnosticFactory2<List<WhenMissingCase>, String> by error2<KtWhenExpression, List<WhenMissingCase>, String>(SourceElementPositioningStrategies.WHEN_EXPRESSION)
    val NON_EXHAUSTIVE_WHEN_STATEMENT: KtDiagnosticFactory2<String, List<WhenMissingCase>> by warning2<KtWhenExpression, String, List<WhenMissingCase>>(SourceElementPositioningStrategies.WHEN_EXPRESSION)
    val INVALID_IF_AS_EXPRESSION: KtDiagnosticFactory0 by error0<KtIfExpression>(SourceElementPositioningStrategies.IF_EXPRESSION)
    val ELSE_MISPLACED_IN_WHEN: KtDiagnosticFactory0 by error0<KtWhenEntry>(SourceElementPositioningStrategies.ELSE_ENTRY)
    val ILLEGAL_DECLARATION_IN_WHEN_SUBJECT: KtDiagnosticFactory1<String> by error1<KtElement, String>()
    val COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.COMMAS)
    val DUPLICATE_BRANCH_CONDITION_IN_WHEN: KtDiagnosticFactory0 by warning0<KtElement>()
    val CONFUSING_BRANCH_CONDITION: KtDiagnosticFactoryForDeprecation0 by deprecationError0<PsiElement>(ProhibitConfusingSyntaxInWhenBranches)

    // Context tracking
    val TYPE_PARAMETER_IS_NOT_AN_EXPRESSION: KtDiagnosticFactory1<FirTypeParameterSymbol> by error1<KtSimpleNameExpression, FirTypeParameterSymbol>()
    val TYPE_PARAMETER_ON_LHS_OF_DOT: KtDiagnosticFactory1<FirTypeParameterSymbol> by error1<KtSimpleNameExpression, FirTypeParameterSymbol>()
    val NO_COMPANION_OBJECT: KtDiagnosticFactory1<FirClassLikeSymbol<*>> by error1<KtExpression, FirClassLikeSymbol<*>>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)
    val EXPRESSION_EXPECTED_PACKAGE_FOUND: KtDiagnosticFactory0 by error0<KtExpression>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)

    // Function contracts
    val ERROR_IN_CONTRACT_DESCRIPTION: KtDiagnosticFactory1<String> by error1<KtElement, String>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)
    val CONTRACT_NOT_ALLOWED: KtDiagnosticFactory1<String> by error1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)

    // Conventions
    val NO_GET_METHOD: KtDiagnosticFactory0 by error0<KtArrayAccessExpression>(SourceElementPositioningStrategies.ARRAY_ACCESS)
    val NO_SET_METHOD: KtDiagnosticFactory0 by error0<KtArrayAccessExpression>(SourceElementPositioningStrategies.ARRAY_ACCESS)
    val ITERATOR_MISSING: KtDiagnosticFactory0 by error0<KtExpression>()
    val HAS_NEXT_MISSING: KtDiagnosticFactory0 by error0<KtExpression>()
    val NEXT_MISSING: KtDiagnosticFactory0 by error0<KtExpression>()
    val HAS_NEXT_FUNCTION_NONE_APPLICABLE: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> by error1<KtExpression, Collection<FirBasedSymbol<*>>>()
    val NEXT_NONE_APPLICABLE: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> by error1<KtExpression, Collection<FirBasedSymbol<*>>>()
    val DELEGATE_SPECIAL_FUNCTION_MISSING: KtDiagnosticFactory3<String, ConeKotlinType, String> by error3<KtExpression, String, ConeKotlinType, String>()
    val DELEGATE_SPECIAL_FUNCTION_AMBIGUITY: KtDiagnosticFactory2<String, Collection<FirBasedSymbol<*>>> by error2<KtExpression, String, Collection<FirBasedSymbol<*>>>()
    val DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE: KtDiagnosticFactory2<String, Collection<FirBasedSymbol<*>>> by error2<KtExpression, String, Collection<FirBasedSymbol<*>>>()
    val DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH: KtDiagnosticFactory3<String, ConeKotlinType, ConeKotlinType> by error3<KtExpression, String, ConeKotlinType, ConeKotlinType>()
    val UNDERSCORE_IS_RESERVED: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val UNDERSCORE_USAGE_WITHOUT_BACKTICKS: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER: KtDiagnosticFactory0 by warning0<KtNameReferenceExpression>()
    val INVALID_CHARACTERS: KtDiagnosticFactory1<String> by error1<PsiElement, String>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val DANGEROUS_CHARACTERS: KtDiagnosticFactory1<String> by warning1<KtNamedDeclaration, String>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val EQUALITY_NOT_APPLICABLE: KtDiagnosticFactory3<String, ConeKotlinType, ConeKotlinType> by error3<KtBinaryExpression, String, ConeKotlinType, ConeKotlinType>()
    val EQUALITY_NOT_APPLICABLE_WARNING: KtDiagnosticFactory3<String, ConeKotlinType, ConeKotlinType> by warning3<KtBinaryExpression, String, ConeKotlinType, ConeKotlinType>()
    val INCOMPATIBLE_ENUM_COMPARISON_ERROR: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by error2<KtElement, ConeKotlinType, ConeKotlinType>()
    val INCOMPATIBLE_ENUM_COMPARISON: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by warning2<KtElement, ConeKotlinType, ConeKotlinType>()
    val FORBIDDEN_IDENTITY_EQUALS: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by error2<KtElement, ConeKotlinType, ConeKotlinType>()
    val FORBIDDEN_IDENTITY_EQUALS_WARNING: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by warning2<KtElement, ConeKotlinType, ConeKotlinType>()
    val DEPRECATED_IDENTITY_EQUALS: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by warning2<KtElement, ConeKotlinType, ConeKotlinType>()
    val IMPLICIT_BOXING_IN_IDENTITY_EQUALS: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by warning2<KtElement, ConeKotlinType, ConeKotlinType>()
    val INC_DEC_SHOULD_NOT_RETURN_UNIT: KtDiagnosticFactory0 by error0<KtExpression>(SourceElementPositioningStrategies.OPERATOR)
    val ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT: KtDiagnosticFactory2<FirNamedFunctionSymbol, String> by error2<KtExpression, FirNamedFunctionSymbol, String>(SourceElementPositioningStrategies.OPERATOR)
    val PROPERTY_AS_OPERATOR: KtDiagnosticFactory1<FirPropertySymbol> by error1<PsiElement, FirPropertySymbol>(SourceElementPositioningStrategies.OPERATOR)
    val DSL_SCOPE_VIOLATION: KtDiagnosticFactory1<FirBasedSymbol<*>> by error1<PsiElement, FirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)

    // Type alias
    val TOPLEVEL_TYPEALIASES_ONLY: KtDiagnosticFactory0 by error0<KtTypeAlias>()
    val RECURSIVE_TYPEALIAS_EXPANSION: KtDiagnosticFactory0 by error0<KtElement>()
    val TYPEALIAS_SHOULD_EXPAND_TO_CLASS: KtDiagnosticFactory1<ConeKotlinType> by error1<KtElement, ConeKotlinType>()
    val CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION: KtDiagnosticFactoryForDeprecation0 by deprecationError0<KtElement>(ProhibitConstructorAndSupertypeOnTypealiasWithTypeProjection)

    // Extended checkers
    val REDUNDANT_VISIBILITY_MODIFIER: KtDiagnosticFactory0 by warning0<KtModifierListOwner>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val REDUNDANT_MODALITY_MODIFIER: KtDiagnosticFactory0 by warning0<KtModifierListOwner>(SourceElementPositioningStrategies.MODALITY_MODIFIER)
    val REDUNDANT_RETURN_UNIT_TYPE: KtDiagnosticFactory0 by warning0<KtTypeReference>()
    val REDUNDANT_EXPLICIT_TYPE: KtDiagnosticFactory0 by warning0<PsiElement>()
    val REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE: KtDiagnosticFactory0 by warning0<PsiElement>()
    val CAN_BE_VAL: KtDiagnosticFactory0 by warning0<KtDeclaration>(SourceElementPositioningStrategies.VAL_OR_VAR_NODE)
    val CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT: KtDiagnosticFactory0 by warning0<KtExpression>(SourceElementPositioningStrategies.OPERATOR)
    val REDUNDANT_CALL_OF_CONVERSION_METHOD: KtDiagnosticFactory0 by warning0<PsiElement>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)
    val ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS: KtDiagnosticFactory0 by warning0<KtExpression>(SourceElementPositioningStrategies.OPERATOR)
    val EMPTY_RANGE: KtDiagnosticFactory0 by warning0<PsiElement>()
    val REDUNDANT_SETTER_PARAMETER_TYPE: KtDiagnosticFactory0 by warning0<PsiElement>()
    val UNUSED_VARIABLE: KtDiagnosticFactory0 by warning0<KtNamedDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val ASSIGNED_VALUE_IS_NEVER_READ: KtDiagnosticFactory0 by warning0<PsiElement>()
    val VARIABLE_INITIALIZER_IS_REDUNDANT: KtDiagnosticFactory0 by warning0<PsiElement>()
    val VARIABLE_NEVER_READ: KtDiagnosticFactory0 by warning0<KtNamedDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val USELESS_CALL_ON_NOT_NULL: KtDiagnosticFactory0 by warning0<PsiElement>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)

    // Returns
    val RETURN_NOT_ALLOWED: KtDiagnosticFactory0 by error0<KtReturnExpression>(SourceElementPositioningStrategies.RETURN_WITH_LABEL)
    val NOT_A_FUNCTION_LABEL: KtDiagnosticFactory0 by error0<KtReturnExpression>(SourceElementPositioningStrategies.RETURN_WITH_LABEL)
    val RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY: KtDiagnosticFactory0 by error0<KtReturnExpression>(SourceElementPositioningStrategies.RETURN_WITH_LABEL)
    val NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY: KtDiagnosticFactory0 by error0<KtDeclarationWithBody>(SourceElementPositioningStrategies.DECLARATION_WITH_BODY)
    val ANONYMOUS_INITIALIZER_IN_INTERFACE: KtDiagnosticFactory0 by error0<KtAnonymousInitializer>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)

    // Inline
    val USAGE_IS_NOT_INLINABLE: KtDiagnosticFactory1<FirBasedSymbol<*>> by error1<KtElement, FirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val NON_LOCAL_RETURN_NOT_ALLOWED: KtDiagnosticFactory1<FirBasedSymbol<*>> by error1<KtElement, FirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val NOT_YET_SUPPORTED_IN_INLINE: KtDiagnosticFactory1<String> by error1<KtDeclaration, String>(SourceElementPositioningStrategies.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT)
    val NOTHING_TO_INLINE: KtDiagnosticFactory0 by warning0<KtDeclaration>(SourceElementPositioningStrategies.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT)
    val NULLABLE_INLINE_PARAMETER: KtDiagnosticFactory2<FirValueParameterSymbol, FirBasedSymbol<*>> by error2<KtDeclaration, FirValueParameterSymbol, FirBasedSymbol<*>>()
    val RECURSION_IN_INLINE: KtDiagnosticFactory1<FirBasedSymbol<*>> by error1<KtElement, FirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val NON_PUBLIC_CALL_FROM_PUBLIC_INLINE: KtDiagnosticFactory2<FirBasedSymbol<*>, FirBasedSymbol<*>> by error2<KtElement, FirBasedSymbol<*>, FirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val NON_PUBLIC_CALL_FROM_PUBLIC_INLINE_DEPRECATION: KtDiagnosticFactory2<FirBasedSymbol<*>, FirBasedSymbol<*>> by warning2<KtElement, FirBasedSymbol<*>, FirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE: KtDiagnosticFactory2<FirBasedSymbol<*>, FirBasedSymbol<*>> by error2<KtElement, FirBasedSymbol<*>, FirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR: KtDiagnosticFactory2<FirBasedSymbol<*>, FirBasedSymbol<*>> by error2<KtElement, FirBasedSymbol<*>, FirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val PROTECTED_CALL_FROM_PUBLIC_INLINE: KtDiagnosticFactory2<FirBasedSymbol<*>, FirBasedSymbol<*>> by warning2<KtElement, FirBasedSymbol<*>, FirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val PRIVATE_CLASS_MEMBER_FROM_INLINE: KtDiagnosticFactory2<FirBasedSymbol<*>, FirBasedSymbol<*>> by error2<KtElement, FirBasedSymbol<*>, FirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val SUPER_CALL_FROM_PUBLIC_INLINE: KtDiagnosticFactory1<FirBasedSymbol<*>> by error1<KtElement, FirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val DECLARATION_CANT_BE_INLINED: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.INLINE_FUN_MODIFIER)
    val DECLARATION_CANT_BE_INLINED_DEPRECATION: KtDiagnosticFactoryForDeprecation0 by deprecationError0<KtDeclaration>(ProhibitInlineModifierOnPrimaryConstructorParameters, SourceElementPositioningStrategies.INLINE_FUN_MODIFIER)
    val OVERRIDE_BY_INLINE: KtDiagnosticFactory0 by warning0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val NON_INTERNAL_PUBLISHED_API: KtDiagnosticFactory0 by error0<KtElement>()
    val INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE: KtDiagnosticFactory1<FirValueParameterSymbol> by error1<KtElement, FirValueParameterSymbol>()
    val NOT_SUPPORTED_INLINE_PARAMETER_IN_INLINE_PARAMETER_DEFAULT_VALUE: KtDiagnosticFactory1<FirValueParameterSymbol> by error1<KtElement, FirValueParameterSymbol>()
    val REIFIED_TYPE_PARAMETER_IN_OVERRIDE: KtDiagnosticFactory0 by error0<KtElement>(SourceElementPositioningStrategies.REIFIED_MODIFIER)
    val INLINE_PROPERTY_WITH_BACKING_FIELD: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val INLINE_PROPERTY_WITH_BACKING_FIELD_DEPRECATION: KtDiagnosticFactoryForDeprecation0 by deprecationError0<KtDeclaration>(ProhibitInlineModifierOnPrimaryConstructorParameters, SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val ILLEGAL_INLINE_PARAMETER_MODIFIER: KtDiagnosticFactory0 by error0<KtElement>(SourceElementPositioningStrategies.INLINE_PARAMETER_MODIFIER)
    val INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED: KtDiagnosticFactory0 by error0<KtParameter>()
    val INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS: KtDiagnosticFactory1<ConeKotlinType> by warning1<KtNamedFunction, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_NAME)

    // Imports
    val CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON: KtDiagnosticFactory1<Name> by error1<KtImportDirective, Name>(SourceElementPositioningStrategies.IMPORT_LAST_NAME)
    val PACKAGE_CANNOT_BE_IMPORTED: KtDiagnosticFactory0 by error0<KtImportDirective>(SourceElementPositioningStrategies.IMPORT_LAST_NAME)
    val CANNOT_BE_IMPORTED: KtDiagnosticFactory1<Name> by error1<KtImportDirective, Name>(SourceElementPositioningStrategies.IMPORT_LAST_NAME)
    val CONFLICTING_IMPORT: KtDiagnosticFactory1<Name> by error1<KtImportDirective, Name>(SourceElementPositioningStrategies.IMPORT_ALIAS)
    val OPERATOR_RENAMED_ON_IMPORT: KtDiagnosticFactory0 by error0<KtImportDirective>(SourceElementPositioningStrategies.IMPORT_LAST_NAME)
    val TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT: KtDiagnosticFactoryForDeprecation2<Name, Name> by deprecationError2<KtImportDirective, Name, Name>(ProhibitTypealiasAsCallableQualifierInImport, SourceElementPositioningStrategies.IMPORT_LAST_BUT_ONE_NAME)

    // Suspend errors
    val ILLEGAL_SUSPEND_FUNCTION_CALL: KtDiagnosticFactory1<FirBasedSymbol<*>> by error1<PsiElement, FirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val ILLEGAL_SUSPEND_PROPERTY_ACCESS: KtDiagnosticFactory1<FirBasedSymbol<*>> by error1<PsiElement, FirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val NON_LOCAL_SUSPENSION_POINT: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND_FUN: KtDiagnosticFactoryForDeprecation0 by deprecationError0<PsiElement>(ModifierNonBuiltinSuspendFunError, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val RETURN_FOR_BUILT_IN_SUSPEND: KtDiagnosticFactory0 by error0<KtReturnExpression>()
    val MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.SUPERTYPES_LIST)
    val MIXING_FUNCTIONAL_KINDS_IN_SUPERTYPES: KtDiagnosticFactory1<Set<FunctionTypeKind>> by error1<PsiElement, Set<FunctionTypeKind>>(SourceElementPositioningStrategies.SUPERTYPES_LIST)

    // label
    val REDUNDANT_LABEL_WARNING: KtDiagnosticFactory0 by warning0<KtLabelReferenceExpression>(SourceElementPositioningStrategies.LABEL)
    val MULTIPLE_LABELS_ARE_FORBIDDEN: KtDiagnosticFactory0 by error0<KtLabelReferenceExpression>(SourceElementPositioningStrategies.LABEL)

    // Enum.entries resolve deprecations
    val DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY: KtDiagnosticFactory0 by warning0<PsiElement>()
    val DEPRECATED_ACCESS_TO_ENTRY_PROPERTY_FROM_ENUM: KtDiagnosticFactory0 by warning0<PsiElement>()
    val DEPRECATED_ACCESS_TO_ENUM_ENTRY_PROPERTY_AS_REFERENCE: KtDiagnosticFactory0 by warning0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DEPRECATED_DECLARATION_OF_ENUM_ENTRY: KtDiagnosticFactory0 by warning0<KtEnumEntry>()

    // Compatibility issues
    val INCOMPATIBLE_CLASS: KtDiagnosticFactory2<String, IncompatibleVersionErrorData<*>> by error2<PsiElement, String, IncompatibleVersionErrorData<*>>()
    val PRE_RELEASE_CLASS: KtDiagnosticFactory1<String> by error1<PsiElement, String>()
    val IR_WITH_UNSTABLE_ABI_COMPILED_CLASS: KtDiagnosticFactory1<String> by error1<PsiElement, String>()

    // Builder inference
    val BUILDER_INFERENCE_STUB_RECEIVER: KtDiagnosticFactory2<Name, Name> by error2<PsiElement, Name, Name>()
    val BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION: KtDiagnosticFactory2<Name, Name> by error2<PsiElement, Name, Name>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FirErrorsDefaultMessages)
    }
}
