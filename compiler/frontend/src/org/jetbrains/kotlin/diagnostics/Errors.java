/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.diagnostics;

import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cfg.WhenMissingCase;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.lexer.KtKeywordToken;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.varianceChecker.VarianceChecker.VarianceConflictDiagnosticData;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeProjection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.PositioningStrategies.*;
import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;
import static org.jetbrains.kotlin.diagnostics.Severity.WARNING;

/**
 * For error messages, see DefaultErrorMessages and IdeErrorMessages.
 */
public interface Errors {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Meta-errors: unsupported features, failure

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    DiagnosticFactory1<PsiElement, String> UNSUPPORTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Throwable> EXCEPTION_FROM_ANALYZER = DiagnosticFactory1.create(ERROR);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Generic errors/warnings: applicable in many contexts

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    DiagnosticFactory1<PsiElement, String> REDECLARATION = DiagnosticFactory1.create(ERROR, FOR_REDECLARATION);

    DiagnosticFactory1<KtReferenceExpression, KtReferenceExpression> UNRESOLVED_REFERENCE =
            DiagnosticFactory1.create(ERROR, FOR_UNRESOLVED_REFERENCE);

    //Elements with "INVISIBLE_REFERENCE" error are marked as unresolved, unlike elements with "INVISIBLE_MEMBER" error
    //"INVISIBLE_REFERENCE" is used for invisible classes references and references in import
    DiagnosticFactory3<KtSimpleNameExpression, DeclarationDescriptor, Visibility, DeclarationDescriptor> INVISIBLE_REFERENCE =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<PsiElement, DeclarationDescriptor, Visibility, DeclarationDescriptor> INVISIBLE_MEMBER = DiagnosticFactory3.create(ERROR, CALL_ELEMENT);

    // Exposed visibility group
    DiagnosticFactory2<KtProperty, EffectiveVisibility, EffectiveVisibility> EXPOSED_PROPERTY_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, EffectiveVisibility, EffectiveVisibility> EXPOSED_FUNCTION_RETURN_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtParameter, EffectiveVisibility, EffectiveVisibility> EXPOSED_PARAMETER_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtTypeReference, EffectiveVisibility, EffectiveVisibility> EXPOSED_RECEIVER_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtTypeParameter, EffectiveVisibility, EffectiveVisibility> EXPOSED_TYPE_PARAMETER_BOUND = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtSuperTypeListEntry, EffectiveVisibility, EffectiveVisibility> EXPOSED_SUPER_CLASS = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtSuperTypeListEntry, EffectiveVisibility, EffectiveVisibility> EXPOSED_SUPER_INTERFACE = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<KtElement, Collection<ClassDescriptor>> PLATFORM_CLASS_MAPPED_TO_KOTLIN = DiagnosticFactory1.create(WARNING);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Errors/warnings in types
    // Note: class/interface declaration is NOT a type. A type is something that may be written on the right-hand side of ":"

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    DiagnosticFactory0<KtTypeProjection> PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT = DiagnosticFactory0.create(ERROR, VARIANCE_IN_PROJECTION);
    DiagnosticFactory2<KtTypeReference, KotlinType, KotlinType> UPPER_BOUND_VIOLATED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<KtNullableType> REDUNDANT_NULLABLE = DiagnosticFactory0.create(WARNING, NULLABLE_TYPE);
    DiagnosticFactory1<KtElement, Integer> WRONG_NUMBER_OF_TYPE_ARGUMENTS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtTypeReference, Integer, String> NO_TYPE_ARGUMENTS_ON_RHS = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<KtTypeProjection, ClassifierDescriptor> CONFLICTING_PROJECTION = DiagnosticFactory1.create(ERROR, VARIANCE_IN_PROJECTION);
    DiagnosticFactory1<KtTypeProjection, ClassifierDescriptor> REDUNDANT_PROJECTION = DiagnosticFactory1.create(WARNING, VARIANCE_IN_PROJECTION);
    DiagnosticFactory1<PsiElement, VarianceConflictDiagnosticData> TYPE_VARIANCE_CONFLICT =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<PsiElement> FINITE_BOUNDS_VIOLATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> FINITE_BOUNDS_VIOLATION_IN_JAVA = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<PsiElement> EXPANSIVE_INHERITANCE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> EXPANSIVE_INHERITANCE_IN_JAVA = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory0<KtTypeArgumentList> TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtParameter> REIFIED_TYPE_IN_CATCH_CLAUSE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeParameterList> GENERIC_THROWABLE_SUBCLASS = DiagnosticFactory0.create(ERROR);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Errors in declarations

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Imports

    DiagnosticFactory1<KtSimpleNameExpression, ClassDescriptor> CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtSimpleNameExpression, Name> CANNOT_BE_IMPORTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtSimpleNameExpression> PACKAGE_CANNOT_BE_IMPORTED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<KtImportDirective, String> CONFLICTING_IMPORT = DiagnosticFactory1.create(ERROR, PositioningStrategies.IMPORT_ALIAS);

