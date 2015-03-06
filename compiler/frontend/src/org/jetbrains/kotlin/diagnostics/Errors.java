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
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.lexer.JetKeywordToken;
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.varianceChecker.VarianceChecker.VarianceConflictDiagnosticData;
import org.jetbrains.kotlin.types.JetType;

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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Generic errors/warnings: applicable in many contexts

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    DiagnosticFactory1<PsiElement, String> REDECLARATION = DiagnosticFactory1.create(ERROR, FOR_REDECLARATION);

    DiagnosticFactory1<JetReferenceExpression, JetReferenceExpression> UNRESOLVED_REFERENCE =
            DiagnosticFactory1.create(ERROR, FOR_UNRESOLVED_REFERENCE);

    //Elements with "INVISIBLE_REFERENCE" error are marked as unresolved, unlike elements with "INVISIBLE_MEMBER" error
    //"INVISIBLE_REFERENCE" is used for invisible classes references and references in import
    DiagnosticFactory3<JetSimpleNameExpression, DeclarationDescriptor, Visibility, DeclarationDescriptor> INVISIBLE_REFERENCE =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<PsiElement, DeclarationDescriptor, Visibility, DeclarationDescriptor> INVISIBLE_MEMBER = DiagnosticFactory3.create(ERROR, CALL_ELEMENT);

    DiagnosticFactory1<JetElement, Collection<ClassDescriptor>> PLATFORM_CLASS_MAPPED_TO_KOTLIN = DiagnosticFactory1.create(WARNING);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Errors/warnings in types
    // Note: class/trait declaration is NOT a type. A type is something that may be written on the right-hand side of ":"

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    DiagnosticFactory0<JetTypeProjection> PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT = DiagnosticFactory0.create(ERROR, VARIANCE_IN_PROJECTION);
    DiagnosticFactory2<JetTypeReference, JetType, JetType> UPPER_BOUND_VIOLATED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<JetNullableType> REDUNDANT_NULLABLE = DiagnosticFactory0.create(WARNING, NULLABLE_TYPE);
    DiagnosticFactory1<JetNullableType, JetType> BASE_WITH_NULLABLE_UPPER_BOUND = DiagnosticFactory1.create(WARNING, NULLABLE_TYPE);
    DiagnosticFactory1<JetElement, Integer> WRONG_NUMBER_OF_TYPE_ARGUMENTS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<JetTypeReference, Integer, String> NO_TYPE_ARGUMENTS_ON_RHS = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<JetTypeProjection, ClassifierDescriptor> CONFLICTING_PROJECTION = DiagnosticFactory1.create(ERROR, VARIANCE_IN_PROJECTION);
    DiagnosticFactory1<JetTypeProjection, ClassifierDescriptor> REDUNDANT_PROJECTION = DiagnosticFactory1.create(WARNING, VARIANCE_IN_PROJECTION);
    DiagnosticFactory1<PsiElement, VarianceConflictDiagnosticData> TYPE_VARIANCE_CONFLICT =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Errors in declarations

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Imports

    DiagnosticFactory1<JetSimpleNameExpression, DeclarationDescriptor> CANNOT_IMPORT_FROM_ELEMENT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetSimpleNameExpression, ClassDescriptor> CANNOT_IMPORT_ON_DEMAND_FROM_SINGLETON = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetSimpleNameExpression, DeclarationDescriptor> CANNOT_BE_IMPORTED = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<JetExpression, String> CONFLICTING_IMPORT = DiagnosticFactory1.create(ERROR);

    // Modifiers

    DiagnosticFactory1<PsiElement, Collection<JetModifierKeywordToken>> INCOMPATIBLE_MODIFIERS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, JetModifierKeywordToken> ILLEGAL_MODIFIER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, JetModifierKeywordToken> REPEATED_MODIFIER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, JetModifierKeywordToken, JetModifierKeywordToken> REDUNDANT_MODIFIER = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory0<PsiElement> INAPPLICABLE_ANNOTATION = DiagnosticFactory0.create(ERROR);

    // Annotations

    DiagnosticFactory0<JetDelegationSpecifierList> SUPERTYPES_FOR_ANNOTATION_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetParameter> MISSING_VAL_ON_ANNOTATION_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetCallExpression> ANNOTATION_CLASS_CONSTRUCTOR_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<JetAnnotationEntry, DeclarationDescriptor> NOT_AN_ANNOTATION_CLASS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> ANNOTATION_CLASS_WITH_BODY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetTypeReference> INVALID_TYPE_OF_ANNOTATION_MEMBER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetTypeReference> NULLABLE_TYPE_OF_ANNOTATION_MEMBER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetModifierListOwner> ILLEGAL_ANNOTATION_KEYWORD = DiagnosticFactory0
            .create(ERROR, modifierSetPosition(JetTokens.ANNOTATION_KEYWORD));
    DiagnosticFactory0<JetExpression> ANNOTATION_PARAMETER_MUST_BE_CONST = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetExpression> ANNOTATION_PARAMETER_MUST_BE_CLASS_LITERAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetExpression> ANNOTATION_PARAMETER_MUST_BE_ENUM_CONST = DiagnosticFactory0.create(ERROR);

    // Classes and traits

    DiagnosticFactory0<JetTypeProjection> PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE =
            DiagnosticFactory0.create(ERROR, VARIANCE_IN_PROJECTION);

    DiagnosticFactory0<PsiElement> CYCLIC_INHERITANCE_HIERARCHY = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetDelegatorToSuperClass> SUPERTYPE_NOT_INITIALIZED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetTypeReference> DELEGATION_NOT_TO_TRAIT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetTypeReference> SUPERTYPE_NOT_A_CLASS_OR_TRAIT = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> NO_GENERICS_IN_SUPERTYPE_SPECIFIER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetTypeReference> MANY_CLASSES_IN_SUPERTYPE_LIST = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetTypeReference> SUPERTYPE_APPEARS_TWICE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory3<JetDelegationSpecifierList, TypeParameterDescriptor, ClassDescriptor, Collection<JetType>>
            INCONSISTENT_TYPE_PARAMETER_VALUES = DiagnosticFactory3.create(ERROR);


    DiagnosticFactory0<JetTypeReference> FINAL_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetTypeReference> SINGLETON_IN_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetNullableType> NULLABLE_SUPERTYPE = DiagnosticFactory0.create(ERROR, NULLABLE_TYPE);
    DiagnosticFactory0<JetTypeReference> DYNAMIC_SUPERTYPE = DiagnosticFactory0.create(ERROR);

    // Trait-specific

    DiagnosticFactory0<JetModifierListOwner> ABSTRACT_MODIFIER_IN_TRAIT = DiagnosticFactory0
            .create(WARNING, ABSTRACT_MODIFIER);
    DiagnosticFactory0<JetModifierListOwner> OPEN_MODIFIER_IN_TRAIT = DiagnosticFactory0
            .create(WARNING, modifierSetPosition(JetTokens.OPEN_KEYWORD));
    DiagnosticFactory0<JetModifierListOwner> TRAIT_CAN_NOT_BE_FINAL = DiagnosticFactory0.create(ERROR, FINAL_MODIFIER);

    DiagnosticFactory0<PsiElement> CONSTRUCTOR_IN_TRAIT = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> SUPERTYPE_INITIALIZED_IN_TRAIT = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetDelegatorByExpressionSpecifier> DELEGATION_IN_TRAIT = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<JetNamedDeclaration, ClassDescriptor, ClassDescriptor> UNMET_TRAIT_REQUIREMENT =
            DiagnosticFactory2.create(ERROR, PositioningStrategies.DECLARATION_NAME);

    // Enum-specific

    DiagnosticFactory0<JetModifierListOwner> ILLEGAL_ENUM_ANNOTATION = DiagnosticFactory0
            .create(ERROR, modifierSetPosition(JetTokens.ENUM_KEYWORD));

    DiagnosticFactory0<JetModifierListOwner> OPEN_MODIFIER_IN_ENUM = DiagnosticFactory0
            .create(ERROR, modifierSetPosition(JetTokens.OPEN_KEYWORD));
    DiagnosticFactory0<JetModifierListOwner> ABSTRACT_MODIFIER_IN_ENUM = DiagnosticFactory0
            .create(ERROR, modifierSetPosition(JetTokens.ABSTRACT_KEYWORD));

    DiagnosticFactory0<PsiElement> CLASS_IN_SUPERTYPE_FOR_ENUM = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetTypeParameterList> TYPE_PARAMETERS_IN_ENUM = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<JetClass, ClassDescriptor> ENUM_ENTRY_SHOULD_BE_INITIALIZED = DiagnosticFactory1.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory1<JetTypeReference, ClassDescriptor> ENUM_ENTRY_ILLEGAL_TYPE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetClass, ClassDescriptor> LOCAL_ENUM_NOT_ALLOWED = DiagnosticFactory1.create(ERROR, DECLARATION_NAME);

    // Default objects

    DiagnosticFactory0<JetObjectDeclaration> MANY_DEFAULT_OBJECTS = DiagnosticFactory0.create(ERROR, DEFAULT_OBJECT);
    DiagnosticFactory0<JetObjectDeclaration> DEFAULT_OBJECT_NOT_ALLOWED = DiagnosticFactory0.create(ERROR, DEFAULT_OBJECT);

    // Objects

    DiagnosticFactory1<JetObjectDeclaration, ClassDescriptor> LOCAL_OBJECT_NOT_ALLOWED = DiagnosticFactory1.create(ERROR, DECLARATION_NAME);

    // Type parameter declarations

    DiagnosticFactory1<JetTypeReference, JetType> FINAL_UPPER_BOUND = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<JetTypeReference, JetType> FINAL_DEFAULT_OBJECT_UPPER_BOUND = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<JetTypeReference> DYNAMIC_UPPER_BOUND = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<JetNamedDeclaration, TypeParameterDescriptor> CONFLICTING_UPPER_BOUNDS =
            DiagnosticFactory1.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory1<JetNamedDeclaration, TypeParameterDescriptor> CONFLICTING_DEFAULT_OBJECT_UPPER_BOUNDS
            = DiagnosticFactory1.create(ERROR, DECLARATION_NAME);

    DiagnosticFactory2<JetSimpleNameExpression, JetTypeConstraint, JetTypeParameterListOwner> NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER =
            DiagnosticFactory2.create(ERROR);

    DiagnosticFactory0<JetTypeParameter>
            VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY = DiagnosticFactory0.create(ERROR, VARIANCE_MODIFIER);

    DiagnosticFactory0<PsiElement> REIFIED_TYPE_PARAMETER_NO_INLINE = DiagnosticFactory0.create(ERROR);

    // Members

    DiagnosticFactory0<JetModifierListOwner> PACKAGE_MEMBER_CANNOT_BE_PROTECTED =
            DiagnosticFactory0.create(ERROR, modifierSetPosition(JetTokens.PROTECTED_KEYWORD));

    DiagnosticFactory0<JetNamedDeclaration> PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory2<JetDeclaration, CallableMemberDescriptor, String> CONFLICTING_OVERLOADS =
            DiagnosticFactory2.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory1<JetAnnotationEntry, String> ILLEGAL_PLATFORM_NAME = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<JetNamedDeclaration> NON_FINAL_MEMBER_IN_FINAL_CLASS = DiagnosticFactory0.create(WARNING, modifierSetPosition(
            JetTokens.OPEN_KEYWORD));

    DiagnosticFactory1<JetModifierListOwner, CallableMemberDescriptor> NOTHING_TO_OVERRIDE = DiagnosticFactory1.create(ERROR, OVERRIDE_MODIFIER);

    DiagnosticFactory3<JetNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor, DeclarationDescriptor> VIRTUAL_MEMBER_HIDDEN =
            DiagnosticFactory3.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory2<JetModifierListOwner, CallableMemberDescriptor, CallableDescriptor> CANNOT_OVERRIDE_INVISIBLE_MEMBER =
            DiagnosticFactory2.create(ERROR, OVERRIDE_MODIFIER);

    DiagnosticFactory2<JetAnnotationEntry, CallableMemberDescriptor, DeclarationDescriptor> DATA_CLASS_OVERRIDE_CONFLICT =
            DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<JetDeclaration, CallableMemberDescriptor> CANNOT_INFER_VISIBILITY =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory2<JetNamedDeclaration, CallableMemberDescriptor, DeclarationDescriptor> OVERRIDING_FINAL_MEMBER =
            DiagnosticFactory2.create(ERROR, OVERRIDE_MODIFIER);

    DiagnosticFactory3<JetModifierListOwner, Visibility, CallableMemberDescriptor, DeclarationDescriptor> CANNOT_WEAKEN_ACCESS_PRIVILEGE =
            DiagnosticFactory3.create(ERROR, VISIBILITY_MODIFIER);
    DiagnosticFactory3<JetModifierListOwner, Visibility, CallableMemberDescriptor, DeclarationDescriptor> CANNOT_CHANGE_ACCESS_PRIVILEGE =
            DiagnosticFactory3.create(ERROR, VISIBILITY_MODIFIER);
    DiagnosticFactory2<JetNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor> RETURN_TYPE_MISMATCH_ON_OVERRIDE =
            DiagnosticFactory2.create(ERROR, DECLARATION_RETURN_TYPE);
    DiagnosticFactory2<JetNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor> PROPERTY_TYPE_MISMATCH_ON_OVERRIDE =
            DiagnosticFactory2.create(ERROR, DECLARATION_RETURN_TYPE);

    DiagnosticFactory2<JetClassOrObject, JetClassOrObject, CallableMemberDescriptor> ABSTRACT_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<JetClassOrObject, JetClassOrObject, CallableMemberDescriptor> MANY_IMPL_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);

    DiagnosticFactory1<JetNamedDeclaration, Collection<JetType>> AMBIGUOUS_ANONYMOUS_TYPE_INFERRED =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE);

    // Property-specific

    DiagnosticFactory2<JetProperty, PropertyDescriptor, PropertyDescriptor> VAR_OVERRIDDEN_BY_VAL =
            DiagnosticFactory2.create(ERROR, VAL_OR_VAR_NODE);

    DiagnosticFactory0<PsiElement> REDUNDANT_MODIFIER_IN_GETTER = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<JetTypeReference, JetType, JetType> WRONG_GETTER_RETURN_TYPE = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory0<JetModifierListOwner>
            ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS = DiagnosticFactory0.create(ERROR, ABSTRACT_MODIFIER);
    DiagnosticFactory0<JetExpression> ABSTRACT_PROPERTY_WITH_INITIALIZER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetPropertyAccessor> ABSTRACT_PROPERTY_WITH_GETTER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetPropertyAccessor> ABSTRACT_PROPERTY_WITH_SETTER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetPropertyDelegate> ABSTRACT_DELEGATED_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetPropertyAccessor> ACCESSOR_FOR_DELEGATED_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetPropertyDelegate> DELEGATED_PROPERTY_IN_TRAIT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetPropertyDelegate> LOCAL_VARIABLE_WITH_DELEGATE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetProperty> PROPERTY_WITH_NO_TYPE_NO_INITIALIZER = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<JetProperty> MUST_BE_INITIALIZED = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<JetProperty> MUST_BE_INITIALIZED_OR_BE_ABSTRACT = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<JetExpression> EXTENSION_PROPERTY_WITH_BACKING_FIELD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetExpression> PROPERTY_INITIALIZER_NO_BACKING_FIELD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetExpression> PROPERTY_INITIALIZER_IN_TRAIT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetProperty> FINAL_PROPERTY_IN_TRAIT = DiagnosticFactory0.create(ERROR, FINAL_MODIFIER);
    DiagnosticFactory0<JetProperty> BACKING_FIELD_IN_TRAIT = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory2<JetModifierListOwner, String, ClassDescriptor> ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS = DiagnosticFactory2.create(ERROR, ABSTRACT_MODIFIER);

    DiagnosticFactory0<JetPropertyAccessor> VAL_WITH_SETTER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetExpression> SETTER_PARAMETER_WITH_DEFAULT_VALUE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<JetTypeReference, JetType, JetType> WRONG_SETTER_PARAMETER_TYPE = DiagnosticFactory2.create(ERROR);

    // Function-specific

    DiagnosticFactory2<JetFunction, String, ClassDescriptor> ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS = DiagnosticFactory2.create(ERROR, ABSTRACT_MODIFIER);

    DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor> ABSTRACT_FUNCTION_WITH_BODY = DiagnosticFactory1.create(ERROR, ABSTRACT_MODIFIER);
    DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor> NON_ABSTRACT_FUNCTION_WITH_NO_BODY =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor> FINAL_FUNCTION_WITH_NO_BODY = DiagnosticFactory1.create(ERROR, FINAL_MODIFIER);

    DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor> NON_MEMBER_FUNCTION_NO_BODY =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory0<JetParameter> VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetNamedFunction> NO_TAIL_CALLS_FOUND = DiagnosticFactory0.create(WARNING, DECLARATION_SIGNATURE);

    // Named parameters

    DiagnosticFactory0<JetParameter> DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE = DiagnosticFactory0.create(ERROR, PARAMETER_DEFAULT_VALUE);

    DiagnosticFactory1<JetParameter, ValueParameterDescriptor> MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<JetClassOrObject, ValueParameterDescriptor> MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE =
            DiagnosticFactory1.create(ERROR, DECLARATION_NAME);

    DiagnosticFactory2<JetParameter, ClassDescriptor, ValueParameterDescriptor> PARAMETER_NAME_CHANGED_ON_OVERRIDE =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);

    DiagnosticFactory2<JetClassOrObject, Collection<? extends CallableMemberDescriptor>, Integer> DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Errors/warnings inside code blocks

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // General

    DiagnosticFactory1<PsiElement, String> NAME_SHADOWING = DiagnosticFactory1.create(WARNING, PositioningStrategies.FOR_REDECLARATION);

    DiagnosticFactory0<JetExpression> TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM = DiagnosticFactory0.create(ERROR);

    // Checking call arguments

    DiagnosticFactory0<PsiElement> MIXING_NAMED_AND_POSITIONED_ARGUMENTS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetReferenceExpression> ARGUMENT_PASSED_TWICE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<JetReferenceExpression, JetReferenceExpression> NAMED_PARAMETER_NOT_FOUND =
            DiagnosticFactory1.create(ERROR, FOR_UNRESOLVED_REFERENCE);
    DiagnosticFactory0<PsiElement> NAMED_ARGUMENTS_NOT_ALLOWED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetExpression> VARARG_OUTSIDE_PARENTHESES = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<LeafPsiElement> NON_VARARG_SPREAD = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetExpression> MANY_FUNCTION_LITERAL_ARGUMENTS = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, CallableDescriptor> TOO_MANY_ARGUMENTS = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<JetElement, ValueParameterDescriptor> NO_VALUE_FOR_PARAMETER = DiagnosticFactory1.create(ERROR, VALUE_ARGUMENTS);

    DiagnosticFactory1<JetExpression, JetType> MISSING_RECEIVER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<JetExpression> NO_RECEIVER_ALLOWED = DiagnosticFactory0.create(ERROR);

    // Call resolution

    DiagnosticFactory1<JetExpression, String> ILLEGAL_SELECTOR = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory2<JetExpression, JetExpression, JetType> FUNCTION_EXPECTED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<JetExpression, JetExpression, Boolean> FUNCTION_CALL_EXPECTED = DiagnosticFactory2.create(ERROR, CALL_EXPRESSION);

    DiagnosticFactory0<PsiElement> NON_TAIL_RECURSIVE_CALL = DiagnosticFactory0.create(WARNING, CALL_EXPRESSION);
    DiagnosticFactory0<PsiElement> TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED = DiagnosticFactory0.create(WARNING, CALL_EXPRESSION);

    DiagnosticFactory0<PsiElement> NO_CONSTRUCTOR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetExpression> NOT_A_CLASS = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> OVERLOAD_RESOLUTION_AMBIGUITY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> NONE_APPLICABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> CANNOT_COMPLETE_RESOLVE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> UNRESOLVED_REFERENCE_WRONG_RECEIVER = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<JetExpression> DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory1<PsiElement, TypeParameterDescriptor> TYPE_PARAMETER_AS_REIFIED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, JetType> REIFIED_TYPE_FORBIDDEN_SUBSTITUTION = DiagnosticFactory1.create(ERROR);

    // Type inference

    DiagnosticFactory0<JetParameter> CANNOT_INFER_PARAMETER_TYPE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_CANNOT_CAPTURE_TYPES = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_UPPER_BOUND_VIOLATED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<JetExpression, JetType, JetType> TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);

    // Reflection

    DiagnosticFactory0<PsiElement> REFLECTION_TYPES_NOT_LOADED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<JetExpression, CallableMemberDescriptor> EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<JetExpression> CALLABLE_REFERENCE_LHS_NOT_A_CLASS = DiagnosticFactory0.create(ERROR);

    // Multi-declarations

    DiagnosticFactory0<JetMultiDeclaration> INITIALIZER_REQUIRED_FOR_MULTIDECLARATION = DiagnosticFactory0.create(ERROR, DEFAULT);
    DiagnosticFactory2<JetExpression, Name, JetType> COMPONENT_FUNCTION_MISSING = DiagnosticFactory2.create(ERROR, DEFAULT);
    DiagnosticFactory2<JetExpression, Name, Collection<? extends ResolvedCall<?>>> COMPONENT_FUNCTION_AMBIGUITY = DiagnosticFactory2.create(ERROR, DEFAULT);
    DiagnosticFactory3<JetExpression, Name, JetType, JetType> COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH = DiagnosticFactory3.create(ERROR, DEFAULT);

    // Super calls

    DiagnosticFactory1<JetSuperExpression, String> SUPER_IS_NOT_AN_EXPRESSION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetSuperExpression, String> SUPER_CANT_BE_EXTENSION_RECEIVER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<JetSuperExpression> SUPER_NOT_AVAILABLE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetSuperExpression> SUPERCLASS_NOT_ACCESSIBLE_FROM_TRAIT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetSuperExpression> AMBIGUOUS_SUPER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetExpression> ABSTRACT_SUPER_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetTypeReference> NOT_A_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER = DiagnosticFactory0.create(WARNING);

    // Conventions

    DiagnosticFactory0<JetArrayAccessExpression> NO_GET_METHOD = DiagnosticFactory0.create(ERROR, ARRAY_ACCESS);
    DiagnosticFactory0<JetArrayAccessExpression> NO_SET_METHOD = DiagnosticFactory0.create(ERROR, ARRAY_ACCESS);

    DiagnosticFactory0<JetSimpleNameExpression> INC_DEC_SHOULD_NOT_RETURN_UNIT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<JetSimpleNameExpression, DeclarationDescriptor, JetSimpleNameExpression> ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>>
            ASSIGN_OPERATOR_AMBIGUITY = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<JetSimpleNameExpression> EQUALS_MISSING = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory3<JetBinaryExpression, JetSimpleNameExpression, JetType, JetType> EQUALITY_NOT_APPLICABLE =
            DiagnosticFactory3.create(ERROR);


    DiagnosticFactory1<JetExpression, JetType> HAS_NEXT_MISSING = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> HAS_NEXT_FUNCTION_AMBIGUITY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> HAS_NEXT_FUNCTION_NONE_APPLICABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> HAS_NEXT_FUNCTION_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<JetExpression, JetType> NEXT_AMBIGUITY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> NEXT_MISSING = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> NEXT_NONE_APPLICABLE = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<JetExpression> ITERATOR_MISSING = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> ITERATOR_AMBIGUITY = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory2<JetExpression, String, JetType> DELEGATE_SPECIAL_FUNCTION_MISSING = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<JetExpression, String, Collection<? extends ResolvedCall<?>>> DELEGATE_SPECIAL_FUNCTION_AMBIGUITY = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<JetExpression, String, Collection<? extends ResolvedCall<?>>> DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory3<JetExpression, String, JetType, JetType> DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory2<JetExpression, String, Collection<? extends ResolvedCall<?>>> DELEGATE_PD_METHOD_NONE_APPLICABLE = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory1<JetSimpleNameExpression, JetType> COMPARE_TO_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);

    // Labels

    DiagnosticFactory0<JetSimpleNameExpression> LABEL_NAME_CLASH = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<JetSimpleNameExpression> AMBIGUOUS_LABEL = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetExpressionWithLabel> BREAK_OR_CONTINUE_OUTSIDE_A_LOOP = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetExpressionWithLabel> BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<JetExpressionWithLabel, String> NOT_A_LOOP_LABEL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetReturnExpression, String> NOT_A_RETURN_LABEL = DiagnosticFactory1.create(ERROR);

    // Control flow / Data flow

    DiagnosticFactory1<JetElement, List<TextRange>> UNREACHABLE_CODE = DiagnosticFactory1.create(
            WARNING, PositioningStrategies.UNREACHABLE_CODE);

    DiagnosticFactory0<JetVariableDeclaration> VARIABLE_WITH_NO_TYPE_NO_INITIALIZER = DiagnosticFactory0.create(ERROR, DECLARATION_NAME);

    DiagnosticFactory1<JetSimpleNameExpression, VariableDescriptor> UNINITIALIZED_VARIABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetSimpleNameExpression, ValueParameterDescriptor> UNINITIALIZED_PARAMETER = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<JetNamedDeclaration, VariableDescriptor> UNUSED_VARIABLE = DiagnosticFactory1.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory1<JetParameter, VariableDescriptor> UNUSED_PARAMETER = DiagnosticFactory1.create(WARNING, DECLARATION_NAME);

    DiagnosticFactory1<JetNamedDeclaration, VariableDescriptor> ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE =
            DiagnosticFactory1.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory1<JetExpression, DeclarationDescriptor> VARIABLE_WITH_REDUNDANT_INITIALIZER = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory2<JetElement, JetElement, DeclarationDescriptor> UNUSED_VALUE = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory1<JetElement, JetElement> UNUSED_CHANGED_VALUE = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<JetElement> UNUSED_EXPRESSION = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<JetFunctionLiteralExpression> UNUSED_FUNCTION_LITERAL = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory1<JetExpression, DeclarationDescriptor> VAL_REASSIGNMENT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, DeclarationDescriptor> SETTER_PROJECTED_OUT = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<JetExpression, DeclarationDescriptor> INITIALIZATION_BEFORE_DECLARATION = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<JetExpression> VARIABLE_EXPECTED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<JetBinaryExpression, JetBinaryExpression, Boolean> SENSELESS_COMPARISON = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory0<JetElement> SENSELESS_NULL_IN_WHEN = DiagnosticFactory0.create(WARNING);

    // Nullability

    DiagnosticFactory1<PsiElement, JetType> UNSAFE_CALL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory3<JetExpression, String, String, String> UNSAFE_INFIX_CALL = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory1<PsiElement, JetType> UNNECESSARY_SAFE_CALL = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<PsiElement, JetType> UNNECESSARY_NOT_NULL_ASSERTION = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory1<JetExpression, JetType> USELESS_ELVIS = DiagnosticFactory1.create(WARNING);

    // Compile-time values

    DiagnosticFactory0<JetExpression> INTEGER_OVERFLOW = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<JetConstantExpression> WRONG_LONG_SUFFIX = DiagnosticFactory0.create(ERROR, LONG_LITERAL_SUFFIX);
    DiagnosticFactory0<JetConstantExpression> INT_LITERAL_OUT_OF_RANGE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetConstantExpression> FLOAT_LITERAL_OUT_OF_RANGE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<JetConstantExpression, String, JetType> CONSTANT_EXPECTED_TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<JetConstantExpression> INCORRECT_CHARACTER_LITERAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetConstantExpression> EMPTY_CHARACTER_LITERAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<JetConstantExpression, JetConstantExpression> TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetElement, JetElement> ILLEGAL_ESCAPE = DiagnosticFactory1.create(ERROR, CUT_CHAR_QUOTES);
    DiagnosticFactory1<JetConstantExpression, JetType> NULL_FOR_NONNULL_TYPE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<JetEscapeStringTemplateEntry> ILLEGAL_ESCAPE_SEQUENCE = DiagnosticFactory0.create(ERROR);

    // Casts and is-checks

    DiagnosticFactory1<JetElement, JetType> CANNOT_CHECK_FOR_ERASED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<JetBinaryExpressionWithTypeRHS, JetType, JetType> UNCHECKED_CAST = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory0<JetBinaryExpressionWithTypeRHS> USELESS_CAST_STATIC_ASSERT_IS_FINE = DiagnosticFactory0.create(WARNING, AS_TYPE);
    DiagnosticFactory0<JetBinaryExpressionWithTypeRHS> USELESS_CAST = DiagnosticFactory0.create(WARNING, AS_TYPE);
    DiagnosticFactory0<JetSimpleNameExpression> CAST_NEVER_SUCCEEDS = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<JetTypeReference> DYNAMIC_NOT_ALLOWED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<JetExpression, JetType> IMPLICIT_CAST_TO_UNIT_OR_ANY = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory2<JetExpression, JetType, String> SMARTCAST_IMPOSSIBLE = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory0<JetNullableType> USELESS_NULLABLE_CHECK = DiagnosticFactory0.create(WARNING, NULLABLE_TYPE);

    // Properties / locals

    DiagnosticFactory1<JetExpression, DeclarationDescriptor> INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, DeclarationDescriptor> INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<JetTypeReference> LOCAL_EXTENSION_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetPropertyAccessor> LOCAL_VARIABLE_WITH_GETTER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetPropertyAccessor> LOCAL_VARIABLE_WITH_SETTER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory3<JetExpression, DeclarationDescriptor, Visibility, DeclarationDescriptor> INVISIBLE_SETTER = DiagnosticFactory3.create(ERROR);

    DiagnosticFactory1<PsiElement, JetKeywordToken> VAL_OR_VAR_ON_LOOP_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, JetKeywordToken> VAL_OR_VAR_ON_FUN_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, JetKeywordToken> VAL_OR_VAR_ON_CATCH_PARAMETER = DiagnosticFactory1.create(ERROR);

    // Backing fields

    DiagnosticFactory0<JetElement> NO_BACKING_FIELD_ABSTRACT_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetElement> NO_BACKING_FIELD_CUSTOM_ACCESSORS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetElement> INACCESSIBLE_BACKING_FIELD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetElement> NOT_PROPERTY_BACKING_FIELD = DiagnosticFactory0.create(ERROR);

    // When expressions

    DiagnosticFactory0<JetWhenCondition> EXPECTED_CONDITION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetWhenEntry> ELSE_MISPLACED_IN_WHEN = DiagnosticFactory0.create(ERROR, ELSE_ENTRY);
    DiagnosticFactory0<JetWhenExpression> NO_ELSE_IN_WHEN = DiagnosticFactory0.create(ERROR, WHEN_EXPRESSION);

    // Type mismatch

    DiagnosticFactory2<JetExpression, JetType, JetType> TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> RETURN_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> EXPECTED_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetBinaryExpression, JetType> ASSIGNMENT_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<JetTypeReference, JetType, JetType> TYPE_MISMATCH_IN_FOR_LOOP = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<JetElement, JetType> TYPE_MISMATCH_IN_CONDITION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory3<JetExpression, String, JetType, JetType> RESULT_TYPE_MISMATCH = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory0<JetWhenConditionInRange> TYPE_MISMATCH_IN_RANGE = DiagnosticFactory0.create(ERROR, WHEN_CONDITION_IN_RANGE);

    DiagnosticFactory1<JetParameter, JetType> EXPECTED_PARAMETER_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetTypeReference, JetType> EXPECTED_RETURN_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<JetFunctionLiteral, Integer, List<JetType>> EXPECTED_PARAMETERS_NUMBER_MISMATCH =
            DiagnosticFactory2.create(ERROR, FUNCTION_LITERAL_PARAMETERS);

    DiagnosticFactory2<JetElement, JetType, JetType> INCOMPATIBLE_TYPES = DiagnosticFactory2.create(ERROR);

    // Context tracking

    DiagnosticFactory1<JetExpression, JetExpression> EXPRESSION_EXPECTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<JetBinaryExpression> ASSIGNMENT_IN_EXPRESSION_CONTEXT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetDeclaration> DECLARATION_IN_ILLEGAL_CONTEXT = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetSimpleNameExpression> EXPRESSION_EXPECTED_PACKAGE_FOUND = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetReturnExpression> RETURN_NOT_ALLOWED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetReturnExpression> RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetDeclarationWithBody>
            NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY = DiagnosticFactory0.create(ERROR, DECLARATION_WITH_BODY);

    DiagnosticFactory0<JetClassInitializer> ANONYMOUS_INITIALIZER_IN_TRAIT = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<JetThisExpression> NO_THIS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<JetRootPackageExpression> PACKAGE_IS_NOT_AN_EXPRESSION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<JetSimpleNameExpression, ClassifierDescriptor> NO_DEFAULT_OBJECT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetSimpleNameExpression, TypeParameterDescriptor> TYPE_PARAMETER_IS_NOT_AN_EXPRESSION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetSimpleNameExpression, TypeParameterDescriptor> TYPE_PARAMETER_ON_LHS_OF_DOT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, ClassDescriptor> NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<JetExpression, ClassDescriptor, String> NESTED_CLASS_SHOULD_BE_QUALIFIED = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<PsiElement, ClassDescriptor> INACCESSIBLE_OUTER_CLASS_EXPRESSION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<JetClass> NESTED_CLASS_NOT_ALLOWED = DiagnosticFactory0.create(ERROR, DECLARATION_NAME);

    DiagnosticFactory0<JetModifierListOwner> INNER_CLASS_IN_TRAIT = DiagnosticFactory0.create(ERROR, PositioningStrategies.INNER_MODIFIER);
    DiagnosticFactory0<JetModifierListOwner> INNER_CLASS_IN_OBJECT = DiagnosticFactory0.create(ERROR, PositioningStrategies.INNER_MODIFIER);

    //Inline and inlinable parameters
    DiagnosticFactory2<JetElement, DeclarationDescriptor, DeclarationDescriptor> INVISIBLE_MEMBER_FROM_INLINE = DiagnosticFactory2.create(ERROR, CALL_ELEMENT);
    DiagnosticFactory3<JetElement, JetElement, DeclarationDescriptor, DeclarationDescriptor> NON_LOCAL_RETURN_NOT_ALLOWED = DiagnosticFactory3.create(ERROR, CALL_ELEMENT);
    DiagnosticFactory2<JetElement, JetNamedDeclaration, DeclarationDescriptor> NOT_YET_SUPPORTED_IN_INLINE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<JetFunction, DeclarationDescriptor> NOTHING_TO_INLINE = DiagnosticFactory1.create(WARNING, DECLARATION_SIGNATURE);
    DiagnosticFactory2<JetElement, JetExpression, DeclarationDescriptor> USAGE_IS_NOT_INLINABLE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<JetElement, JetElement, DeclarationDescriptor> NULLABLE_INLINE_PARAMETER = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<JetElement, JetElement, DeclarationDescriptor> RECURSION_IN_INLINE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<JetElement> DECLARATION_CANT_BE_INLINED = DiagnosticFactory0.create(ERROR);

    // Error sets
    ImmutableSet<? extends DiagnosticFactory<?>> UNRESOLVED_REFERENCE_DIAGNOSTICS = ImmutableSet.of(
            UNRESOLVED_REFERENCE, NAMED_PARAMETER_NOT_FOUND, UNRESOLVED_REFERENCE_WRONG_RECEIVER);
    ImmutableSet<? extends DiagnosticFactory<?>> INVISIBLE_REFERENCE_DIAGNOSTICS = ImmutableSet.of(
            INVISIBLE_MEMBER, INVISIBLE_MEMBER_FROM_INLINE, INVISIBLE_REFERENCE, INVISIBLE_SETTER);
    ImmutableSet<? extends DiagnosticFactory<?>> UNUSED_ELEMENT_DIAGNOSTICS = ImmutableSet.of(
            UNUSED_VARIABLE, UNUSED_PARAMETER, ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE, VARIABLE_WITH_REDUNDANT_INITIALIZER,
            UNUSED_FUNCTION_LITERAL, USELESS_CAST, USELESS_CAST_STATIC_ASSERT_IS_FINE);
    ImmutableSet<? extends DiagnosticFactory<?>> TYPE_INFERENCE_ERRORS = ImmutableSet.of(
            TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS, TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH,
            TYPE_INFERENCE_UPPER_BOUND_VIOLATED, TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH);

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
