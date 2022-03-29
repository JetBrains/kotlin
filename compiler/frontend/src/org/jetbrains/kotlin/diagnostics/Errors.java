/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics;

import com.google.common.collect.ImmutableSet;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.config.LanguageVersion;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.rendering.DeclarationWithDiagnosticComponents;
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticRenderer;
import org.jetbrains.kotlin.lexer.KtKeywordToken;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.VarianceConflictDiagnosticData;
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableTypeConstructor;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.tower.WrongResolutionToClassifier;
import org.jetbrains.kotlin.resolve.calls.util.BuilderLambdaLabelingInfo;
import org.jetbrains.kotlin.resolve.deprecation.DescriptorBasedDeprecationInfo;
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.Incompatible;
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData;
import org.jetbrains.kotlin.types.KotlinType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.kotlin.config.LanguageFeature.ReportTypeVarianceConflictOnQualifierArguments;
import static org.jetbrains.kotlin.diagnostics.ClassicPositioningStrategies.ACTUAL_DECLARATION_NAME;
import static org.jetbrains.kotlin.diagnostics.ClassicPositioningStrategies.INCOMPATIBLE_DECLARATION;
import static org.jetbrains.kotlin.diagnostics.PositioningStrategies.*;
import static org.jetbrains.kotlin.diagnostics.Severity.*;

/**
 * For error messages, see DefaultErrorMessages and IdeErrorMessages.
 */
public interface Errors {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Meta-errors: unsupported features, failure

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    DiagnosticFactory1<PsiElement, String> UNSUPPORTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> UNSUPPORTED_WARNING = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory1<PsiElement, String> NEW_INFERENCE_ERROR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> NEW_INFERENCE_DIAGNOSTIC = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<KtElement> NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory1<PsiElement, Pair<LanguageFeature, LanguageVersionSettings>> UNSUPPORTED_FEATURE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Throwable> EXCEPTION_FROM_ANALYZER = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<PsiElement, String> MISSING_STDLIB = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<PsiElement, Pair<LanguageFeature, LanguageVersionSettings>> EXPERIMENTAL_FEATURE_WARNING =
            DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<PsiElement, Pair<LanguageFeature, LanguageVersionSettings>> EXPERIMENTAL_FEATURE_ERROR =
            DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtElement> EXPLICIT_BACKING_FIELDS_UNSUPPORTED = DiagnosticFactory0.create(ERROR);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Generic errors/warnings: applicable in many contexts

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    DiagnosticFactory1<PsiElement, Collection<DeclarationDescriptor>> REDECLARATION =
            DiagnosticFactory1.create(ERROR, FOR_REDECLARATION);
    DiagnosticFactory1<PsiElement, String> PACKAGE_OR_CLASSIFIER_REDECLARATION =
            DiagnosticFactory1.create(ERROR, FOR_REDECLARATION);
    DiagnosticFactory0<KtParameter> DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE = DiagnosticFactory0.create(ERROR, DECLARATION_NAME);

    DiagnosticFactory1<KtDeclaration, CallableMemberDescriptor> EXTENSION_SHADOWED_BY_MEMBER =
            DiagnosticFactory1.create(WARNING, FOR_REDECLARATION);
    DiagnosticFactory1<KtDeclaration, ConstructorDescriptor> EXTENSION_FUNCTION_SHADOWED_BY_INNER_CLASS_CONSTRUCTOR =
            DiagnosticFactory1.create(WARNING, FOR_REDECLARATION);
    DiagnosticFactory2<KtDeclaration, PropertyDescriptor, FunctionDescriptor> EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE =
            DiagnosticFactory2.create(WARNING, FOR_REDECLARATION);

    DiagnosticFactory1<KtReferenceExpression, KtReferenceExpression> UNRESOLVED_REFERENCE =
            DiagnosticFactory1.create(ERROR, FOR_UNRESOLVED_REFERENCE);

    DiagnosticFactory2<PsiElement, DeclarationDescriptor, String> DEPRECATION = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, DeclarationDescriptor, String> DEPRECATION_ERROR = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory3<PsiElement, TypeAliasDescriptor, DeclarationDescriptor, String> TYPEALIAS_EXPANSION_DEPRECATION =
            DiagnosticFactory3.create(WARNING);
    DiagnosticFactory3<PsiElement, TypeAliasDescriptor, DeclarationDescriptor, String> TYPEALIAS_EXPANSION_DEPRECATION_ERROR =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<PsiElement, DeclarationDescriptor, VersionRequirement.Version, Pair<String, String>>
            VERSION_REQUIREMENT_DEPRECATION = DiagnosticFactory3.create(WARNING);
    DiagnosticFactory3<PsiElement, DeclarationDescriptor, VersionRequirement.Version, Pair<String, String>>
            VERSION_REQUIREMENT_DEPRECATION_ERROR = DiagnosticFactory3.create(ERROR);
    // descriptor and deprecation infos are needed only for IDE quickfix for this warning
    DiagnosticFactory3<KtNamedDeclaration, String, CallableMemberDescriptor, List<DescriptorBasedDeprecationInfo>> OVERRIDE_DEPRECATION =
            DiagnosticFactory3.create(WARNING, DECLARATION_NAME);

    DiagnosticFactory0<PsiElement> DEPRECATED_SINCE_KOTLIN_WITHOUT_DEPRECATED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<PsiElement, String, String> API_NOT_AVAILABLE = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<PsiElement, FqName> MISSING_DEPENDENCY_CLASS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, FqName, FqName> MISSING_DEPENDENCY_SUPERCLASS = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<PsiElement, FqName> MISSING_BUILT_IN_DECLARATION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> MISSING_SCRIPT_BASE_CLASS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> MISSING_SCRIPT_STANDARD_TEMPLATE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> MISSING_SCRIPT_RECEIVER_CLASS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> MISSING_IMPORTED_SCRIPT_FILE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> MISSING_IMPORTED_SCRIPT_PSI = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> MISSING_SCRIPT_PROVIDED_PROPERTY_CLASS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> PRE_RELEASE_CLASS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> IR_WITH_UNSTABLE_ABI_COMPILED_CLASS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> FIR_COMPILED_CLASS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, String, IncompatibleVersionErrorData<?>> INCOMPATIBLE_CLASS = DiagnosticFactory2.create(ERROR);

    //Elements with "INVISIBLE_REFERENCE" error are marked as unresolved, unlike elements with "INVISIBLE_MEMBER" error
    //"INVISIBLE_REFERENCE" is used for invisible classes references and references in import
    DiagnosticFactory3<KtSimpleNameExpression, DeclarationDescriptor, DescriptorVisibility, DeclarationDescriptor> INVISIBLE_REFERENCE =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<PsiElement, DeclarationDescriptor, DescriptorVisibility, DeclarationDescriptor> INVISIBLE_MEMBER =
            DiagnosticFactory3.create(ERROR, CALL_ELEMENT);
    DiagnosticFactory1<KtElement, DeclarationDescriptor> DEPRECATED_ACCESS_BY_SHORT_NAME = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory1<PsiElement, PropertyDescriptor> DEPRECATED_ACCESS_TO_ENUM_COMPANION_PROPERTY = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory1<PsiElement, ConstructorDescriptor> PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL = DiagnosticFactory1.create(ERROR);

    // Exposed visibility group
    DiagnosticFactory3<PsiElement, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility> EXPOSED_PROPERTY_TYPE =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactoryForDeprecation3<PsiElement, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility>
            EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR =
            DiagnosticFactoryForDeprecation3.create(LanguageFeature.ForbidExposingTypesInPrimaryConstructorProperties);
    DiagnosticFactory3<PsiElement, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility> EXPOSED_FUNCTION_RETURN_TYPE =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<KtParameter, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility> EXPOSED_PARAMETER_TYPE =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<KtTypeReference, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility> EXPOSED_RECEIVER_TYPE =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<KtTypeParameter, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility> EXPOSED_TYPE_PARAMETER_BOUND =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<KtSuperTypeListEntry, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility> EXPOSED_SUPER_CLASS =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<KtSuperTypeListEntry, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility> EXPOSED_SUPER_INTERFACE =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<PsiElement, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility> EXPOSED_TYPEALIAS_EXPANDED_TYPE =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<PsiElement, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility> EXPOSED_FROM_PRIVATE_IN_FILE =
            DiagnosticFactory3.create(WARNING);

