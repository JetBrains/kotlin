/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.diagnostics;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.PositioningStrategies.*;
import static org.jetbrains.jet.lang.diagnostics.Severity.ERROR;
import static org.jetbrains.jet.lang.diagnostics.Severity.WARNING;
import static org.jetbrains.jet.lang.resolve.calls.inference.InferenceErrorData.ExtendedInferenceErrorData;

/**
 * For error messages, see DefaultErrorMessages and IdeErrorMessages.
 */
public interface Errors {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Meta-errors: unsupported features, failure

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    DiagnosticFactory1<JetFile, Throwable> EXCEPTION_WHILE_ANALYZING = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<PsiElement, String> UNSUPPORTED = DiagnosticFactory1.create(ERROR);

    // TODO: Temporary error message: to deprecate tuples we report this error and provide a quick fix
    @Deprecated // Tuples will be dropped in Kotlin M4
    SimpleDiagnosticFactory<PsiElement> TUPLES_ARE_NOT_SUPPORTED = SimpleDiagnosticFactory.create(ERROR);
    @Deprecated // Tuples will be dropped in Kotlin M4
    SimpleDiagnosticFactory<PsiElement> TUPLES_ARE_NOT_SUPPORTED_BIG = SimpleDiagnosticFactory.create(ERROR);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Generic errors/warnings: applicable in many contexts

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    RedeclarationDiagnosticFactory REDECLARATION = new RedeclarationDiagnosticFactory(ERROR);

    UnresolvedReferenceDiagnosticFactory UNRESOLVED_REFERENCE = UnresolvedReferenceDiagnosticFactory.create();

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

    SimpleDiagnosticFactory<JetTypeProjection> PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT = SimpleDiagnosticFactory.create(ERROR, VARIANCE_IN_PROJECTION);
    DiagnosticFactory2<JetTypeReference, JetType, JetType> UPPER_BOUND_VIOLATED = DiagnosticFactory2.create(ERROR);
    SimpleDiagnosticFactory<JetNullableType> REDUNDANT_NULLABLE = SimpleDiagnosticFactory.create(WARNING, NULLABLE_TYPE);
    DiagnosticFactory1<JetNullableType, JetType> BASE_WITH_NULLABLE_UPPER_BOUND = DiagnosticFactory1.create(WARNING, NULLABLE_TYPE);
    DiagnosticFactory1<JetElement, Integer> WRONG_NUMBER_OF_TYPE_ARGUMENTS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<JetUserType, Integer, String> NO_TYPE_ARGUMENTS_ON_RHS_OF_IS_EXPRESSION = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<JetTypeProjection, ClassifierDescriptor> CONFLICTING_PROJECTION = DiagnosticFactory1.create(ERROR, VARIANCE_IN_PROJECTION);
    DiagnosticFactory1<JetTypeProjection, ClassifierDescriptor> REDUNDANT_PROJECTION = DiagnosticFactory1.create(WARNING, VARIANCE_IN_PROJECTION);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Errors in declarations

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Imports

    DiagnosticFactory1<JetSimpleNameExpression, DeclarationDescriptor> CANNOT_IMPORT_FROM_ELEMENT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetSimpleNameExpression, DeclarationDescriptor> CANNOT_BE_IMPORTED = DiagnosticFactory1.create(ERROR);

    SimpleDiagnosticFactory<JetExpression> USELESS_HIDDEN_IMPORT = SimpleDiagnosticFactory.create(WARNING);

    SimpleDiagnosticFactory<JetExpression> USELESS_SIMPLE_IMPORT = SimpleDiagnosticFactory.create(WARNING);

    // Modifiers

    DiagnosticFactory1<PsiElement, Collection<JetKeywordToken>> INCOMPATIBLE_MODIFIERS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, JetKeywordToken> ILLEGAL_MODIFIER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, JetKeywordToken, JetKeywordToken> REDUNDANT_MODIFIER = DiagnosticFactory2.create(Severity.WARNING);

    // Annotations

    DiagnosticFactory1<JetAnnotationEntry, String> NOT_AN_ANNOTATION_CLASS = DiagnosticFactory1.create(ERROR);
    SimpleDiagnosticFactory<PsiElement> ANNOTATION_CLASS_WITH_BODY = SimpleDiagnosticFactory.create(ERROR);

    // Classes and traits