    // Modifiers

    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, KtModifierKeywordToken> INCOMPATIBLE_MODIFIERS = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, KtModifierKeywordToken> DEPRECATED_MODIFIER_PAIR = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory1<PsiElement, KtModifierKeywordToken> REPEATED_MODIFIER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, KtModifierKeywordToken> REDUNDANT_MODIFIER = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, String> WRONG_MODIFIER_TARGET = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, String> DEPRECATED_MODIFIER_FOR_TARGET = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, String> REDUNDANT_MODIFIER_FOR_TARGET = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, String> WRONG_MODIFIER_CONTAINING_DECLARATION = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, KtModifierKeywordToken, String> DEPRECATED_MODIFIER_CONTAINING_DECLARATION = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory1<PsiElement, KtModifierKeywordToken> ILLEGAL_INLINE_PARAMETER_MODIFIER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtAnnotationEntry, String> WRONG_ANNOTATION_TARGET = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtAnnotationEntry, String, String> WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> REPEATED_ANNOTATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION = DiagnosticFactory0.create(ERROR);

    // Annotations

    DiagnosticFactory0<KtSuperTypeList> SUPERTYPES_FOR_ANNOTATION_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtParameter> MISSING_VAL_ON_ANNOTATION_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtCallExpression> ANNOTATION_CLASS_CONSTRUCTOR_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtAnnotationEntry, DeclarationDescriptor> NOT_AN_ANNOTATION_CLASS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> ANNOTATION_CLASS_WITH_BODY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> INVALID_TYPE_OF_ANNOTATION_MEMBER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> NULLABLE_TYPE_OF_ANNOTATION_MEMBER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ANNOTATION_PARAMETER_MUST_BE_CONST = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ANNOTATION_PARAMETER_MUST_BE_KCLASS_LITERAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ANNOTATION_PARAMETER_MUST_BE_ENUM_CONST = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT = DiagnosticFactory0.create(ERROR);

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
    DiagnosticFactory0<PsiElement> INAPPLICABLE_RECEIVER_TARGET = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> INAPPLICABLE_PARAM_TARGET = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> REDUNDANT_ANNOTATION_TARGET = DiagnosticFactory1.create(WARNING);

    // Classes and interfaces

    DiagnosticFactory0<KtTypeProjection> PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE =
            DiagnosticFactory0.create(ERROR, VARIANCE_IN_PROJECTION);

    DiagnosticFactory0<PsiElement> CYCLIC_INHERITANCE_HIERARCHY = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtSuperTypeEntry> SUPERTYPE_NOT_INITIALIZED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtTypeReference> DELEGATION_NOT_TO_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> SUPERTYPE_NOT_A_CLASS_OR_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> NO_GENERICS_IN_SUPERTYPE_SPECIFIER = DiagnosticFactory0.create(ERROR);

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

    DiagnosticFactory0<KtElement> MISSING_CONSTRUCTOR_KEYWORD = DiagnosticFactory0.create(ERROR);

    // Secondary constructors

    DiagnosticFactory0<KtConstructorDelegationReferenceExpression> CYCLIC_CONSTRUCTOR_DELEGATION_CALL = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtDeclaration> CONSTRUCTOR_IN_OBJECT = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtSuperTypeCallEntry> SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtConstructorDelegationCall> PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED =
            DiagnosticFactory0.create(ERROR, PositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL);

    DiagnosticFactory0<KtConstructorDelegationReferenceExpression> DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR =
            DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtConstructorDelegationCall> EXPLICIT_DELEGATION_CALL_REQUIRED =
            DiagnosticFactory0.create(ERROR, PositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL);

    DiagnosticFactory1<PsiElement, DeclarationDescriptor> INSTANCE_ACCESS_BEFORE_SUPER_CALL = DiagnosticFactory1.create(ERROR);

    // Interface-specific

    DiagnosticFactory0<KtModifierListOwner> ABSTRACT_MODIFIER_IN_INTERFACE = DiagnosticFactory0
            .create(WARNING, ABSTRACT_MODIFIER);

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
    DiagnosticFactory0<KtTypeReference> SEALED_SUPERTYPE_IN_LOCAL_CLASS = DiagnosticFactory0.create(ERROR);

    // Companion objects

    DiagnosticFactory0<KtObjectDeclaration> MANY_COMPANION_OBJECTS = DiagnosticFactory0.create(ERROR, COMPANION_OBJECT);

    DiagnosticFactory2<PsiElement, DeclarationDescriptor, String> DEPRECATION = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, DeclarationDescriptor, String> DEPRECATION_ERROR = DiagnosticFactory2.create(ERROR);

    // Objects

    DiagnosticFactory1<KtObjectDeclaration, ClassDescriptor> LOCAL_OBJECT_NOT_ALLOWED = DiagnosticFactory1.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory1<KtClass, ClassDescriptor> LOCAL_INTERFACE_NOT_ALLOWED = DiagnosticFactory1.create(ERROR, DECLARATION_NAME);

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

