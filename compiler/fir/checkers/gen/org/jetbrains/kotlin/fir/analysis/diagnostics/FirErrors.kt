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
import org.jetbrains.kotlin.config.LanguageFeature.ErrorAboutDataClassCopyVisibilityChange
import org.jetbrains.kotlin.config.LanguageFeature.ForbidAnnotationsWithUseSiteTargetOnExpressions
import org.jetbrains.kotlin.config.LanguageFeature.ForbidCompanionInLocalInnerClass
import org.jetbrains.kotlin.config.LanguageFeature.ForbidEnumEntryNamedEntries
import org.jetbrains.kotlin.config.LanguageFeature.ForbidExposingLessVisibleTypesInInline
import org.jetbrains.kotlin.config.LanguageFeature.ForbidInferOfInvisibleTypeAsReifiedVarargOrReturnType
import org.jetbrains.kotlin.config.LanguageFeature.ForbidInferringTypeVariablesIntoEmptyIntersection
import org.jetbrains.kotlin.config.LanguageFeature.ForbidObjectDelegationToItself
import org.jetbrains.kotlin.config.LanguageFeature.ForbidParenthesizedLhsInAssignments
import org.jetbrains.kotlin.config.LanguageFeature.ForbidProjectionsInAnnotationProperties
import org.jetbrains.kotlin.config.LanguageFeature.ForbidReifiedTypeParametersOnTypeAliases
import org.jetbrains.kotlin.config.LanguageFeature.ForbidUsingExtensionPropertyTypeParameterInDelegate
import org.jetbrains.kotlin.config.LanguageFeature.ModifierNonBuiltinSuspendFunError
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitAllMultipleDefaultsInheritedFromSupertypes
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitAssigningSingleElementsToVarargsInNamedForm
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitConfusingSyntaxInWhenBranches
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitConstructorAndSupertypeOnTypealiasWithTypeProjection
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitCyclesInAnnotations
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitGenericQualifiersOnConstructorCalls
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitImplementingVarByInheritedVal
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitInlineModifierOnPrimaryConstructorParameters
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitIntersectionReifiedTypeParameter
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitNullableTypeThroughTypealias
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitScriptTopLevelInnerClasses
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitSingleNamedFunctionAsExpression
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitTypealiasAsCallableQualifierInImport
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.RelationToType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
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
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.Severity.ERROR
import org.jetbrains.kotlin.diagnostics.Severity.WARNING
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirDeprecationInfo
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
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtContextReceiver
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
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReturnExpression
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
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility.Mismatch
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.types.Variance

/**
 * Generated from: [org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DIAGNOSTICS_LIST]
 */