    SimpleDiagnosticFactory<JetTypeProjection> PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE =
            SimpleDiagnosticFactory.create(ERROR, VARIANCE_IN_PROJECTION);

    SimpleDiagnosticFactory<PsiElement> CYCLIC_INHERITANCE_HIERARCHY = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetDelegatorToSuperClass> SUPERTYPE_NOT_INITIALIZED = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetDelegatorToSuperClass> SUPERTYPE_NOT_INITIALIZED_DEFAULT = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetTypeReference> DELEGATION_NOT_TO_TRAIT = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetTypeReference> SUPERTYPE_NOT_A_CLASS_OR_TRAIT = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<PsiElement> NO_GENERICS_IN_SUPERTYPE_SPECIFIER = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetTypeReference> MANY_CLASSES_IN_SUPERTYPE_LIST = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetTypeReference> SUPERTYPE_APPEARS_TWICE = SimpleDiagnosticFactory.create(ERROR);
    DiagnosticFactory3<JetDelegationSpecifierList, TypeParameterDescriptor, ClassDescriptor, Collection<JetType>>
            INCONSISTENT_TYPE_PARAMETER_VALUES = DiagnosticFactory3.create(ERROR);


    SimpleDiagnosticFactory<JetTypeReference> FINAL_SUPERTYPE = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetNullableType> NULLABLE_SUPERTYPE = SimpleDiagnosticFactory.create(ERROR, NULLABLE_TYPE);

    // Trait-specific