    DiagnosticFactory0<KtTypeParameter>
            VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY = DiagnosticFactory0.create(ERROR, VARIANCE_MODIFIER);

    DiagnosticFactory0<KtTypeParameterList> DEPRECATED_TYPE_PARAMETER_SYNTAX = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> REIFIED_TYPE_PARAMETER_NO_INLINE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtDeclaration> TYPE_PARAMETERS_NOT_ALLOWED
            = DiagnosticFactory0.create(ERROR, TYPE_PARAMETERS_OR_DECLARATION_SIGNATURE);

    DiagnosticFactory0<KtTypeParameter> TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> CYCLIC_GENERIC_UPPER_BOUND = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtTypeParameter> MISPLACED_TYPE_PARAMETER_CONSTRAINTS = DiagnosticFactory0.create(WARNING);

    // Members

    DiagnosticFactory2<KtDeclaration, CallableMemberDescriptor, String> CONFLICTING_OVERLOADS =
            DiagnosticFactory2.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory0<KtNamedDeclaration> NON_FINAL_MEMBER_IN_FINAL_CLASS = DiagnosticFactory0.create(WARNING, modifierSetPosition(
            KtTokens.OPEN_KEYWORD));

    DiagnosticFactory1<KtModifierListOwner, CallableMemberDescriptor> NOTHING_TO_OVERRIDE = DiagnosticFactory1.create(ERROR, OVERRIDE_MODIFIER);

    DiagnosticFactory3<KtNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor, DeclarationDescriptor> VIRTUAL_MEMBER_HIDDEN =
            DiagnosticFactory3.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory2<KtModifierListOwner, CallableMemberDescriptor, CallableDescriptor> CANNOT_OVERRIDE_INVISIBLE_MEMBER =
            DiagnosticFactory2.create(ERROR, OVERRIDE_MODIFIER);

    DiagnosticFactory2<PsiElement, CallableMemberDescriptor, DeclarationDescriptor> DATA_CLASS_OVERRIDE_CONFLICT =
            DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<KtDeclaration, CallableMemberDescriptor> CANNOT_INFER_VISIBILITY =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory2<KtNamedDeclaration, CallableMemberDescriptor, DeclarationDescriptor> OVERRIDING_FINAL_MEMBER =
            DiagnosticFactory2.create(ERROR, OVERRIDE_MODIFIER);

    DiagnosticFactory3<KtModifierListOwner, Visibility, CallableMemberDescriptor, DeclarationDescriptor> CANNOT_WEAKEN_ACCESS_PRIVILEGE =
            DiagnosticFactory3.create(ERROR, VISIBILITY_MODIFIER);
    DiagnosticFactory3<KtModifierListOwner, Visibility, CallableMemberDescriptor, DeclarationDescriptor> CANNOT_CHANGE_ACCESS_PRIVILEGE =
            DiagnosticFactory3.create(ERROR, VISIBILITY_MODIFIER);
    DiagnosticFactory2<KtNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor> RETURN_TYPE_MISMATCH_ON_OVERRIDE =
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