    DiagnosticFactory2<KtExpression, KotlinType, Collection<ClassDescriptor>> INACCESSIBLE_TYPE = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory1<KtElement, Collection<ClassDescriptor>> PLATFORM_CLASS_MAPPED_TO_KOTLIN = DiagnosticFactory1.create(WARNING);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Errors/warnings in types
    // Note: class/interface declaration is NOT a type. A type is something that may be written on the right-hand side of ":"

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    DiagnosticFactory0<KtTypeProjection> PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT = DiagnosticFactory0.create(ERROR, VARIANCE_IN_PROJECTION);
    DiagnosticFactory2<KtTypeReference, KotlinType, KotlinType> UPPER_BOUND_VIOLATED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtTypeReference, KotlinType, KotlinType> UPPER_BOUND_VIOLATED_WARNING = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory0<KtNullableType> REDUNDANT_NULLABLE = DiagnosticFactory0.create(WARNING, NULLABLE_TYPE);
    DiagnosticFactory0<KtNullableType> NULLABLE_ON_DEFINITELY_NOT_NULLABLE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> INCORRECT_LEFT_COMPONENT_OF_INTERSECTION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> INCORRECT_RIGHT_COMPONENT_OF_INTERSECTION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<KtElement, Integer, DeclarationDescriptor> WRONG_NUMBER_OF_TYPE_ARGUMENTS = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<KtElement, ClassDescriptor> OUTER_CLASS_ARGUMENTS_REQUIRED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtElement, String> TYPE_ARGUMENTS_NOT_ALLOWED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtTypeReference, Integer, String> NO_TYPE_ARGUMENTS_ON_RHS = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<KtTypeProjection, ClassifierDescriptor> CONFLICTING_PROJECTION =
            DiagnosticFactory1.create(ERROR, VARIANCE_IN_PROJECTION);
    DiagnosticFactory1<KtTypeProjection, ClassifierDescriptor> REDUNDANT_PROJECTION =
            DiagnosticFactory1.create(WARNING, VARIANCE_IN_PROJECTION);
    DiagnosticFactoryForDeprecation1<PsiElement, VarianceConflictDiagnosticData> TYPE_VARIANCE_CONFLICT =
            DiagnosticFactoryForDeprecation1.create(LanguageFeature.ReportTypeVarianceConflictOnQualifierArguments, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory1<PsiElement, VarianceConflictDiagnosticData> TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<PsiElement> FINITE_BOUNDS_VIOLATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> FINITE_BOUNDS_VIOLATION_IN_JAVA = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<PsiElement> EXPANSIVE_INHERITANCE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> EXPANSIVE_INHERITANCE_IN_JAVA = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory0<KtTypeArgumentList> TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtParameter> REIFIED_TYPE_IN_CATCH_CLAUSE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtParameter> TYPE_PARAMETER_IN_CATCH_CLAUSE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeParameterList> GENERIC_THROWABLE_SUBCLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtClassOrObject> INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS =
            DiagnosticFactory0.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory0<KtClassOrObject> INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS_WARNING =
            DiagnosticFactory0.create(WARNING, DECLARATION_NAME);

    DiagnosticFactory0<KtTypeAlias> TOPLEVEL_TYPEALIASES_ONLY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtElement, ClassifierDescriptor> RECURSIVE_TYPEALIAS_EXPANSION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory3<KtElement, KotlinType, KotlinType, ClassifierDescriptor> UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<KtElement, KotlinType, KotlinType, ClassifierDescriptor> UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_WARNING =
            DiagnosticFactory3.create(WARNING);
    DiagnosticFactory1<KtElement, KotlinType> CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtTypeReference, KotlinType> TYPEALIAS_SHOULD_EXPAND_TO_CLASS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtTypeReference, KotlinType, String> TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<KtElement, KotlinType> EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtTypeElement, KotlinType> EXPANDED_TYPE_CANNOT_BE_INHERITED = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtPostfixExpression> DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtModifierList> MODIFIER_LIST_NOT_ALLOWED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactoryForDeprecation1<PsiElement, CallableDescriptor> PROGRESSIONS_CHANGING_RESOLVE = DiagnosticFactoryForDeprecation1.create(LanguageFeature.ProgressionsChangingResolve);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Errors in declarations

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Imports

    DiagnosticFactory1<KtSimpleNameExpression, ClassDescriptor> CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtSimpleNameExpression, Name> CANNOT_BE_IMPORTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtSimpleNameExpression> PACKAGE_CANNOT_BE_IMPORTED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<KtImportDirective, String> CONFLICTING_IMPORT = DiagnosticFactory1.create(ERROR, PositioningStrategies.IMPORT_ALIAS);

    DiagnosticFactory0<KtSimpleNameExpression> OPERATOR_RENAMED_ON_IMPORT = DiagnosticFactory0.create(ERROR);

    // Modifiers

    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, KtModifierKeywordToken> INCOMPATIBLE_MODIFIERS =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, KtModifierKeywordToken> DEPRECATED_MODIFIER_PAIR =
            DiagnosticFactory2.create(WARNING);
    DiagnosticFactory1<PsiElement, KtModifierKeywordToken> REPEATED_MODIFIER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, KtModifierKeywordToken> REDUNDANT_MODIFIER = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, String> WRONG_MODIFIER_TARGET = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, String> DEPRECATED_MODIFIER_FOR_TARGET = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, KtModifierKeywordToken> DEPRECATED_MODIFIER = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, String> REDUNDANT_MODIFIER_FOR_TARGET = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory0<KtDeclaration> NO_EXPLICIT_VISIBILITY_IN_API_MODE = DiagnosticFactory0.create(ERROR, DECLARATION_START_TO_NAME);
    DiagnosticFactory0<KtNamedDeclaration> NO_EXPLICIT_RETURN_TYPE_IN_API_MODE = DiagnosticFactory0.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory0<KtDeclaration> NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING = DiagnosticFactory0.create(WARNING,
                                                                                                             DECLARATION_START_TO_NAME);
    DiagnosticFactory0<KtNamedDeclaration> NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING =
            DiagnosticFactory0.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, String> WRONG_MODIFIER_CONTAINING_DECLARATION = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, String> DEPRECATED_MODIFIER_CONTAINING_DECLARATION =
            DiagnosticFactory2.create(WARNING);
    DiagnosticFactory1<PsiElement, KtModifierKeywordToken> ILLEGAL_INLINE_PARAMETER_MODIFIER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtParameter> INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory1<KtAnnotationEntry, String> WRONG_ANNOTATION_TARGET = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtAnnotationEntry, String, String> WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<KtAnnotationEntry, String> WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET_ON_TYPE = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<KtAnnotationEntry> REPEATED_ANNOTATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> REPEATED_ANNOTATION_WARNING = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtAnnotationEntry> NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> WRONG_EXTENSION_FUNCTION_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> WRONG_EXTENSION_FUNCTION_TYPE_WARNING = DiagnosticFactory0.create(WARNING);

    // Annotations

    DiagnosticFactory0<KtSuperTypeList> SUPERTYPES_FOR_ANNOTATION_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactoryForDeprecation0<KtAnnotationEntry> ANNOTATION_ON_SUPERCLASS =
            DiagnosticFactoryForDeprecation0.create(LanguageFeature.ProhibitUseSiteTargetAnnotationsOnSuperTypes);
    DiagnosticFactory0<KtParameter> MISSING_VAL_ON_ANNOTATION_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtParameter> VAR_ANNOTATION_PARAMETER = DiagnosticFactory0.create(ERROR, VAL_OR_VAR_NODE);
    DiagnosticFactory0<KtCallExpression> ANNOTATION_CLASS_CONSTRUCTOR_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtAnnotationEntry, DeclarationDescriptor> NOT_AN_ANNOTATION_CLASS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> ANNOTATION_CLASS_MEMBER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> INVALID_TYPE_OF_ANNOTATION_MEMBER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> NULLABLE_TYPE_OF_ANNOTATION_MEMBER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ANNOTATION_ARGUMENT_MUST_BE_CONST = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtExpression> ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotatedExpression> ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtAnnotationEntry> ANNOTATION_USED_AS_ANNOTATION_ARGUMENT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ANNOTATION_ARGUMENT_IS_NON_CONST = DiagnosticFactory0.create(WARNING);
    DiagnosticFactoryForDeprecation0<KtParameter> CYCLE_IN_ANNOTATION_PARAMETER = DiagnosticFactoryForDeprecation0.create(LanguageFeature.ProhibitCyclesInAnnotations);

    DiagnosticFactoryForDeprecation0<PsiElement> RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION =
            DiagnosticFactoryForDeprecation0.create(LanguageFeature.RestrictRetentionForExpressionAnnotations);

    DiagnosticFactoryForDeprecation0<KtClassOrObject> LOCAL_ANNOTATION_CLASS =
            DiagnosticFactoryForDeprecation0.create(LanguageFeature.ProhibitLocalAnnotations);

    DiagnosticFactory1<PsiElement, FqName> ILLEGAL_KOTLIN_VERSION_STRING_VALUE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> NEWER_VERSION_IN_SINCE_KOTLIN = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory2<PsiElement, FqName, String> OPT_IN_USAGE = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, FqName, String> OPT_IN_USAGE_ERROR = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, FqName, String> OPT_IN_USAGE_FUTURE_ERROR = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory2<PsiElement, FqName, String> OPT_IN_OVERRIDE = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, FqName, String> OPT_IN_OVERRIDE_ERROR = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory0<PsiElement> OPT_IN_IS_NOT_ENABLED = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement>
            OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtAnnotationEntry> OPT_IN_WITHOUT_ARGUMENTS = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory1<KtAnnotationEntry, FqName> OPT_IN_ARGUMENT_IS_NOT_MARKER = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<KtAnnotationEntry, String> OPT_IN_MARKER_WITH_WRONG_TARGET = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> OPT_IN_MARKER_WITH_WRONG_RETENTION = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<KtAnnotationEntry, String> OPT_IN_MARKER_ON_WRONG_TARGET = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> OPT_IN_MARKER_ON_OVERRIDE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> OPT_IN_MARKER_ON_OVERRIDE_WARNING = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory1<PsiElement, String> EXPERIMENTAL_UNSIGNED_LITERALS = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<PsiElement, String> EXPERIMENTAL_UNSIGNED_LITERALS_ERROR = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<PsiElement> NON_PARENTHESIZED_ANNOTATIONS_ON_FUNCTIONAL_TYPES = DiagnosticFactory0.create(ERROR);

    // Const
    DiagnosticFactory0<PsiElement> CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> CONST_VAL_WITH_GETTER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> CONST_VAL_WITH_DELEGATE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> TYPE_CANT_BE_USED_FOR_CONST_VAL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> CONST_VAL_WITHOUT_INITIALIZER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> CONST_VAL_WITH_NON_CONST_INITIALIZER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtExpression> NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, String> INAPPLICABLE_TARGET_ON_PROPERTY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> INAPPLICABLE_PARAM_TARGET = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> REDUNDANT_ANNOTATION_TARGET = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<KtAnnotationUseSiteTarget> INAPPLICABLE_FILE_TARGET = DiagnosticFactory0.create(ERROR);

    // Classes and interfaces

    DiagnosticFactory0<KtTypeProjection> PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE =
            DiagnosticFactory0.create(ERROR, VARIANCE_IN_PROJECTION);

    DiagnosticFactory0<PsiElement> CYCLIC_INHERITANCE_HIERARCHY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> CYCLIC_SCOPES_WITH_COMPANION = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtSuperTypeEntry> SUPERTYPE_NOT_INITIALIZED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtTypeReference> DELEGATION_NOT_TO_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<KtClassOrObject, CallableMemberDescriptor, List<CallableMemberDescriptor>>
            DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);