    SimpleDiagnosticFactory<JetModifierListOwner> ABSTRACT_MODIFIER_IN_TRAIT = SimpleDiagnosticFactory
            .create(WARNING, ABSTRACT_MODIFIER);
    SimpleDiagnosticFactory<JetModifierListOwner> OPEN_MODIFIER_IN_TRAIT = SimpleDiagnosticFactory
            .create(WARNING, modifierSetPosition(JetTokens.OPEN_KEYWORD));
    SimpleDiagnosticFactory<PsiElement> TRAIT_CAN_NOT_BE_FINAL = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<PsiElement> CONSTRUCTOR_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<PsiElement> SUPERTYPE_INITIALIZED_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetDelegatorByExpressionSpecifier> DELEGATION_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR);

    // Enum-specific

    SimpleDiagnosticFactory<JetModifierListOwner> ILLEGAL_ENUM_ANNOTATION = SimpleDiagnosticFactory
            .create(ERROR, modifierSetPosition(JetTokens.ENUM_KEYWORD));

    SimpleDiagnosticFactory<JetModifierListOwner> OPEN_MODIFIER_IN_ENUM = SimpleDiagnosticFactory
            .create(ERROR, modifierSetPosition(JetTokens.OPEN_KEYWORD));

    SimpleDiagnosticFactory<PsiElement> CLASS_IN_SUPERTYPE_FOR_ENUM = SimpleDiagnosticFactory.create(ERROR);

    DiagnosticFactory1<JetClass, ClassDescriptor> ENUM_ENTRY_SHOULD_BE_INITIALIZED = DiagnosticFactory1.create(ERROR, NAME_IDENTIFIER);
    DiagnosticFactory1<JetTypeReference, ClassDescriptor> ENUM_ENTRY_ILLEGAL_TYPE = DiagnosticFactory1.create(ERROR);

    // Class objects

    SimpleDiagnosticFactory<JetClassObject> MANY_CLASS_OBJECTS = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetClassObject> CLASS_OBJECT_NOT_ALLOWED = SimpleDiagnosticFactory.create(ERROR);

    // Type parameter declarations

    DiagnosticFactory1<JetTypeReference, JetType> FINAL_UPPER_BOUND = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<JetTypeReference, JetType> FINAL_CLASS_OBJECT_UPPER_BOUND = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<PsiElement, TypeParameterDescriptor> CONFLICTING_UPPER_BOUNDS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, TypeParameterDescriptor> CONFLICTING_CLASS_OBJECT_UPPER_BOUNDS = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory2<JetSimpleNameExpression, JetTypeConstraint, JetTypeParameterListOwner> NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER =
            DiagnosticFactory2.create(ERROR);

    SimpleDiagnosticFactory<JetTypeParameter> VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY = SimpleDiagnosticFactory.create(ERROR, VARIANCE_MODIFIER);

    // Members

    SimpleDiagnosticFactory<PsiElement> PACKAGE_MEMBER_CANNOT_BE_PROTECTED = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetNamedDeclaration> PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE = SimpleDiagnosticFactory.create(ERROR, NAMED_ELEMENT);

    DiagnosticFactory2<JetDeclaration, CallableMemberDescriptor, String> CONFLICTING_OVERLOADS = DiagnosticFactory2.create(ERROR, DECLARATION);

    SimpleDiagnosticFactory<JetNamedDeclaration> NON_FINAL_MEMBER_IN_FINAL_CLASS = SimpleDiagnosticFactory.create(WARNING, modifierSetPosition(
            JetTokens.OPEN_KEYWORD));

    DiagnosticFactory1<JetModifierListOwner, CallableMemberDescriptor> NOTHING_TO_OVERRIDE = DiagnosticFactory1.create(ERROR, OVERRIDE_MODIFIER);

    DiagnosticFactory3<PsiNameIdentifierOwner, CallableMemberDescriptor, CallableMemberDescriptor, DeclarationDescriptor> VIRTUAL_MEMBER_HIDDEN =
            DiagnosticFactory3.create(ERROR, NAMED_ELEMENT);

    DiagnosticFactory3<JetModifierListOwner, CallableMemberDescriptor, CallableDescriptor, DeclarationDescriptor> CANNOT_OVERRIDE_INVISIBLE_MEMBER =
            DiagnosticFactory3.create(ERROR, OVERRIDE_MODIFIER);

    DiagnosticFactory2<JetAnnotationEntry, CallableMemberDescriptor, DeclarationDescriptor> DATA_CLASS_OVERRIDE_CONFLICT =
            DiagnosticFactory2.create(ERROR);

    SimpleDiagnosticFactory<JetDeclaration> CANNOT_INFER_VISIBILITY = SimpleDiagnosticFactory.create(ERROR, DECLARATION);

    DiagnosticFactory2<PsiElement, CallableMemberDescriptor, DeclarationDescriptor> OVERRIDING_FINAL_MEMBER = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory3<JetModifierListOwner, Visibility, CallableMemberDescriptor, DeclarationDescriptor> CANNOT_WEAKEN_ACCESS_PRIVILEGE =
            DiagnosticFactory3.create(ERROR, VISIBILITY_MODIFIER);
    DiagnosticFactory3<JetModifierListOwner, Visibility, CallableMemberDescriptor, DeclarationDescriptor> CANNOT_CHANGE_ACCESS_PRIVILEGE =
            DiagnosticFactory3.create(ERROR, VISIBILITY_MODIFIER);
    DiagnosticFactory2<JetNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor> RETURN_TYPE_MISMATCH_ON_OVERRIDE =
            DiagnosticFactory2.create(ERROR, DECLARATION_RETURN_TYPE);
    DiagnosticFactory2<JetNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor> PROPERTY_TYPE_MISMATCH_ON_OVERRIDE =
            DiagnosticFactory2.create(ERROR, DECLARATION_RETURN_TYPE);

    DiagnosticFactory2<PsiElement, JetClassOrObject, CallableMemberDescriptor> ABSTRACT_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, JetClassOrObject, CallableMemberDescriptor> MANY_IMPL_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<JetNamedDeclaration, Collection<JetType>> AMBIGUOUS_ANONYMOUS_TYPE_INFERRED = DiagnosticFactory1.create(ERROR, NAMED_ELEMENT);

    // Property-specific

    DiagnosticFactory2<JetProperty, PropertyDescriptor, PropertyDescriptor> VAR_OVERRIDDEN_BY_VAL =
            DiagnosticFactory2.create(ERROR, VAL_OR_VAR_NODE);

    SimpleDiagnosticFactory<PsiElement> REDUNDANT_MODIFIER_IN_GETTER = SimpleDiagnosticFactory.create(WARNING);
    SimpleDiagnosticFactory<PsiElement> GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY = SimpleDiagnosticFactory.create(ERROR);
    DiagnosticFactory2<JetTypeReference, JetType, JetType> WRONG_GETTER_RETURN_TYPE = DiagnosticFactory2.create(ERROR);

    SimpleDiagnosticFactory<JetModifierListOwner> ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS = SimpleDiagnosticFactory.create(ERROR, ABSTRACT_MODIFIER);
    SimpleDiagnosticFactory<JetExpression> ABSTRACT_PROPERTY_WITH_INITIALIZER = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetPropertyAccessor> ABSTRACT_PROPERTY_WITH_GETTER = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetPropertyAccessor>ABSTRACT_PROPERTY_WITH_SETTER = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetProperty> PROPERTY_WITH_NO_TYPE_NO_INITIALIZER = SimpleDiagnosticFactory.create(ERROR, NAMED_ELEMENT);

    SimpleDiagnosticFactory<JetProperty> MUST_BE_INITIALIZED = SimpleDiagnosticFactory.create(ERROR, NAMED_ELEMENT);
    SimpleDiagnosticFactory<JetProperty> MUST_BE_INITIALIZED_OR_BE_ABSTRACT = SimpleDiagnosticFactory.create(ERROR, NAMED_ELEMENT);

    SimpleDiagnosticFactory<JetExpression> PROPERTY_INITIALIZER_NO_BACKING_FIELD = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> PROPERTY_INITIALIZER_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetProperty> FINAL_PROPERTY_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR, FINAL_MODIFIER);
    SimpleDiagnosticFactory<JetProperty> BACKING_FIELD_IN_TRAIT = SimpleDiagnosticFactory.create(ERROR, NAMED_ELEMENT);

    DiagnosticFactory2<JetModifierListOwner, String, ClassDescriptor> ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS = DiagnosticFactory2.create(ERROR, ABSTRACT_MODIFIER);

    SimpleDiagnosticFactory<JetPropertyAccessor> VAL_WITH_SETTER = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetExpression> SETTER_PARAMETER_WITH_DEFAULT_VALUE = SimpleDiagnosticFactory.create(ERROR);

    DiagnosticFactory2<JetTypeReference, JetType, JetType> WRONG_SETTER_PARAMETER_TYPE = DiagnosticFactory2.create(ERROR);

    // Function-specific

    DiagnosticFactory2<JetFunction, String, ClassDescriptor> ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS = DiagnosticFactory2.create(ERROR, ABSTRACT_MODIFIER);

    DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor> ABSTRACT_FUNCTION_WITH_BODY = DiagnosticFactory1.create(ERROR, ABSTRACT_MODIFIER);
    DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor> NON_ABSTRACT_FUNCTION_WITH_NO_BODY = DiagnosticFactory1.create(ERROR, NAMED_ELEMENT);
    DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor> FINAL_FUNCTION_WITH_NO_BODY = DiagnosticFactory1.create(ERROR, FINAL_MODIFIER);

    DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor> NON_MEMBER_FUNCTION_NO_BODY = DiagnosticFactory1.create(ERROR, NAMED_ELEMENT);

    SimpleDiagnosticFactory<JetParameter> VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION = SimpleDiagnosticFactory.create(ERROR);

    // Named parameters

    SimpleDiagnosticFactory<JetParameter> DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE = SimpleDiagnosticFactory.create(ERROR, PARAMETER_DEFAULT_VALUE);

    DiagnosticFactory1<JetParameter, ValueParameterDescriptor> MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<JetClassOrObject, ValueParameterDescriptor> MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE =
            DiagnosticFactory1.create(ERROR, NAME_IDENTIFIER);

    DiagnosticFactory2<JetParameter, ClassDescriptor, ValueParameterDescriptor> PARAMETER_NAME_CHANGED_ON_OVERRIDE =
            DiagnosticFactory2.create(WARNING, NAME_IDENTIFIER);

    DiagnosticFactory2<JetClassOrObject, Collection<? extends CallableMemberDescriptor>, Integer> DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES =
            DiagnosticFactory2.create(WARNING, NAME_IDENTIFIER);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Errors/warnings inside code blocks

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // General

    RedeclarationDiagnosticFactory NAME_SHADOWING = new RedeclarationDiagnosticFactory(WARNING);

    SimpleDiagnosticFactory<JetExpression> TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM = SimpleDiagnosticFactory.create(ERROR);

    // Checking call arguments

    SimpleDiagnosticFactory<PsiElement> MIXING_NAMED_AND_POSITIONED_ARGUMENTS = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetReferenceExpression> ARGUMENT_PASSED_TWICE = SimpleDiagnosticFactory.create(ERROR);
    UnresolvedReferenceDiagnosticFactory NAMED_PARAMETER_NOT_FOUND = UnresolvedReferenceDiagnosticFactory.create();

    SimpleDiagnosticFactory<JetExpression> VARARG_OUTSIDE_PARENTHESES = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<LeafPsiElement> NON_VARARG_SPREAD = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetExpression> MANY_FUNCTION_LITERAL_ARGUMENTS = SimpleDiagnosticFactory.create(ERROR);

    DiagnosticFactory1<PsiElement, CallableDescriptor> TOO_MANY_ARGUMENTS = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<JetElement, ValueParameterDescriptor> NO_VALUE_FOR_PARAMETER = DiagnosticFactory1.create(ERROR, VALUE_ARGUMENTS);

    DiagnosticFactory1<JetReferenceExpression, JetType> MISSING_RECEIVER = DiagnosticFactory1.create(ERROR);
    SimpleDiagnosticFactory<JetReferenceExpression> NO_RECEIVER_ADMITTED = SimpleDiagnosticFactory.create(ERROR);

    // Call resolution

    DiagnosticFactory1<JetExpression, String> ILLEGAL_SELECTOR = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<JetExpression, JetType> CALLEE_NOT_A_FUNCTION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<JetReferenceExpression, JetExpression, JetType> FUNCTION_EXPECTED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<JetExpression, JetExpression, Boolean> FUNCTION_CALL_EXPECTED = DiagnosticFactory2.create(ERROR, CALL_EXPRESSION);

    SimpleDiagnosticFactory<PsiElement> NO_CONSTRUCTOR = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<PsiElement> CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> NOT_A_CLASS = SimpleDiagnosticFactory.create(ERROR);

    AmbiguousDescriptorDiagnosticFactory OVERLOAD_RESOLUTION_AMBIGUITY = new AmbiguousDescriptorDiagnosticFactory();
    AmbiguousDescriptorDiagnosticFactory NONE_APPLICABLE = new AmbiguousDescriptorDiagnosticFactory();
    AmbiguousDescriptorDiagnosticFactory CANNOT_COMPLETE_RESOLVE = new AmbiguousDescriptorDiagnosticFactory();

    SimpleDiagnosticFactory<JetExpression> DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED = SimpleDiagnosticFactory.create(WARNING);

    // Type inference

    SimpleDiagnosticFactory<JetParameter> CANNOT_INFER_PARAMETER_TYPE = SimpleDiagnosticFactory.create(ERROR);

    DiagnosticFactory1<PsiElement, ExtendedInferenceErrorData> TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, ExtendedInferenceErrorData> TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, ExtendedInferenceErrorData> TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_UPPER_BOUND_VIOLATED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, JetType, JetType> TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);

    Collection<AbstractDiagnosticFactory> TYPE_INFERENCE_ERRORS = Lists.<AbstractDiagnosticFactory>newArrayList(TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER,
        TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS, TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH, TYPE_INFERENCE_UPPER_BOUND_VIOLATED,
        TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH);

    // Multi-declarations

    SimpleDiagnosticFactory<JetMultiDeclaration> INITIALIZER_REQUIRED_FOR_MULTIDECLARATION = SimpleDiagnosticFactory.create(ERROR, DEFAULT);
    DiagnosticFactory1<JetExpression, Name> COMPONENT_FUNCTION_MISSING = DiagnosticFactory1.create(ERROR, DEFAULT);
    DiagnosticFactory2<JetExpression, Name, Collection<? extends ResolvedCall<? extends CallableDescriptor>>> COMPONENT_FUNCTION_AMBIGUITY = DiagnosticFactory2.create(ERROR, DEFAULT);
    DiagnosticFactory3<JetExpression, Name, JetType, JetType> COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH = DiagnosticFactory3.create(ERROR, DEFAULT);

    // Super calls

    DiagnosticFactory1<JetSuperExpression, String> SUPER_IS_NOT_AN_EXPRESSION = DiagnosticFactory1.create(ERROR);
    SimpleDiagnosticFactory<JetSuperExpression> SUPER_NOT_AVAILABLE = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetSuperExpression> AMBIGUOUS_SUPER = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetExpression> ABSTRACT_SUPER_CALL = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetTypeReference> NOT_A_SUPERTYPE = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<PsiElement> TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER = SimpleDiagnosticFactory.create(WARNING);

    // Conventions

    SimpleDiagnosticFactory<JetArrayAccessExpression> NO_GET_METHOD = SimpleDiagnosticFactory.create(ERROR, ARRAY_ACCESS);
    SimpleDiagnosticFactory<JetArrayAccessExpression> NO_SET_METHOD = SimpleDiagnosticFactory.create(ERROR, ARRAY_ACCESS);

    SimpleDiagnosticFactory<JetSimpleNameExpression> INC_DEC_SHOULD_NOT_RETURN_UNIT = SimpleDiagnosticFactory.create(ERROR);
    DiagnosticFactory2<JetSimpleNameExpression, DeclarationDescriptor, JetSimpleNameExpression> ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT =
            DiagnosticFactory2.create(ERROR);
    AmbiguousDescriptorDiagnosticFactory ASSIGN_OPERATOR_AMBIGUITY = AmbiguousDescriptorDiagnosticFactory.create();

    SimpleDiagnosticFactory<JetSimpleNameExpression> EQUALS_MISSING = SimpleDiagnosticFactory.create(ERROR);
    DiagnosticFactory3<JetBinaryExpression, JetSimpleNameExpression, JetType, JetType> EQUALITY_NOT_APPLICABLE =
            DiagnosticFactory3.create(ERROR);


    DiagnosticFactory1<JetExpression, JetType> HAS_NEXT_MISSING = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> HAS_NEXT_FUNCTION_AMBIGUITY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> HAS_NEXT_FUNCTION_NONE_APPLICABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> HAS_NEXT_FUNCTION_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<JetExpression, JetType> NEXT_AMBIGUITY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> NEXT_MISSING = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> NEXT_NONE_APPLICABLE = DiagnosticFactory1.create(ERROR);

    SimpleDiagnosticFactory<JetExpression> ITERATOR_MISSING = SimpleDiagnosticFactory.create(ERROR);
    AmbiguousDescriptorDiagnosticFactory ITERATOR_AMBIGUITY = AmbiguousDescriptorDiagnosticFactory.create();

    DiagnosticFactory1<JetSimpleNameExpression, JetType> COMPARE_TO_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);

    // Labels

    SimpleDiagnosticFactory<JetSimpleNameExpression>LABEL_NAME_CLASH = SimpleDiagnosticFactory.create(WARNING);
    SimpleDiagnosticFactory<JetSimpleNameExpression> AMBIGUOUS_LABEL = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetLabelQualifiedExpression> BREAK_OR_CONTINUE_OUTSIDE_A_LOOP = SimpleDiagnosticFactory.create(ERROR);
    DiagnosticFactory1<JetLabelQualifiedExpression, String> NOT_A_LOOP_LABEL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetReturnExpression, String> NOT_A_RETURN_LABEL = DiagnosticFactory1.create(ERROR);

    // Control flow / Data flow

    SimpleDiagnosticFactory<JetElement> UNREACHABLE_CODE = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetVariableDeclaration> VARIABLE_WITH_NO_TYPE_NO_INITIALIZER = SimpleDiagnosticFactory.create(ERROR, NAME_IDENTIFIER);

    DiagnosticFactory1<JetSimpleNameExpression, VariableDescriptor> UNINITIALIZED_VARIABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetSimpleNameExpression, ValueParameterDescriptor> UNINITIALIZED_PARAMETER = DiagnosticFactory1.create(ERROR);

    UnusedElementDiagnosticFactory<JetNamedDeclaration, VariableDescriptor> UNUSED_VARIABLE = UnusedElementDiagnosticFactory.create(WARNING, NAME_IDENTIFIER);
    UnusedElementDiagnosticFactory<JetParameter, VariableDescriptor> UNUSED_PARAMETER = UnusedElementDiagnosticFactory.create(WARNING, NAME_IDENTIFIER);

    UnusedElementDiagnosticFactory<JetNamedDeclaration, DeclarationDescriptor> ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE = UnusedElementDiagnosticFactory.create(WARNING, NAME_IDENTIFIER);
    DiagnosticFactory1<JetExpression, DeclarationDescriptor> VARIABLE_WITH_REDUNDANT_INITIALIZER = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory2<JetElement, JetElement, DeclarationDescriptor> UNUSED_VALUE = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory1<JetElement, JetElement> UNUSED_CHANGED_VALUE = DiagnosticFactory1.create(WARNING);
    SimpleDiagnosticFactory<JetElement> UNUSED_EXPRESSION = SimpleDiagnosticFactory.create(WARNING);
    SimpleDiagnosticFactory<JetFunctionLiteralExpression> UNUSED_FUNCTION_LITERAL = SimpleDiagnosticFactory.create(WARNING);

    DiagnosticFactory1<JetExpression, DeclarationDescriptor> VAL_REASSIGNMENT = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<JetExpression, DeclarationDescriptor> INITIALIZATION_BEFORE_DECLARATION = DiagnosticFactory1.create(ERROR);

    SimpleDiagnosticFactory<JetExpression> VARIABLE_EXPECTED = SimpleDiagnosticFactory.create(ERROR);

    DiagnosticFactory2<JetBinaryExpression, JetBinaryExpression, Boolean> SENSELESS_COMPARISON = DiagnosticFactory2.create(WARNING);

    SimpleDiagnosticFactory<JetElement> SENSELESS_NULL_IN_WHEN = SimpleDiagnosticFactory.create(WARNING);

    // Nullability

    DiagnosticFactory1<PsiElement, JetType> UNSAFE_CALL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory3<JetReferenceExpression, String, String, String> UNSAFE_INFIX_CALL = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory1<PsiElement, JetType> UNNECESSARY_SAFE_CALL = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<PsiElement, JetType> UNNECESSARY_NOT_NULL_ASSERTION = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory1<JetExpression, JetType> USELESS_ELVIS = DiagnosticFactory1.create(WARNING);

    // Compile-time values

    DiagnosticFactory1<PsiElement, String> ERROR_COMPILE_TIME_VALUE = DiagnosticFactory1.create(ERROR);
    SimpleDiagnosticFactory<JetEscapeStringTemplateEntry> ILLEGAL_ESCAPE_SEQUENCE = SimpleDiagnosticFactory.create(ERROR);

    // Casts and is-checks

    DiagnosticFactory1<JetElement, JetType> CANNOT_CHECK_FOR_ERASED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<JetBinaryExpressionWithTypeRHS, JetType, JetType> UNCHECKED_CAST = DiagnosticFactory2.create(WARNING);

    SimpleDiagnosticFactory<JetSimpleNameExpression> USELESS_CAST_STATIC_ASSERT_IS_FINE = SimpleDiagnosticFactory.create(WARNING);
    SimpleDiagnosticFactory<JetSimpleNameExpression> USELESS_CAST = SimpleDiagnosticFactory.create(WARNING);
    SimpleDiagnosticFactory<JetSimpleNameExpression> CAST_NEVER_SUCCEEDS = SimpleDiagnosticFactory.create(WARNING);

    DiagnosticFactory1<JetExpression, JetType> IMPLICIT_CAST_TO_UNIT_OR_ANY = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory2<JetExpression, JetType, String> AUTOCAST_IMPOSSIBLE = DiagnosticFactory2.create(ERROR);

    SimpleDiagnosticFactory<JetNullableType> USELESS_NULLABLE_CHECK = SimpleDiagnosticFactory.create(WARNING, NULLABLE_TYPE);

    // Properties / locals

    DiagnosticFactory1<JetExpression, DeclarationDescriptor> INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, DeclarationDescriptor> INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER = DiagnosticFactory1.create(ERROR);

    SimpleDiagnosticFactory<JetTypeReference> LOCAL_EXTENSION_PROPERTY = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetPropertyAccessor> LOCAL_VARIABLE_WITH_GETTER = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetPropertyAccessor> LOCAL_VARIABLE_WITH_SETTER = SimpleDiagnosticFactory.create(ERROR);

    DiagnosticFactory3<JetExpression, DeclarationDescriptor, Visibility, DeclarationDescriptor> INVISIBLE_SETTER = DiagnosticFactory3.create(ERROR);

    DiagnosticFactory1<PsiElement, JetKeywordToken> VAL_OR_VAR_ON_LOOP_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, JetKeywordToken> VAL_OR_VAR_ON_FUN_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, JetKeywordToken> VAL_OR_VAR_ON_CATCH_PARAMETER = DiagnosticFactory1.create(ERROR);

    // Backing fields

    SimpleDiagnosticFactory<JetElement> NO_BACKING_FIELD_ABSTRACT_PROPERTY = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetElement> NO_BACKING_FIELD_CUSTOM_ACCESSORS = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetElement> INACCESSIBLE_BACKING_FIELD = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetElement> NOT_PROPERTY_BACKING_FIELD = SimpleDiagnosticFactory.create(ERROR);

    // When expressions

    SimpleDiagnosticFactory<JetWhenCondition> EXPECTED_CONDITION = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetWhenEntry> ELSE_MISPLACED_IN_WHEN = SimpleDiagnosticFactory.create(ERROR, ELSE_ENTRY);
    SimpleDiagnosticFactory<JetWhenExpression> NO_ELSE_IN_WHEN = new SimpleDiagnosticFactory<JetWhenExpression>(ERROR, WHEN_EXPRESSION);

    // Type mismatch

    DiagnosticFactory2<PsiElement, JetType, JetType> TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> RETURN_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetExpression, JetType> EXPECTED_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetBinaryExpression, JetType> ASSIGNMENT_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<JetTypeReference, JetType, JetType> TYPE_MISMATCH_IN_FOR_LOOP = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<JetElement, JetType> TYPE_MISMATCH_IN_CONDITION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory3<JetExpression, String, JetType, JetType> RESULT_TYPE_MISMATCH = DiagnosticFactory3.create(ERROR);
    SimpleDiagnosticFactory<JetWhenConditionInRange> TYPE_MISMATCH_IN_RANGE = new SimpleDiagnosticFactory<JetWhenConditionInRange>(ERROR, WHEN_CONDITION_IN_RANGE);

    DiagnosticFactory1<JetParameter, JetType> EXPECTED_PARAMETER_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<JetTypeReference, JetType> EXPECTED_RETURN_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<JetFunctionLiteral, Integer, List<JetType>> EXPECTED_PARAMETERS_NUMBER_MISMATCH = DiagnosticFactory2.create(ERROR, FUNCTION_LITERAL_PARAMETERS);

    DiagnosticFactory2<JetElement, JetType, JetType> INCOMPATIBLE_TYPES = DiagnosticFactory2.create(ERROR);

    // Context tracking

    DiagnosticFactory1<JetExpression, JetExpression> EXPRESSION_EXPECTED = DiagnosticFactory1.create(ERROR);
    SimpleDiagnosticFactory<JetBinaryExpression> ASSIGNMENT_IN_EXPRESSION_CONTEXT = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetDeclaration> DECLARATION_IN_ILLEGAL_CONTEXT = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetSimpleNameExpression> EXPRESSION_EXPECTED_NAMESPACE_FOUND = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetReturnExpression> RETURN_NOT_ALLOWED = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetReturnExpression> RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetDeclarationWithBody> NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY = SimpleDiagnosticFactory.create(ERROR, DECLARATION_WITH_BODY);

    SimpleDiagnosticFactory<JetClassInitializer> ANONYMOUS_INITIALIZER_WITHOUT_CONSTRUCTOR = SimpleDiagnosticFactory.create(ERROR);

    SimpleDiagnosticFactory<JetThisExpression> NO_THIS = SimpleDiagnosticFactory.create(ERROR);
    SimpleDiagnosticFactory<JetRootNamespaceExpression> NAMESPACE_IS_NOT_AN_EXPRESSION = SimpleDiagnosticFactory.create(ERROR);
    DiagnosticFactory1<JetSimpleNameExpression, ClassifierDescriptor> NO_CLASS_OBJECT = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<PsiElement, ClassDescriptor> INACCESSIBLE_OUTER_CLASS_EXPRESSION = DiagnosticFactory1.create(ERROR);
    SimpleDiagnosticFactory<PsiElement> NESTED_CLASS_NOT_ALLOWED = SimpleDiagnosticFactory.create(ERROR);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // This field is needed to make the Initializer class load (interfaces cannot have static initializers)

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("UnusedDeclaration")
    Initializer __initializer = Initializer.INSTANCE;

    class Initializer {
        static {
            for (Field field : Errors.class.getFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    try {
                        Object value = field.get(null);
                        if (value instanceof AbstractDiagnosticFactory) {
                            AbstractDiagnosticFactory factory = (AbstractDiagnosticFactory)value;
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