@Suppress("IncorrectFormatting")
object FirErrors : KtDiagnosticsContainer() {
    // Meta-errors
    val UNSUPPORTED: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("UNSUPPORTED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UNSUPPORTED_FEATURE: KtDiagnosticFactory1<Pair<LanguageFeature, LanguageVersionSettings>> = KtDiagnosticFactory1("UNSUPPORTED_FEATURE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UNSUPPORTED_SUSPEND_TEST: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNSUPPORTED_SUSPEND_TEST", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NEW_INFERENCE_ERROR: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("NEW_INFERENCE_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Miscellaneous
    val OTHER_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("OTHER_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OTHER_ERROR_WITH_REASON: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("OTHER_ERROR_WITH_REASON", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // General syntax
    val ILLEGAL_CONST_EXPRESSION: KtDiagnosticFactory0 = KtDiagnosticFactory0("ILLEGAL_CONST_EXPRESSION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ILLEGAL_UNDERSCORE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ILLEGAL_UNDERSCORE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXPRESSION_EXPECTED: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPRESSION_EXPECTED", ERROR, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val ASSIGNMENT_IN_EXPRESSION_CONTEXT: KtDiagnosticFactory0 = KtDiagnosticFactory0("ASSIGNMENT_IN_EXPRESSION_CONTEXT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtBinaryExpression::class, getRendererFactory())
    val BREAK_OR_CONTINUE_OUTSIDE_A_LOOP: KtDiagnosticFactory0 = KtDiagnosticFactory0("BREAK_OR_CONTINUE_OUTSIDE_A_LOOP", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NOT_A_LOOP_LABEL: KtDiagnosticFactory0 = KtDiagnosticFactory0("NOT_A_LOOP_LABEL", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY: KtDiagnosticFactory0 = KtDiagnosticFactory0("BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpressionWithLabel::class, getRendererFactory())
    val VARIABLE_EXPECTED: KtDiagnosticFactory0 = KtDiagnosticFactory0("VARIABLE_EXPECTED", ERROR, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val DELEGATION_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("DELEGATION_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DELEGATION_NOT_TO_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("DELEGATION_NOT_TO_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NESTED_CLASS_NOT_ALLOWED: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("NESTED_CLASS_NOT_ALLOWED", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val NESTED_CLASS_NOT_ALLOWED_IN_LOCAL: KtDiagnosticFactoryForDeprecation1<String> = KtDiagnosticFactoryForDeprecation1("NESTED_CLASS_NOT_ALLOWED_IN_LOCAL", ForbidCompanionInLocalInnerClass, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val INCORRECT_CHARACTER_LITERAL: KtDiagnosticFactory0 = KtDiagnosticFactory0("INCORRECT_CHARACTER_LITERAL", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EMPTY_CHARACTER_LITERAL: KtDiagnosticFactory0 = KtDiagnosticFactory0("EMPTY_CHARACTER_LITERAL", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL: KtDiagnosticFactory0 = KtDiagnosticFactory0("TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ILLEGAL_ESCAPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ILLEGAL_ESCAPE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INT_LITERAL_OUT_OF_RANGE: KtDiagnosticFactory0 = KtDiagnosticFactory0("INT_LITERAL_OUT_OF_RANGE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val FLOAT_LITERAL_OUT_OF_RANGE: KtDiagnosticFactory0 = KtDiagnosticFactory0("FLOAT_LITERAL_OUT_OF_RANGE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val WRONG_LONG_SUFFIX: KtDiagnosticFactory0 = KtDiagnosticFactory0("WRONG_LONG_SUFFIX", ERROR, SourceElementPositioningStrategies.LONG_LITERAL_SUFFIX, KtElement::class, getRendererFactory())
    val UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val DIVISION_BY_ZERO: KtDiagnosticFactory0 = KtDiagnosticFactory0("DIVISION_BY_ZERO", WARNING, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val VAL_OR_VAR_ON_LOOP_PARAMETER: KtDiagnosticFactory1<KtKeywordToken> = KtDiagnosticFactory1("VAL_OR_VAR_ON_LOOP_PARAMETER", ERROR, SourceElementPositioningStrategies.VAL_OR_VAR_NODE, KtParameter::class, getRendererFactory())
    val VAL_OR_VAR_ON_FUN_PARAMETER: KtDiagnosticFactory1<KtKeywordToken> = KtDiagnosticFactory1("VAL_OR_VAR_ON_FUN_PARAMETER", ERROR, SourceElementPositioningStrategies.VAL_OR_VAR_NODE, KtParameter::class, getRendererFactory())
    val VAL_OR_VAR_ON_CATCH_PARAMETER: KtDiagnosticFactory1<KtKeywordToken> = KtDiagnosticFactory1("VAL_OR_VAR_ON_CATCH_PARAMETER", ERROR, SourceElementPositioningStrategies.VAL_OR_VAR_NODE, KtParameter::class, getRendererFactory())
    val VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER: KtDiagnosticFactory1<KtKeywordToken> = KtDiagnosticFactory1("VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER", ERROR, SourceElementPositioningStrategies.VAL_OR_VAR_NODE, KtParameter::class, getRendererFactory())
    val INVISIBLE_SETTER: KtDiagnosticFactory3<FirPropertySymbol, Visibility, CallableId> = KtDiagnosticFactory3("INVISIBLE_SETTER", ERROR, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val INNER_ON_TOP_LEVEL_SCRIPT_CLASS: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("INNER_ON_TOP_LEVEL_SCRIPT_CLASS", ProhibitScriptTopLevelInnerClasses, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ERROR_SUPPRESSION: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("ERROR_SUPPRESSION", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MISSING_CONSTRUCTOR_KEYWORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("MISSING_CONSTRUCTOR_KEYWORD", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val REDUNDANT_INTERPOLATION_PREFIX: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_INTERPOLATION_PREFIX", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val WRAPPED_LHS_IN_ASSIGNMENT: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("WRAPPED_LHS_IN_ASSIGNMENT", ForbidParenthesizedLhsInAssignments, SourceElementPositioningStrategies.OUTERMOST_PARENTHESES_IN_ASSIGNMENT_LHS, PsiElement::class, getRendererFactory())

    // Unresolved
    val INVISIBLE_REFERENCE: KtDiagnosticFactory3<FirBasedSymbol<*>, Visibility, ClassId?> = KtDiagnosticFactory3("INVISIBLE_REFERENCE", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val UNRESOLVED_REFERENCE: KtDiagnosticFactory2<String, String?> = KtDiagnosticFactory2("UNRESOLVED_REFERENCE", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val UNRESOLVED_LABEL: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNRESOLVED_LABEL", ERROR, SourceElementPositioningStrategies.LABEL, PsiElement::class, getRendererFactory())
    val AMBIGUOUS_LABEL: KtDiagnosticFactory0 = KtDiagnosticFactory0("AMBIGUOUS_LABEL", ERROR, SourceElementPositioningStrategies.LABEL, PsiElement::class, getRendererFactory())
    val LABEL_NAME_CLASH: KtDiagnosticFactory0 = KtDiagnosticFactory0("LABEL_NAME_CLASH", WARNING, SourceElementPositioningStrategies.LABEL, PsiElement::class, getRendererFactory())
    val DESERIALIZATION_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("DESERIALIZATION_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ERROR_FROM_JAVA_RESOLUTION: KtDiagnosticFactory0 = KtDiagnosticFactory0("ERROR_FROM_JAVA_RESOLUTION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MISSING_STDLIB_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("MISSING_STDLIB_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NO_THIS: KtDiagnosticFactory0 = KtDiagnosticFactory0("NO_THIS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATION_ERROR: KtDiagnosticFactory2<FirBasedSymbol<*>, String> = KtDiagnosticFactory2("DEPRECATION_ERROR", ERROR, SourceElementPositioningStrategies.DEPRECATION, PsiElement::class, getRendererFactory())
    val DEPRECATION: KtDiagnosticFactory2<FirBasedSymbol<*>, String> = KtDiagnosticFactory2("DEPRECATION", WARNING, SourceElementPositioningStrategies.DEPRECATION, PsiElement::class, getRendererFactory())
    val VERSION_REQUIREMENT_DEPRECATION_ERROR: KtDiagnosticFactory4<FirBasedSymbol<*>, Version, String, String> = KtDiagnosticFactory4("VERSION_REQUIREMENT_DEPRECATION_ERROR", ERROR, SourceElementPositioningStrategies.DEPRECATION, PsiElement::class, getRendererFactory())
    val VERSION_REQUIREMENT_DEPRECATION: KtDiagnosticFactory4<FirBasedSymbol<*>, Version, String, String> = KtDiagnosticFactory4("VERSION_REQUIREMENT_DEPRECATION", WARNING, SourceElementPositioningStrategies.DEPRECATION, PsiElement::class, getRendererFactory())
    val TYPEALIAS_EXPANSION_DEPRECATION_ERROR: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("TYPEALIAS_EXPANSION_DEPRECATION_ERROR", ERROR, SourceElementPositioningStrategies.DEPRECATION, PsiElement::class, getRendererFactory())
    val TYPEALIAS_EXPANSION_DEPRECATION: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("TYPEALIAS_EXPANSION_DEPRECATION", WARNING, SourceElementPositioningStrategies.DEPRECATION, PsiElement::class, getRendererFactory())
    val API_NOT_AVAILABLE: KtDiagnosticFactory2<ApiVersion, ApiVersion> = KtDiagnosticFactory2("API_NOT_AVAILABLE", ERROR, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val UNRESOLVED_REFERENCE_WRONG_RECEIVER: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("UNRESOLVED_REFERENCE_WRONG_RECEIVER", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val UNRESOLVED_IMPORT: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("UNRESOLVED_IMPORT", ERROR, SourceElementPositioningStrategies.IMPORT_LAST_NAME, PsiElement::class, getRendererFactory())
    val PLACEHOLDER_PROJECTION_IN_QUALIFIER: KtDiagnosticFactory0 = KtDiagnosticFactory0("PLACEHOLDER_PROJECTION_IN_QUALIFIER", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MISSING_DEPENDENCY_CLASS: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("MISSING_DEPENDENCY_CLASS", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE", WARNING, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val MISSING_DEPENDENCY_SUPERCLASS: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("MISSING_DEPENDENCY_SUPERCLASS", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val MISSING_DEPENDENCY_SUPERCLASS_WARNING: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("MISSING_DEPENDENCY_SUPERCLASS_WARNING", WARNING, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT", WARNING, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER: KtDiagnosticFactory2<ConeKotlinType, Name> = KtDiagnosticFactory2("MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER", WARNING, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val MISSING_DEPENDENCY_CLASS_IN_LAMBDA_RECEIVER: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("MISSING_DEPENDENCY_CLASS_IN_LAMBDA_RECEIVER", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Call resolution
    val CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val NO_CONSTRUCTOR: KtDiagnosticFactory0 = KtDiagnosticFactory0("NO_CONSTRUCTOR", ERROR, SourceElementPositioningStrategies.VALUE_ARGUMENTS_LIST, PsiElement::class, getRendererFactory())
    val FUNCTION_CALL_EXPECTED: KtDiagnosticFactory2<String, Boolean> = KtDiagnosticFactory2("FUNCTION_CALL_EXPECTED", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val ILLEGAL_SELECTOR: KtDiagnosticFactory0 = KtDiagnosticFactory0("ILLEGAL_SELECTOR", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NO_RECEIVER_ALLOWED: KtDiagnosticFactory0 = KtDiagnosticFactory0("NO_RECEIVER_ALLOWED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val FUNCTION_EXPECTED: KtDiagnosticFactory2<String, ConeKotlinType> = KtDiagnosticFactory2("FUNCTION_EXPECTED", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val INTERFACE_AS_FUNCTION: KtDiagnosticFactory1<FirRegularClassSymbol> = KtDiagnosticFactory1("INTERFACE_AS_FUNCTION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXPECT_CLASS_AS_FUNCTION: KtDiagnosticFactory1<FirRegularClassSymbol> = KtDiagnosticFactory1("EXPECT_CLASS_AS_FUNCTION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INNER_CLASS_CONSTRUCTOR_NO_RECEIVER: KtDiagnosticFactory1<FirRegularClassSymbol> = KtDiagnosticFactory1("INNER_CLASS_CONSTRUCTOR_NO_RECEIVER", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val PLUGIN_AMBIGUOUS_INTERCEPTED_SYMBOL: KtDiagnosticFactory1<List<String>> = KtDiagnosticFactory1("PLUGIN_AMBIGUOUS_INTERCEPTED_SYMBOL", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val RESOLUTION_TO_CLASSIFIER: KtDiagnosticFactory1<FirRegularClassSymbol> = KtDiagnosticFactory1("RESOLUTION_TO_CLASSIFIER", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val AMBIGUOUS_ALTERED_ASSIGN: KtDiagnosticFactory1<List<String?>> = KtDiagnosticFactory1("AMBIGUOUS_ALTERED_ASSIGN", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Super
    val SUPER_IS_NOT_AN_EXPRESSION: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUPER_IS_NOT_AN_EXPRESSION", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val SUPER_NOT_AVAILABLE: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUPER_NOT_AVAILABLE", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val ABSTRACT_SUPER_CALL: KtDiagnosticFactory0 = KtDiagnosticFactory0("ABSTRACT_SUPER_CALL", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val ABSTRACT_SUPER_CALL_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("ABSTRACT_SUPER_CALL_WARNING", WARNING, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val INSTANCE_ACCESS_BEFORE_SUPER_CALL: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INSTANCE_ACCESS_BEFORE_SUPER_CALL", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SUPER_CALL_WITH_DEFAULT_PARAMETERS: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("SUPER_CALL_WITH_DEFAULT_PARAMETERS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Supertypes
    val NOT_A_SUPERTYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("NOT_A_SUPERTYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER: KtDiagnosticFactory0 = KtDiagnosticFactory0("TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SUPERTYPE_INITIALIZED_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUPERTYPE_INITIALIZED_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val INTERFACE_WITH_SUPERCLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("INTERFACE_WITH_SUPERCLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val FINAL_SUPERTYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("FINAL_SUPERTYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val CLASS_CANNOT_BE_EXTENDED_DIRECTLY: KtDiagnosticFactory1<FirRegularClassSymbol> = KtDiagnosticFactory1("CLASS_CANNOT_BE_EXTENDED_DIRECTLY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val SINGLETON_IN_SUPERTYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("SINGLETON_IN_SUPERTYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val NULLABLE_SUPERTYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("NULLABLE_SUPERTYPE", ERROR, SourceElementPositioningStrategies.QUESTION_MARK_BY_TYPE, KtElement::class, getRendererFactory())
    val NULLABLE_SUPERTYPE_THROUGH_TYPEALIAS: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("NULLABLE_SUPERTYPE_THROUGH_TYPEALIAS", ProhibitNullableTypeThroughTypealias, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val MANY_CLASSES_IN_SUPERTYPE_LIST: KtDiagnosticFactory0 = KtDiagnosticFactory0("MANY_CLASSES_IN_SUPERTYPE_LIST", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val SUPERTYPE_APPEARS_TWICE: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUPERTYPE_APPEARS_TWICE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val CLASS_IN_SUPERTYPE_FOR_ENUM: KtDiagnosticFactory0 = KtDiagnosticFactory0("CLASS_IN_SUPERTYPE_FOR_ENUM", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val SEALED_SUPERTYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("SEALED_SUPERTYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val SEALED_SUPERTYPE_IN_LOCAL_CLASS: KtDiagnosticFactory2<String, ClassKind> = KtDiagnosticFactory2("SEALED_SUPERTYPE_IN_LOCAL_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val SEALED_INHERITOR_IN_DIFFERENT_PACKAGE: KtDiagnosticFactory0 = KtDiagnosticFactory0("SEALED_INHERITOR_IN_DIFFERENT_PACKAGE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val SEALED_INHERITOR_IN_DIFFERENT_MODULE: KtDiagnosticFactory0 = KtDiagnosticFactory0("SEALED_INHERITOR_IN_DIFFERENT_MODULE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val CLASS_INHERITS_JAVA_SEALED_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("CLASS_INHERITS_JAVA_SEALED_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val UNSUPPORTED_SEALED_FUN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNSUPPORTED_SEALED_FUN_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SUPERTYPE_NOT_A_CLASS_OR_INTERFACE: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("SUPERTYPE_NOT_A_CLASS_OR_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, PsiElement::class, getRendererFactory())
    val CYCLIC_INHERITANCE_HIERARCHY: KtDiagnosticFactory0 = KtDiagnosticFactory0("CYCLIC_INHERITANCE_HIERARCHY", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXPANDED_TYPE_CANNOT_BE_INHERITED: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("EXPANDED_TYPE_CANNOT_BE_INHERITED", ERROR, SourceElementPositioningStrategies.DEFAULT, KtTypeReference::class, getRendererFactory())
    val PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE", ERROR, SourceElementPositioningStrategies.VARIANCE_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val INCONSISTENT_TYPE_PARAMETER_VALUES: KtDiagnosticFactory3<FirTypeParameterSymbol, FirClassSymbol<*>, Collection<ConeKotlinType>> = KtDiagnosticFactory3("INCONSISTENT_TYPE_PARAMETER_VALUES", ERROR, SourceElementPositioningStrategies.SUPERTYPES_LIST, KtClassOrObject::class, getRendererFactory())
    val INCONSISTENT_TYPE_PARAMETER_BOUNDS: KtDiagnosticFactory3<FirTypeParameterSymbol, FirClassSymbol<*>, Collection<ConeKotlinType>> = KtDiagnosticFactory3("INCONSISTENT_TYPE_PARAMETER_BOUNDS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val AMBIGUOUS_SUPER: KtDiagnosticFactory1<List<ConeKotlinType>> = KtDiagnosticFactory1("AMBIGUOUS_SUPER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtSuperExpression::class, getRendererFactory())

    // Constructor problems
    val CONSTRUCTOR_IN_OBJECT: KtDiagnosticFactory0 = KtDiagnosticFactory0("CONSTRUCTOR_IN_OBJECT", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, KtDeclaration::class, getRendererFactory())
    val CONSTRUCTOR_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("CONSTRUCTOR_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, KtDeclaration::class, getRendererFactory())
    val NON_PRIVATE_CONSTRUCTOR_IN_ENUM: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_PRIVATE_CONSTRUCTOR_IN_ENUM", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CYCLIC_CONSTRUCTOR_DELEGATION_CALL: KtDiagnosticFactory0 = KtDiagnosticFactory0("CYCLIC_CONSTRUCTOR_DELEGATION_CALL", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED: KtDiagnosticFactory0 = KtDiagnosticFactory0("PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED", ERROR, SourceElementPositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL, PsiElement::class, getRendererFactory())
    val PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val SUPERTYPE_NOT_INITIALIZED: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUPERTYPE_NOT_INITIALIZED", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR: KtDiagnosticFactory0 = KtDiagnosticFactory0("DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXPLICIT_DELEGATION_CALL_REQUIRED: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPLICIT_DELEGATION_CALL_REQUIRED", ERROR, SourceElementPositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL, PsiElement::class, getRendererFactory())
    val SEALED_CLASS_CONSTRUCTOR_CALL: KtDiagnosticFactory0 = KtDiagnosticFactory0("SEALED_CLASS_CONSTRUCTOR_CALL", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DATA_CLASS_CONSISTENT_COPY_AND_EXPOSED_COPY_ARE_INCOMPATIBLE_ANNOTATIONS: KtDiagnosticFactory0 = KtDiagnosticFactory0("DATA_CLASS_CONSISTENT_COPY_AND_EXPOSED_COPY_ARE_INCOMPATIBLE_ANNOTATIONS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET: KtDiagnosticFactory0 = KtDiagnosticFactory0("DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED", ErrorAboutDataClassCopyVisibilityChange, SourceElementPositioningStrategies.VISIBILITY_MODIFIER, KtPrimaryConstructor::class, getRendererFactory())
    val DATA_CLASS_INVISIBLE_COPY_USAGE: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("DATA_CLASS_INVISIBLE_COPY_USAGE", ErrorAboutDataClassCopyVisibilityChange, SourceElementPositioningStrategies.DEFAULT, KtNameReferenceExpression::class, getRendererFactory())
    val DATA_CLASS_WITHOUT_PARAMETERS: KtDiagnosticFactory0 = KtDiagnosticFactory0("DATA_CLASS_WITHOUT_PARAMETERS", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val DATA_CLASS_VARARG_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("DATA_CLASS_VARARG_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtParameter::class, getRendererFactory())
    val DATA_CLASS_NOT_PROPERTY_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("DATA_CLASS_NOT_PROPERTY_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtParameter::class, getRendererFactory())

    // Annotations
    val ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val ANNOTATION_ARGUMENT_MUST_BE_CONST: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANNOTATION_ARGUMENT_MUST_BE_CONST", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val ANNOTATION_CLASS_MEMBER: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANNOTATION_CLASS_MEMBER", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, PsiElement::class, getRendererFactory())
    val ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val INVALID_TYPE_OF_ANNOTATION_MEMBER: KtDiagnosticFactory0 = KtDiagnosticFactory0("INVALID_TYPE_OF_ANNOTATION_MEMBER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val PROJECTION_IN_TYPE_OF_ANNOTATION_MEMBER: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("PROJECTION_IN_TYPE_OF_ANNOTATION_MEMBER", ForbidProjectionsInAnnotationProperties, SourceElementPositioningStrategies.DEFAULT, KtTypeReference::class, getRendererFactory())
    val LOCAL_ANNOTATION_CLASS_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("LOCAL_ANNOTATION_CLASS_ERROR", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val MISSING_VAL_ON_ANNOTATION_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("MISSING_VAL_ON_ANNOTATION_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtParameter::class, getRendererFactory())
    val NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val CYCLE_IN_ANNOTATION_PARAMETER: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("CYCLE_IN_ANNOTATION_PARAMETER", ProhibitCyclesInAnnotations, SourceElementPositioningStrategies.DEFAULT, KtParameter::class, getRendererFactory())
    val ANNOTATION_CLASS_CONSTRUCTOR_CALL: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANNOTATION_CLASS_CONSTRUCTOR_CALL", ERROR, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val ENUM_CLASS_CONSTRUCTOR_CALL: KtDiagnosticFactory0 = KtDiagnosticFactory0("ENUM_CLASS_CONSTRUCTOR_CALL", ERROR, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val NOT_AN_ANNOTATION_CLASS: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("NOT_AN_ANNOTATION_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NULLABLE_TYPE_OF_ANNOTATION_MEMBER: KtDiagnosticFactory0 = KtDiagnosticFactory0("NULLABLE_TYPE_OF_ANNOTATION_MEMBER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val VAR_ANNOTATION_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("VAR_ANNOTATION_PARAMETER", ERROR, SourceElementPositioningStrategies.VAL_OR_VAR_NODE, KtParameter::class, getRendererFactory())
    val SUPERTYPES_FOR_ANNOTATION_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUPERTYPES_FOR_ANNOTATION_CLASS", ERROR, SourceElementPositioningStrategies.SUPERTYPES_LIST, KtClass::class, getRendererFactory())
    val ANNOTATION_USED_AS_ANNOTATION_ARGUMENT: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANNOTATION_USED_AS_ANNOTATION_ARGUMENT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val ANNOTATION_ON_ANNOTATION_ARGUMENT: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANNOTATION_ON_ANNOTATION_ARGUMENT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val ILLEGAL_KOTLIN_VERSION_STRING_VALUE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ILLEGAL_KOTLIN_VERSION_STRING_VALUE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val NEWER_VERSION_IN_SINCE_KOTLIN: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("NEWER_VERSION_IN_SINCE_KOTLIN", WARNING, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS: KtDiagnosticFactory0 = KtDiagnosticFactory0("DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS: KtDiagnosticFactory0 = KtDiagnosticFactory0("DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATED_SINCE_KOTLIN_WITHOUT_DEPRECATED: KtDiagnosticFactory0 = KtDiagnosticFactory0("DEPRECATED_SINCE_KOTLIN_WITHOUT_DEPRECATED", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL: KtDiagnosticFactory0 = KtDiagnosticFactory0("DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE: KtDiagnosticFactory0 = KtDiagnosticFactory0("DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val KOTLIN_ACTUAL_ANNOTATION_HAS_NO_EFFECT_IN_KOTLIN: KtDiagnosticFactory0 = KtDiagnosticFactory0("KOTLIN_ACTUAL_ANNOTATION_HAS_NO_EFFECT_IN_KOTLIN", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val OVERRIDE_DEPRECATION: KtDiagnosticFactory2<FirBasedSymbol<*>, FirDeprecationInfo> = KtDiagnosticFactory2("OVERRIDE_DEPRECATION", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val REDUNDANT_ANNOTATION: KtDiagnosticFactory1<ClassId> = KtDiagnosticFactory1("REDUNDANT_ANNOTATION", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val ANNOTATION_ON_SUPERCLASS_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANNOTATION_ON_SUPERCLASS_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val WRONG_ANNOTATION_TARGET: KtDiagnosticFactory2<String, Collection<KotlinTarget>> = KtDiagnosticFactory2("WRONG_ANNOTATION_TARGET", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val WRONG_ANNOTATION_TARGET_WARNING: KtDiagnosticFactory2<String, Collection<KotlinTarget>> = KtDiagnosticFactory2("WRONG_ANNOTATION_TARGET_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET: KtDiagnosticFactory3<String, String, Collection<KotlinTarget>> = KtDiagnosticFactory3("WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION", ForbidAnnotationsWithUseSiteTargetOnExpressions, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val INAPPLICABLE_TARGET_ON_PROPERTY: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INAPPLICABLE_TARGET_ON_PROPERTY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val INAPPLICABLE_TARGET_ON_PROPERTY_WARNING: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INAPPLICABLE_TARGET_ON_PROPERTY_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val INAPPLICABLE_PARAM_TARGET: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_PARAM_TARGET", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val REDUNDANT_ANNOTATION_TARGET: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("REDUNDANT_ANNOTATION_TARGET", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val INAPPLICABLE_FILE_TARGET: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_FILE_TARGET", ERROR, SourceElementPositioningStrategies.ANNOTATION_USE_SITE, KtAnnotationEntry::class, getRendererFactory())
    val INAPPLICABLE_ALL_TARGET: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_ALL_TARGET", ERROR, SourceElementPositioningStrategies.ANNOTATION_USE_SITE, KtAnnotationEntry::class, getRendererFactory())
    val INAPPLICABLE_ALL_TARGET_IN_MULTI_ANNOTATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_ALL_TARGET_IN_MULTI_ANNOTATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val REPEATED_ANNOTATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("REPEATED_ANNOTATION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val REPEATED_ANNOTATION_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("REPEATED_ANNOTATION_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NOT_A_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("NOT_A_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val WRONG_EXTENSION_FUNCTION_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("WRONG_EXTENSION_FUNCTION_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val WRONG_EXTENSION_FUNCTION_TYPE_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("WRONG_EXTENSION_FUNCTION_TYPE_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val ANNOTATION_IN_WHERE_CLAUSE_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANNOTATION_IN_WHERE_CLAUSE_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val ANNOTATION_IN_CONTRACT_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANNOTATION_IN_CONTRACT_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val COMPILER_REQUIRED_ANNOTATION_AMBIGUITY: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("COMPILER_REQUIRED_ANNOTATION_AMBIGUITY", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val AMBIGUOUS_ANNOTATION_ARGUMENT: KtDiagnosticFactory1<List<FirBasedSymbol<*>>> = KtDiagnosticFactory1("AMBIGUOUS_ANNOTATION_ARGUMENT", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val VOLATILE_ON_VALUE: KtDiagnosticFactory0 = KtDiagnosticFactory0("VOLATILE_ON_VALUE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val VOLATILE_ON_DELEGATE: KtDiagnosticFactory0 = KtDiagnosticFactory0("VOLATILE_ON_DELEGATE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val POTENTIALLY_NON_REPORTED_ANNOTATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("POTENTIALLY_NON_REPORTED_ANNOTATION", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val IGNORABILITY_ANNOTATIONS_WITH_CHECKER_DISABLED: KtDiagnosticFactory0 = KtDiagnosticFactory0("IGNORABILITY_ANNOTATIONS_WITH_CHECKER_DISABLED", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val DSL_MARKER_PROPAGATES_TO_MANY: KtDiagnosticFactory0 = KtDiagnosticFactory0("DSL_MARKER_PROPAGATES_TO_MANY", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())

    // OptIn
    val OPT_IN_USAGE: KtDiagnosticFactory2<ClassId, String> = KtDiagnosticFactory2("OPT_IN_USAGE", WARNING, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val OPT_IN_USAGE_ERROR: KtDiagnosticFactory2<ClassId, String> = KtDiagnosticFactory2("OPT_IN_USAGE_ERROR", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val OPT_IN_TO_INHERITANCE: KtDiagnosticFactory2<ClassId, String> = KtDiagnosticFactory2("OPT_IN_TO_INHERITANCE", WARNING, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val OPT_IN_TO_INHERITANCE_ERROR: KtDiagnosticFactory2<ClassId, String> = KtDiagnosticFactory2("OPT_IN_TO_INHERITANCE_ERROR", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val OPT_IN_OVERRIDE: KtDiagnosticFactory2<ClassId, String> = KtDiagnosticFactory2("OPT_IN_OVERRIDE", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, PsiElement::class, getRendererFactory())
    val OPT_IN_OVERRIDE_ERROR: KtDiagnosticFactory2<ClassId, String> = KtDiagnosticFactory2("OPT_IN_OVERRIDE_ERROR", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, PsiElement::class, getRendererFactory())
    val OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN: KtDiagnosticFactory0 = KtDiagnosticFactory0("OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OPT_IN_WITHOUT_ARGUMENTS: KtDiagnosticFactory0 = KtDiagnosticFactory0("OPT_IN_WITHOUT_ARGUMENTS", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val OPT_IN_ARGUMENT_IS_NOT_MARKER: KtDiagnosticFactory1<ClassId> = KtDiagnosticFactory1("OPT_IN_ARGUMENT_IS_NOT_MARKER", WARNING, SourceElementPositioningStrategies.DEFAULT, KtClassLiteralExpression::class, getRendererFactory())
    val OPT_IN_MARKER_WITH_WRONG_TARGET: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("OPT_IN_MARKER_WITH_WRONG_TARGET", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val OPT_IN_MARKER_WITH_WRONG_RETENTION: KtDiagnosticFactory0 = KtDiagnosticFactory0("OPT_IN_MARKER_WITH_WRONG_RETENTION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val OPT_IN_MARKER_ON_WRONG_TARGET: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("OPT_IN_MARKER_ON_WRONG_TARGET", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val OPT_IN_MARKER_ON_OVERRIDE: KtDiagnosticFactory0 = KtDiagnosticFactory0("OPT_IN_MARKER_ON_OVERRIDE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val OPT_IN_MARKER_ON_OVERRIDE_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("OPT_IN_MARKER_ON_OVERRIDE_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val SUBCLASS_OPT_IN_INAPPLICABLE: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("SUBCLASS_OPT_IN_INAPPLICABLE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val SUBCLASS_OPT_IN_ARGUMENT_IS_NOT_MARKER: KtDiagnosticFactory1<ClassId> = KtDiagnosticFactory1("SUBCLASS_OPT_IN_ARGUMENT_IS_NOT_MARKER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtClassLiteralExpression::class, getRendererFactory())

    // Exposed visibility
    val EXPOSED_TYPEALIAS_EXPANDED_TYPE: KtDiagnosticFactory4<EffectiveVisibility, FirClassLikeSymbol<*>, RelationToType, EffectiveVisibility> = KtDiagnosticFactory4("EXPOSED_TYPEALIAS_EXPANDED_TYPE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val EXPOSED_FUNCTION_RETURN_TYPE: KtDiagnosticFactory4<EffectiveVisibility, FirClassLikeSymbol<*>, RelationToType, EffectiveVisibility> = KtDiagnosticFactory4("EXPOSED_FUNCTION_RETURN_TYPE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val EXPOSED_RECEIVER_TYPE: KtDiagnosticFactory4<EffectiveVisibility, FirClassLikeSymbol<*>, RelationToType, EffectiveVisibility> = KtDiagnosticFactory4("EXPOSED_RECEIVER_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val EXPOSED_PROPERTY_TYPE: KtDiagnosticFactory4<EffectiveVisibility, FirClassLikeSymbol<*>, RelationToType, EffectiveVisibility> = KtDiagnosticFactory4("EXPOSED_PROPERTY_TYPE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR: KtDiagnosticFactory4<EffectiveVisibility, FirClassLikeSymbol<*>, RelationToType, EffectiveVisibility> = KtDiagnosticFactory4("EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val EXPOSED_PARAMETER_TYPE: KtDiagnosticFactory4<EffectiveVisibility, FirClassLikeSymbol<*>, RelationToType, EffectiveVisibility> = KtDiagnosticFactory4("EXPOSED_PARAMETER_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtParameter::class, getRendererFactory())
    val EXPOSED_SUPER_INTERFACE: KtDiagnosticFactory4<EffectiveVisibility, FirClassLikeSymbol<*>, RelationToType, EffectiveVisibility> = KtDiagnosticFactory4("EXPOSED_SUPER_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val EXPOSED_SUPER_CLASS: KtDiagnosticFactory4<EffectiveVisibility, FirClassLikeSymbol<*>, RelationToType, EffectiveVisibility> = KtDiagnosticFactory4("EXPOSED_SUPER_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val EXPOSED_TYPE_PARAMETER_BOUND: KtDiagnosticFactory4<EffectiveVisibility, FirClassLikeSymbol<*>, RelationToType, EffectiveVisibility> = KtDiagnosticFactory4("EXPOSED_TYPE_PARAMETER_BOUND", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val EXPOSED_TYPE_PARAMETER_BOUND_DEPRECATION_WARNING: KtDiagnosticFactory4<EffectiveVisibility, FirClassLikeSymbol<*>, RelationToType, EffectiveVisibility> = KtDiagnosticFactory4("EXPOSED_TYPE_PARAMETER_BOUND_DEPRECATION_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val EXPOSED_PACKAGE_PRIVATE_TYPE_FROM_INTERNAL_WARNING: KtDiagnosticFactory4<EffectiveVisibility, FirClassLikeSymbol<*>, RelationToType, EffectiveVisibility> = KtDiagnosticFactory4("EXPOSED_PACKAGE_PRIVATE_TYPE_FROM_INTERNAL_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())

    // Modifiers
    val INAPPLICABLE_INFIX_MODIFIER: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_INFIX_MODIFIER", ERROR, SourceElementPositioningStrategies.INFIX_MODIFIER, PsiElement::class, getRendererFactory())
    val REPEATED_MODIFIER: KtDiagnosticFactory1<KtModifierKeywordToken> = KtDiagnosticFactory1("REPEATED_MODIFIER", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val REDUNDANT_MODIFIER: KtDiagnosticFactory2<KtModifierKeywordToken, KtModifierKeywordToken> = KtDiagnosticFactory2("REDUNDANT_MODIFIER", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATED_MODIFIER: KtDiagnosticFactory2<KtModifierKeywordToken, KtModifierKeywordToken> = KtDiagnosticFactory2("DEPRECATED_MODIFIER", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATED_MODIFIER_PAIR: KtDiagnosticFactory2<KtModifierKeywordToken, KtModifierKeywordToken> = KtDiagnosticFactory2("DEPRECATED_MODIFIER_PAIR", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATED_MODIFIER_FOR_TARGET: KtDiagnosticFactory2<KtModifierKeywordToken, String> = KtDiagnosticFactory2("DEPRECATED_MODIFIER_FOR_TARGET", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val REDUNDANT_MODIFIER_FOR_TARGET: KtDiagnosticFactory2<KtModifierKeywordToken, String> = KtDiagnosticFactory2("REDUNDANT_MODIFIER_FOR_TARGET", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INCOMPATIBLE_MODIFIERS: KtDiagnosticFactory2<KtModifierKeywordToken, KtModifierKeywordToken> = KtDiagnosticFactory2("INCOMPATIBLE_MODIFIERS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val REDUNDANT_OPEN_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_OPEN_IN_INTERFACE", WARNING, SourceElementPositioningStrategies.OPEN_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val WRONG_MODIFIER_TARGET: KtDiagnosticFactory2<KtModifierKeywordToken, String> = KtDiagnosticFactory2("WRONG_MODIFIER_TARGET", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OPERATOR_MODIFIER_REQUIRED: KtDiagnosticFactory1<FirNamedFunctionSymbol> = KtDiagnosticFactory1("OPERATOR_MODIFIER_REQUIRED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OPERATOR_CALL_ON_CONSTRUCTOR: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("OPERATOR_CALL_ON_CONSTRUCTOR", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INFIX_MODIFIER_REQUIRED: KtDiagnosticFactory1<FirNamedFunctionSymbol> = KtDiagnosticFactory1("INFIX_MODIFIER_REQUIRED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val WRONG_MODIFIER_CONTAINING_DECLARATION: KtDiagnosticFactory2<KtModifierKeywordToken, String> = KtDiagnosticFactory2("WRONG_MODIFIER_CONTAINING_DECLARATION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATED_MODIFIER_CONTAINING_DECLARATION: KtDiagnosticFactory2<KtModifierKeywordToken, String> = KtDiagnosticFactory2("DEPRECATED_MODIFIER_CONTAINING_DECLARATION", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INAPPLICABLE_OPERATOR_MODIFIER: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INAPPLICABLE_OPERATOR_MODIFIER", ERROR, SourceElementPositioningStrategies.OPERATOR_MODIFIER, PsiElement::class, getRendererFactory())
    val INAPPLICABLE_OPERATOR_MODIFIER_WARNING: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INAPPLICABLE_OPERATOR_MODIFIER_WARNING", WARNING, SourceElementPositioningStrategies.OPERATOR_MODIFIER, PsiElement::class, getRendererFactory())
    val NO_EXPLICIT_VISIBILITY_IN_API_MODE: KtDiagnosticFactory0 = KtDiagnosticFactory0("NO_EXPLICIT_VISIBILITY_IN_API_MODE", ERROR, SourceElementPositioningStrategies.DECLARATION_START_TO_NAME, KtDeclaration::class, getRendererFactory())
    val NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING", WARNING, SourceElementPositioningStrategies.DECLARATION_START_TO_NAME, KtDeclaration::class, getRendererFactory())
    val NO_EXPLICIT_RETURN_TYPE_IN_API_MODE: KtDiagnosticFactory0 = KtDiagnosticFactory0("NO_EXPLICIT_RETURN_TYPE_IN_API_MODE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtDeclaration::class, getRendererFactory())
    val NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, KtDeclaration::class, getRendererFactory())
    val ANONYMOUS_SUSPEND_FUNCTION: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANONYMOUS_SUSPEND_FUNCTION", ERROR, SourceElementPositioningStrategies.SUSPEND_MODIFIER, KtDeclaration::class, getRendererFactory())

    // Value classes
    val VALUE_CLASS_NOT_TOP_LEVEL: KtDiagnosticFactory0 = KtDiagnosticFactory0("VALUE_CLASS_NOT_TOP_LEVEL", ERROR, SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER, KtDeclaration::class, getRendererFactory())
    val VALUE_CLASS_NOT_FINAL: KtDiagnosticFactory0 = KtDiagnosticFactory0("VALUE_CLASS_NOT_FINAL", ERROR, SourceElementPositioningStrategies.MODALITY_MODIFIER, KtDeclaration::class, getRendererFactory())
    val ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS", ERROR, SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER, KtDeclaration::class, getRendererFactory())
    val INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE: KtDiagnosticFactory0 = KtDiagnosticFactory0("INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val VALUE_CLASS_EMPTY_CONSTRUCTOR: KtDiagnosticFactory0 = KtDiagnosticFactory0("VALUE_CLASS_EMPTY_CONSTRUCTOR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtParameter::class, getRendererFactory())
    val PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val DELEGATED_PROPERTY_INSIDE_VALUE_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("DELEGATED_PROPERTY_INSIDE_VALUE_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val VALUE_CLASS_CANNOT_EXTEND_CLASSES: KtDiagnosticFactory0 = KtDiagnosticFactory0("VALUE_CLASS_CANNOT_EXTEND_CLASSES", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val VALUE_CLASS_CANNOT_BE_RECURSIVE: KtDiagnosticFactory0 = KtDiagnosticFactory0("VALUE_CLASS_CANNOT_BE_RECURSIVE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val MULTI_FIELD_VALUE_CLASS_PRIMARY_CONSTRUCTOR_DEFAULT_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("MULTI_FIELD_VALUE_CLASS_PRIMARY_CONSTRUCTOR_DEFAULT_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val RESERVED_MEMBER_INSIDE_VALUE_CLASS: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("RESERVED_MEMBER_INSIDE_VALUE_CLASS", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtFunction::class, getRendererFactory())
    val RESERVED_MEMBER_FROM_INTERFACE_INSIDE_VALUE_CLASS: KtDiagnosticFactory2<String, String> = KtDiagnosticFactory2("RESERVED_MEMBER_FROM_INTERFACE_INSIDE_VALUE_CLASS", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClass::class, getRendererFactory())
    val TYPE_ARGUMENT_ON_TYPED_VALUE_CLASS_EQUALS: KtDiagnosticFactory0 = KtDiagnosticFactory0("TYPE_ARGUMENT_ON_TYPED_VALUE_CLASS_EQUALS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val INNER_CLASS_INSIDE_VALUE_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("INNER_CLASS_INSIDE_VALUE_CLASS", ERROR, SourceElementPositioningStrategies.INNER_MODIFIER, KtDeclaration::class, getRendererFactory())
    val VALUE_CLASS_CANNOT_BE_CLONEABLE: KtDiagnosticFactory0 = KtDiagnosticFactory0("VALUE_CLASS_CANNOT_BE_CLONEABLE", ERROR, SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER, KtDeclaration::class, getRendererFactory())
    val VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS: KtDiagnosticFactory0 = KtDiagnosticFactory0("VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS", ERROR, SourceElementPositioningStrategies.CONTEXT_KEYWORD, KtDeclaration::class, getRendererFactory())
    val ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())

    // Applicability
    val NONE_APPLICABLE: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("NONE_APPLICABLE", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val INAPPLICABLE_CANDIDATE: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("INAPPLICABLE_CANDIDATE", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val TYPE_MISMATCH: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, Boolean> = KtDiagnosticFactory3("TYPE_MISMATCH", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR: KtDiagnosticFactory1<FirTypeParameterSymbol> = KtDiagnosticFactory1("TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val THROWABLE_TYPE_MISMATCH: KtDiagnosticFactory2<ConeKotlinType, Boolean> = KtDiagnosticFactory2("THROWABLE_TYPE_MISMATCH", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CONDITION_TYPE_MISMATCH: KtDiagnosticFactory2<ConeKotlinType, Boolean> = KtDiagnosticFactory2("CONDITION_TYPE_MISMATCH", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ARGUMENT_TYPE_MISMATCH: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, Boolean> = KtDiagnosticFactory3("ARGUMENT_TYPE_MISMATCH", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MEMBER_PROJECTED_OUT: KtDiagnosticFactory3<ConeKotlinType, String, FirCallableSymbol<*>> = KtDiagnosticFactory3("MEMBER_PROJECTED_OUT", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NULL_FOR_NONNULL_TYPE: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("NULL_FOR_NONNULL_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INAPPLICABLE_LATEINIT_MODIFIER: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INAPPLICABLE_LATEINIT_MODIFIER", ERROR, SourceElementPositioningStrategies.LATEINIT_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val VARARG_OUTSIDE_PARENTHESES: KtDiagnosticFactory0 = KtDiagnosticFactory0("VARARG_OUTSIDE_PARENTHESES", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val NAMED_ARGUMENTS_NOT_ALLOWED: KtDiagnosticFactory1<ForbiddenNamedArgumentsTarget> = KtDiagnosticFactory1("NAMED_ARGUMENTS_NOT_ALLOWED", ERROR, SourceElementPositioningStrategies.NAME_OF_NAMED_ARGUMENT, KtValueArgument::class, getRendererFactory())
    val NON_VARARG_SPREAD: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_VARARG_SPREAD", ERROR, SourceElementPositioningStrategies.DEFAULT, LeafPsiElement::class, getRendererFactory())
    val ARGUMENT_PASSED_TWICE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ARGUMENT_PASSED_TWICE", ERROR, SourceElementPositioningStrategies.NAME_OF_NAMED_ARGUMENT, KtValueArgument::class, getRendererFactory())
    val TOO_MANY_ARGUMENTS: KtDiagnosticFactory1<FirCallableSymbol<*>> = KtDiagnosticFactory1("TOO_MANY_ARGUMENTS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NO_VALUE_FOR_PARAMETER: KtDiagnosticFactory1<FirValueParameterSymbol> = KtDiagnosticFactory1("NO_VALUE_FOR_PARAMETER", ERROR, SourceElementPositioningStrategies.VALUE_ARGUMENTS, KtElement::class, getRendererFactory())
    val NAMED_PARAMETER_NOT_FOUND: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("NAMED_PARAMETER_NOT_FOUND", ERROR, SourceElementPositioningStrategies.NAME_OF_NAMED_ARGUMENT, KtValueArgument::class, getRendererFactory())
    val NAME_FOR_AMBIGUOUS_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("NAME_FOR_AMBIGUOUS_PARAMETER", ERROR, SourceElementPositioningStrategies.NAME_OF_NAMED_ARGUMENT, KtValueArgument::class, getRendererFactory())
    val MIXING_NAMED_AND_POSITIONAL_ARGUMENTS: KtDiagnosticFactory0 = KtDiagnosticFactory0("MIXING_NAMED_AND_POSITIONAL_ARGUMENTS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ASSIGNMENT_TYPE_MISMATCH: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, Boolean> = KtDiagnosticFactory3("ASSIGNMENT_TYPE_MISMATCH", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val RESULT_TYPE_MISMATCH: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("RESULT_TYPE_MISMATCH", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val MANY_LAMBDA_EXPRESSION_ARGUMENTS: KtDiagnosticFactory0 = KtDiagnosticFactory0("MANY_LAMBDA_EXPRESSION_ARGUMENTS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtLambdaExpression::class, getRendererFactory())
    val SPREAD_OF_NULLABLE: KtDiagnosticFactory0 = KtDiagnosticFactory0("SPREAD_OF_NULLABLE", ERROR, SourceElementPositioningStrategies.SPREAD_OPERATOR, PsiElement::class, getRendererFactory())
    val ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION: KtDiagnosticFactoryForDeprecation1<ConeKotlinType> = KtDiagnosticFactoryForDeprecation1("ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION", ProhibitAssigningSingleElementsToVarargsInNamedForm, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION", ProhibitAssigningSingleElementsToVarargsInNamedForm, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION", WARNING, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION", WARNING, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE: KtDiagnosticFactory1<FirClassLikeSymbol<*>> = KtDiagnosticFactory1("NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val COMPARE_TO_TYPE_MISMATCH: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("COMPARE_TO_TYPE_MISMATCH", ERROR, SourceElementPositioningStrategies.OPERATOR, KtExpression::class, getRendererFactory())
    val HAS_NEXT_FUNCTION_TYPE_MISMATCH: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("HAS_NEXT_FUNCTION_TYPE_MISMATCH", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_WARNING: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_WARNING", WARNING, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())

    // Ambiguity
    val OVERLOAD_RESOLUTION_AMBIGUITY: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("OVERLOAD_RESOLUTION_AMBIGUITY", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val ASSIGN_OPERATOR_AMBIGUITY: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("ASSIGN_OPERATOR_AMBIGUITY", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val ITERATOR_AMBIGUITY: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("ITERATOR_AMBIGUITY", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val HAS_NEXT_FUNCTION_AMBIGUITY: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("HAS_NEXT_FUNCTION_AMBIGUITY", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val NEXT_AMBIGUITY: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("NEXT_AMBIGUITY", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val AMBIGUOUS_FUNCTION_TYPE_KIND: KtDiagnosticFactory1<Collection<FunctionTypeKind>> = KtDiagnosticFactory1("AMBIGUOUS_FUNCTION_TYPE_KIND", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Context parameters resolution
    val NO_CONTEXT_ARGUMENT: KtDiagnosticFactory1<FirValueParameterSymbol> = KtDiagnosticFactory1("NO_CONTEXT_ARGUMENT", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val AMBIGUOUS_CONTEXT_ARGUMENT: KtDiagnosticFactory1<FirValueParameterSymbol> = KtDiagnosticFactory1("AMBIGUOUS_CONTEXT_ARGUMENT", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val AMBIGUOUS_CALL_WITH_IMPLICIT_CONTEXT_RECEIVER: KtDiagnosticFactory0 = KtDiagnosticFactory0("AMBIGUOUS_CALL_WITH_IMPLICIT_CONTEXT_RECEIVER", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL", ERROR, SourceElementPositioningStrategies.NAME_IDENTIFIER, KtElement::class, getRendererFactory())
    val SUBTYPING_BETWEEN_CONTEXT_RECEIVERS: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val CONTEXT_PARAMETERS_WITH_BACKING_FIELD: KtDiagnosticFactory0 = KtDiagnosticFactory0("CONTEXT_PARAMETERS_WITH_BACKING_FIELD", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val CONTEXT_RECEIVERS_DEPRECATED: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("CONTEXT_RECEIVERS_DEPRECATED", WARNING, SourceElementPositioningStrategies.CONTEXT_KEYWORD, KtElement::class, getRendererFactory())
    val CONTEXT_CLASS_OR_CONSTRUCTOR: KtDiagnosticFactory0 = KtDiagnosticFactory0("CONTEXT_CLASS_OR_CONSTRUCTOR", WARNING, SourceElementPositioningStrategies.CONTEXT_KEYWORD, KtElement::class, getRendererFactory())
    val CONTEXT_PARAMETER_WITHOUT_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("CONTEXT_PARAMETER_WITHOUT_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, KtContextReceiver::class, getRendererFactory())
    val CONTEXT_PARAMETER_WITH_DEFAULT: KtDiagnosticFactory0 = KtDiagnosticFactory0("CONTEXT_PARAMETER_WITH_DEFAULT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION: KtDiagnosticFactory1<FirCallableSymbol<*>> = KtDiagnosticFactory1("CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val MULTIPLE_CONTEXT_LISTS: KtDiagnosticFactory0 = KtDiagnosticFactory0("MULTIPLE_CONTEXT_LISTS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val NAMED_CONTEXT_PARAMETER_IN_FUNCTION_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("NAMED_CONTEXT_PARAMETER_IN_FUNCTION_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val CONTEXTUAL_OVERLOAD_SHADOWED: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("CONTEXTUAL_OVERLOAD_SHADOWED", WARNING, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtElement::class, getRendererFactory())

    // Types & type parameters
    val RECURSION_IN_IMPLICIT_TYPES: KtDiagnosticFactory0 = KtDiagnosticFactory0("RECURSION_IN_IMPLICIT_TYPES", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INFERENCE_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("INFERENCE_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT: KtDiagnosticFactory0 = KtDiagnosticFactory0("PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UPPER_BOUND_VIOLATED: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, String> = KtDiagnosticFactory3("UPPER_BOUND_VIOLATED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UPPER_BOUND_VIOLATED_DEPRECATION_WARNING: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, String> = KtDiagnosticFactory3("UPPER_BOUND_VIOLATED_DEPRECATION_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_DEPRECATION_WARNING: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_DEPRECATION_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TYPE_ARGUMENTS_NOT_ALLOWED: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("TYPE_ARGUMENTS_NOT_ALLOWED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED: KtDiagnosticFactory0 = KtDiagnosticFactory0("TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val WRONG_NUMBER_OF_TYPE_ARGUMENTS: KtDiagnosticFactory2<Int, FirBasedSymbol<*>> = KtDiagnosticFactory2("WRONG_NUMBER_OF_TYPE_ARGUMENTS", ERROR, SourceElementPositioningStrategies.TYPE_ARGUMENT_LIST_OR_SELF, PsiElement::class, getRendererFactory())
    val NO_TYPE_ARGUMENTS_ON_RHS: KtDiagnosticFactory2<Int, FirClassLikeSymbol<*>> = KtDiagnosticFactory2("NO_TYPE_ARGUMENTS_ON_RHS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OUTER_CLASS_ARGUMENTS_REQUIRED: KtDiagnosticFactory1<FirClassLikeSymbol<*>> = KtDiagnosticFactory1("OUTER_CLASS_ARGUMENTS_REQUIRED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TYPE_PARAMETERS_IN_OBJECT: KtDiagnosticFactory0 = KtDiagnosticFactory0("TYPE_PARAMETERS_IN_OBJECT", ERROR, SourceElementPositioningStrategies.TYPE_PARAMETERS_LIST, PsiElement::class, getRendererFactory())
    val TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT: KtDiagnosticFactory0 = KtDiagnosticFactory0("TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT", ERROR, SourceElementPositioningStrategies.TYPE_PARAMETERS_LIST, PsiElement::class, getRendererFactory())
    val ILLEGAL_PROJECTION_USAGE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ILLEGAL_PROJECTION_USAGE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TYPE_PARAMETERS_IN_ENUM: KtDiagnosticFactory0 = KtDiagnosticFactory0("TYPE_PARAMETERS_IN_ENUM", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CONFLICTING_PROJECTION: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("CONFLICTING_PROJECTION", ERROR, SourceElementPositioningStrategies.VARIANCE_MODIFIER, KtTypeProjection::class, getRendererFactory())
    val CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION", ERROR, SourceElementPositioningStrategies.VARIANCE_MODIFIER, KtElement::class, getRendererFactory())
    val REDUNDANT_PROJECTION: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("REDUNDANT_PROJECTION", WARNING, SourceElementPositioningStrategies.VARIANCE_MODIFIER, KtTypeProjection::class, getRendererFactory())
    val VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED: KtDiagnosticFactory0 = KtDiagnosticFactory0("VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED", ERROR, SourceElementPositioningStrategies.VARIANCE_MODIFIER, KtTypeParameter::class, getRendererFactory())
    val CATCH_PARAMETER_WITH_DEFAULT_VALUE: KtDiagnosticFactory0 = KtDiagnosticFactory0("CATCH_PARAMETER_WITH_DEFAULT_VALUE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TYPE_PARAMETER_IN_CATCH_CLAUSE: KtDiagnosticFactory0 = KtDiagnosticFactory0("TYPE_PARAMETER_IN_CATCH_CLAUSE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val GENERIC_THROWABLE_SUBCLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("GENERIC_THROWABLE_SUBCLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtTypeParameter::class, getRendererFactory())
    val INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE: KtDiagnosticFactory1<FirTypeParameterSymbol> = KtDiagnosticFactory1("KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val TYPE_PARAMETER_AS_REIFIED: KtDiagnosticFactory1<FirTypeParameterSymbol> = KtDiagnosticFactory1("TYPE_PARAMETER_AS_REIFIED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TYPE_PARAMETER_AS_REIFIED_ARRAY_ERROR: KtDiagnosticFactory1<FirTypeParameterSymbol> = KtDiagnosticFactory1("TYPE_PARAMETER_AS_REIFIED_ARRAY_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val REIFIED_TYPE_FORBIDDEN_SUBSTITUTION: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("REIFIED_TYPE_FORBIDDEN_SUBSTITUTION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEFINITELY_NON_NULLABLE_AS_REIFIED: KtDiagnosticFactory0 = KtDiagnosticFactory0("DEFINITELY_NON_NULLABLE_AS_REIFIED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TYPE_INTERSECTION_AS_REIFIED: KtDiagnosticFactoryForDeprecation2<FirTypeParameterSymbol, Collection<ConeKotlinType>> = KtDiagnosticFactoryForDeprecation2("TYPE_INTERSECTION_AS_REIFIED", ProhibitIntersectionReifiedTypeParameter, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val FINAL_UPPER_BOUND: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("FINAL_UPPER_BOUND", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val UPPER_BOUND_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("UPPER_BOUND_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val ONLY_ONE_CLASS_BOUND_ALLOWED: KtDiagnosticFactory0 = KtDiagnosticFactory0("ONLY_ONE_CLASS_BOUND_ALLOWED", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val REPEATED_BOUND: KtDiagnosticFactory0 = KtDiagnosticFactory0("REPEATED_BOUND", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val CONFLICTING_UPPER_BOUNDS: KtDiagnosticFactory1<FirTypeParameterSymbol> = KtDiagnosticFactory1("CONFLICTING_UPPER_BOUNDS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtNamedDeclaration::class, getRendererFactory())
    val NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER: KtDiagnosticFactory2<Name, FirBasedSymbol<*>> = KtDiagnosticFactory2("NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtSimpleNameExpression::class, getRendererFactory())
    val BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED: KtDiagnosticFactory0 = KtDiagnosticFactory0("BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val REIFIED_TYPE_PARAMETER_NO_INLINE: KtDiagnosticFactory0 = KtDiagnosticFactory0("REIFIED_TYPE_PARAMETER_NO_INLINE", ERROR, SourceElementPositioningStrategies.REIFIED_MODIFIER, KtTypeParameter::class, getRendererFactory())
    val REIFIED_TYPE_PARAMETER_ON_ALIAS: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("REIFIED_TYPE_PARAMETER_ON_ALIAS", ForbidReifiedTypeParametersOnTypeAliases, SourceElementPositioningStrategies.REIFIED_MODIFIER, KtTypeParameter::class, getRendererFactory())
    val TYPE_PARAMETERS_NOT_ALLOWED: KtDiagnosticFactory0 = KtDiagnosticFactory0("TYPE_PARAMETERS_NOT_ALLOWED", ERROR, SourceElementPositioningStrategies.TYPE_PARAMETERS_LIST, KtDeclaration::class, getRendererFactory())
    val TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER: KtDiagnosticFactory0 = KtDiagnosticFactory0("TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtTypeParameter::class, getRendererFactory())
    val RETURN_TYPE_MISMATCH: KtDiagnosticFactory4<ConeKotlinType, ConeKotlinType, FirFunction, Boolean> = KtDiagnosticFactory4("RETURN_TYPE_MISMATCH", ERROR, SourceElementPositioningStrategies.WHOLE_ELEMENT, KtExpression::class, getRendererFactory())
    val IMPLICIT_NOTHING_RETURN_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("IMPLICIT_NOTHING_RETURN_TYPE", ERROR, SourceElementPositioningStrategies.NAME_IDENTIFIER, PsiElement::class, getRendererFactory())
    val IMPLICIT_NOTHING_PROPERTY_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("IMPLICIT_NOTHING_PROPERTY_TYPE", ERROR, SourceElementPositioningStrategies.NAME_IDENTIFIER, PsiElement::class, getRendererFactory())
    val ABBREVIATED_NOTHING_RETURN_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ABBREVIATED_NOTHING_RETURN_TYPE", ERROR, SourceElementPositioningStrategies.NAME_IDENTIFIER, PsiElement::class, getRendererFactory())
    val ABBREVIATED_NOTHING_PROPERTY_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ABBREVIATED_NOTHING_PROPERTY_TYPE", ERROR, SourceElementPositioningStrategies.NAME_IDENTIFIER, PsiElement::class, getRendererFactory())
    val CYCLIC_GENERIC_UPPER_BOUND: KtDiagnosticFactory1<List<FirTypeParameterSymbol>> = KtDiagnosticFactory1("CYCLIC_GENERIC_UPPER_BOUND", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val FINITE_BOUNDS_VIOLATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("FINITE_BOUNDS_VIOLATION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val FINITE_BOUNDS_VIOLATION_IN_JAVA: KtDiagnosticFactory1<List<FirBasedSymbol<*>>> = KtDiagnosticFactory1("FINITE_BOUNDS_VIOLATION_IN_JAVA", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, PsiElement::class, getRendererFactory())
    val EXPANSIVE_INHERITANCE: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPANSIVE_INHERITANCE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXPANSIVE_INHERITANCE_IN_JAVA: KtDiagnosticFactory1<List<FirBasedSymbol<*>>> = KtDiagnosticFactory1("EXPANSIVE_INHERITANCE_IN_JAVA", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, PsiElement::class, getRendererFactory())
    val DEPRECATED_TYPE_PARAMETER_SYNTAX: KtDiagnosticFactory0 = KtDiagnosticFactory0("DEPRECATED_TYPE_PARAMETER_SYNTAX", ERROR, SourceElementPositioningStrategies.TYPE_PARAMETERS_LIST, KtDeclaration::class, getRendererFactory())
    val MISPLACED_TYPE_PARAMETER_CONSTRAINTS: KtDiagnosticFactory0 = KtDiagnosticFactory0("MISPLACED_TYPE_PARAMETER_CONSTRAINTS", WARNING, SourceElementPositioningStrategies.DEFAULT, KtTypeParameter::class, getRendererFactory())
    val DYNAMIC_SUPERTYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("DYNAMIC_SUPERTYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val DYNAMIC_UPPER_BOUND: KtDiagnosticFactory0 = KtDiagnosticFactory0("DYNAMIC_UPPER_BOUND", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val DYNAMIC_RECEIVER_NOT_ALLOWED: KtDiagnosticFactory0 = KtDiagnosticFactory0("DYNAMIC_RECEIVER_NOT_ALLOWED", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val DYNAMIC_RECEIVER_EXPECTED_BUT_WAS_NON_DYNAMIC: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("DYNAMIC_RECEIVER_EXPECTED_BUT_WAS_NON_DYNAMIC", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INCOMPATIBLE_TYPES: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("INCOMPATIBLE_TYPES", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val INCOMPATIBLE_TYPES_WARNING: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("INCOMPATIBLE_TYPES_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val TYPE_VARIANCE_CONFLICT_ERROR: KtDiagnosticFactory4<FirTypeParameterSymbol, Variance, Variance, ConeKotlinType> = KtDiagnosticFactory4("TYPE_VARIANCE_CONFLICT_ERROR", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, PsiElement::class, getRendererFactory())
    val TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE: KtDiagnosticFactory4<FirTypeParameterSymbol, Variance, Variance, ConeKotlinType> = KtDiagnosticFactory4("TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, PsiElement::class, getRendererFactory())
    val SMARTCAST_IMPOSSIBLE: KtDiagnosticFactory4<ConeKotlinType, FirExpression, String, Boolean> = KtDiagnosticFactory4("SMARTCAST_IMPOSSIBLE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val SMARTCAST_IMPOSSIBLE_ON_IMPLICIT_INVOKE_RECEIVER: KtDiagnosticFactory4<ConeKotlinType, FirExpression, String, Boolean> = KtDiagnosticFactory4("SMARTCAST_IMPOSSIBLE_ON_IMPLICIT_INVOKE_RECEIVER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val DEPRECATED_SMARTCAST_ON_DELEGATED_PROPERTY: KtDiagnosticFactory2<ConeKotlinType, FirCallableSymbol<*>> = KtDiagnosticFactory2("DEPRECATED_SMARTCAST_ON_DELEGATED_PROPERTY", WARNING, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val REDUNDANT_NULLABLE: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_NULLABLE", WARNING, SourceElementPositioningStrategies.REDUNDANT_NULLABLE, KtElement::class, getRendererFactory())
    val PLATFORM_CLASS_MAPPED_TO_KOTLIN: KtDiagnosticFactory1<ClassId> = KtDiagnosticFactory1("PLATFORM_CLASS_MAPPED_TO_KOTLIN", WARNING, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION: KtDiagnosticFactoryForDeprecation4<String, Collection<ConeKotlinType>, String, String> = KtDiagnosticFactoryForDeprecation4("INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION", ForbidInferringTypeVariablesIntoEmptyIntersection, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION: KtDiagnosticFactory4<String, Collection<ConeKotlinType>, String, String> = KtDiagnosticFactory4("INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INCORRECT_LEFT_COMPONENT_OF_INTERSECTION: KtDiagnosticFactory0 = KtDiagnosticFactory0("INCORRECT_LEFT_COMPONENT_OF_INTERSECTION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val INCORRECT_RIGHT_COMPONENT_OF_INTERSECTION: KtDiagnosticFactory0 = KtDiagnosticFactory0("INCORRECT_RIGHT_COMPONENT_OF_INTERSECTION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val NULLABLE_ON_DEFINITELY_NOT_NULLABLE: KtDiagnosticFactory0 = KtDiagnosticFactory0("NULLABLE_ON_DEFINITELY_NOT_NULLABLE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT: KtDiagnosticFactoryForDeprecation2<FirTypeParameterSymbol, ConeKotlinType> = KtDiagnosticFactoryForDeprecation2("INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT", ForbidInferOfInvisibleTypeAsReifiedVarargOrReturnType, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val INFERRED_INVISIBLE_VARARG_TYPE_ARGUMENT: KtDiagnosticFactoryForDeprecation3<FirTypeParameterSymbol, ConeKotlinType, FirValueParameterSymbol> = KtDiagnosticFactoryForDeprecation3("INFERRED_INVISIBLE_VARARG_TYPE_ARGUMENT", ForbidInferOfInvisibleTypeAsReifiedVarargOrReturnType, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val INFERRED_INVISIBLE_RETURN_TYPE: KtDiagnosticFactoryForDeprecation2<FirBasedSymbol<*>, ConeKotlinType> = KtDiagnosticFactoryForDeprecation2("INFERRED_INVISIBLE_RETURN_TYPE", ForbidInferOfInvisibleTypeAsReifiedVarargOrReturnType, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val GENERIC_QUALIFIER_ON_CONSTRUCTOR_CALL: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("GENERIC_QUALIFIER_ON_CONSTRUCTOR_CALL", ProhibitGenericQualifiersOnConstructorCalls, SourceElementPositioningStrategies.TYPE_ARGUMENT_LIST_OR_SELF, PsiElement::class, getRendererFactory())
    val ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY: KtDiagnosticFactory3<ClassId, ConeKotlinType, ClassId?> = KtDiagnosticFactory3("ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Reflection
    val EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED: KtDiagnosticFactory1<FirCallableSymbol<*>> = KtDiagnosticFactory1("EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtExpression::class, getRendererFactory())
    val CALLABLE_REFERENCE_LHS_NOT_A_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("CALLABLE_REFERENCE_LHS_NOT_A_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR: KtDiagnosticFactory0 = KtDiagnosticFactory0("CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtExpression::class, getRendererFactory())
    val ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val CLASS_LITERAL_LHS_NOT_A_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("CLASS_LITERAL_LHS_NOT_A_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val NULLABLE_TYPE_IN_CLASS_LITERAL_LHS: KtDiagnosticFactory0 = KtDiagnosticFactory0("NULLABLE_TYPE_IN_CLASS_LITERAL_LHS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UNSUPPORTED_CLASS_LITERALS_WITH_EMPTY_LHS: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNSUPPORTED_CLASS_LITERALS_WITH_EMPTY_LHS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val MUTABLE_PROPERTY_WITH_CAPTURED_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("MUTABLE_PROPERTY_WITH_CAPTURED_TYPE", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // overrides
    val NOTHING_TO_OVERRIDE: KtDiagnosticFactory2<FirCallableSymbol<*>, List<FirCallableSymbol<*>>> = KtDiagnosticFactory2("NOTHING_TO_OVERRIDE", ERROR, SourceElementPositioningStrategies.OVERRIDE_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val CANNOT_OVERRIDE_INVISIBLE_MEMBER: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("CANNOT_OVERRIDE_INVISIBLE_MEMBER", ERROR, SourceElementPositioningStrategies.OVERRIDE_MODIFIER, KtNamedDeclaration::class, getRendererFactory())
    val DATA_CLASS_OVERRIDE_CONFLICT: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("DATA_CLASS_OVERRIDE_CONFLICT", ERROR, SourceElementPositioningStrategies.DATA_MODIFIER, KtClassOrObject::class, getRendererFactory())
    val DATA_CLASS_OVERRIDE_DEFAULT_VALUES: KtDiagnosticFactory2<FirCallableSymbol<*>, FirClassSymbol<*>> = KtDiagnosticFactory2("DATA_CLASS_OVERRIDE_DEFAULT_VALUES", ERROR, SourceElementPositioningStrategies.DATA_MODIFIER, KtElement::class, getRendererFactory())
    val CANNOT_WEAKEN_ACCESS_PRIVILEGE: KtDiagnosticFactory3<Visibility, FirCallableSymbol<*>, Name> = KtDiagnosticFactory3("CANNOT_WEAKEN_ACCESS_PRIVILEGE", ERROR, SourceElementPositioningStrategies.VISIBILITY_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING: KtDiagnosticFactory3<Visibility, FirCallableSymbol<*>, Name> = KtDiagnosticFactory3("CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING", WARNING, SourceElementPositioningStrategies.VISIBILITY_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val CANNOT_CHANGE_ACCESS_PRIVILEGE: KtDiagnosticFactory3<Visibility, FirCallableSymbol<*>, Name> = KtDiagnosticFactory3("CANNOT_CHANGE_ACCESS_PRIVILEGE", ERROR, SourceElementPositioningStrategies.VISIBILITY_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val CANNOT_CHANGE_ACCESS_PRIVILEGE_WARNING: KtDiagnosticFactory3<Visibility, FirCallableSymbol<*>, Name> = KtDiagnosticFactory3("CANNOT_CHANGE_ACCESS_PRIVILEGE_WARNING", WARNING, SourceElementPositioningStrategies.VISIBILITY_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val CANNOT_INFER_VISIBILITY: KtDiagnosticFactory1<FirCallableSymbol<*>> = KtDiagnosticFactory1("CANNOT_INFER_VISIBILITY", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtDeclaration::class, getRendererFactory())
    val CANNOT_INFER_VISIBILITY_WARNING: KtDiagnosticFactory1<FirCallableSymbol<*>> = KtDiagnosticFactory1("CANNOT_INFER_VISIBILITY_WARNING", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, KtDeclaration::class, getRendererFactory())
    val MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES: KtDiagnosticFactory3<Name, FirValueParameterSymbol, List<FirCallableSymbol<*>>> = KtDiagnosticFactory3("MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE: KtDiagnosticFactory3<Name, FirValueParameterSymbol, List<FirCallableSymbol<*>>> = KtDiagnosticFactory3("MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtElement::class, getRendererFactory())
    val MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_DEPRECATION: KtDiagnosticFactoryForDeprecation3<Name, FirValueParameterSymbol, List<FirCallableSymbol<*>>> = KtDiagnosticFactoryForDeprecation3("MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_DEPRECATION", ProhibitAllMultipleDefaultsInheritedFromSupertypes, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE_DEPRECATION: KtDiagnosticFactoryForDeprecation3<Name, FirValueParameterSymbol, List<FirCallableSymbol<*>>> = KtDiagnosticFactoryForDeprecation3("MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE_DEPRECATION", ProhibitAllMultipleDefaultsInheritedFromSupertypes, SourceElementPositioningStrategies.DECLARATION_NAME, KtElement::class, getRendererFactory())
    val TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val OVERRIDING_FINAL_MEMBER: KtDiagnosticFactory2<FirCallableSymbol<*>, Name> = KtDiagnosticFactory2("OVERRIDING_FINAL_MEMBER", ERROR, SourceElementPositioningStrategies.OVERRIDE_MODIFIER, KtNamedDeclaration::class, getRendererFactory())
    val RETURN_TYPE_MISMATCH_ON_INHERITANCE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("RETURN_TYPE_MISMATCH_ON_INHERITANCE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val PROPERTY_TYPE_MISMATCH_ON_INHERITANCE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("PROPERTY_TYPE_MISMATCH_ON_INHERITANCE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val VAR_TYPE_MISMATCH_ON_INHERITANCE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("VAR_TYPE_MISMATCH_ON_INHERITANCE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val RETURN_TYPE_MISMATCH_BY_DELEGATION: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("RETURN_TYPE_MISMATCH_BY_DELEGATION", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val PROPERTY_TYPE_MISMATCH_BY_DELEGATION: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("PROPERTY_TYPE_MISMATCH_BY_DELEGATION", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val CONFLICTING_INHERITED_MEMBERS: KtDiagnosticFactory2<FirClassSymbol<*>, List<FirCallableSymbol<*>>> = KtDiagnosticFactory2("CONFLICTING_INHERITED_MEMBERS", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val ABSTRACT_MEMBER_NOT_IMPLEMENTED: KtDiagnosticFactory2<FirClassSymbol<*>, List<FirCallableSymbol<*>>> = KtDiagnosticFactory2("ABSTRACT_MEMBER_NOT_IMPLEMENTED", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val ABSTRACT_MEMBER_INCORRECTLY_DELEGATED: KtDiagnosticFactoryForDeprecation2<FirClassSymbol<*>, List<FirCallableSymbol<*>>> = KtDiagnosticFactoryForDeprecation2("ABSTRACT_MEMBER_INCORRECTLY_DELEGATED", ForbidObjectDelegationToItself, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val ABSTRACT_MEMBER_NOT_IMPLEMENTED_BY_ENUM_ENTRY: KtDiagnosticFactory2<FirEnumEntrySymbol, List<FirCallableSymbol<*>>> = KtDiagnosticFactory2("ABSTRACT_MEMBER_NOT_IMPLEMENTED_BY_ENUM_ENTRY", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtEnumEntry::class, getRendererFactory())
    val ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED: KtDiagnosticFactory2<FirClassSymbol<*>, List<FirCallableSymbol<*>>> = KtDiagnosticFactory2("ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR: KtDiagnosticFactory2<FirClassSymbol<*>, List<FirCallableSymbol<*>>> = KtDiagnosticFactory2("INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val AMBIGUOUS_ANONYMOUS_TYPE_INFERRED: KtDiagnosticFactory1<Collection<ConeKotlinType>> = KtDiagnosticFactory1("AMBIGUOUS_ANONYMOUS_TYPE_INFERRED", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtDeclaration::class, getRendererFactory())
    val MANY_IMPL_MEMBER_NOT_IMPLEMENTED: KtDiagnosticFactory2<FirClassSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("MANY_IMPL_MEMBER_NOT_IMPLEMENTED", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED: KtDiagnosticFactory2<FirClassSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val OVERRIDING_FINAL_MEMBER_BY_DELEGATION: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("OVERRIDING_FINAL_MEMBER_BY_DELEGATION", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val RETURN_TYPE_MISMATCH_ON_OVERRIDE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("RETURN_TYPE_MISMATCH_ON_OVERRIDE", ERROR, SourceElementPositioningStrategies.DECLARATION_RETURN_TYPE, KtNamedDeclaration::class, getRendererFactory())
    val PROPERTY_TYPE_MISMATCH_ON_OVERRIDE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("PROPERTY_TYPE_MISMATCH_ON_OVERRIDE", ERROR, SourceElementPositioningStrategies.DECLARATION_RETURN_TYPE, KtNamedDeclaration::class, getRendererFactory())
    val VAR_TYPE_MISMATCH_ON_OVERRIDE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("VAR_TYPE_MISMATCH_ON_OVERRIDE", ERROR, SourceElementPositioningStrategies.DECLARATION_RETURN_TYPE, KtNamedDeclaration::class, getRendererFactory())
    val VAR_OVERRIDDEN_BY_VAL: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("VAR_OVERRIDDEN_BY_VAL", ERROR, SourceElementPositioningStrategies.VAL_OR_VAR_NODE, KtNamedDeclaration::class, getRendererFactory())
    val VAR_IMPLEMENTED_BY_INHERITED_VAL: KtDiagnosticFactoryForDeprecation3<FirClassSymbol<*>, FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactoryForDeprecation3("VAR_IMPLEMENTED_BY_INHERITED_VAL", ProhibitImplementingVarByInheritedVal, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val NON_FINAL_MEMBER_IN_FINAL_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_FINAL_MEMBER_IN_FINAL_CLASS", WARNING, SourceElementPositioningStrategies.OPEN_MODIFIER, KtNamedDeclaration::class, getRendererFactory())
    val NON_FINAL_MEMBER_IN_OBJECT: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_FINAL_MEMBER_IN_OBJECT", WARNING, SourceElementPositioningStrategies.OPEN_MODIFIER, KtNamedDeclaration::class, getRendererFactory())
    val VIRTUAL_MEMBER_HIDDEN: KtDiagnosticFactory2<FirCallableSymbol<*>, FirRegularClassSymbol> = KtDiagnosticFactory2("VIRTUAL_MEMBER_HIDDEN", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val PARAMETER_NAME_CHANGED_ON_OVERRIDE: KtDiagnosticFactory2<FirRegularClassSymbol, FirValueParameterSymbol> = KtDiagnosticFactory2("PARAMETER_NAME_CHANGED_ON_OVERRIDE", WARNING, SourceElementPositioningStrategies.NAME_IDENTIFIER, KtParameter::class, getRendererFactory())
    val DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES: KtDiagnosticFactory4<FirValueParameterSymbol, FirValueParameterSymbol, Int, List<FirNamedFunctionSymbol>> = KtDiagnosticFactory4("DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, KtClassOrObject::class, getRendererFactory())
    val SUSPEND_OVERRIDDEN_BY_NON_SUSPEND: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("SUSPEND_OVERRIDDEN_BY_NON_SUSPEND", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtCallableDeclaration::class, getRendererFactory())
    val NON_SUSPEND_OVERRIDDEN_BY_SUSPEND: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("NON_SUSPEND_OVERRIDDEN_BY_SUSPEND", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtCallableDeclaration::class, getRendererFactory())

    // Redeclarations
    val MANY_COMPANION_OBJECTS: KtDiagnosticFactory0 = KtDiagnosticFactory0("MANY_COMPANION_OBJECTS", ERROR, SourceElementPositioningStrategies.COMPANION_OBJECT, KtObjectDeclaration::class, getRendererFactory())
    val CONFLICTING_OVERLOADS: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("CONFLICTING_OVERLOADS", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, PsiElement::class, getRendererFactory())
    val REDECLARATION: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("REDECLARATION", ERROR, SourceElementPositioningStrategies.NAME_IDENTIFIER, KtNamedDeclaration::class, getRendererFactory())
    val CLASSIFIER_REDECLARATION: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("CLASSIFIER_REDECLARATION", ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val PACKAGE_CONFLICTS_WITH_CLASSIFIER: KtDiagnosticFactory1<ClassId> = KtDiagnosticFactory1("PACKAGE_CONFLICTS_WITH_CLASSIFIER", ERROR, SourceElementPositioningStrategies.PACKAGE_DIRECTIVE_NAME_EXPRESSION, KtPackageDirective::class, getRendererFactory())
    val EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE", ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, PsiElement::class, getRendererFactory())
    val EXTENSION_SHADOWED_BY_MEMBER: KtDiagnosticFactory1<FirCallableSymbol<*>> = KtDiagnosticFactory1("EXTENSION_SHADOWED_BY_MEMBER", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, PsiElement::class, getRendererFactory())
    val EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, PsiElement::class, getRendererFactory())

    // Invalid local declarations
    val LOCAL_OBJECT_NOT_ALLOWED: KtDiagnosticFactory1<Name> = KtDiagnosticFactory1("LOCAL_OBJECT_NOT_ALLOWED", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val LOCAL_INTERFACE_NOT_ALLOWED: KtDiagnosticFactory1<Name> = KtDiagnosticFactory1("LOCAL_INTERFACE_NOT_ALLOWED", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())

    // Functions
    val ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS: KtDiagnosticFactory2<FirCallableSymbol<*>, FirClassSymbol<*>> = KtDiagnosticFactory2("ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS", ERROR, SourceElementPositioningStrategies.MODALITY_MODIFIER, KtFunction::class, getRendererFactory())
    val ABSTRACT_FUNCTION_WITH_BODY: KtDiagnosticFactory1<FirCallableSymbol<*>> = KtDiagnosticFactory1("ABSTRACT_FUNCTION_WITH_BODY", ERROR, SourceElementPositioningStrategies.MODALITY_MODIFIER, KtFunction::class, getRendererFactory())
    val NON_ABSTRACT_FUNCTION_WITH_NO_BODY: KtDiagnosticFactory1<FirCallableSymbol<*>> = KtDiagnosticFactory1("NON_ABSTRACT_FUNCTION_WITH_NO_BODY", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtFunction::class, getRendererFactory())
    val PRIVATE_FUNCTION_WITH_NO_BODY: KtDiagnosticFactory1<FirCallableSymbol<*>> = KtDiagnosticFactory1("PRIVATE_FUNCTION_WITH_NO_BODY", ERROR, SourceElementPositioningStrategies.VISIBILITY_MODIFIER, KtFunction::class, getRendererFactory())
    val NON_MEMBER_FUNCTION_NO_BODY: KtDiagnosticFactory1<FirCallableSymbol<*>> = KtDiagnosticFactory1("NON_MEMBER_FUNCTION_NO_BODY", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtFunction::class, getRendererFactory())
    val FUNCTION_DECLARATION_WITH_NO_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("FUNCTION_DECLARATION_WITH_NO_NAME", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtFunction::class, getRendererFactory())
    val ANONYMOUS_FUNCTION_WITH_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANONYMOUS_FUNCTION_WITH_NAME", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtFunction::class, getRendererFactory())
    val SINGLE_ANONYMOUS_FUNCTION_WITH_NAME: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("SINGLE_ANONYMOUS_FUNCTION_WITH_NAME", ProhibitSingleNamedFunctionAsExpression, SourceElementPositioningStrategies.DECLARATION_NAME, KtFunction::class, getRendererFactory())
    val ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE", ERROR, SourceElementPositioningStrategies.PARAMETER_DEFAULT_VALUE, KtParameter::class, getRendererFactory())
    val USELESS_VARARG_ON_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("USELESS_VARARG_ON_PARAMETER", WARNING, SourceElementPositioningStrategies.DEFAULT, KtParameter::class, getRendererFactory())
    val MULTIPLE_VARARG_PARAMETERS: KtDiagnosticFactory0 = KtDiagnosticFactory0("MULTIPLE_VARARG_PARAMETERS", ERROR, SourceElementPositioningStrategies.PARAMETER_VARARG_MODIFIER, KtParameter::class, getRendererFactory())
    val FORBIDDEN_VARARG_PARAMETER_TYPE: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("FORBIDDEN_VARARG_PARAMETER_TYPE", ERROR, SourceElementPositioningStrategies.PARAMETER_VARARG_MODIFIER, KtParameter::class, getRendererFactory())
    val VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtParameter::class, getRendererFactory())
    val CANNOT_INFER_PARAMETER_TYPE: KtDiagnosticFactory1<FirTypeParameterSymbol> = KtDiagnosticFactory1("CANNOT_INFER_PARAMETER_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val CANNOT_INFER_VALUE_PARAMETER_TYPE: KtDiagnosticFactory1<FirValueParameterSymbol> = KtDiagnosticFactory1("CANNOT_INFER_VALUE_PARAMETER_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val CANNOT_INFER_IT_PARAMETER_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("CANNOT_INFER_IT_PARAMETER_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val CANNOT_INFER_RECEIVER_PARAMETER_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("CANNOT_INFER_RECEIVER_PARAMETER_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val NO_TAIL_CALLS_FOUND: KtDiagnosticFactory0 = KtDiagnosticFactory0("NO_TAIL_CALLS_FOUND", WARNING, SourceElementPositioningStrategies.TAILREC_MODIFIER, KtNamedFunction::class, getRendererFactory())
    val TAILREC_ON_VIRTUAL_MEMBER_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("TAILREC_ON_VIRTUAL_MEMBER_ERROR", ERROR, SourceElementPositioningStrategies.TAILREC_MODIFIER, KtNamedFunction::class, getRendererFactory())
    val NON_TAIL_RECURSIVE_CALL: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_TAIL_RECURSIVE_CALL", WARNING, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED: KtDiagnosticFactory0 = KtDiagnosticFactory0("TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED", WARNING, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val DATA_OBJECT_CUSTOM_EQUALS_OR_HASH_CODE: KtDiagnosticFactory0 = KtDiagnosticFactory0("DATA_OBJECT_CUSTOM_EQUALS_OR_HASH_CODE", ERROR, SourceElementPositioningStrategies.OVERRIDE_MODIFIER, KtNamedFunction::class, getRendererFactory())

    // Parameter default values
    val DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE: KtDiagnosticFactory0 = KtDiagnosticFactory0("DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())

    // Fun interfaces
    val FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS: KtDiagnosticFactory0 = KtDiagnosticFactory0("FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS", ERROR, SourceElementPositioningStrategies.FUN_MODIFIER, KtClass::class, getRendererFactory())
    val FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES: KtDiagnosticFactory0 = KtDiagnosticFactory0("FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES", ERROR, SourceElementPositioningStrategies.FUN_INTERFACE, KtDeclaration::class, getRendererFactory())
    val FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS: KtDiagnosticFactory0 = KtDiagnosticFactory0("FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS", ERROR, SourceElementPositioningStrategies.FUN_INTERFACE, KtDeclaration::class, getRendererFactory())
    val FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE: KtDiagnosticFactory0 = KtDiagnosticFactory0("FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE", ERROR, SourceElementPositioningStrategies.FUN_INTERFACE, KtDeclaration::class, getRendererFactory())
    val FUN_INTERFACE_WITH_SUSPEND_FUNCTION: KtDiagnosticFactory0 = KtDiagnosticFactory0("FUN_INTERFACE_WITH_SUSPEND_FUNCTION", ERROR, SourceElementPositioningStrategies.FUN_INTERFACE, KtDeclaration::class, getRendererFactory())

    // Properties & accessors
    val ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS: KtDiagnosticFactory2<FirCallableSymbol<*>, FirClassSymbol<*>> = KtDiagnosticFactory2("ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS", ERROR, SourceElementPositioningStrategies.MODALITY_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val PRIVATE_PROPERTY_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("PRIVATE_PROPERTY_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.VISIBILITY_MODIFIER, KtProperty::class, getRendererFactory())
    val ABSTRACT_PROPERTY_WITH_INITIALIZER: KtDiagnosticFactory0 = KtDiagnosticFactory0("ABSTRACT_PROPERTY_WITH_INITIALIZER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val PROPERTY_INITIALIZER_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("PROPERTY_INITIALIZER_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val PROPERTY_WITH_NO_TYPE_NO_INITIALIZER: KtDiagnosticFactory0 = KtDiagnosticFactory0("PROPERTY_WITH_NO_TYPE_NO_INITIALIZER", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val ABSTRACT_PROPERTY_WITHOUT_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ABSTRACT_PROPERTY_WITHOUT_TYPE", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val LATEINIT_PROPERTY_WITHOUT_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("LATEINIT_PROPERTY_WITHOUT_TYPE", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val MUST_BE_INITIALIZED: KtDiagnosticFactory0 = KtDiagnosticFactory0("MUST_BE_INITIALIZED", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val MUST_BE_INITIALIZED_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("MUST_BE_INITIALIZED_WARNING", WARNING, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val MUST_BE_INITIALIZED_OR_BE_FINAL: KtDiagnosticFactory0 = KtDiagnosticFactory0("MUST_BE_INITIALIZED_OR_BE_FINAL", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val MUST_BE_INITIALIZED_OR_BE_FINAL_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("MUST_BE_INITIALIZED_OR_BE_FINAL_WARNING", WARNING, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val MUST_BE_INITIALIZED_OR_BE_ABSTRACT: KtDiagnosticFactory0 = KtDiagnosticFactory0("MUST_BE_INITIALIZED_OR_BE_ABSTRACT", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val MUST_BE_INITIALIZED_OR_BE_ABSTRACT_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("MUST_BE_INITIALIZED_OR_BE_ABSTRACT_WARNING", WARNING, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT: KtDiagnosticFactory0 = KtDiagnosticFactory0("MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT_WARNING", WARNING, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val UNNECESSARY_LATEINIT: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNNECESSARY_LATEINIT", WARNING, SourceElementPositioningStrategies.LATEINIT_MODIFIER, KtProperty::class, getRendererFactory())
    val BACKING_FIELD_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("BACKING_FIELD_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val EXTENSION_PROPERTY_WITH_BACKING_FIELD: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTENSION_PROPERTY_WITH_BACKING_FIELD", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val PROPERTY_INITIALIZER_NO_BACKING_FIELD: KtDiagnosticFactory0 = KtDiagnosticFactory0("PROPERTY_INITIALIZER_NO_BACKING_FIELD", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val ABSTRACT_DELEGATED_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("ABSTRACT_DELEGATED_PROPERTY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val DELEGATED_PROPERTY_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("DELEGATED_PROPERTY_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val ABSTRACT_PROPERTY_WITH_GETTER: KtDiagnosticFactory0 = KtDiagnosticFactory0("ABSTRACT_PROPERTY_WITH_GETTER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtPropertyAccessor::class, getRendererFactory())
    val ABSTRACT_PROPERTY_WITH_SETTER: KtDiagnosticFactory0 = KtDiagnosticFactory0("ABSTRACT_PROPERTY_WITH_SETTER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtPropertyAccessor::class, getRendererFactory())
    val PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY", ERROR, SourceElementPositioningStrategies.PRIVATE_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val PRIVATE_SETTER_FOR_OPEN_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("PRIVATE_SETTER_FOR_OPEN_PROPERTY", ERROR, SourceElementPositioningStrategies.PRIVATE_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val VAL_WITH_SETTER: KtDiagnosticFactory0 = KtDiagnosticFactory0("VAL_WITH_SETTER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtPropertyAccessor::class, getRendererFactory())
    val CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT: KtDiagnosticFactory0 = KtDiagnosticFactory0("CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT", ERROR, SourceElementPositioningStrategies.CONST_MODIFIER, KtElement::class, getRendererFactory())
    val CONST_VAL_WITH_GETTER: KtDiagnosticFactory0 = KtDiagnosticFactory0("CONST_VAL_WITH_GETTER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val CONST_VAL_WITH_DELEGATE: KtDiagnosticFactory0 = KtDiagnosticFactory0("CONST_VAL_WITH_DELEGATE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val TYPE_CANT_BE_USED_FOR_CONST_VAL: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("TYPE_CANT_BE_USED_FOR_CONST_VAL", ERROR, SourceElementPositioningStrategies.CONST_MODIFIER, KtProperty::class, getRendererFactory())
    val CONST_VAL_WITHOUT_INITIALIZER: KtDiagnosticFactory0 = KtDiagnosticFactory0("CONST_VAL_WITHOUT_INITIALIZER", ERROR, SourceElementPositioningStrategies.CONST_MODIFIER, KtProperty::class, getRendererFactory())
    val CONST_VAL_WITH_NON_CONST_INITIALIZER: KtDiagnosticFactory0 = KtDiagnosticFactory0("CONST_VAL_WITH_NON_CONST_INITIALIZER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val WRONG_SETTER_PARAMETER_TYPE: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("WRONG_SETTER_PARAMETER_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER: KtDiagnosticFactoryForDeprecation1<FirTypeParameterSymbol> = KtDiagnosticFactoryForDeprecation1("DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER", ForbidUsingExtensionPropertyTypeParameterInDelegate, SourceElementPositioningStrategies.PROPERTY_DELEGATE, KtProperty::class, getRendererFactory())
    val INITIALIZER_TYPE_MISMATCH: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, Boolean> = KtDiagnosticFactory3("INITIALIZER_TYPE_MISMATCH", ERROR, SourceElementPositioningStrategies.PROPERTY_INITIALIZER, KtNamedDeclaration::class, getRendererFactory())
    val GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY: KtDiagnosticFactory0 = KtDiagnosticFactory0("GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY", ERROR, SourceElementPositioningStrategies.VISIBILITY_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY: KtDiagnosticFactory0 = KtDiagnosticFactory0("SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY", ERROR, SourceElementPositioningStrategies.VISIBILITY_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val WRONG_SETTER_RETURN_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("WRONG_SETTER_RETURN_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val WRONG_GETTER_RETURN_TYPE: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("WRONG_GETTER_RETURN_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val ACCESSOR_FOR_DELEGATED_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("ACCESSOR_FOR_DELEGATED_PROPERTY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtPropertyAccessor::class, getRendererFactory())
    val PROPERTY_INITIALIZER_WITH_EXPLICIT_FIELD_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("PROPERTY_INITIALIZER_WITH_EXPLICIT_FIELD_DECLARATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val PROPERTY_FIELD_DECLARATION_MISSING_INITIALIZER: KtDiagnosticFactory0 = KtDiagnosticFactory0("PROPERTY_FIELD_DECLARATION_MISSING_INITIALIZER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtBackingField::class, getRendererFactory())
    val LATEINIT_PROPERTY_FIELD_DECLARATION_WITH_INITIALIZER: KtDiagnosticFactory0 = KtDiagnosticFactory0("LATEINIT_PROPERTY_FIELD_DECLARATION_WITH_INITIALIZER", ERROR, SourceElementPositioningStrategies.LATEINIT_MODIFIER, KtBackingField::class, getRendererFactory())
    val LATEINIT_FIELD_IN_VAL_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("LATEINIT_FIELD_IN_VAL_PROPERTY", ERROR, SourceElementPositioningStrategies.LATEINIT_MODIFIER, KtBackingField::class, getRendererFactory())
    val LATEINIT_NULLABLE_BACKING_FIELD: KtDiagnosticFactory0 = KtDiagnosticFactory0("LATEINIT_NULLABLE_BACKING_FIELD", ERROR, SourceElementPositioningStrategies.LATEINIT_MODIFIER, KtBackingField::class, getRendererFactory())
    val BACKING_FIELD_FOR_DELEGATED_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("BACKING_FIELD_FOR_DELEGATED_PROPERTY", ERROR, SourceElementPositioningStrategies.FIELD_KEYWORD, KtBackingField::class, getRendererFactory())
    val PROPERTY_MUST_HAVE_GETTER: KtDiagnosticFactory0 = KtDiagnosticFactory0("PROPERTY_MUST_HAVE_GETTER", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val PROPERTY_MUST_HAVE_SETTER: KtDiagnosticFactory0 = KtDiagnosticFactory0("PROPERTY_MUST_HAVE_SETTER", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtProperty::class, getRendererFactory())
    val EXPLICIT_BACKING_FIELD_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPLICIT_BACKING_FIELD_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.FIELD_KEYWORD, KtBackingField::class, getRendererFactory())
    val EXPLICIT_BACKING_FIELD_IN_ABSTRACT_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPLICIT_BACKING_FIELD_IN_ABSTRACT_PROPERTY", ERROR, SourceElementPositioningStrategies.FIELD_KEYWORD, KtBackingField::class, getRendererFactory())
    val EXPLICIT_BACKING_FIELD_IN_EXTENSION: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPLICIT_BACKING_FIELD_IN_EXTENSION", ERROR, SourceElementPositioningStrategies.FIELD_KEYWORD, KtBackingField::class, getRendererFactory())
    val REDUNDANT_EXPLICIT_BACKING_FIELD: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_EXPLICIT_BACKING_FIELD", WARNING, SourceElementPositioningStrategies.FIELD_KEYWORD, KtBackingField::class, getRendererFactory())
    val ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS: KtDiagnosticFactory0 = KtDiagnosticFactory0("ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS", ERROR, SourceElementPositioningStrategies.ABSTRACT_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val LOCAL_VARIABLE_WITH_TYPE_PARAMETERS_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("LOCAL_VARIABLE_WITH_TYPE_PARAMETERS_WARNING", WARNING, SourceElementPositioningStrategies.TYPE_PARAMETERS_LIST, KtProperty::class, getRendererFactory())
    val LOCAL_VARIABLE_WITH_TYPE_PARAMETERS: KtDiagnosticFactory0 = KtDiagnosticFactory0("LOCAL_VARIABLE_WITH_TYPE_PARAMETERS", ERROR, SourceElementPositioningStrategies.TYPE_PARAMETERS_LIST, KtProperty::class, getRendererFactory())
    val EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, KtExpression::class, getRendererFactory())
    val SAFE_CALLABLE_REFERENCE_CALL: KtDiagnosticFactory0 = KtDiagnosticFactory0("SAFE_CALLABLE_REFERENCE_CALL", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val LATEINIT_INTRINSIC_CALL_ON_NON_LITERAL: KtDiagnosticFactory0 = KtDiagnosticFactory0("LATEINIT_INTRINSIC_CALL_ON_NON_LITERAL", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT: KtDiagnosticFactory0 = KtDiagnosticFactory0("LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION: KtDiagnosticFactory0 = KtDiagnosticFactory0("LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val LOCAL_EXTENSION_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("LOCAL_EXTENSION_PROPERTY", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UNNAMED_VAR_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNNAMED_VAR_PROPERTY", ERROR, SourceElementPositioningStrategies.VAL_OR_VAR_NODE, PsiElement::class, getRendererFactory())
    val UNNAMED_DELEGATED_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNNAMED_DELEGATED_PROPERTY", ERROR, SourceElementPositioningStrategies.PROPERTY_DELEGATE_BY_KEYWORD, PsiElement::class, getRendererFactory())

    // Multi-platform projects
    val EXPECTED_DECLARATION_WITH_BODY: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECTED_DECLARATION_WITH_BODY", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, KtDeclaration::class, getRendererFactory())
    val EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL", ERROR, SourceElementPositioningStrategies.DEFAULT, KtConstructorDelegationCall::class, getRendererFactory())
    val EXPECTED_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECTED_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtParameter::class, getRendererFactory())
    val EXPECTED_ENUM_CONSTRUCTOR: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECTED_ENUM_CONSTRUCTOR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtConstructor::class, getRendererFactory())
    val EXPECTED_ENUM_ENTRY_WITH_BODY: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECTED_ENUM_ENTRY_WITH_BODY", ERROR, SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER, KtEnumEntry::class, getRendererFactory())
    val EXPECTED_PROPERTY_INITIALIZER: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECTED_PROPERTY_INITIALIZER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val EXPECTED_DELEGATED_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECTED_DELEGATED_PROPERTY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val EXPECTED_LATEINIT_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECTED_LATEINIT_PROPERTY", ERROR, SourceElementPositioningStrategies.LATEINIT_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS", ERROR, SourceElementPositioningStrategies.SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS_DIAGNOSTIC, KtElement::class, getRendererFactory())
    val EXPECTED_PRIVATE_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECTED_PRIVATE_DECLARATION", ERROR, SourceElementPositioningStrategies.VISIBILITY_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val EXPECTED_EXTERNAL_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECTED_EXTERNAL_DECLARATION", ERROR, SourceElementPositioningStrategies.EXTERNAL_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val EXPECTED_TAILREC_FUNCTION: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECTED_TAILREC_FUNCTION", ERROR, SourceElementPositioningStrategies.TAILREC_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtDelegatedSuperTypeEntry::class, getRendererFactory())
    val ACTUAL_TYPE_ALIAS_NOT_TO_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("ACTUAL_TYPE_ALIAS_NOT_TO_CLASS", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, KtTypeAlias::class, getRendererFactory())
    val ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, KtTypeAlias::class, getRendererFactory())
    val ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, KtTypeAlias::class, getRendererFactory())
    val ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION: KtDiagnosticFactory0 = KtDiagnosticFactory0("ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, KtTypeAlias::class, getRendererFactory())
    val ACTUAL_TYPE_ALIAS_TO_NULLABLE_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ACTUAL_TYPE_ALIAS_TO_NULLABLE_TYPE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, KtTypeAlias::class, getRendererFactory())
    val ACTUAL_TYPE_ALIAS_TO_NOTHING: KtDiagnosticFactory0 = KtDiagnosticFactory0("ACTUAL_TYPE_ALIAS_TO_NOTHING", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, KtTypeAlias::class, getRendererFactory())
    val ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS: KtDiagnosticFactory0 = KtDiagnosticFactory0("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS", ERROR, SourceElementPositioningStrategies.PARAMETERS_WITH_DEFAULT_VALUE, KtFunction::class, getRendererFactory())
    val DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS: KtDiagnosticFactory2<FirClassSymbol<*>, Collection<FirCallableSymbol<*>>> = KtDiagnosticFactory2("DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtTypeAlias::class, getRendererFactory())
    val DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE: KtDiagnosticFactory2<FirRegularClassSymbol, Collection<FirNamedFunctionSymbol>> = KtDiagnosticFactory2("DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE", ERROR, SourceElementPositioningStrategies.SUPERTYPES_LIST, KtClass::class, getRendererFactory())
    val EXPECTED_FUNCTION_SOURCE_WITH_DEFAULT_ARGUMENTS_NOT_FOUND: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECTED_FUNCTION_SOURCE_WITH_DEFAULT_ARGUMENTS_NOT_FOUND", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ACTUAL_WITHOUT_EXPECT: KtDiagnosticFactory2<FirBasedSymbol<*>, Map<out ExpectActualMatchingCompatibility, Collection<FirBasedSymbol<*>>>> = KtDiagnosticFactory2("ACTUAL_WITHOUT_EXPECT", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_CLASS_TYPE_PARAMETER_COUNT: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_CLASS_TYPE_PARAMETER_COUNT", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_RETURN_TYPE: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_RETURN_TYPE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_PARAMETER_NAMES: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_PARAMETER_NAMES", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_CONTEXT_PARAMETER_NAMES: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_CONTEXT_PARAMETER_NAMES", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_NAMES: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_NAMES", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_VARARG: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_VARARG", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_NOINLINE: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_NOINLINE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_CROSSINLINE: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_CROSSINLINE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_FUNCTION_MODIFIERS_DIFFERENT: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_FUNCTION_MODIFIERS_DIFFERENT", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_FUNCTION_MODIFIERS_NOT_SUBSET: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_FUNCTION_MODIFIERS_NOT_SUBSET", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_PARAMETERS_WITH_DEFAULT_VALUES_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_PARAMETERS_WITH_DEFAULT_VALUES_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_KIND: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_KIND", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_LATEINIT_MODIFIER: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_LATEINIT_MODIFIER", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_CONST_MODIFIER: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_CONST_MODIFIER", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_SETTER_VISIBILITY: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_SETTER_VISIBILITY", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_CLASS_KIND: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_CLASS_KIND", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_FUN_INTERFACE_MODIFIER: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_FUN_INTERFACE_MODIFIER", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_NESTED_TYPE_ALIAS: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_NESTED_TYPE_ALIAS", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_ENUM_ENTRIES: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_ENUM_ENTRIES", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_ILLEGAL_REQUIRES_OPT_IN: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_ILLEGAL_REQUIRES_OPT_IN", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_MODALITY: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_MODALITY", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_VISIBILITY: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_VISIBILITY", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_CLASS_TYPE_PARAMETER_UPPER_BOUNDS: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_CLASS_TYPE_PARAMETER_UPPER_BOUNDS", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_VARIANCE: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_VARIANCE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_REIFIED: KtDiagnosticFactory3<FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory3("EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_REIFIED", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE: KtDiagnosticFactory4<FirBasedSymbol<*>, FirBasedSymbol<*>, FirBasedSymbol<*>, String> = KtDiagnosticFactory4("EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE", ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_REFINEMENT_ANNOTATION_WRONG_TARGET: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECT_REFINEMENT_ANNOTATION_WRONG_TARGET", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val AMBIGUOUS_EXPECTS: KtDiagnosticFactory2<FirBasedSymbol<*>, Collection<FirModuleData>> = KtDiagnosticFactory2("AMBIGUOUS_EXPECTS", ERROR, SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER, KtNamedDeclaration::class, getRendererFactory())
    val NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS: KtDiagnosticFactory2<FirBasedSymbol<*>, List<Pair<FirBasedSymbol<*>, Map<out Mismatch, Collection<FirBasedSymbol<*>>>>>> = KtDiagnosticFactory2("NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS", ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val ACTUAL_MISSING: KtDiagnosticFactory0 = KtDiagnosticFactory0("ACTUAL_MISSING", ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_REFINEMENT_ANNOTATION_MISSING: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECT_REFINEMENT_ANNOTATION_MISSING", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", WARNING, SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER, KtClassLikeDeclaration::class, getRendererFactory())
    val NOT_A_MULTIPLATFORM_COMPILATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("NOT_A_MULTIPLATFORM_COMPILATION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXPECT_ACTUAL_OPT_IN_ANNOTATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECT_ACTUAL_OPT_IN_ANNOTATION", ERROR, SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER, KtNamedDeclaration::class, getRendererFactory())
    val ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION: KtDiagnosticFactory1<ClassId> = KtDiagnosticFactory1("ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION", ERROR, SourceElementPositioningStrategies.TYPEALIAS_TYPE_REFERENCE, KtTypeAlias::class, getRendererFactory())
    val ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT: KtDiagnosticFactory4<FirBasedSymbol<*>, FirBasedSymbol<*>, KtSourceElement?, ExpectActualAnnotationsIncompatibilityType<FirAnnotation>> = KtDiagnosticFactory4("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME_ONLY, KtElement::class, getRendererFactory())
    val OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY: KtDiagnosticFactory0 = KtDiagnosticFactory0("OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE: KtDiagnosticFactory0 = KtDiagnosticFactory0("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OPTIONAL_EXPECTATION_NOT_ON_EXPECTED: KtDiagnosticFactory0 = KtDiagnosticFactory0("OPTIONAL_EXPECTATION_NOT_ON_EXPECTED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Destructuring declaration
    val INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtDestructuringDeclaration::class, getRendererFactory())
    val COMPONENT_FUNCTION_MISSING: KtDiagnosticFactory2<Name, ConeKotlinType> = KtDiagnosticFactory2("COMPONENT_FUNCTION_MISSING", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val COMPONENT_FUNCTION_AMBIGUITY: KtDiagnosticFactory3<Name, Collection<FirBasedSymbol<*>>, ConeKotlinType> = KtDiagnosticFactory3("COMPONENT_FUNCTION_AMBIGUITY", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val COMPONENT_FUNCTION_ON_NULLABLE: KtDiagnosticFactory2<Name, ConeKotlinType> = KtDiagnosticFactory2("COMPONENT_FUNCTION_ON_NULLABLE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH: KtDiagnosticFactory3<Name, ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory3("COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())

    // Control flow diagnostics
    val UNINITIALIZED_VARIABLE: KtDiagnosticFactory1<FirPropertySymbol> = KtDiagnosticFactory1("UNINITIALIZED_VARIABLE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val UNINITIALIZED_PARAMETER: KtDiagnosticFactory1<FirValueParameterSymbol> = KtDiagnosticFactory1("UNINITIALIZED_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtSimpleNameExpression::class, getRendererFactory())
    val UNINITIALIZED_ENUM_ENTRY: KtDiagnosticFactory1<FirEnumEntrySymbol> = KtDiagnosticFactory1("UNINITIALIZED_ENUM_ENTRY", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtExpression::class, getRendererFactory())
    val UNINITIALIZED_ENUM_COMPANION: KtDiagnosticFactory1<FirRegularClassSymbol> = KtDiagnosticFactory1("UNINITIALIZED_ENUM_COMPANION", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtExpression::class, getRendererFactory())
    val VAL_REASSIGNMENT: KtDiagnosticFactory1<FirVariableSymbol<*>> = KtDiagnosticFactory1("VAL_REASSIGNMENT", ERROR, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, KtExpression::class, getRendererFactory())
    val VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR: KtDiagnosticFactory1<FirBackingFieldSymbol> = KtDiagnosticFactory1("VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR", ERROR, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, KtExpression::class, getRendererFactory())
    val CAPTURED_VAL_INITIALIZATION: KtDiagnosticFactory1<FirPropertySymbol> = KtDiagnosticFactory1("CAPTURED_VAL_INITIALIZATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val CAPTURED_MEMBER_VAL_INITIALIZATION: KtDiagnosticFactory1<FirPropertySymbol> = KtDiagnosticFactory1("CAPTURED_MEMBER_VAL_INITIALIZATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val NON_INLINE_MEMBER_VAL_INITIALIZATION: KtDiagnosticFactory1<FirPropertySymbol> = KtDiagnosticFactory1("NON_INLINE_MEMBER_VAL_INITIALIZATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val SETTER_PROJECTED_OUT: KtDiagnosticFactory3<ConeKotlinType, String, FirPropertySymbol> = KtDiagnosticFactory3("SETTER_PROJECTED_OUT", ERROR, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, KtBinaryExpression::class, getRendererFactory())
    val WRONG_INVOCATION_KIND: KtDiagnosticFactory3<FirBasedSymbol<*>, EventOccurrencesRange, EventOccurrencesRange> = KtDiagnosticFactory3("WRONG_INVOCATION_KIND", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val LEAKED_IN_PLACE_LAMBDA: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("LEAKED_IN_PLACE_LAMBDA", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val VARIABLE_WITH_NO_TYPE_NO_INITIALIZER: KtDiagnosticFactory0 = KtDiagnosticFactory0("VARIABLE_WITH_NO_TYPE_NO_INITIALIZER", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtVariableDeclaration::class, getRendererFactory())
    val INITIALIZATION_BEFORE_DECLARATION: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("INITIALIZATION_BEFORE_DECLARATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val INITIALIZATION_BEFORE_DECLARATION_WARNING: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("INITIALIZATION_BEFORE_DECLARATION_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val UNREACHABLE_CODE: KtDiagnosticFactory2<Set<KtSourceElement>, Set<KtSourceElement>> = KtDiagnosticFactory2("UNREACHABLE_CODE", WARNING, SourceElementPositioningStrategies.UNREACHABLE_CODE, KtElement::class, getRendererFactory())
    val SENSELESS_COMPARISON: KtDiagnosticFactory1<Boolean> = KtDiagnosticFactory1("SENSELESS_COMPARISON", WARNING, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val SENSELESS_NULL_IN_WHEN: KtDiagnosticFactory0 = KtDiagnosticFactory0("SENSELESS_NULL_IN_WHEN", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM: KtDiagnosticFactory0 = KtDiagnosticFactory0("TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val RETURN_VALUE_NOT_USED: KtDiagnosticFactory1<Name?> = KtDiagnosticFactory1("RETURN_VALUE_NOT_USED", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())

    // Nullability
    val UNSAFE_CALL: KtDiagnosticFactory2<ConeKotlinType, FirExpression?> = KtDiagnosticFactory2("UNSAFE_CALL", ERROR, SourceElementPositioningStrategies.DOT_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val UNSAFE_IMPLICIT_INVOKE_CALL: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("UNSAFE_IMPLICIT_INVOKE_CALL", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val UNSAFE_INFIX_CALL: KtDiagnosticFactory4<ConeKotlinType, FirExpression, String, FirExpression?> = KtDiagnosticFactory4("UNSAFE_INFIX_CALL", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtExpression::class, getRendererFactory())
    val UNSAFE_OPERATOR_CALL: KtDiagnosticFactory4<ConeKotlinType, FirExpression, String, FirExpression?> = KtDiagnosticFactory4("UNSAFE_OPERATOR_CALL", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtExpression::class, getRendererFactory())
    val ITERATOR_ON_NULLABLE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ITERATOR_ON_NULLABLE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val UNNECESSARY_SAFE_CALL: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("UNNECESSARY_SAFE_CALL", WARNING, SourceElementPositioningStrategies.SAFE_ACCESS, PsiElement::class, getRendererFactory())
    val UNEXPECTED_SAFE_CALL: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNEXPECTED_SAFE_CALL", ERROR, SourceElementPositioningStrategies.SAFE_ACCESS, PsiElement::class, getRendererFactory())
    val UNNECESSARY_NOT_NULL_ASSERTION: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("UNNECESSARY_NOT_NULL_ASSERTION", WARNING, SourceElementPositioningStrategies.OPERATOR, KtExpression::class, getRendererFactory())
    val NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION: KtDiagnosticFactory0 = KtDiagnosticFactory0("NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION", WARNING, SourceElementPositioningStrategies.OPERATOR, KtExpression::class, getRendererFactory())
    val NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE: KtDiagnosticFactory0 = KtDiagnosticFactory0("NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE", WARNING, SourceElementPositioningStrategies.OPERATOR, KtExpression::class, getRendererFactory())
    val USELESS_ELVIS: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("USELESS_ELVIS", WARNING, SourceElementPositioningStrategies.USELESS_ELVIS, KtBinaryExpression::class, getRendererFactory())
    val USELESS_ELVIS_RIGHT_IS_NULL: KtDiagnosticFactory0 = KtDiagnosticFactory0("USELESS_ELVIS_RIGHT_IS_NULL", WARNING, SourceElementPositioningStrategies.USELESS_ELVIS, KtBinaryExpression::class, getRendererFactory())

    // Casts and is-checks
    val CANNOT_CHECK_FOR_ERASED: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("CANNOT_CHECK_FOR_ERASED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CAST_NEVER_SUCCEEDS: KtDiagnosticFactory0 = KtDiagnosticFactory0("CAST_NEVER_SUCCEEDS", WARNING, SourceElementPositioningStrategies.OPERATOR, KtBinaryExpressionWithTypeRHS::class, getRendererFactory())
    val USELESS_CAST: KtDiagnosticFactory0 = KtDiagnosticFactory0("USELESS_CAST", WARNING, SourceElementPositioningStrategies.AS_TYPE, KtBinaryExpressionWithTypeRHS::class, getRendererFactory())
    val UNCHECKED_CAST: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("UNCHECKED_CAST", WARNING, SourceElementPositioningStrategies.AS_TYPE, KtBinaryExpressionWithTypeRHS::class, getRendererFactory())
    val USELESS_IS_CHECK: KtDiagnosticFactory1<Boolean> = KtDiagnosticFactory1("USELESS_IS_CHECK", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val IS_ENUM_ENTRY: KtDiagnosticFactory0 = KtDiagnosticFactory0("IS_ENUM_ENTRY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val DYNAMIC_NOT_ALLOWED: KtDiagnosticFactory0 = KtDiagnosticFactory0("DYNAMIC_NOT_ALLOWED", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val ENUM_ENTRY_AS_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ENUM_ENTRY_AS_TYPE", ERROR, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, KtElement::class, getRendererFactory())

    // When expressions
    val EXPECTED_CONDITION: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPECTED_CONDITION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtWhenCondition::class, getRendererFactory())
    val NO_ELSE_IN_WHEN: KtDiagnosticFactory2<List<WhenMissingCase>, String> = KtDiagnosticFactory2("NO_ELSE_IN_WHEN", ERROR, SourceElementPositioningStrategies.WHEN_EXPRESSION, KtWhenExpression::class, getRendererFactory())
    val INVALID_IF_AS_EXPRESSION: KtDiagnosticFactory0 = KtDiagnosticFactory0("INVALID_IF_AS_EXPRESSION", ERROR, SourceElementPositioningStrategies.IF_EXPRESSION, KtIfExpression::class, getRendererFactory())
    val ELSE_MISPLACED_IN_WHEN: KtDiagnosticFactory0 = KtDiagnosticFactory0("ELSE_MISPLACED_IN_WHEN", ERROR, SourceElementPositioningStrategies.ELSE_ENTRY, KtWhenEntry::class, getRendererFactory())
    val REDUNDANT_ELSE_IN_WHEN: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_ELSE_IN_WHEN", WARNING, SourceElementPositioningStrategies.ELSE_ENTRY, KtWhenEntry::class, getRendererFactory())
    val ILLEGAL_DECLARATION_IN_WHEN_SUBJECT: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("ILLEGAL_DECLARATION_IN_WHEN_SUBJECT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT: KtDiagnosticFactory0 = KtDiagnosticFactory0("COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT", ERROR, SourceElementPositioningStrategies.COMMAS, PsiElement::class, getRendererFactory())
    val DUPLICATE_BRANCH_CONDITION_IN_WHEN: KtDiagnosticFactory0 = KtDiagnosticFactory0("DUPLICATE_BRANCH_CONDITION_IN_WHEN", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val CONFUSING_BRANCH_CONDITION: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("CONFUSING_BRANCH_CONDITION", ProhibitConfusingSyntaxInWhenBranches, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val WRONG_CONDITION_SUGGEST_GUARD: KtDiagnosticFactory0 = KtDiagnosticFactory0("WRONG_CONDITION_SUGGEST_GUARD", ERROR, SourceElementPositioningStrategies.OPERATOR, PsiElement::class, getRendererFactory())
    val COMMA_IN_WHEN_CONDITION_WITH_WHEN_GUARD: KtDiagnosticFactory0 = KtDiagnosticFactory0("COMMA_IN_WHEN_CONDITION_WITH_WHEN_GUARD", ERROR, SourceElementPositioningStrategies.WHEN_GUARD, PsiElement::class, getRendererFactory())
    val WHEN_GUARD_WITHOUT_SUBJECT: KtDiagnosticFactory0 = KtDiagnosticFactory0("WHEN_GUARD_WITHOUT_SUBJECT", ERROR, SourceElementPositioningStrategies.WHEN_GUARD, PsiElement::class, getRendererFactory())
    val INFERRED_INVISIBLE_WHEN_TYPE: KtDiagnosticFactoryForDeprecation2<ConeKotlinType, String> = KtDiagnosticFactoryForDeprecation2("INFERRED_INVISIBLE_WHEN_TYPE", ForbidInferOfInvisibleTypeAsReifiedVarargOrReturnType, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())

    // Context tracking
    val TYPE_PARAMETER_IS_NOT_AN_EXPRESSION: KtDiagnosticFactory1<FirTypeParameterSymbol> = KtDiagnosticFactory1("TYPE_PARAMETER_IS_NOT_AN_EXPRESSION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtSimpleNameExpression::class, getRendererFactory())
    val TYPE_PARAMETER_ON_LHS_OF_DOT: KtDiagnosticFactory1<FirTypeParameterSymbol> = KtDiagnosticFactory1("TYPE_PARAMETER_ON_LHS_OF_DOT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtSimpleNameExpression::class, getRendererFactory())
    val NO_COMPANION_OBJECT: KtDiagnosticFactory1<FirClassLikeSymbol<*>> = KtDiagnosticFactory1("NO_COMPANION_OBJECT", ERROR, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, KtExpression::class, getRendererFactory())
    val EXPRESSION_EXPECTED_PACKAGE_FOUND: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXPRESSION_EXPECTED_PACKAGE_FOUND", ERROR, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, KtExpression::class, getRendererFactory())

    // Function contracts
    val ERROR_IN_CONTRACT_DESCRIPTION: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("ERROR_IN_CONTRACT_DESCRIPTION", ERROR, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val CONTRACT_NOT_ALLOWED: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("CONTRACT_NOT_ALLOWED", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, KtElement::class, getRendererFactory())

    // Conventions
    val NO_GET_METHOD: KtDiagnosticFactory0 = KtDiagnosticFactory0("NO_GET_METHOD", ERROR, SourceElementPositioningStrategies.ARRAY_ACCESS, KtArrayAccessExpression::class, getRendererFactory())
    val NO_SET_METHOD: KtDiagnosticFactory0 = KtDiagnosticFactory0("NO_SET_METHOD", ERROR, SourceElementPositioningStrategies.ARRAY_ACCESS, KtArrayAccessExpression::class, getRendererFactory())
    val ITERATOR_MISSING: KtDiagnosticFactory0 = KtDiagnosticFactory0("ITERATOR_MISSING", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val HAS_NEXT_MISSING: KtDiagnosticFactory0 = KtDiagnosticFactory0("HAS_NEXT_MISSING", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val NEXT_MISSING: KtDiagnosticFactory0 = KtDiagnosticFactory0("NEXT_MISSING", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val HAS_NEXT_FUNCTION_NONE_APPLICABLE: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("HAS_NEXT_FUNCTION_NONE_APPLICABLE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val NEXT_NONE_APPLICABLE: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("NEXT_NONE_APPLICABLE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val DELEGATE_SPECIAL_FUNCTION_MISSING: KtDiagnosticFactory3<String, ConeKotlinType, String> = KtDiagnosticFactory3("DELEGATE_SPECIAL_FUNCTION_MISSING", ERROR, SourceElementPositioningStrategies.PROPERTY_DELEGATE_BY_KEYWORD, KtExpression::class, getRendererFactory())
    val DELEGATE_SPECIAL_FUNCTION_AMBIGUITY: KtDiagnosticFactory2<String, Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory2("DELEGATE_SPECIAL_FUNCTION_AMBIGUITY", ERROR, SourceElementPositioningStrategies.PROPERTY_DELEGATE_BY_KEYWORD, KtExpression::class, getRendererFactory())
    val DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE: KtDiagnosticFactory2<String, Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory2("DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE", ERROR, SourceElementPositioningStrategies.PROPERTY_DELEGATE_BY_KEYWORD, KtExpression::class, getRendererFactory())
    val DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH: KtDiagnosticFactory3<String, ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory3("DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH", ERROR, SourceElementPositioningStrategies.PROPERTY_DELEGATE_BY_KEYWORD, KtExpression::class, getRendererFactory())
    val UNDERSCORE_IS_RESERVED: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNDERSCORE_IS_RESERVED", ERROR, SourceElementPositioningStrategies.NAME_IDENTIFIER, PsiElement::class, getRendererFactory())
    val UNDERSCORE_USAGE_WITHOUT_BACKTICKS: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNDERSCORE_USAGE_WITHOUT_BACKTICKS", ERROR, SourceElementPositioningStrategies.NAME_IDENTIFIER, PsiElement::class, getRendererFactory())
    val RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER", WARNING, SourceElementPositioningStrategies.DEFAULT, KtNameReferenceExpression::class, getRendererFactory())
    val INVALID_CHARACTERS: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INVALID_CHARACTERS", ERROR, SourceElementPositioningStrategies.NAME_IDENTIFIER, PsiElement::class, getRendererFactory())
    val EQUALITY_NOT_APPLICABLE: KtDiagnosticFactory3<String, ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory3("EQUALITY_NOT_APPLICABLE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtBinaryExpression::class, getRendererFactory())
    val EQUALITY_NOT_APPLICABLE_WARNING: KtDiagnosticFactory3<String, ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory3("EQUALITY_NOT_APPLICABLE_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, KtBinaryExpression::class, getRendererFactory())
    val INCOMPATIBLE_ENUM_COMPARISON_ERROR: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("INCOMPATIBLE_ENUM_COMPARISON_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val INCOMPATIBLE_ENUM_COMPARISON: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("INCOMPATIBLE_ENUM_COMPARISON", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val FORBIDDEN_IDENTITY_EQUALS: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("FORBIDDEN_IDENTITY_EQUALS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val FORBIDDEN_IDENTITY_EQUALS_WARNING: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("FORBIDDEN_IDENTITY_EQUALS_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val DEPRECATED_IDENTITY_EQUALS: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("DEPRECATED_IDENTITY_EQUALS", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val IMPLICIT_BOXING_IN_IDENTITY_EQUALS: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("IMPLICIT_BOXING_IN_IDENTITY_EQUALS", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val INC_DEC_SHOULD_NOT_RETURN_UNIT: KtDiagnosticFactory0 = KtDiagnosticFactory0("INC_DEC_SHOULD_NOT_RETURN_UNIT", ERROR, SourceElementPositioningStrategies.OPERATOR, KtExpression::class, getRendererFactory())
    val ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT: KtDiagnosticFactory2<FirNamedFunctionSymbol, String> = KtDiagnosticFactory2("ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT", ERROR, SourceElementPositioningStrategies.OPERATOR, KtExpression::class, getRendererFactory())
    val NOT_FUNCTION_AS_OPERATOR: KtDiagnosticFactory2<String, FirBasedSymbol<*>> = KtDiagnosticFactory2("NOT_FUNCTION_AS_OPERATOR", ERROR, SourceElementPositioningStrategies.OPERATOR, PsiElement::class, getRendererFactory())
    val DSL_SCOPE_VIOLATION: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("DSL_SCOPE_VIOLATION", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())

    // Type alias
    val TOPLEVEL_TYPEALIASES_ONLY: KtDiagnosticFactory0 = KtDiagnosticFactory0("TOPLEVEL_TYPEALIASES_ONLY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtTypeAlias::class, getRendererFactory())
    val RECURSIVE_TYPEALIAS_EXPANSION: KtDiagnosticFactory0 = KtDiagnosticFactory0("RECURSIVE_TYPEALIAS_EXPANSION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val TYPEALIAS_SHOULD_EXPAND_TO_CLASS: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("TYPEALIAS_SHOULD_EXPAND_TO_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION", ProhibitConstructorAndSupertypeOnTypealiasWithTypeProjection, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS: KtDiagnosticFactory1<Set<FirTypeParameterSymbol>> = KtDiagnosticFactory1("TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())

    // Extra checkers
    val REDUNDANT_VISIBILITY_MODIFIER: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_VISIBILITY_MODIFIER", WARNING, SourceElementPositioningStrategies.VISIBILITY_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val REDUNDANT_MODALITY_MODIFIER: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_MODALITY_MODIFIER", WARNING, SourceElementPositioningStrategies.MODALITY_MODIFIER, KtModifierListOwner::class, getRendererFactory())
    val REDUNDANT_RETURN_UNIT_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_RETURN_UNIT_TYPE", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CAN_BE_VAL: KtDiagnosticFactory0 = KtDiagnosticFactory0("CAN_BE_VAL", WARNING, SourceElementPositioningStrategies.VAL_OR_VAR_NODE, KtDeclaration::class, getRendererFactory())
    val CAN_BE_VAL_LATEINIT: KtDiagnosticFactory0 = KtDiagnosticFactory0("CAN_BE_VAL_LATEINIT", WARNING, SourceElementPositioningStrategies.VAL_OR_VAR_NODE, KtDeclaration::class, getRendererFactory())
    val CAN_BE_VAL_DELAYED_INITIALIZATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("CAN_BE_VAL_DELAYED_INITIALIZATION", WARNING, SourceElementPositioningStrategies.VAL_OR_VAR_NODE, KtDeclaration::class, getRendererFactory())
    val REDUNDANT_CALL_OF_CONVERSION_METHOD: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_CALL_OF_CONVERSION_METHOD", WARNING, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS: KtDiagnosticFactory0 = KtDiagnosticFactory0("ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS", WARNING, SourceElementPositioningStrategies.OPERATOR, KtExpression::class, getRendererFactory())
    val EMPTY_RANGE: KtDiagnosticFactory0 = KtDiagnosticFactory0("EMPTY_RANGE", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val REDUNDANT_SETTER_PARAMETER_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_SETTER_PARAMETER_TYPE", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UNUSED_VARIABLE: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNUSED_VARIABLE", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val ASSIGNED_VALUE_IS_NEVER_READ: KtDiagnosticFactory0 = KtDiagnosticFactory0("ASSIGNED_VALUE_IS_NEVER_READ", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val VARIABLE_INITIALIZER_IS_REDUNDANT: KtDiagnosticFactory0 = KtDiagnosticFactory0("VARIABLE_INITIALIZER_IS_REDUNDANT", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val VARIABLE_NEVER_READ: KtDiagnosticFactory0 = KtDiagnosticFactory0("VARIABLE_NEVER_READ", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedDeclaration::class, getRendererFactory())
    val USELESS_CALL_ON_NOT_NULL: KtDiagnosticFactory0 = KtDiagnosticFactory0("USELESS_CALL_ON_NOT_NULL", WARNING, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val UNUSED_ANONYMOUS_PARAMETER: KtDiagnosticFactory1<FirValueParameterSymbol> = KtDiagnosticFactory1("UNUSED_ANONYMOUS_PARAMETER", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, KtElement::class, getRendererFactory())
    val UNUSED_EXPRESSION: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNUSED_EXPRESSION", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UNUSED_LAMBDA_EXPRESSION: KtDiagnosticFactory0 = KtDiagnosticFactory0("UNUSED_LAMBDA_EXPRESSION", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Returns
    val RETURN_NOT_ALLOWED: KtDiagnosticFactory0 = KtDiagnosticFactory0("RETURN_NOT_ALLOWED", ERROR, SourceElementPositioningStrategies.RETURN_WITH_LABEL, KtReturnExpression::class, getRendererFactory())
    val NOT_A_FUNCTION_LABEL: KtDiagnosticFactory0 = KtDiagnosticFactory0("NOT_A_FUNCTION_LABEL", ERROR, SourceElementPositioningStrategies.RETURN_WITH_LABEL, KtReturnExpression::class, getRendererFactory())
    val RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY: KtDiagnosticFactory0 = KtDiagnosticFactory0("RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY", ERROR, SourceElementPositioningStrategies.RETURN_WITH_LABEL, KtReturnExpression::class, getRendererFactory())
    val RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_WARNING", WARNING, SourceElementPositioningStrategies.RETURN_WITH_LABEL, KtReturnExpression::class, getRendererFactory())
    val RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE", ERROR, SourceElementPositioningStrategies.RETURN_WITH_LABEL, KtReturnExpression::class, getRendererFactory())
    val NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY: KtDiagnosticFactory0 = KtDiagnosticFactory0("NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY", ERROR, SourceElementPositioningStrategies.DECLARATION_WITH_BODY, KtDeclarationWithBody::class, getRendererFactory())
    val REDUNDANT_RETURN: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_RETURN", WARNING, SourceElementPositioningStrategies.RETURN_WITH_LABEL, KtReturnExpression::class, getRendererFactory())
    val ANONYMOUS_INITIALIZER_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANONYMOUS_INITIALIZER_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, KtAnonymousInitializer::class, getRendererFactory())

    // Inline
    val USAGE_IS_NOT_INLINABLE: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("USAGE_IS_NOT_INLINABLE", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val NON_LOCAL_RETURN_NOT_ALLOWED: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("NON_LOCAL_RETURN_NOT_ALLOWED", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val NOT_YET_SUPPORTED_IN_INLINE: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("NOT_YET_SUPPORTED_IN_INLINE", ERROR, SourceElementPositioningStrategies.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT, KtDeclaration::class, getRendererFactory())
    val NOTHING_TO_INLINE: KtDiagnosticFactory0 = KtDiagnosticFactory0("NOTHING_TO_INLINE", WARNING, SourceElementPositioningStrategies.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT, KtDeclaration::class, getRendererFactory())
    val NULLABLE_INLINE_PARAMETER: KtDiagnosticFactory2<FirValueParameterSymbol, FirBasedSymbol<*>> = KtDiagnosticFactory2("NULLABLE_INLINE_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtDeclaration::class, getRendererFactory())
    val RECURSION_IN_INLINE: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("RECURSION_IN_INLINE", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val NON_PUBLIC_CALL_FROM_PUBLIC_INLINE: KtDiagnosticFactory2<FirBasedSymbol<*>, FirBasedSymbol<*>> = KtDiagnosticFactory2("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE: KtDiagnosticFactory2<FirBasedSymbol<*>, FirBasedSymbol<*>> = KtDiagnosticFactory2("NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val NON_PUBLIC_CALL_FROM_PUBLIC_INLINE_DEPRECATION: KtDiagnosticFactory2<FirBasedSymbol<*>, FirBasedSymbol<*>> = KtDiagnosticFactory2("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE_DEPRECATION", WARNING, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val NON_PUBLIC_DATA_COPY_CALL_FROM_PUBLIC_INLINE: KtDiagnosticFactoryForDeprecation1<FirBasedSymbol<*>> = KtDiagnosticFactoryForDeprecation1("NON_PUBLIC_DATA_COPY_CALL_FROM_PUBLIC_INLINE", ErrorAboutDataClassCopyVisibilityChange, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE: KtDiagnosticFactory2<FirBasedSymbol<*>, FirBasedSymbol<*>> = KtDiagnosticFactory2("PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR: KtDiagnosticFactory2<FirBasedSymbol<*>, FirBasedSymbol<*>> = KtDiagnosticFactory2("PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val PRIVATE_CLASS_MEMBER_FROM_INLINE: KtDiagnosticFactory2<FirBasedSymbol<*>, FirBasedSymbol<*>> = KtDiagnosticFactory2("PRIVATE_CLASS_MEMBER_FROM_INLINE", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val SUPER_CALL_FROM_PUBLIC_INLINE: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("SUPER_CALL_FROM_PUBLIC_INLINE", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val DECLARATION_CANT_BE_INLINED: KtDiagnosticFactory0 = KtDiagnosticFactory0("DECLARATION_CANT_BE_INLINED", ERROR, SourceElementPositioningStrategies.INLINE_FUN_MODIFIER, KtDeclaration::class, getRendererFactory())
    val DECLARATION_CANT_BE_INLINED_DEPRECATION: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("DECLARATION_CANT_BE_INLINED_DEPRECATION", ProhibitInlineModifierOnPrimaryConstructorParameters, SourceElementPositioningStrategies.INLINE_FUN_MODIFIER, KtDeclaration::class, getRendererFactory())
    val OVERRIDE_BY_INLINE: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERRIDE_BY_INLINE", WARNING, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtDeclaration::class, getRendererFactory())
    val NON_INTERNAL_PUBLISHED_API: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_INTERNAL_PUBLISHED_API", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE: KtDiagnosticFactory1<FirValueParameterSymbol> = KtDiagnosticFactory1("INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val NOT_SUPPORTED_INLINE_PARAMETER_IN_INLINE_PARAMETER_DEFAULT_VALUE: KtDiagnosticFactory1<FirValueParameterSymbol> = KtDiagnosticFactory1("NOT_SUPPORTED_INLINE_PARAMETER_IN_INLINE_PARAMETER_DEFAULT_VALUE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val REIFIED_TYPE_PARAMETER_IN_OVERRIDE: KtDiagnosticFactory0 = KtDiagnosticFactory0("REIFIED_TYPE_PARAMETER_IN_OVERRIDE", ERROR, SourceElementPositioningStrategies.REIFIED_MODIFIER, KtElement::class, getRendererFactory())
    val INLINE_PROPERTY_WITH_BACKING_FIELD: KtDiagnosticFactory0 = KtDiagnosticFactory0("INLINE_PROPERTY_WITH_BACKING_FIELD", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtDeclaration::class, getRendererFactory())
    val INLINE_PROPERTY_WITH_BACKING_FIELD_DEPRECATION: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("INLINE_PROPERTY_WITH_BACKING_FIELD_DEPRECATION", ProhibitInlineModifierOnPrimaryConstructorParameters, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, KtDeclaration::class, getRendererFactory())
    val ILLEGAL_INLINE_PARAMETER_MODIFIER: KtDiagnosticFactory0 = KtDiagnosticFactory0("ILLEGAL_INLINE_PARAMETER_MODIFIER", ERROR, SourceElementPositioningStrategies.INLINE_PARAMETER_MODIFIER, KtElement::class, getRendererFactory())
    val INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED: KtDiagnosticFactory0 = KtDiagnosticFactory0("INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED", ERROR, SourceElementPositioningStrategies.DEFAULT, KtParameter::class, getRendererFactory())
    val INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedFunction::class, getRendererFactory())
    val INLINE_CLASS_DEPRECATED: KtDiagnosticFactory0 = KtDiagnosticFactory0("INLINE_CLASS_DEPRECATED", WARNING, SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER, KtElement::class, getRendererFactory())
    val LESS_VISIBLE_TYPE_ACCESS_IN_INLINE: KtDiagnosticFactoryForDeprecation3<EffectiveVisibility, ConeKotlinType, EffectiveVisibility> = KtDiagnosticFactoryForDeprecation3("LESS_VISIBLE_TYPE_ACCESS_IN_INLINE", ForbidExposingLessVisibleTypesInInline, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE: KtDiagnosticFactoryForDeprecation4<FirBasedSymbol<*>, EffectiveVisibility, ConeKotlinType, EffectiveVisibility> = KtDiagnosticFactoryForDeprecation4("LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE", ForbidExposingLessVisibleTypesInInline, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE: KtDiagnosticFactoryForDeprecation4<FirBasedSymbol<*>, EffectiveVisibility, FirRegularClassSymbol, EffectiveVisibility> = KtDiagnosticFactoryForDeprecation4("LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE", ForbidExposingLessVisibleTypesInInline, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())
    val CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE: KtDiagnosticFactoryForDeprecation3<FirBasedSymbol<*>, EffectiveVisibility, EffectiveVisibility> = KtDiagnosticFactoryForDeprecation3("CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE", ForbidExposingLessVisibleTypesInInline, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, KtElement::class, getRendererFactory())

    // Imports
    val CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON: KtDiagnosticFactory1<Name> = KtDiagnosticFactory1("CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON", ERROR, SourceElementPositioningStrategies.IMPORT_LAST_NAME, KtImportDirective::class, getRendererFactory())
    val PACKAGE_CANNOT_BE_IMPORTED: KtDiagnosticFactory0 = KtDiagnosticFactory0("PACKAGE_CANNOT_BE_IMPORTED", ERROR, SourceElementPositioningStrategies.IMPORT_LAST_NAME, KtImportDirective::class, getRendererFactory())
    val CANNOT_BE_IMPORTED: KtDiagnosticFactory1<Name> = KtDiagnosticFactory1("CANNOT_BE_IMPORTED", ERROR, SourceElementPositioningStrategies.IMPORT_LAST_NAME, KtImportDirective::class, getRendererFactory())
    val CONFLICTING_IMPORT: KtDiagnosticFactory1<Name> = KtDiagnosticFactory1("CONFLICTING_IMPORT", ERROR, SourceElementPositioningStrategies.IMPORT_ALIAS, KtImportDirective::class, getRendererFactory())
    val OPERATOR_RENAMED_ON_IMPORT: KtDiagnosticFactory0 = KtDiagnosticFactory0("OPERATOR_RENAMED_ON_IMPORT", ERROR, SourceElementPositioningStrategies.IMPORT_LAST_NAME, KtImportDirective::class, getRendererFactory())
    val TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT: KtDiagnosticFactoryForDeprecation2<Name, Name> = KtDiagnosticFactoryForDeprecation2("TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT", ProhibitTypealiasAsCallableQualifierInImport, SourceElementPositioningStrategies.IMPORT_LAST_BUT_ONE_NAME, KtImportDirective::class, getRendererFactory())

    // Suspend errors
    val ILLEGAL_SUSPEND_FUNCTION_CALL: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("ILLEGAL_SUSPEND_FUNCTION_CALL", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val ILLEGAL_SUSPEND_PROPERTY_ACCESS: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("ILLEGAL_SUSPEND_PROPERTY_ACCESS", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val NON_LOCAL_SUSPENSION_POINT: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_LOCAL_SUSPENSION_POINT", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL: KtDiagnosticFactory0 = KtDiagnosticFactory0("ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND: KtDiagnosticFactory0 = KtDiagnosticFactory0("MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND_FUN: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND_FUN", ModifierNonBuiltinSuspendFunError, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val RETURN_FOR_BUILT_IN_SUSPEND: KtDiagnosticFactory0 = KtDiagnosticFactory0("RETURN_FOR_BUILT_IN_SUSPEND", ERROR, SourceElementPositioningStrategies.DEFAULT, KtReturnExpression::class, getRendererFactory())
    val MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES: KtDiagnosticFactory0 = KtDiagnosticFactory0("MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES", ERROR, SourceElementPositioningStrategies.SUPERTYPES_LIST, PsiElement::class, getRendererFactory())
    val MIXING_FUNCTIONAL_KINDS_IN_SUPERTYPES: KtDiagnosticFactory1<Set<FunctionTypeKind>> = KtDiagnosticFactory1("MIXING_FUNCTIONAL_KINDS_IN_SUPERTYPES", ERROR, SourceElementPositioningStrategies.SUPERTYPES_LIST, PsiElement::class, getRendererFactory())

    // label
    val REDUNDANT_LABEL_WARNING: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_LABEL_WARNING", WARNING, SourceElementPositioningStrategies.LABEL, KtLabelReferenceExpression::class, getRendererFactory())
    val MULTIPLE_LABELS_ARE_FORBIDDEN: KtDiagnosticFactory0 = KtDiagnosticFactory0("MULTIPLE_LABELS_ARE_FORBIDDEN", ERROR, SourceElementPositioningStrategies.LABEL, KtLabelReferenceExpression::class, getRendererFactory())

    // Enum.entries resolve deprecations
    val DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATED_ACCESS_TO_ENTRY_PROPERTY_FROM_ENUM: KtDiagnosticFactory0 = KtDiagnosticFactory0("DEPRECATED_ACCESS_TO_ENTRY_PROPERTY_FROM_ENUM", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATED_ACCESS_TO_ENTRIES_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("DEPRECATED_ACCESS_TO_ENTRIES_PROPERTY", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATED_ACCESS_TO_ENUM_ENTRY_PROPERTY_AS_REFERENCE: KtDiagnosticFactory0 = KtDiagnosticFactory0("DEPRECATED_ACCESS_TO_ENUM_ENTRY_PROPERTY_AS_REFERENCE", WARNING, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val DEPRECATED_ACCESS_TO_ENTRIES_AS_QUALIFIER: KtDiagnosticFactory0 = KtDiagnosticFactory0("DEPRECATED_ACCESS_TO_ENTRIES_AS_QUALIFIER", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DECLARATION_OF_ENUM_ENTRY_ENTRIES: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("DECLARATION_OF_ENUM_ENTRY_ENTRIES", ForbidEnumEntryNamedEntries, SourceElementPositioningStrategies.DECLARATION_NAME, KtEnumEntry::class, getRendererFactory())

    // Compatibility issues
    val INCOMPATIBLE_CLASS: KtDiagnosticFactory2<String, IncompatibleVersionErrorData<*>> = KtDiagnosticFactory2("INCOMPATIBLE_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val PRE_RELEASE_CLASS: KtDiagnosticFactory2<String, List<String>> = KtDiagnosticFactory2("PRE_RELEASE_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val IR_WITH_UNSTABLE_ABI_COMPILED_CLASS: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("IR_WITH_UNSTABLE_ABI_COMPILED_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Builder inference
    val BUILDER_INFERENCE_STUB_RECEIVER: KtDiagnosticFactory2<Name, Name> = KtDiagnosticFactory2("BUILDER_INFERENCE_STUB_RECEIVER", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION: KtDiagnosticFactory2<Name, Name> = KtDiagnosticFactory2("BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = FirErrorsDefaultMessages
}