    DiagnosticFactory0<KtTypeReference> SUPERTYPE_NOT_A_CLASS_OR_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> SUPERTYPE_IS_SUSPEND_FUNCTION_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> SUPERTYPE_IS_KSUSPEND_FUNCTION_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtTypeReference> MANY_CLASSES_IN_SUPERTYPE_LIST = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> SUPERTYPE_APPEARS_TWICE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory3<KtSuperTypeList, TypeParameterDescriptor, ClassDescriptor, Collection<KotlinType>>
            INCONSISTENT_TYPE_PARAMETER_VALUES = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<KtTypeParameter, TypeParameterDescriptor, ClassDescriptor, Collection<KotlinType>>
            INCONSISTENT_TYPE_PARAMETER_BOUNDS = DiagnosticFactory3.create(ERROR);


    DiagnosticFactory0<KtTypeReference> FINAL_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> DATA_CLASS_CANNOT_HAVE_CLASS_SUPERTYPES = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> SINGLETON_IN_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtNullableType> NULLABLE_SUPERTYPE = DiagnosticFactory0.create(ERROR, NULLABLE_TYPE);
    DiagnosticFactory0<KtTypeReference> DYNAMIC_SUPERTYPE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<KtTypeReference, ClassDescriptor> CLASS_CANNOT_BE_EXTENDED_DIRECTLY = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtElement> MISSING_CONSTRUCTOR_KEYWORD = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> MISSING_CONSTRUCTOR_BRACKETS = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> NON_PRIVATE_CONSTRUCTOR_IN_ENUM = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NON_PRIVATE_CONSTRUCTOR_IN_SEALED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED = DiagnosticFactory0.create(ERROR);

    // Inline classes, Value classes

    DiagnosticFactory0<PsiElement> VALUE_CLASS_NOT_TOP_LEVEL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> VALUE_CLASS_NOT_FINAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtElement> INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtElement> VALUE_CLASS_EMPTY_CONSTRUCTOR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtParameter> VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtProperty> PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<PsiElement> DELEGATED_PROPERTY_INSIDE_VALUE_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtTypeReference, KotlinType> VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> VALUE_CLASS_CANNOT_EXTEND_CLASSES = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> VALUE_CLASS_CANNOT_BE_RECURSIVE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> RESERVED_MEMBER_INSIDE_VALUE_CLASS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> INNER_CLASS_INSIDE_VALUE_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> VALUE_CLASS_CANNOT_BE_CLONEABLE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> INLINE_CLASS_DEPRECATED = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtContextReceiverList> INLINE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS = DiagnosticFactory0.create(ERROR);

    // Result class

    DiagnosticFactory0<PsiElement> RESULT_CLASS_IN_RETURN_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> RESULT_CLASS_WITH_NULLABLE_OPERATOR = DiagnosticFactory1.create(ERROR);

    // Fun interfaces

    DiagnosticFactory0<PsiElement> FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> FUN_INTERFACE_CONSTRUCTOR_REFERENCE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> FUN_INTERFACE_WITH_SUSPEND_FUNCTION = DiagnosticFactory0.create(ERROR);

    // Secondary constructors

    DiagnosticFactory0<KtConstructorDelegationReferenceExpression> CYCLIC_CONSTRUCTOR_DELEGATION_CALL = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtDeclaration> CONSTRUCTOR_IN_OBJECT = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtSuperTypeCallEntry> SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED =
            DiagnosticFactory0.create(ERROR, PositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL);

    DiagnosticFactory0<PsiElement> PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED_IN_ENUM =
            DiagnosticFactory0.create(WARNING, PositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL);

    DiagnosticFactory0<KtConstructorDelegationReferenceExpression> DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR =
            DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> EXPLICIT_DELEGATION_CALL_REQUIRED =
            DiagnosticFactory0.create(ERROR, PositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL);

    DiagnosticFactory1<PsiElement, DeclarationDescriptor> INSTANCE_ACCESS_BEFORE_SUPER_CALL = DiagnosticFactory1.create(ERROR);

    // Interface-specific

    DiagnosticFactory0<KtModifierListOwner> REDUNDANT_OPEN_IN_INTERFACE = DiagnosticFactory0.create(WARNING, OPEN_MODIFIER);

    DiagnosticFactory0<KtDeclaration> CONSTRUCTOR_IN_INTERFACE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtDeclaration> METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> SUPERTYPE_INITIALIZED_IN_INTERFACE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtDelegatedSuperTypeEntry> DELEGATION_IN_INTERFACE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> INTERFACE_WITH_SUPERCLASS = DiagnosticFactory0.create(ERROR);

    // Enum-specific

    DiagnosticFactory0<PsiElement> CLASS_IN_SUPERTYPE_FOR_ENUM = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeParameterList> TYPE_PARAMETERS_IN_ENUM = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtClass> ENUM_ENTRY_SHOULD_BE_INITIALIZED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtCallExpression> ENUM_CLASS_CONSTRUCTOR_CALL = DiagnosticFactory0.create(ERROR);

    // Sealed-specific
    DiagnosticFactory0<KtCallExpression> SEALED_CLASS_CONSTRUCTOR_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> SEALED_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<KtTypeReference, String, ClassKind> SEALED_SUPERTYPE_IN_LOCAL_CLASS = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtTypeReference, FqName, FqName> SEALED_INHERITOR_IN_DIFFERENT_PACKAGE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<KtTypeReference> SEALED_INHERITOR_IN_DIFFERENT_MODULE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> CLASS_INHERITS_JAVA_SEALED_CLASS = DiagnosticFactory0.create(ERROR);

    // Companion objects

    DiagnosticFactory0<KtObjectDeclaration> MANY_COMPANION_OBJECTS = DiagnosticFactory0.create(ERROR, COMPANION_OBJECT);
    DiagnosticFactoryForDeprecation0<KtExpression> SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR =
            DiagnosticFactoryForDeprecation0.create(LanguageFeature.ProhibitSelfCallsInNestedObjects);

    // Objects

    DiagnosticFactory1<KtObjectDeclaration, ClassDescriptor> LOCAL_OBJECT_NOT_ALLOWED = DiagnosticFactory1.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory1<KtClass, ClassDescriptor> LOCAL_INTERFACE_NOT_ALLOWED = DiagnosticFactory1.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory0<KtTypeParameterList> TYPE_PARAMETERS_IN_OBJECT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeParameterList> TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT = DiagnosticFactory0.create(WARNING);

    // Type parameter declarations

    DiagnosticFactory1<KtTypeReference, KotlinType> FINAL_UPPER_BOUND = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<KtTypeReference> DYNAMIC_UPPER_BOUND = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> ONLY_ONE_CLASS_BOUND_ALLOWED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtElement> BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> REPEATED_BOUND = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<KtNamedDeclaration, TypeParameterDescriptor> CONFLICTING_UPPER_BOUNDS =
            DiagnosticFactory1.create(ERROR, DECLARATION_NAME);

    DiagnosticFactory2<KtSimpleNameExpression, KtTypeConstraint, KtTypeParameterListOwner> NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER =
            DiagnosticFactory2.create(ERROR);

    DiagnosticFactory0<KtTypeParameter> VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED = DiagnosticFactory0.create(ERROR, VARIANCE_MODIFIER);
    DiagnosticFactory0<KtTypeReference> BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtTypeParameterList> DEPRECATED_TYPE_PARAMETER_SYNTAX = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> REIFIED_TYPE_PARAMETER_NO_INLINE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtDeclaration> TYPE_PARAMETERS_NOT_ALLOWED
            = DiagnosticFactory0.create(ERROR, TYPE_PARAMETERS_OR_DECLARATION_SIGNATURE);

    DiagnosticFactory0<KtTypeParameter> TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> CYCLIC_GENERIC_UPPER_BOUND = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtTypeParameter> MISPLACED_TYPE_PARAMETER_CONSTRAINTS = DiagnosticFactory0.create(WARNING);

    // Members

    DiagnosticFactory1<PsiElement, Collection<DeclarationDescriptor>> CONFLICTING_OVERLOADS =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory0<KtNamedDeclaration> NON_FINAL_MEMBER_IN_FINAL_CLASS = DiagnosticFactory0.create(WARNING, modifierSetPosition(
            KtTokens.OPEN_KEYWORD));

    DiagnosticFactory0<KtNamedDeclaration> NON_FINAL_MEMBER_IN_OBJECT = DiagnosticFactory0.create(WARNING, modifierSetPosition(
            KtTokens.OPEN_KEYWORD));

    DiagnosticFactory1<KtModifierListOwner, CallableMemberDescriptor> NOTHING_TO_OVERRIDE =
            DiagnosticFactory1.create(ERROR, OVERRIDE_MODIFIER);

    DiagnosticFactory3<KtNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor, DeclarationDescriptor>
            VIRTUAL_MEMBER_HIDDEN =
            DiagnosticFactory3.create(ERROR, DECLARATION_NAME);

    DiagnosticFactory2<KtModifierListOwner, CallableMemberDescriptor, CallableDescriptor> CANNOT_OVERRIDE_INVISIBLE_MEMBER =
            DiagnosticFactory2.create(ERROR, OVERRIDE_MODIFIER);

    DiagnosticFactory2<PsiElement, CallableMemberDescriptor, DeclarationDescriptor> DATA_CLASS_OVERRIDE_CONFLICT =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactoryForDeprecation2<PsiElement, CallableMemberDescriptor, DeclarationDescriptor> DATA_CLASS_OVERRIDE_DEFAULT_VALUES =
            DiagnosticFactoryForDeprecation2.create(LanguageFeature.ProhibitDataClassesOverridingCopy);
    //        DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<KtDeclaration, CallableMemberDescriptor> CANNOT_INFER_VISIBILITY =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory2<KtNamedDeclaration, CallableMemberDescriptor, DeclarationDescriptor> OVERRIDING_FINAL_MEMBER =
            DiagnosticFactory2.create(ERROR, OVERRIDE_MODIFIER);

    DiagnosticFactory3<KtModifierListOwner, DescriptorVisibility, CallableMemberDescriptor, DeclarationDescriptor>
            CANNOT_WEAKEN_ACCESS_PRIVILEGE =
            DiagnosticFactory3.create(ERROR, VISIBILITY_MODIFIER);
    DiagnosticFactory3<KtModifierListOwner, DescriptorVisibility, CallableMemberDescriptor, DeclarationDescriptor>
            CANNOT_CHANGE_ACCESS_PRIVILEGE =
            DiagnosticFactory3.create(ERROR, VISIBILITY_MODIFIER);
    DiagnosticFactory2<KtNamedDeclaration, CallableMemberDescriptor, DeclarationWithDiagnosticComponents> RETURN_TYPE_MISMATCH_ON_OVERRIDE =
            DiagnosticFactory2.create(ERROR, DECLARATION_RETURN_TYPE);
    DiagnosticFactory2<KtNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor> PROPERTY_TYPE_MISMATCH_ON_OVERRIDE =
            DiagnosticFactory2.create(ERROR, DECLARATION_RETURN_TYPE);
    DiagnosticFactory2<KtNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor> VAR_TYPE_MISMATCH_ON_OVERRIDE =
            DiagnosticFactory2.create(ERROR, DECLARATION_RETURN_TYPE);