    DiagnosticFactory2<KtClassOrObject, KtClassOrObject, CallableMemberDescriptor> ABSTRACT_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, KtClassOrObject, CallableMemberDescriptor> ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, KtClassOrObject, CallableMemberDescriptor> MANY_IMPL_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<KtClassOrObject, KtClassOrObject, CallableMemberDescriptor> MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);


    DiagnosticFactory1<KtNamedDeclaration, Collection<KotlinType>> AMBIGUOUS_ANONYMOUS_TYPE_INFERRED =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE);

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
    DiagnosticFactory0<KtPropertyDelegate> LOCAL_VARIABLE_WITH_DELEGATE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtProperty> PROPERTY_WITH_NO_TYPE_NO_INITIALIZER = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<KtProperty> MUST_BE_INITIALIZED = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<KtProperty> MUST_BE_INITIALIZED_OR_BE_ABSTRACT = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<KtExpression> EXTENSION_PROPERTY_WITH_BACKING_FIELD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> PROPERTY_INITIALIZER_NO_BACKING_FIELD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> PROPERTY_INITIALIZER_IN_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtProperty> PRIVATE_PROPERTY_IN_INTERFACE = DiagnosticFactory0.create(ERROR, PRIVATE_MODIFIER);
    DiagnosticFactory0<KtProperty> BACKING_FIELD_IN_INTERFACE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory1<PsiElement, String> INAPPLICABLE_LATEINIT_MODIFIER = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory2<KtModifierListOwner, String, ClassDescriptor> ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS = DiagnosticFactory2.create(ERROR, ABSTRACT_MODIFIER);

    DiagnosticFactory0<KtPropertyAccessor> VAL_WITH_SETTER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtExpression> SETTER_PARAMETER_WITH_DEFAULT_VALUE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<KtTypeReference, KotlinType, KotlinType> WRONG_SETTER_PARAMETER_TYPE = DiagnosticFactory2.create(ERROR);

    // Function-specific

    DiagnosticFactory2<KtFunction, String, ClassDescriptor> ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS = DiagnosticFactory2.create(ERROR, ABSTRACT_MODIFIER);

    DiagnosticFactory1<KtFunction, SimpleFunctionDescriptor> ABSTRACT_FUNCTION_WITH_BODY = DiagnosticFactory1.create(ERROR, ABSTRACT_MODIFIER);
    DiagnosticFactory1<KtFunction, SimpleFunctionDescriptor> NON_ABSTRACT_FUNCTION_WITH_NO_BODY =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory1<KtFunction, SimpleFunctionDescriptor> PRIVATE_FUNCTION_WITH_NO_BODY = DiagnosticFactory1.create(ERROR, PRIVATE_MODIFIER);

    DiagnosticFactory1<KtFunction, SimpleFunctionDescriptor> NON_MEMBER_FUNCTION_NO_BODY =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<KtFunction> FUNCTION_DECLARATION_WITH_NO_NAME = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<PsiElement> ANONYMOUS_FUNCTION_WITH_NAME = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtParameter> VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtNamedFunction> NO_TAIL_CALLS_FOUND = DiagnosticFactory0.create(WARNING, DECLARATION_SIGNATURE);

    DiagnosticFactory0<KtParameter>
            ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE = DiagnosticFactory0.create(ERROR, PARAMETER_DEFAULT_VALUE);

    DiagnosticFactory0<KtParameter> USELESS_VARARG_ON_PARAMETER = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtParameter> MULTIPLE_VARARG_PARAMETERS = DiagnosticFactory0.create(ERROR, PARAMETER_VARARG_MODIFIER);

    // Named parameters

    DiagnosticFactory0<KtParameter> DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE = DiagnosticFactory0.create(ERROR, PARAMETER_DEFAULT_VALUE);

    DiagnosticFactory1<KtParameter, ValueParameterDescriptor> MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<KtClassOrObject, ValueParameterDescriptor> MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE =
            DiagnosticFactory1.create(ERROR, DECLARATION_NAME);

    DiagnosticFactory2<KtParameter, ClassDescriptor, ValueParameterDescriptor> PARAMETER_NAME_CHANGED_ON_OVERRIDE =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);

    DiagnosticFactory2<KtClassOrObject, Collection<? extends CallableMemberDescriptor>, Integer> DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);

    DiagnosticFactory0<KtReferenceExpression> NAME_FOR_AMBIGUOUS_PARAMETER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> DATA_CLASS_WITHOUT_PARAMETERS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtParameter> DATA_CLASS_VARARG_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtParameter> DATA_CLASS_NOT_PROPERTY_PARAMETER = DiagnosticFactory0.create(ERROR);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Errors/warnings inside code blocks

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // General

    DiagnosticFactory1<PsiElement, String> NAME_SHADOWING = DiagnosticFactory1.create(WARNING, PositioningStrategies.FOR_REDECLARATION);
    DiagnosticFactory0<PsiElement> ACCESSOR_PARAMETER_NAME_SHADOWING = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtExpression> TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM = DiagnosticFactory0.create(ERROR);

    // Checking call arguments

    DiagnosticFactory0<PsiElement> MIXING_NAMED_AND_POSITIONED_ARGUMENTS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtReferenceExpression> ARGUMENT_PASSED_TWICE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtReferenceExpression, KtReferenceExpression> NAMED_PARAMETER_NOT_FOUND =
            DiagnosticFactory1.create(ERROR, FOR_UNRESOLVED_REFERENCE);
    DiagnosticFactory1<PsiElement, BadNamedArgumentsTarget> NAMED_ARGUMENTS_NOT_ALLOWED = DiagnosticFactory1.create(ERROR);

    enum BadNamedArgumentsTarget {
        NON_KOTLIN_FUNCTION,
        INVOKE_ON_FUNCTION_TYPE
    }

    DiagnosticFactory0<KtExpression> VARARG_OUTSIDE_PARENTHESES = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<LeafPsiElement> NON_VARARG_SPREAD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<LeafPsiElement> SPREAD_OF_NULLABLE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtExpression> MANY_LAMBDA_EXPRESSION_ARGUMENTS = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, CallableDescriptor> TOO_MANY_ARGUMENTS = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<KtElement, ValueParameterDescriptor> NO_VALUE_FOR_PARAMETER = DiagnosticFactory1.create(ERROR, VALUE_ARGUMENTS);

    DiagnosticFactory1<KtExpression, KotlinType> MISSING_RECEIVER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtExpression> NO_RECEIVER_ALLOWED = DiagnosticFactory0.create(ERROR);

    // Call resolution

    DiagnosticFactory1<KtExpression, String> ILLEGAL_SELECTOR = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<PsiElement> SAFE_CALL_IN_QUALIFIER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<KtExpression, KtExpression, KotlinType> FUNCTION_EXPECTED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtExpression, KtExpression, Boolean> FUNCTION_CALL_EXPECTED = DiagnosticFactory2.create(ERROR, CALL_EXPRESSION);

    DiagnosticFactory0<PsiElement> NON_TAIL_RECURSIVE_CALL = DiagnosticFactory0.create(WARNING, CALL_EXPRESSION);
    DiagnosticFactory0<PsiElement> TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED = DiagnosticFactory0.create(WARNING, CALL_EXPRESSION);

    DiagnosticFactory0<PsiElement> NO_CONSTRUCTOR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> NOT_A_CLASS = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> OVERLOAD_RESOLUTION_AMBIGUITY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> NONE_APPLICABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> CANNOT_COMPLETE_RESOLVE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> UNRESOLVED_REFERENCE_WRONG_RECEIVER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KtExpression> INVOKE_EXTENSION_ON_NOT_EXTENSION_FUNCTION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtElement> INVOKE_ON_EXTENSION_FUNCTION_WITH_EXPLICIT_DISPATCH_RECEIVER = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory1<PsiElement, TypeParameterDescriptor> TYPE_PARAMETER_AS_REIFIED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> REIFIED_TYPE_FORBIDDEN_SUBSTITUTION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> REIFIED_TYPE_UNSAFE_SUBSTITUTION = DiagnosticFactory1.create(WARNING);

    // Type inference

    DiagnosticFactory0<KtParameter> CANNOT_INFER_PARAMETER_TYPE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_CANNOT_CAPTURE_TYPES = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> TYPE_INFERENCE_INCORPORATION_ERROR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, TypeParameterDescriptor> TYPE_INFERENCE_ONLY_INPUT_TYPES = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_UPPER_BOUND_VIOLATED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtElement, KotlinType, KotlinType> TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);

    // Reflection

    DiagnosticFactory1<KtExpression, CallableMemberDescriptor> EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtExpression> CALLABLE_REFERENCE_LHS_NOT_A_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> CALLABLE_REFERENCE_TO_OBJECT_MEMBER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtExpression> CLASS_LITERAL_LHS_NOT_A_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ARRAY_CLASS_LITERAL_REQUIRES_ARGUMENT = DiagnosticFactory0.create(ERROR);

    // Destructuring-declarations

    DiagnosticFactory0<KtDestructuringDeclaration> INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION = DiagnosticFactory0.create(ERROR, DEFAULT);
    DiagnosticFactory2<KtExpression, Name, KotlinType> COMPONENT_FUNCTION_MISSING = DiagnosticFactory2.create(ERROR, DEFAULT);
    DiagnosticFactory2<KtExpression, Name, Collection<? extends ResolvedCall<?>>> COMPONENT_FUNCTION_AMBIGUITY = DiagnosticFactory2.create(ERROR, DEFAULT);
    DiagnosticFactory3<KtExpression, Name, KotlinType, KotlinType> COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH = DiagnosticFactory3.create(ERROR, DEFAULT);

    // Super calls

    DiagnosticFactory1<KtSuperExpression, String> SUPER_IS_NOT_AN_EXPRESSION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtSuperExpression, String> SUPER_CANT_BE_EXTENSION_RECEIVER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtSuperExpression> SUPER_NOT_AVAILABLE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtSuperExpression> SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtSuperExpression> AMBIGUOUS_SUPER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpression> ABSTRACT_SUPER_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> NOT_A_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER = DiagnosticFactory0.create(WARNING);

    // Conventions

    DiagnosticFactory0<KtArrayAccessExpression> NO_GET_METHOD = DiagnosticFactory0.create(ERROR, ARRAY_ACCESS);
    DiagnosticFactory0<KtArrayAccessExpression> NO_SET_METHOD = DiagnosticFactory0.create(ERROR, ARRAY_ACCESS);

    DiagnosticFactory2<KtUnaryExpression, FunctionDescriptor, String> DEPRECATED_UNARY_PLUS_MINUS = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory0<KtSimpleNameExpression> INC_DEC_SHOULD_NOT_RETURN_UNIT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<KtSimpleNameExpression, DeclarationDescriptor, KtSimpleNameExpression> ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>>
            ASSIGN_OPERATOR_AMBIGUITY = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtSimpleNameExpression> EQUALS_MISSING = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory3<KtBinaryExpression, KtSimpleNameExpression, KotlinType, KotlinType> EQUALITY_NOT_APPLICABLE =
            DiagnosticFactory3.create(ERROR);


    DiagnosticFactory1<KtExpression, KotlinType> HAS_NEXT_MISSING = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> HAS_NEXT_FUNCTION_AMBIGUITY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> HAS_NEXT_FUNCTION_NONE_APPLICABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> HAS_NEXT_FUNCTION_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<KtExpression, KotlinType> NEXT_AMBIGUITY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> NEXT_MISSING = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> NEXT_NONE_APPLICABLE = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtExpression> ITERATOR_MISSING = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> ITERATOR_AMBIGUITY = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory2<KtExpression, String, KotlinType> DELEGATE_SPECIAL_FUNCTION_MISSING = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory3<PsiElement, FunctionDescriptor, KotlinType, String> DELEGATE_RESOLVED_TO_DEPRECATED_CONVENTION = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory2<KtExpression, String, Collection<? extends ResolvedCall<?>>> DELEGATE_SPECIAL_FUNCTION_AMBIGUITY = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtExpression, String, Collection<? extends ResolvedCall<?>>> DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory3<KtExpression, String, KotlinType, KotlinType> DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory2<KtExpression, String, Collection<? extends ResolvedCall<?>>> DELEGATE_PD_METHOD_NONE_APPLICABLE = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory1<KtSimpleNameExpression, KotlinType> COMPARE_TO_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<PsiElement> UNDERSCORE_IS_RESERVED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> INVALID_CHARACTERS = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<PsiElement> INAPPLICABLE_OPERATOR_MODIFIER = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> INAPPLICABLE_INFIX_MODIFIER = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory2<PsiElement, FunctionDescriptor, String> OPERATOR_MODIFIER_REQUIRED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtOperationReferenceExpression, FunctionDescriptor, String> INFIX_MODIFIER_REQUIRED = DiagnosticFactory2.create(ERROR);

    // Labels

    DiagnosticFactory0<KtSimpleNameExpression> LABEL_NAME_CLASH = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtSimpleNameExpression> AMBIGUOUS_LABEL = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtExpressionWithLabel> BREAK_OR_CONTINUE_OUTSIDE_A_LOOP = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpressionWithLabel> BREAK_OR_CONTINUE_IN_WHEN = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtExpressionWithLabel> BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtExpressionWithLabel, String> NOT_A_LOOP_LABEL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtReturnExpression, String> NOT_A_RETURN_LABEL = DiagnosticFactory1.create(ERROR);

    // Control flow / Data flow

    DiagnosticFactory1<KtElement, List<TextRange>> UNREACHABLE_CODE = DiagnosticFactory1.create(
            WARNING, PositioningStrategies.UNREACHABLE_CODE);

    DiagnosticFactory0<KtVariableDeclaration> VARIABLE_WITH_NO_TYPE_NO_INITIALIZER = DiagnosticFactory0.create(ERROR, DECLARATION_NAME);

    DiagnosticFactory1<KtSimpleNameExpression, VariableDescriptor> UNINITIALIZED_VARIABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtSimpleNameExpression, ValueParameterDescriptor> UNINITIALIZED_PARAMETER = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<KtNamedDeclaration, VariableDescriptor> UNUSED_VARIABLE = DiagnosticFactory1.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory1<KtParameter, VariableDescriptor> UNUSED_PARAMETER = DiagnosticFactory1.create(WARNING, DECLARATION_NAME);

    DiagnosticFactory1<KtNamedDeclaration, VariableDescriptor> ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE =
            DiagnosticFactory1.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory1<KtExpression, DeclarationDescriptor> VARIABLE_WITH_REDUNDANT_INITIALIZER = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory2<KtBinaryExpression, KtElement, DeclarationDescriptor> UNUSED_VALUE = DiagnosticFactory2.create(WARNING, PositioningStrategies.UNUSED_VALUE);
    DiagnosticFactory1<KtElement, KtElement> UNUSED_CHANGED_VALUE = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<KtElement> UNUSED_EXPRESSION = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtLambdaExpression> UNUSED_LAMBDA_EXPRESSION = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory1<KtExpression, DeclarationDescriptor> VAL_REASSIGNMENT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, DeclarationDescriptor> SETTER_PROJECTED_OUT = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<KtExpression, DeclarationDescriptor> INITIALIZATION_BEFORE_DECLARATION = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<KtExpression> VARIABLE_EXPECTED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<KtBinaryExpression, KtBinaryExpression, Boolean> SENSELESS_COMPARISON = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory0<KtElement> SENSELESS_NULL_IN_WHEN = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtIfExpression> INVALID_IF_AS_EXPRESSION = DiagnosticFactory0.create(ERROR);

    // Nullability

    DiagnosticFactory1<PsiElement, KotlinType> UNSAFE_CALL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> UNSAFE_IMPLICIT_INVOKE_CALL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory3<KtExpression, String, String, String> UNSAFE_INFIX_CALL = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> UNNECESSARY_SAFE_CALL = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<PsiElement> UNEXPECTED_SAFE_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> UNNECESSARY_NOT_NULL_ASSERTION = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<PsiElement> NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory1<KtBinaryExpression, KotlinType> USELESS_ELVIS = DiagnosticFactory1.create(WARNING, PositioningStrategies.USELESS_ELVIS);
    DiagnosticFactory0<PsiElement> USELESS_ELVIS_ON_LAMBDA_EXPRESSION = DiagnosticFactory0.create(WARNING);

    // Compile-time values

    DiagnosticFactory0<KtExpression> DIVISION_BY_ZERO = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtExpression> INTEGER_OVERFLOW = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtConstantExpression> WRONG_LONG_SUFFIX = DiagnosticFactory0.create(ERROR, LONG_LITERAL_SUFFIX);
    DiagnosticFactory0<KtConstantExpression> INT_LITERAL_OUT_OF_RANGE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtConstantExpression> FLOAT_LITERAL_OUT_OF_RANGE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<KtConstantExpression, String, KotlinType> CONSTANT_EXPECTED_TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<KtConstantExpression> INCORRECT_CHARACTER_LITERAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtConstantExpression> EMPTY_CHARACTER_LITERAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtConstantExpression, KtConstantExpression> TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtElement, KtElement> ILLEGAL_ESCAPE = DiagnosticFactory1.create(ERROR, CUT_CHAR_QUOTES);
    DiagnosticFactory1<KtConstantExpression, KotlinType> NULL_FOR_NONNULL_TYPE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtEscapeStringTemplateEntry> ILLEGAL_ESCAPE_SEQUENCE = DiagnosticFactory0.create(ERROR);

    // Casts and is-checks

    DiagnosticFactory1<KtElement, KotlinType> CANNOT_CHECK_FOR_ERASED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtBinaryExpressionWithTypeRHS, KotlinType, KotlinType> UNCHECKED_CAST = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory0<KtBinaryExpressionWithTypeRHS> USELESS_CAST = DiagnosticFactory0.create(WARNING, AS_TYPE);
    DiagnosticFactory0<KtSimpleNameExpression> CAST_NEVER_SUCCEEDS = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtTypeReference> DYNAMIC_NOT_ALLOWED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> IS_ENUM_ENTRY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtTypeReference> ENUM_ENTRY_AS_TYPE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<KtExpression, KotlinType, KotlinType> IMPLICIT_CAST_TO_ANY = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory3<KtExpression, KotlinType, String, String> SMARTCAST_IMPOSSIBLE = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory0<KtExpression> ALWAYS_NULL = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtNullableType> USELESS_NULLABLE_CHECK = DiagnosticFactory0.create(WARNING, NULLABLE_TYPE);

    // Properties / locals

    DiagnosticFactory0<KtTypeReference> LOCAL_EXTENSION_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtPropertyAccessor> LOCAL_VARIABLE_WITH_GETTER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtPropertyAccessor> LOCAL_VARIABLE_WITH_SETTER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory3<KtExpression, DeclarationDescriptor, Visibility, DeclarationDescriptor> INVISIBLE_SETTER = DiagnosticFactory3.create(ERROR);

    DiagnosticFactory1<PsiElement, KtKeywordToken> VAL_OR_VAR_ON_LOOP_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KtKeywordToken> VAL_OR_VAR_ON_LOOP_MULTI_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KtKeywordToken> VAL_OR_VAR_ON_FUN_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KtKeywordToken> VAL_OR_VAR_ON_CATCH_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, KtKeywordToken> VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER = DiagnosticFactory1.create(ERROR);

    // When expressions

    DiagnosticFactory0<KtWhenCondition> EXPECTED_CONDITION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtWhenEntry> ELSE_MISPLACED_IN_WHEN = DiagnosticFactory0.create(ERROR, ELSE_ENTRY);
    DiagnosticFactory1<KtWhenExpression, List<WhenMissingCase>> NO_ELSE_IN_WHEN = DiagnosticFactory1.create(ERROR, WHEN_EXPRESSION);
    DiagnosticFactory1<KtWhenExpression, List<WhenMissingCase>> NON_EXHAUSTIVE_WHEN = DiagnosticFactory1.create(WARNING, WHEN_EXPRESSION);
    DiagnosticFactory0<PsiElement> COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT = DiagnosticFactory0.create(ERROR);

    // Type mismatch

    DiagnosticFactory2<KtExpression, KotlinType, KotlinType> TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<KtElement, TypeMismatchDueToTypeProjectionsData> TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtElement, CallableDescriptor, KotlinType> MEMBER_PROJECTED_OUT = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> RETURN_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, KotlinType> EXPECTED_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtBinaryExpression, KotlinType> ASSIGNMENT_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtTypeReference, KotlinType, KotlinType> TYPE_MISMATCH_IN_FOR_LOOP = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<KtElement, KotlinType> TYPE_MISMATCH_IN_CONDITION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory3<KtExpression, String, KotlinType, KotlinType> RESULT_TYPE_MISMATCH = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory0<KtWhenConditionInRange> TYPE_MISMATCH_IN_RANGE = DiagnosticFactory0.create(ERROR, WHEN_CONDITION_IN_RANGE);

    DiagnosticFactory1<KtParameter, KotlinType> EXPECTED_PARAMETER_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtFunction, Integer, List<KotlinType>> EXPECTED_PARAMETERS_NUMBER_MISMATCH =
            DiagnosticFactory2.create(ERROR, FUNCTION_PARAMETERS);

    DiagnosticFactory2<KtElement, KotlinType, KotlinType> INCOMPATIBLE_TYPES = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory0<PsiElement> IMPLICIT_NOTHING_RETURN_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> IMPLICIT_NOTHING_PROPERTY_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> IMPLICIT_INTERSECTION_TYPE = DiagnosticFactory1.create(ERROR);

    // Context tracking

    DiagnosticFactory1<KtExpression, KtExpression> EXPRESSION_EXPECTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtBinaryExpression> ASSIGNMENT_IN_EXPRESSION_CONTEXT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtDeclaration> DECLARATION_IN_ILLEGAL_CONTEXT = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtSimpleNameExpression> EXPRESSION_EXPECTED_PACKAGE_FOUND = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtReturnExpression> RETURN_NOT_ALLOWED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtReturnExpression> RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtDeclarationWithBody>
            NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY = DiagnosticFactory0.create(ERROR, DECLARATION_WITH_BODY);

    DiagnosticFactory0<KtAnonymousInitializer> ANONYMOUS_INITIALIZER_IN_INTERFACE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<KtThisExpression> NO_THIS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtSimpleNameExpression, ClassifierDescriptor> NO_COMPANION_OBJECT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtSimpleNameExpression, TypeParameterDescriptor> TYPE_PARAMETER_IS_NOT_AN_EXPRESSION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtSimpleNameExpression, TypeParameterDescriptor> TYPE_PARAMETER_ON_LHS_OF_DOT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtExpression, ClassDescriptor> NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtExpression, ClassDescriptor, String> NESTED_CLASS_SHOULD_BE_QUALIFIED = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<PsiElement, ClassDescriptor> INACCESSIBLE_OUTER_CLASS_EXPRESSION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtClass> NESTED_CLASS_NOT_ALLOWED = DiagnosticFactory0.create(ERROR, DECLARATION_NAME);

    //Inline and inlinable parameters
    DiagnosticFactory2<KtElement, DeclarationDescriptor, DeclarationDescriptor> INVISIBLE_MEMBER_FROM_INLINE = DiagnosticFactory2.create(ERROR, CALL_ELEMENT);
    DiagnosticFactory3<KtElement, KtElement, DeclarationDescriptor, DeclarationDescriptor> NON_LOCAL_RETURN_NOT_ALLOWED = DiagnosticFactory3.create(ERROR, CALL_ELEMENT);
    DiagnosticFactory2<KtElement, KtNamedDeclaration, DeclarationDescriptor> NOT_YET_SUPPORTED_IN_INLINE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<PsiElement, DeclarationDescriptor> NOTHING_TO_INLINE = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory2<KtElement, KtExpression, DeclarationDescriptor> USAGE_IS_NOT_INLINABLE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtElement, KtElement, DeclarationDescriptor> NULLABLE_INLINE_PARAMETER = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtElement, KtElement, DeclarationDescriptor> RECURSION_IN_INLINE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<KtElement> DECLARATION_CANT_BE_INLINED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, CallableDescriptor> INLINE_CALL_CYCLE = DiagnosticFactory1.create(ERROR, DEFAULT);
    DiagnosticFactory0<PsiElement> NON_LOCAL_RETURN_IN_DISABLED_INLINE = DiagnosticFactory0.create(ERROR);


    // Error sets
    ImmutableSet<? extends DiagnosticFactory<?>> UNRESOLVED_REFERENCE_DIAGNOSTICS = ImmutableSet.of(
            UNRESOLVED_REFERENCE, NAMED_PARAMETER_NOT_FOUND, UNRESOLVED_REFERENCE_WRONG_RECEIVER);
    ImmutableSet<? extends DiagnosticFactory<?>> INVISIBLE_REFERENCE_DIAGNOSTICS = ImmutableSet.of(
            INVISIBLE_MEMBER, INVISIBLE_MEMBER_FROM_INLINE, INVISIBLE_REFERENCE, INVISIBLE_SETTER);
    ImmutableSet<? extends DiagnosticFactory<?>> UNUSED_ELEMENT_DIAGNOSTICS = ImmutableSet.of(
            UNUSED_VARIABLE, UNUSED_PARAMETER, ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE, VARIABLE_WITH_REDUNDANT_INITIALIZER,
            UNUSED_LAMBDA_EXPRESSION, USELESS_CAST, UNUSED_VALUE, USELESS_ELVIS);
    ImmutableSet<? extends DiagnosticFactory<?>> TYPE_INFERENCE_ERRORS = ImmutableSet.of(
            TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS,
            TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR,
            TYPE_INFERENCE_UPPER_BOUND_VIOLATED, TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH);
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
        static {
            initializeFactoryNames(Errors.class);
        }

        public static void initializeFactoryNames(@NotNull Class<?> aClass) {
            for (Field field : aClass.getFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    try {
                        Object value = field.get(null);
                        if (value instanceof DiagnosticFactory) {
                            DiagnosticFactory<?> factory = (DiagnosticFactory<?>)value;
                            factory.setName(field.getName());
                        }
                    }
                    catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }

        private static final Initializer INSTANCE = new Initializer();

        private Initializer() {
        }
    }
}