    DiagnosticFactory2<KtClassOrObject, CallableMemberDescriptor, CallableMemberDescriptor> RETURN_TYPE_MISMATCH_ON_INHERITANCE =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, CallableMemberDescriptor, CallableMemberDescriptor> PROPERTY_TYPE_MISMATCH_ON_INHERITANCE =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, CallableMemberDescriptor, CallableMemberDescriptor> VAR_TYPE_MISMATCH_ON_INHERITANCE =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);

    DiagnosticFactory2<KtClassOrObject, CallableMemberDescriptor, CallableMemberDescriptor> OVERRIDING_FINAL_MEMBER_BY_DELEGATION =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, CallableMemberDescriptor, CallableMemberDescriptor> VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, CallableMemberDescriptor, CallableMemberDescriptor> RETURN_TYPE_MISMATCH_BY_DELEGATION =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, CallableMemberDescriptor, CallableMemberDescriptor> PROPERTY_TYPE_MISMATCH_BY_DELEGATION =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);

    DiagnosticFactory2<KtClassOrObject, ClassDescriptor, Collection<CallableMemberDescriptor>> CONFLICTING_INHERITED_MEMBERS =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, ClassDescriptor, Collection<CallableMemberDescriptor>> CONFLICTING_INHERITED_MEMBERS_WARNING =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, KtClassOrObject, CallableMemberDescriptor> ABSTRACT_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, KtClassOrObject, CallableMemberDescriptor> ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, KtClassOrObject, CallableMemberDescriptor> ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED_WARNING =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, KtClassOrObject, CallableMemberDescriptor> MANY_IMPL_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, KtClassOrObject, CallableMemberDescriptor> MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, KtClassOrObject, CallableMemberDescriptor> MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED_WARNING =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);
    DiagnosticFactoryForDeprecation2<KtClassOrObject, ClassDescriptor, Collection<CallableMemberDescriptor>>
            INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER =
            DiagnosticFactoryForDeprecation2.create(LanguageFeature.ProhibitInvisibleAbstractMethodsInSuperclasses, DECLARATION_NAME);

    DiagnosticFactory1<KtDeclaration, Collection<KotlinType>> AMBIGUOUS_ANONYMOUS_TYPE_INFERRED =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory1<KtNamedDeclaration, TypeParameterDescriptor>
            KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE = DiagnosticFactory1.create(ERROR, PositioningStrategies.DECLARATION_NAME);

    // Property-specific

    DiagnosticFactory2<KtNamedDeclaration, PropertyDescriptor, PropertyDescriptor> VAR_OVERRIDDEN_BY_VAL =
            DiagnosticFactory2.create(ERROR, VAL_OR_VAR_NODE);

    DiagnosticFactory0<PsiElement> REDUNDANT_MODIFIER_IN_GETTER = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> PRIVATE_SETTER_FOR_OPEN_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<KtTypeReference, KotlinType, KotlinType> WRONG_GETTER_RETURN_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<KtTypeReference> WRONG_SETTER_RETURN_TYPE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtModifierListOwner>
            ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS = DiagnosticFactory0.create(ERROR, ABSTRACT_MODIFIER);
    DiagnosticFactory0<KtExpression> ABSTRACT_PROPERTY_WITH_INITIALIZER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtPropertyAccessor> ABSTRACT_PROPERTY_WITH_GETTER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtPropertyAccessor> ABSTRACT_PROPERTY_WITH_SETTER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtPropertyDelegate> ABSTRACT_DELEGATED_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtPropertyAccessor> ACCESSOR_FOR_DELEGATED_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtPropertyDelegate> DELEGATED_PROPERTY_IN_INTERFACE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtProperty> PROPERTY_WITH_NO_TYPE_NO_INITIALIZER = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<KtProperty> MUST_BE_INITIALIZED = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtProperty> MUST_BE_INITIALIZED_OR_BE_ABSTRACT = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtProperty> EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT =
            DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtProperty> UNNECESSARY_LATEINIT = DiagnosticFactory0.create(WARNING, LATEINIT_MODIFIER);

    DiagnosticFactory0<KtExpression> EXTENSION_PROPERTY_WITH_BACKING_FIELD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> CONTEXT_RECEIVERS_WITH_BACKING_FIELD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> PROPERTY_INITIALIZER_NO_BACKING_FIELD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> PROPERTY_INITIALIZER_IN_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtProperty> PRIVATE_PROPERTY_IN_INTERFACE = DiagnosticFactory0.create(ERROR, PRIVATE_MODIFIER);
    DiagnosticFactory0<KtProperty> BACKING_FIELD_IN_INTERFACE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory1<KtModifierListOwner, String> INAPPLICABLE_LATEINIT_MODIFIER = DiagnosticFactory1.create(ERROR, LATEINIT_MODIFIER);
    DiagnosticFactory0<PsiElement> LATEINIT_INTRINSIC_CALL_ON_NON_LITERAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, PropertyDescriptor> LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY =
            DiagnosticFactory1.create(ERROR);

    DiagnosticFactory2<KtModifierListOwner, String, ClassDescriptor> ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS =
            DiagnosticFactory2.create(ERROR, ABSTRACT_MODIFIER);

    DiagnosticFactory0<KtPropertyAccessor> VAL_WITH_SETTER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtExpression> SETTER_PARAMETER_WITH_DEFAULT_VALUE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<KtTypeReference, KotlinType, KotlinType> WRONG_SETTER_PARAMETER_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactoryForDeprecation1<KtPropertyDelegate, String> DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER = DiagnosticFactoryForDeprecation1.create(LanguageFeature.ForbidUsingExtensionPropertyTypeParameterInDelegate);

    // Function-specific

    DiagnosticFactory2<KtFunction, String, ClassDescriptor> ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS =
            DiagnosticFactory2.create(ERROR, ABSTRACT_MODIFIER);

    DiagnosticFactory1<KtFunction, SimpleFunctionDescriptor> ABSTRACT_FUNCTION_WITH_BODY =
            DiagnosticFactory1.create(ERROR, ABSTRACT_MODIFIER);
    DiagnosticFactory1<KtFunction, SimpleFunctionDescriptor> NON_ABSTRACT_FUNCTION_WITH_NO_BODY =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory1<KtFunction, SimpleFunctionDescriptor> PRIVATE_FUNCTION_WITH_NO_BODY =
            DiagnosticFactory1.create(ERROR, PRIVATE_MODIFIER);

    DiagnosticFactory1<KtFunction, SimpleFunctionDescriptor> NON_MEMBER_FUNCTION_NO_BODY =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<KtFunction> FUNCTION_DECLARATION_WITH_NO_NAME = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<PsiElement> ANONYMOUS_FUNCTION_WITH_NAME = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtParameter> VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtNamedFunction> NO_TAIL_CALLS_FOUND = DiagnosticFactory0.create(WARNING, TAILREC_MODIFIER);
    DiagnosticFactoryForDeprecation0<KtNamedFunction> TAILREC_ON_VIRTUAL_MEMBER =
            DiagnosticFactoryForDeprecation0.create(LanguageFeature.ProhibitTailrecOnVirtualMember, TAILREC_MODIFIER);

    DiagnosticFactory0<KtParameter>
            ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE = DiagnosticFactory0.create(ERROR, PARAMETER_DEFAULT_VALUE);

    DiagnosticFactory0<KtParameter> USELESS_VARARG_ON_PARAMETER = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtParameter> MULTIPLE_VARARG_PARAMETERS = DiagnosticFactory0.create(ERROR, PARAMETER_VARARG_MODIFIER);

    DiagnosticFactory1<KtParameter, KotlinType> FORBIDDEN_VARARG_PARAMETER_TYPE =
            DiagnosticFactory1.create(ERROR, PARAMETER_VARARG_MODIFIER);

    DiagnosticFactory0<PsiElement> CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS = DiagnosticFactory0.create(WARNING);

    // Named parameters

    DiagnosticFactory0<KtParameter> DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE = DiagnosticFactory0.create(ERROR, PARAMETER_DEFAULT_VALUE);

    DiagnosticFactory1<KtParameter, ValueParameterDescriptor> MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES =
            DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<KtClassOrObject, ValueParameterDescriptor> MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE =
            DiagnosticFactory1.create(ERROR, DECLARATION_NAME);

    DiagnosticFactory2<KtParameter, ClassDescriptor, ValueParameterDescriptor> PARAMETER_NAME_CHANGED_ON_OVERRIDE =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);

    DiagnosticFactory2<KtClassOrObject, Collection<? extends CallableMemberDescriptor>, Integer>
            DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);

    DiagnosticFactory0<KtReferenceExpression> NAME_FOR_AMBIGUOUS_PARAMETER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> DATA_CLASS_WITHOUT_PARAMETERS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtParameter> DATA_CLASS_VARARG_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtParameter> DATA_CLASS_NOT_PROPERTY_PARAMETER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtParameter> CATCH_PARAMETER_WITH_DEFAULT_VALUE = DiagnosticFactory0.create(ERROR);

    // Multi-platform projects

    DiagnosticFactory0<KtDeclaration> EXPECTED_DECLARATION_WITH_BODY = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtConstructorDelegationCall> EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtParameter> EXPECTED_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtConstructor<?>> EXPECTED_ENUM_CONSTRUCTOR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtEnumEntry> EXPECTED_ENUM_ENTRY_WITH_BODY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> EXPECTED_PROPERTY_INITIALIZER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtPropertyDelegate> EXPECTED_DELEGATED_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> EXPECTED_LATEINIT_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> EXPECTED_PRIVATE_DECLARATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtDelegatedSuperTypeEntry> IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtTypeAlias> ACTUAL_TYPE_ALIAS_NOT_TO_CLASS = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtTypeAlias>
            ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtTypeAlias> ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtTypeAlias> ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<PsiElement> ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, ValueParameterDescriptor> ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE =
            DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<PsiElement> EXPECTED_FUNCTION_SOURCE_WITH_DEFAULT_ARGUMENTS_NOT_FOUND = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory3<KtNamedDeclaration, MemberDescriptor, ModuleDescriptor,
            Map<Incompatible<MemberDescriptor>, Collection<MemberDescriptor>>> NO_ACTUAL_FOR_EXPECT =
            DiagnosticFactory3.create(ERROR, INCOMPATIBLE_DECLARATION);
    DiagnosticFactory2<KtNamedDeclaration, MemberDescriptor,
            Map<Incompatible<MemberDescriptor>, Collection<MemberDescriptor>>> ACTUAL_WITHOUT_EXPECT =
            DiagnosticFactory2.create(ERROR, INCOMPATIBLE_DECLARATION);
    DiagnosticFactory2<KtNamedDeclaration, DeclarationDescriptor, Collection<ModuleDescriptor>> AMBIGUOUS_ACTUALS =
            DiagnosticFactory2.create(ERROR, INCOMPATIBLE_DECLARATION);
    DiagnosticFactory2<KtNamedDeclaration, DeclarationDescriptor, Collection<ModuleDescriptor>> AMBIGUOUS_EXPECTS =
            DiagnosticFactory2.create(ERROR, INCOMPATIBLE_DECLARATION);

    DiagnosticFactory2<KtNamedDeclaration, ClassDescriptor,
            List<Pair<MemberDescriptor, Map<Incompatible<MemberDescriptor>, Collection<MemberDescriptor>>>>>
            NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS =
            DiagnosticFactory2.create(ERROR, ACTUAL_DECLARATION_NAME);
    DiagnosticFactory0<KtNamedDeclaration> ACTUAL_MISSING = DiagnosticFactory0.create(ERROR, ACTUAL_DECLARATION_NAME);

    DiagnosticFactory0<PsiElement> OPTIONAL_EXPECTATION_NOT_ON_EXPECTED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NESTED_OPTIONAL_EXPECTATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE = DiagnosticFactory0.create(ERROR);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Errors/warnings inside code blocks

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // General

    DiagnosticFactory1<PsiElement, String> NAME_SHADOWING = DiagnosticFactory1.create(WARNING, PositioningStrategies.FOR_REDECLARATION);
    DiagnosticFactory0<PsiElement> ACCESSOR_PARAMETER_NAME_SHADOWING = DiagnosticFactory0.create(WARNING);

    DiagnosticFactoryForDeprecation0<KtExpression> TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM =
            DiagnosticFactoryForDeprecation0.create(LanguageFeature.ForbidRecursiveDelegateExpressions);

    // Checking call arguments

    DiagnosticFactory0<PsiElement> MIXING_NAMED_AND_POSITIONED_ARGUMENTS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtReferenceExpression> ARGUMENT_PASSED_TWICE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtReferenceExpression, KtReferenceExpression> NAMED_PARAMETER_NOT_FOUND =
            DiagnosticFactory1.create(ERROR, FOR_UNRESOLVED_REFERENCE);
    DiagnosticFactory1<PsiElement, BadNamedArgumentsTarget> NAMED_ARGUMENTS_NOT_ALLOWED = DiagnosticFactory1.create(ERROR);

    enum BadNamedArgumentsTarget {
        NON_KOTLIN_FUNCTION, // a function provided by non-Kotlin artifact, ex: Java function
        INTEROP_FUNCTION, // deserialized Kotlin function that serves as a bridge to a function written in another language, ex: Obj-C
        INVOKE_ON_FUNCTION_TYPE,
        EXPECTED_CLASS_MEMBER,
    }

    DiagnosticFactory0<KtExpression> VARARG_OUTSIDE_PARENTHESES = DiagnosticFactory0.create(ERROR);
    DiagnosticFactoryForDeprecation0<LeafPsiElement> NON_VARARG_SPREAD =
            DiagnosticFactoryForDeprecation0.create(LanguageFeature.ReportNonVarargSpreadOnGenericCalls);
    DiagnosticFactory0<LeafPsiElement> SPREAD_OF_NULLABLE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<LeafPsiElement> SPREAD_OF_LAMBDA_OR_CALLABLE_REFERENCE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtExpression> MANY_LAMBDA_EXPRESSION_ARGUMENTS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtLambdaExpression> UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, CallableDescriptor> TOO_MANY_ARGUMENTS = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<KtElement, ValueParameterDescriptor> NO_VALUE_FOR_PARAMETER = DiagnosticFactory1.create(ERROR, VALUE_ARGUMENTS);

    DiagnosticFactory1<KtExpression, KotlinType> MISSING_RECEIVER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtExpression> NO_RECEIVER_ALLOWED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactoryForDeprecation1<KtExpression, KotlinType> ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION =
            DiagnosticFactoryForDeprecation1.create(LanguageFeature.ProhibitAssigningSingleElementsToVarargsInNamedForm);
    DiagnosticFactory0<KtExpression> REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION = DiagnosticFactory0.create(WARNING);
    DiagnosticFactoryForDeprecation0<KtExpression> ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION =
            DiagnosticFactoryForDeprecation0.create(LanguageFeature.ProhibitAssigningSingleElementsToVarargsInNamedForm);
    DiagnosticFactory0<KtExpression> REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION = DiagnosticFactory0.create(WARNING);

    // Call resolution

    DiagnosticFactory0<KtExpression> ILLEGAL_SELECTOR = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<KtExpression, KtExpression, KotlinType> FUNCTION_EXPECTED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtExpression, KtExpression, Boolean> FUNCTION_CALL_EXPECTED = DiagnosticFactory2.create(ERROR, CALL_EXPRESSION);

    DiagnosticFactory0<PsiElement> NON_TAIL_RECURSIVE_CALL = DiagnosticFactory0.create(WARNING, CALL_EXPRESSION);
    DiagnosticFactory0<PsiElement> TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED = DiagnosticFactory0.create(WARNING, CALL_EXPRESSION);

    DiagnosticFactory0<PsiElement> NO_CONSTRUCTOR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NO_CONSTRUCTOR_WARNING = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> NOT_A_CLASS = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> OVERLOAD_RESOLUTION_AMBIGUITY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory3<PsiElement, String, String, String> OVERLOAD_RESOLUTION_AMBIGUITY_BECAUSE_OF_STUB_TYPES = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<PsiElement, KotlinType, String, String> STUB_TYPE_IN_ARGUMENT_CAUSES_AMBIGUITY = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory4<PsiElement, KotlinType, String, String, BuilderLambdaLabelingInfo> STUB_TYPE_IN_RECEIVER_CAUSES_AMBIGUITY = DiagnosticFactory4.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> NONE_APPLICABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> CANNOT_COMPLETE_RESOLVE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> UNRESOLVED_REFERENCE_WRONG_RECEIVER =
            DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends CallableDescriptor>> CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY =
            DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<PsiElement, TypeParameterDescriptor> TYPE_PARAMETER_AS_REIFIED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> DEFINITELY_NON_NULLABLE_AS_REIFIED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactoryForDeprecation1<PsiElement, TypeParameterDescriptor> TYPE_PARAMETER_AS_REIFIED_ARRAY =
            DiagnosticFactoryForDeprecation1.create(LanguageFeature.ProhibitNonReifiedArraysAsReifiedTypeArguments);
    DiagnosticFactory1<PsiElement, KotlinType> REIFIED_TYPE_FORBIDDEN_SUBSTITUTION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> REIFIED_TYPE_UNSAFE_SUBSTITUTION = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<KtElement> CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory1<KtElement, CallableDescriptor> COMPATIBILITY_WARNING = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory3<KtReferenceExpression, ClassifierDescriptor, WrongResolutionToClassifier, String> RESOLUTION_TO_CLASSIFIER =
            DiagnosticFactory3.create(ERROR);

    DiagnosticFactory0<KtExpression> RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtParenthesizedExpression> PARENTHESIZED_COMPANION_LHS_DEPRECATION = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<PsiElement> RESOLUTION_TO_PRIVATE_CONSTRUCTOR_OF_SEALED_CLASS = DiagnosticFactory0.create(WARNING);

    // Type inference

    DiagnosticFactory0<KtParameter> CANNOT_INFER_PARAMETER_TYPE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> COULD_BE_INFERRED_ONLY_WITH_UNRESTRICTED_BUILDER_INFERENCE = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_CANNOT_CAPTURE_TYPES = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> TYPE_INFERENCE_INCORPORATION_ERROR = DiagnosticFactory0.create(ERROR);

    DiagnosticFactoryForDeprecation1<PsiElement, TypeParameterDescriptor> TYPE_INFERENCE_ONLY_INPUT_TYPES =
            DiagnosticFactoryForDeprecation1.create(LanguageFeature.StrictOnlyInputTypesChecks);
    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_UPPER_BOUND_VIOLATED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtElement, KotlinType, KotlinType> TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<PsiElement> TYPE_INFERENCE_CANDIDATE_WITH_SAM_AND_VARARG = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> TYPE_INFERENCE_POSTPONED_VARIABLE_IN_RECEIVER_TYPE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtExpression> TYPE_INFERENCE_FAILED_ON_SPECIAL_CONSTRUCT = DiagnosticFactory0.create(ERROR, SPECIAL_CONSTRUCT_TOKEN);

    DiagnosticFactory0<PsiElement> IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> IMPLICIT_NOTHING_TYPE_ARGUMENT_AGAINST_NOT_NOTHING_EXPECTED_TYPE = DiagnosticFactory0.create(WARNING);

    // Reflection

    DiagnosticFactory1<KtExpression, CallableMemberDescriptor> EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtExpression> CALLABLE_REFERENCE_LHS_NOT_A_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtExpression> CLASS_LITERAL_LHS_NOT_A_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ARRAY_CLASS_LITERAL_REQUIRES_ARGUMENT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> NULLABLE_TYPE_IN_CLASS_LITERAL_LHS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<PsiElement> CALLABLE_REFERENCE_TO_JAVA_SYNTHETIC_PROPERTY = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<PsiElement> ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE = DiagnosticFactory0.create(ERROR);

    // Destructuring-declarations

    DiagnosticFactory0<KtDestructuringDeclaration> INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION =
            DiagnosticFactory0.create(ERROR, DEFAULT);
    DiagnosticFactory2<KtExpression, Name, KotlinType> COMPONENT_FUNCTION_MISSING = DiagnosticFactory2.create(ERROR, DEFAULT);
    DiagnosticFactory1<KtExpression, Name> COMPONENT_FUNCTION_ON_NULLABLE = DiagnosticFactory1.create(ERROR, DEFAULT);
    DiagnosticFactory2<KtExpression, Name, Collection<? extends ResolvedCall<?>>> COMPONENT_FUNCTION_AMBIGUITY =
            DiagnosticFactory2.create(ERROR, DEFAULT);
    DiagnosticFactory3<KtExpression, Name, KotlinType, KotlinType> COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH =
            DiagnosticFactory3.create(ERROR, DEFAULT);

    // Super calls

    DiagnosticFactory1<KtSuperExpression, String> SUPER_IS_NOT_AN_EXPRESSION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtSuperExpression, String> SUPER_CANT_BE_EXTENSION_RECEIVER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtSuperExpression> SUPER_NOT_AVAILABLE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtSuperExpression> SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtSuperExpression> AMBIGUOUS_SUPER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ABSTRACT_SUPER_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ABSTRACT_SUPER_CALL_WARNING = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtTypeReference> NOT_A_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory1<KtTypeReference, KotlinType> QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE = DiagnosticFactory1.create(ERROR);

    // Conventions

    DiagnosticFactory2<KtBinaryExpression, KotlinType, KotlinType> DEPRECATED_IDENTITY_EQUALS = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<KtBinaryExpression, KotlinType, KotlinType> IMPLICIT_BOXING_IN_IDENTITY_EQUALS = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<KtBinaryExpression, KotlinType, KotlinType> FORBIDDEN_IDENTITY_EQUALS = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory2<PsiElement, FunctionDescriptor, String> DEPRECATED_BINARY_MOD = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, FunctionDescriptor, String> FORBIDDEN_BINARY_MOD = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, FunctionDescriptor, String> DEPRECATED_BINARY_MOD_AS_REM = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, FunctionDescriptor, String> FORBIDDEN_BINARY_MOD_AS_REM = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory0<KtArrayAccessExpression> NO_GET_METHOD = DiagnosticFactory0.create(ERROR, ARRAY_ACCESS);
    DiagnosticFactory0<KtArrayAccessExpression> NO_SET_METHOD = DiagnosticFactory0.create(ERROR, ARRAY_ACCESS);

    DiagnosticFactory0<KtSimpleNameExpression> INC_DEC_SHOULD_NOT_RETURN_UNIT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<KtSimpleNameExpression, DeclarationDescriptor, KtSimpleNameExpression> ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>>
            ASSIGN_OPERATOR_AMBIGUITY = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtSimpleNameExpression> EQUALS_MISSING = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory3<KtBinaryExpression, KtSimpleNameExpression, KotlinType, KotlinType> EQUALITY_NOT_APPLICABLE =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<KtBinaryExpression, KtSimpleNameExpression, KotlinType, KotlinType> EQUALITY_NOT_APPLICABLE_WARNING =
            DiagnosticFactory3.create(WARNING);

    DiagnosticFactory2<KtElement, KotlinType, KotlinType> INCOMPATIBLE_ENUM_COMPARISON =
            DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<KtElement, KotlinType, KotlinType> INCOMPATIBLE_ENUM_COMPARISON_ERROR =
            DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<KtExpression, KotlinType> HAS_NEXT_MISSING = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> HAS_NEXT_FUNCTION_AMBIGUITY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> HAS_NEXT_FUNCTION_NONE_APPLICABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> HAS_NEXT_FUNCTION_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<KtExpression, KotlinType> NEXT_AMBIGUITY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> NEXT_MISSING = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> NEXT_NONE_APPLICABLE = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtExpression> ITERATOR_MISSING = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ITERATOR_ON_NULLABLE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> ITERATOR_AMBIGUITY = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory3<KtExpression, String, KotlinType, String> DELEGATE_SPECIAL_FUNCTION_MISSING = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory2<KtExpression, String, Collection<? extends ResolvedCall<?>>> DELEGATE_SPECIAL_FUNCTION_AMBIGUITY =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtExpression, String, Collection<? extends ResolvedCall<?>>> DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory3<KtExpression, String, KotlinType, KotlinType> DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory2<KtExpression, String, Collection<? extends ResolvedCall<?>>> DELEGATE_PD_METHOD_NONE_APPLICABLE =
            DiagnosticFactory2.create(WARNING);

    DiagnosticFactory1<KtSimpleNameExpression, KotlinType> COMPARE_TO_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> YIELD_IS_RESERVED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> UNDERSCORE_IS_RESERVED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> UNDERSCORE_USAGE_WITHOUT_BACKTICKS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory1<PsiElement, String> INVALID_CHARACTERS = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<PsiElement, String> INAPPLICABLE_OPERATOR_MODIFIER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> INAPPLICABLE_INFIX_MODIFIER = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory2<PsiElement, FunctionDescriptor, String> OPERATOR_MODIFIER_REQUIRED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, FunctionDescriptor, String> INFIX_MODIFIER_REQUIRED = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory2<PsiElement, FunctionDescriptor, String> PROPERTY_AS_OPERATOR = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, String> INAPPLICABLE_MODIFIER = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<PsiElement, CallableDescriptor> DSL_SCOPE_VIOLATION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, CallableDescriptor> DSL_SCOPE_VIOLATION_WARNING = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory0<PsiElement> NULLABLE_EXTENSION_OPERATOR_WITH_SAFE_CALL_RECEIVER = DiagnosticFactory0.create(WARNING);

    // Labels

    DiagnosticFactory0<KtSimpleNameExpression> LABEL_NAME_CLASH = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory2<KtSimpleNameExpression, String, String> LABEL_RESOLVE_WILL_CHANGE = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory0<KtSimpleNameExpression> AMBIGUOUS_LABEL = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtExpressionWithLabel> BREAK_OR_CONTINUE_OUTSIDE_A_LOOP = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpressionWithLabel> BREAK_OR_CONTINUE_IN_WHEN = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpressionWithLabel> BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtExpressionWithLabel, String> NOT_A_LOOP_LABEL = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtExpressionWithLabel> NOT_A_FUNCTION_LABEL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpressionWithLabel> NOT_A_FUNCTION_LABEL_WARNING = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtElement> REDUNDANT_LABEL_WARNING = DiagnosticFactory0.create(WARNING);

    // Control flow / Data flow

    DiagnosticFactory2<KtElement, Set<KtElement>, Set<KtElement>> UNREACHABLE_CODE = DiagnosticFactory2.create(
            WARNING, ClassicPositioningStrategies.UNREACHABLE_CODE);

    DiagnosticFactory0<KtVariableDeclaration> VARIABLE_WITH_NO_TYPE_NO_INITIALIZER = DiagnosticFactory0.create(ERROR, DECLARATION_NAME);

    DiagnosticFactory1<KtSimpleNameExpression, VariableDescriptor> UNINITIALIZED_VARIABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtSimpleNameExpression, ValueParameterDescriptor> UNINITIALIZED_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtSimpleNameExpression, ValueParameterDescriptor> UNINITIALIZED_PARAMETER_WARNING = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<KtSimpleNameExpression, ClassDescriptor> UNINITIALIZED_ENUM_ENTRY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, ClassDescriptor> UNINITIALIZED_ENUM_COMPANION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, ClassDescriptor> UNINITIALIZED_ENUM_COMPANION_WARNING = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory1<KtNamedDeclaration, VariableDescriptor> UNUSED_VARIABLE = DiagnosticFactory1.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory1<KtParameter, VariableDescriptor> UNUSED_PARAMETER = DiagnosticFactory1.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory1<KtParameter, VariableDescriptor> UNUSED_ANONYMOUS_PARAMETER = DiagnosticFactory1.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory1<KtDestructuringDeclarationEntry, VariableDescriptor> UNUSED_DESTRUCTURED_PARAMETER_ENTRY =
            DiagnosticFactory1.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory2<KtTypeParameter, TypeParameterDescriptor, KotlinType> UNUSED_TYPEALIAS_PARAMETER =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);

    DiagnosticFactory1<KtNamedDeclaration, VariableDescriptor> ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE =
            DiagnosticFactory1.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory1<KtExpression, DeclarationDescriptor> VARIABLE_WITH_REDUNDANT_INITIALIZER = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory2<KtBinaryExpression, KtElement, DeclarationDescriptor> UNUSED_VALUE =
            DiagnosticFactory2.create(WARNING, PositioningStrategies.UNUSED_VALUE);
    DiagnosticFactory1<KtElement, KtElement> UNUSED_CHANGED_VALUE = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<KtElement> UNUSED_EXPRESSION = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtLambdaExpression> UNUSED_LAMBDA_EXPRESSION = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory1<KtExpression, DeclarationDescriptor> VAL_REASSIGNMENT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactoryForDeprecation1<KtExpression, DeclarationDescriptor> VAL_REASSIGNMENT_VIA_BACKING_FIELD =
            DiagnosticFactoryForDeprecation1.create(LanguageFeature.RestrictionOfValReassignmentViaBackingField);
    DiagnosticFactory1<KtExpression, DeclarationDescriptor> CAPTURED_VAL_INITIALIZATION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, DeclarationDescriptor> CAPTURED_MEMBER_VAL_INITIALIZATION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, DeclarationDescriptor> SETTER_PROJECTED_OUT = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<KtExpression, DeclarationDescriptor> INITIALIZATION_BEFORE_DECLARATION = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtExpression> VARIABLE_EXPECTED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<KtBinaryExpression, KtBinaryExpression, Boolean> SENSELESS_COMPARISON = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory0<KtElement> SENSELESS_NULL_IN_WHEN = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<PsiElement> INVALID_IF_AS_EXPRESSION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> INVALID_IF_AS_EXPRESSION_WARNING = DiagnosticFactory0.create(WARNING);
    DiagnosticFactoryForDeprecation0<PsiElement> CONFUSING_BRANCH_CONDITION =
            DiagnosticFactoryForDeprecation0.create(LanguageFeature.ProhibitConfusingSyntaxInWhenBranches);

    // Nullability

    DiagnosticFactory1<PsiElement, KotlinType> UNSAFE_CALL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> UNSAFE_IMPLICIT_INVOKE_CALL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory3<KtExpression, PsiElement, String, PsiElement> UNSAFE_INFIX_CALL = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<KtExpression, PsiElement, String, PsiElement> UNSAFE_OPERATOR_CALL = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> UNNECESSARY_SAFE_CALL = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<KtQualifiedExpression> SAFE_CALL_WILL_CHANGE_NULLABILITY = DiagnosticFactory0.create(WARNING, PositioningStrategies.CALL_ELEMENT_WITH_DOT);
    DiagnosticFactory0<PsiElement> UNEXPECTED_SAFE_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> UNNECESSARY_NOT_NULL_ASSERTION = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<PsiElement> NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory1<KtBinaryExpression, KotlinType> USELESS_ELVIS =
            DiagnosticFactory1.create(WARNING, PositioningStrategies.USELESS_ELVIS);
    DiagnosticFactory0<KtBinaryExpression> USELESS_ELVIS_RIGHT_IS_NULL =
            DiagnosticFactory0.create(WARNING, PositioningStrategies.USELESS_ELVIS);

    // Compile-time values

    DiagnosticFactory0<KtExpression> DIVISION_BY_ZERO = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtExpression> INTEGER_OVERFLOW = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtConstantExpression> WRONG_LONG_SUFFIX = DiagnosticFactory0.create(ERROR, LONG_LITERAL_SUFFIX);
    DiagnosticFactory0<KtConstantExpression> INT_LITERAL_OUT_OF_RANGE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtConstantExpression> FLOAT_LITERAL_OUT_OF_RANGE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtConstantExpression> FLOAT_LITERAL_CONFORMS_INFINITY = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtConstantExpression> FLOAT_LITERAL_CONFORMS_ZERO = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory2<KtConstantExpression, String, KotlinType> CONSTANT_EXPECTED_TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<KtConstantExpression> INCORRECT_CHARACTER_LITERAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtConstantExpression> EMPTY_CHARACTER_LITERAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtConstantExpression> ILLEGAL_UNDERSCORE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtConstantExpression, KtConstantExpression> TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL =
            DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtElement, KtElement> ILLEGAL_ESCAPE = DiagnosticFactory1.create(ERROR, CUT_CHAR_QUOTES);
    DiagnosticFactory1<KtConstantExpression, KotlinType> NULL_FOR_NONNULL_TYPE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtConstantExpression, KotlinType> NULL_FOR_NONNULL_TYPE_WARNING = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<KtEscapeStringTemplateEntry> ILLEGAL_ESCAPE_SEQUENCE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtConstantExpression> UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> INTEGER_OPERATOR_RESOLVE_WILL_CHANGE = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<KtExpression, Boolean> NON_TRIVIAL_BOOLEAN_CONSTANT = DiagnosticFactory1.create(WARNING);

    // Casts and is-checks

    DiagnosticFactory1<KtElement, KotlinType> CANNOT_CHECK_FOR_ERASED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtBinaryExpressionWithTypeRHS, KotlinType, KotlinType> UNCHECKED_CAST = DiagnosticFactory2.create(WARNING, AS_TYPE);

    DiagnosticFactory0<KtBinaryExpressionWithTypeRHS> USELESS_CAST = DiagnosticFactory0.create(WARNING, AS_TYPE);
    DiagnosticFactory0<KtSimpleNameExpression> CAST_NEVER_SUCCEEDS = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtTypeReference> DYNAMIC_NOT_ALLOWED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> IS_ENUM_ENTRY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtSimpleNameExpression> ENUM_ENTRY_AS_TYPE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<KtExpression, KotlinType, KotlinType> IMPLICIT_CAST_TO_ANY = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory3<KtExpression, KotlinType, String, String> SMARTCAST_IMPOSSIBLE = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<KtExpression, KotlinType, String, String> DEPRECATED_SMARTCAST = DiagnosticFactory3.create(WARNING);
    DiagnosticFactory0<KtExpression> ALWAYS_NULL = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtNullableType> USELESS_NULLABLE_CHECK = DiagnosticFactory0.create(WARNING, NULLABLE_TYPE);
    DiagnosticFactory1<KtElement, Boolean> USELESS_IS_CHECK = DiagnosticFactory1.create(WARNING);


    // Properties / locals

    DiagnosticFactory0<KtTypeReference> LOCAL_EXTENSION_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtPropertyAccessor> LOCAL_VARIABLE_WITH_GETTER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtPropertyAccessor> LOCAL_VARIABLE_WITH_SETTER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeParameterList> LOCAL_VARIABLE_WITH_TYPE_PARAMETERS_WARNING = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtTypeParameterList> LOCAL_VARIABLE_WITH_TYPE_PARAMETERS = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory3<PsiElement, DeclarationDescriptor, DescriptorVisibility, DeclarationDescriptor> INVISIBLE_SETTER =
            DiagnosticFactory3.create(ERROR);

    DiagnosticFactory1<PsiElement, KtKeywordToken> VAL_OR_VAR_ON_LOOP_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KtKeywordToken> VAL_OR_VAR_ON_FUN_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KtKeywordToken> VAL_OR_VAR_ON_CATCH_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KtKeywordToken> VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER = DiagnosticFactory1.create(ERROR);

    // When expressions

    DiagnosticFactory0<KtWhenCondition> EXPECTED_CONDITION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtWhenEntry> ELSE_MISPLACED_IN_WHEN = DiagnosticFactory0.create(ERROR, ELSE_ENTRY);
    DiagnosticFactory0<KtWhenEntry> REDUNDANT_ELSE_IN_WHEN = DiagnosticFactory0.create(WARNING, ELSE_ENTRY);
    DiagnosticFactory1<KtWhenExpression, List<WhenMissingCase>> NO_ELSE_IN_WHEN = DiagnosticFactory1.create(ERROR, WHEN_EXPRESSION);
    DiagnosticFactory1<KtWhenExpression, List<WhenMissingCase>> NO_ELSE_IN_WHEN_WARNING =
            DiagnosticFactory1.create(WARNING, WHEN_EXPRESSION);
    DiagnosticFactory1<KtWhenExpression, List<WhenMissingCase>> NON_EXHAUSTIVE_WHEN = DiagnosticFactory1.create(WARNING, WHEN_EXPRESSION);
    DiagnosticFactory2<KtWhenExpression, String, List<WhenMissingCase>> NON_EXHAUSTIVE_WHEN_STATEMENT =
            DiagnosticFactory2.create(WARNING, WHEN_EXPRESSION);
    DiagnosticFactory1<KtWhenExpression, List<WhenMissingCase>>
            NON_EXHAUSTIVE_WHEN_ON_SEALED_CLASS = DiagnosticFactory1.create(INFO, WHEN_EXPRESSION);
    DiagnosticFactory1<KtWhenExpression, String> EXPECT_TYPE_IN_WHEN_WITHOUT_ELSE = DiagnosticFactory1.create(ERROR, WHEN_EXPRESSION);

    DiagnosticFactory0<PsiElement> COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> DUPLICATE_LABEL_IN_WHEN = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory1<PsiElement, String> ILLEGAL_DECLARATION_IN_WHEN_SUBJECT = DiagnosticFactory1.create(ERROR);

    // Type mismatch

    DiagnosticFactory2<KtExpression, KotlinType, KotlinType> TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtExpression, KotlinType, KotlinType> TYPE_MISMATCH_WARNING = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory1<KtElement, KotlinType> TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtElement, TypeMismatchDueToTypeProjectionsData> TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS =
            DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtElement, CallableDescriptor, KotlinType> MEMBER_PROJECTED_OUT = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> RETURN_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> EXPECTED_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtBinaryExpression, KotlinType> ASSIGNMENT_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtTypeReference, KotlinType, KotlinType> TYPE_MISMATCH_IN_FOR_LOOP = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory3<KtExpression, String, KotlinType, KotlinType> RESULT_TYPE_MISMATCH = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory0<KtWhenConditionInRange> TYPE_MISMATCH_IN_RANGE = DiagnosticFactory0.create(ERROR, WHEN_CONDITION_IN_RANGE);

    DiagnosticFactory1<KtParameter, KotlinType> EXPECTED_PARAMETER_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtParameter, KotlinType> EXPECTED_PARAMETER_TYPE_MISMATCH_WARNING = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory2<KtFunction, Integer, List<KotlinType>> EXPECTED_PARAMETERS_NUMBER_MISMATCH =
            DiagnosticFactory2.create(ERROR, FUNCTION_PARAMETERS);

    DiagnosticFactory2<KtElement, KotlinType, KotlinType> INCOMPATIBLE_TYPES = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory0<PsiElement> IMPLICIT_NOTHING_RETURN_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> IMPLICIT_NOTHING_PROPERTY_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> ABBREVIATED_NOTHING_RETURN_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> ABBREVIATED_NOTHING_PROPERTY_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> IMPLICIT_INTERSECTION_TYPE = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtCallableDeclaration> DYNAMIC_RECEIVER_NOT_ALLOWED =
            DiagnosticFactory0.create(ERROR, PositioningStrategies.RECEIVER);

    // Context tracking

    DiagnosticFactory1<KtExpression, KtExpression> EXPRESSION_EXPECTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtBinaryExpression> ASSIGNMENT_IN_EXPRESSION_CONTEXT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtDeclaration> DECLARATION_IN_ILLEGAL_CONTEXT = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtSimpleNameExpression> EXPRESSION_EXPECTED_PACKAGE_FOUND = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtReturnExpression> RETURN_NOT_ALLOWED = DiagnosticFactory0.create(ERROR, PositioningStrategies.RETURN_WITH_LABEL);
    DiagnosticFactory0<KtReturnExpression> RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY =
            DiagnosticFactory0.create(ERROR, PositioningStrategies.RETURN_WITH_LABEL);
    DiagnosticFactory0<KtDeclarationWithBody>
            NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY = DiagnosticFactory0.create(ERROR, DECLARATION_WITH_BODY);
    DiagnosticFactory0<KtDeclarationWithBody>
            NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY_MIGRATION = DiagnosticFactory0.create(ERROR, DECLARATION_WITH_BODY);

    DiagnosticFactory0<KtAnonymousInitializer> ANONYMOUS_INITIALIZER_IN_INTERFACE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<KtThisExpression> NO_THIS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtSimpleNameExpression, ClassifierDescriptor> NO_COMPANION_OBJECT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtSimpleNameExpression, TypeParameterDescriptor> TYPE_PARAMETER_IS_NOT_AN_EXPRESSION =
            DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtSimpleNameExpression, TypeParameterDescriptor> TYPE_PARAMETER_ON_LHS_OF_DOT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, ClassifierDescriptorWithTypeParameters> NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE =
            DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtExpression, ClassDescriptor, String> NESTED_CLASS_SHOULD_BE_QUALIFIED = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<PsiElement, ClassDescriptor> INACCESSIBLE_OUTER_CLASS_EXPRESSION =
            DiagnosticFactory1.create(ERROR, SECONDARY_CONSTRUCTOR_DELEGATION_CALL);
    DiagnosticFactory1<KtClassOrObject, String> NESTED_CLASS_NOT_ALLOWED = DiagnosticFactory1.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory1<KtClassOrObject, String> NESTED_CLASS_DEPRECATED = DiagnosticFactory1.create(WARNING, DECLARATION_NAME);

    //Inline and inlinable parameters
    DiagnosticFactory2<KtElement, DeclarationDescriptor, DeclarationDescriptor> NON_PUBLIC_CALL_FROM_PUBLIC_INLINE =
            DiagnosticFactory2.create(ERROR, CALL_ELEMENT);
    DiagnosticFactory2<KtElement, DeclarationDescriptor, DeclarationDescriptor> PRIVATE_CLASS_MEMBER_FROM_INLINE =
            DiagnosticFactory2.create(ERROR, CALL_ELEMENT);
    DiagnosticFactory1<KtElement, KtElement> NON_LOCAL_RETURN_NOT_ALLOWED = DiagnosticFactory1.create(ERROR, CALL_ELEMENT);
    DiagnosticFactory1<KtDeclaration, String> NOT_YET_SUPPORTED_IN_INLINE =
            DiagnosticFactory1.create(ERROR, NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT);
    DiagnosticFactory0<PsiElement> NOTHING_TO_INLINE = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory2<KtElement, KtExpression, DeclarationDescriptor> USAGE_IS_NOT_INLINABLE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtElement, KtElement, DeclarationDescriptor> NULLABLE_INLINE_PARAMETER = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtElement, KtElement, DeclarationDescriptor> RECURSION_IN_INLINE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<KtDeclaration> DECLARATION_CANT_BE_INLINED = DiagnosticFactory0.create(ERROR, INLINE_FUN_MODIFIER);
    DiagnosticFactory0<KtDeclaration> OVERRIDE_BY_INLINE = DiagnosticFactory0.create(WARNING, DECLARATION_SIGNATURE);
    DiagnosticFactory0<PsiElement> REIFIED_TYPE_PARAMETER_IN_OVERRIDE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, CallableDescriptor> INLINE_CALL_CYCLE = DiagnosticFactory1.create(ERROR, DEFAULT);
    DiagnosticFactory0<PsiElement> NON_LOCAL_RETURN_IN_DISABLED_INLINE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtDeclaration> INLINE_PROPERTY_WITH_BACKING_FIELD = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtAnnotationEntry> NON_INTERNAL_PUBLISHED_API = DiagnosticFactory0.create(ERROR);
    DiagnosticFactoryForDeprecation1<PsiElement, CallableDescriptor> PROTECTED_CALL_FROM_PUBLIC_INLINE =
            DiagnosticFactoryForDeprecation1.create(LanguageFeature.ProhibitProtectedCallFromInline);
    DiagnosticFactoryForDeprecation1<PsiElement, CallableDescriptor> PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE =
            DiagnosticFactoryForDeprecation1.create(LanguageFeature.ProhibitProtectedConstructorCallFromPublicInline);
    DiagnosticFactoryForDeprecation1<PsiElement, CallableDescriptor> SUPER_CALL_FROM_PUBLIC_INLINE =
            DiagnosticFactoryForDeprecation1.create(LanguageFeature.ProhibitSuperCallsFromPublicInline);
    DiagnosticFactory2<KtElement, KtExpression, DeclarationDescriptor> INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtElement, KtExpression, DeclarationDescriptor> NOT_SUPPORTED_INLINE_PARAMETER_IN_INLINE_PARAMETER_DEFAULT_VALUE =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<PsiElement> PRIVATE_INLINE_FUNCTIONS_RETURNING_ANONYMOUS_OBJECTS = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<PsiElement> NON_LOCAL_SUSPENSION_POINT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, CallableDescriptor> ILLEGAL_SUSPEND_FUNCTION_CALL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, CallableDescriptor> ILLEGAL_SUSPEND_PROPERTY_ACCESS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtReturnExpression> RETURN_FOR_BUILT_IN_SUSPEND = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND = DiagnosticFactory0.create(ERROR);
    DiagnosticFactoryForDeprecation0<PsiElement> MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND_FUN =
            DiagnosticFactoryForDeprecation0.create(LanguageFeature.ModifierNonBuiltinSuspendFunError);

    DiagnosticFactory1<PsiElement, RenderedDiagnostic<?>> PLUGIN_ERROR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, RenderedDiagnostic<?>> PLUGIN_WARNING = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<PsiElement, RenderedDiagnostic<?>> PLUGIN_INFO = DiagnosticFactory1.create(INFO);

    // Function contracts
    DiagnosticFactory1<KtElement, String> ERROR_IN_CONTRACT_DESCRIPTION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtElement, String> CONTRACT_NOT_ALLOWED = DiagnosticFactory1.create(ERROR);

    // Context receivers
    DiagnosticFactory1<KtElement, String> NO_CONTEXT_RECEIVER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtElement, String> MULTIPLE_ARGUMENTS_APPLICABLE_FOR_CONTEXT_RECEIVER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtElement> AMBIGUOUS_CALL_WITH_IMPLICIT_CONTEXT_RECEIVER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtElement> UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL = DiagnosticFactory0.create(ERROR);

    // Error sets
    ImmutableSet<? extends DiagnosticFactory<?>> UNRESOLVED_REFERENCE_DIAGNOSTICS = ImmutableSet.of(
            UNRESOLVED_REFERENCE, NAMED_PARAMETER_NOT_FOUND, UNRESOLVED_REFERENCE_WRONG_RECEIVER);
    ImmutableSet<? extends DiagnosticFactory<?>> INVISIBLE_REFERENCE_DIAGNOSTICS = ImmutableSet.of(
            INVISIBLE_MEMBER, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, INVISIBLE_REFERENCE, INVISIBLE_SETTER);
    ImmutableSet<? extends DiagnosticFactory<?>> UNUSED_ELEMENT_DIAGNOSTICS = ImmutableSet.of(
            UNUSED_VARIABLE, UNUSED_PARAMETER, ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE, VARIABLE_WITH_REDUNDANT_INITIALIZER,
            UNUSED_LAMBDA_EXPRESSION, USELESS_CAST, UNUSED_VALUE, USELESS_ELVIS, UNNECESSARY_LATEINIT, REDUNDANT_ELSE_IN_WHEN);
    ImmutableSet<? extends DiagnosticFactory<?>> TYPE_INFERENCE_ERRORS = ImmutableSet.of(
            TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS,
            TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR,
            TYPE_INFERENCE_UPPER_BOUND_VIOLATED, TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH,
            NEW_INFERENCE_ERROR, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER);
    ImmutableSet<? extends DiagnosticFactory<?>> MUST_BE_INITIALIZED_DIAGNOSTICS = ImmutableSet.of(
            MUST_BE_INITIALIZED, MUST_BE_INITIALIZED_OR_BE_ABSTRACT
    );
    ImmutableSet<? extends DiagnosticFactory<?>> TYPE_MISMATCH_ERRORS = ImmutableSet.of(
            TYPE_MISMATCH, CONSTANT_EXPECTED_TYPE_MISMATCH, NULL_FOR_NONNULL_TYPE, TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS,
            MEMBER_PROJECTED_OUT);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // This field is needed to make the Initializer class load (interfaces cannot have static initializers)

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("UnusedDeclaration")
    Initializer __initializer = Initializer.INSTANCE;

    class Initializer {
        private static final String WARNING = "_WARNING";
        private static final String ERROR = "_ERROR";

        static {
            initializeFactoryNames(Errors.class);
        }

        public static void initializeFactoryNames(@NotNull Class<?> aClass) {
            initializeFactoryNamesAndDefaultErrorMessages(aClass, DiagnosticFactoryToRendererMap::new);
        }

        public static void initializeFactoryNamesAndDefaultErrorMessages(
                @NotNull Class<?> aClass,
                @NotNull DefaultErrorMessages.Extension defaultErrorMessages
        ) {
            DiagnosticFactoryToRendererMap diagnosticToRendererMap = defaultErrorMessages.getMap();
            for (Field field : aClass.getFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    try {
                        Object value = field.get(null);
                        if (value instanceof DiagnosticFactory) {
                            initializeNameAndRenderer(diagnosticToRendererMap, field.getName(), (DiagnosticFactory<?>) value);
                        }
                        if (value instanceof DiagnosticFactoryForDeprecation) {
                            String errorName = field.getName();
                            DiagnosticFactoryForDeprecation<?, ?, ?> factory = (DiagnosticFactoryForDeprecation<?, ?, ?>) value;
                            initializeNameAndRenderer(diagnosticToRendererMap, field.getName() + ERROR, factory.getErrorFactory());
                            initializeNameAndRenderer(diagnosticToRendererMap, field.getName() + WARNING, factory.getWarningFactory());
                        }
                    }
                    catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        private static void initializeNameAndRenderer(
                DiagnosticFactoryToRendererMap diagnosticToRendererMap,
                String name,
                DiagnosticFactory<?> factory
        ) {
            factory.initializeName(name);
            factory.setDefaultRenderer((DiagnosticRenderer) diagnosticToRendererMap.get(factory));
        }

        private static final Initializer INSTANCE = new Initializer();

        private Initializer() {
        }
    }
}
