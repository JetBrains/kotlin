/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypeElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirEffectiveVisibility
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

object FirErrors {
    // Miscellaneous
    val SYNTAX by error0<FirSourceElement, PsiElement>()
    val OTHER_ERROR by error0<FirSourceElement, PsiElement>()

    // General syntax
    val ILLEGAL_CONST_EXPRESSION by error0<FirSourceElement, PsiElement>()
    val ILLEGAL_UNDERSCORE by error0<FirSourceElement, PsiElement>()
    val EXPRESSION_REQUIRED by error0<FirSourceElement, PsiElement>()
    val BREAK_OR_CONTINUE_OUTSIDE_A_LOOP by error0<FirSourceElement, PsiElement>()
    val NOT_A_LOOP_LABEL by error0<FirSourceElement, PsiElement>()
    val VARIABLE_EXPECTED by error0<FirSourceElement, PsiElement>()
    val RETURN_NOT_ALLOWED by error0<FirSourceElement, PsiElement>()
    val DELEGATION_IN_INTERFACE by error0<FirSourceElement, PsiElement>()

    // Unresolved
    val HIDDEN by error1<FirSourceElement, PsiElement, AbstractFirBasedSymbol<*>>()
    val UNRESOLVED_REFERENCE by error1<FirSourceElement, PsiElement, String>()
    val UNRESOLVED_LABEL by error0<FirSourceElement, PsiElement>()
    val DESERIALIZATION_ERROR by error0<FirSourceElement, PsiElement>()
    val ERROR_FROM_JAVA_RESOLUTION by error0<FirSourceElement, PsiElement>()
    val UNKNOWN_CALLABLE_KIND by error0<FirSourceElement, PsiElement>()
    val MISSING_STDLIB_CLASS by error0<FirSourceElement, PsiElement>()
    val NO_THIS by error0<FirSourceElement, PsiElement>()

    // Super
    val SUPER_IS_NOT_AN_EXPRESSION by error0<FirSourceElement, PsiElement>()
    val SUPER_NOT_AVAILABLE by error0<FirSourceElement, PsiElement>()
    val ABSTRACT_SUPER_CALL by error0<FirSourceElement, PsiElement>()
    val INSTANCE_ACCESS_BEFORE_SUPER_CALL by error1<FirSourceElement, PsiElement, String>()

    // Supertypes
    val TYPE_PARAMETER_AS_SUPERTYPE by error0<FirSourceElement, PsiElement>()
    val ENUM_AS_SUPERTYPE by error0<FirSourceElement, PsiElement>()
    val RECURSION_IN_SUPERTYPES by error0<FirSourceElement, PsiElement>()
    val NOT_A_SUPERTYPE by error0<FirSourceElement, PsiElement>()
    val SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE by error0<FirSourceElement, PsiElement>()
    val QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE by error1<FirSourceElement, PsiElement, FirClass<*>>()
    val SUPERTYPE_INITIALIZED_IN_INTERFACE by error0<FirSourceElement, PsiElement>()
    val INTERFACE_WITH_SUPERCLASS by error0<FirSourceElement, PsiElement>()
    val CLASS_IN_SUPERTYPE_FOR_ENUM by error0<FirSourceElement, PsiElement>()
    val SEALED_SUPERTYPE by error0<FirSourceElement, PsiElement>()
    val SEALED_SUPERTYPE_IN_LOCAL_CLASS by error0<FirSourceElement, PsiElement>()

    // Constructor problems
    val CONSTRUCTOR_IN_OBJECT by existing0<FirSourceElement, KtDeclaration>()
    val CONSTRUCTOR_IN_INTERFACE by existing0<FirSourceElement, KtDeclaration>()
    val NON_PRIVATE_CONSTRUCTOR_IN_ENUM by existing0<FirSourceElement, PsiElement>()
    val NON_PRIVATE_CONSTRUCTOR_IN_SEALED by existing0<FirSourceElement, PsiElement>()
    val CYCLIC_CONSTRUCTOR_DELEGATION_CALL by warning0<FirSourceElement, PsiElement>()
    val PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED by warning0<FirSourceElement, PsiElement>()
    val SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR by warning0<FirSourceElement, PsiElement>()
    val DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR by warning0<FirSourceElement, PsiElement>()
    val PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS by warning0<FirSourceElement, PsiElement>()
    val EXPLICIT_DELEGATION_CALL_REQUIRED by warning0<FirSourceElement, PsiElement>()
    val SEALED_CLASS_CONSTRUCTOR_CALL by error0<FirSourceElement, PsiElement>()

    // Annotations
    val ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR by existing0<FirSourceElement, KtExpression>()
    val ANNOTATION_ARGUMENT_MUST_BE_CONST by existing0<FirSourceElement, KtExpression>()
    val ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST by existing0<FirSourceElement, KtExpression>()
    val ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL by existing0<FirSourceElement, KtExpression>()
    val ANNOTATION_CLASS_MEMBER by existing0<FirSourceElement, PsiElement>()
    val ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT by existing0<FirSourceElement, KtExpression>()
    val INVALID_TYPE_OF_ANNOTATION_MEMBER by existing0<FirSourceElement, KtTypeReference>()
    val LOCAL_ANNOTATION_CLASS_ERROR by existing0<FirSourceElement, KtClassOrObject>()
    val MISSING_VAL_ON_ANNOTATION_PARAMETER by existing0<FirSourceElement, KtParameter>()
    val NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION by existing0<FirSourceElement, KtExpression>()
    val NOT_AN_ANNOTATION_CLASS by error1<FirSourceElement, PsiElement, String>()
    val NULLABLE_TYPE_OF_ANNOTATION_MEMBER by existing0<FirSourceElement, KtTypeReference>()
    val VAR_ANNOTATION_PARAMETER by existing0<FirSourceElement, KtParameter>()

    // Exposed visibility group
    val EXPOSED_TYPEALIAS_EXPANDED_TYPE by error3<FirSourceElement, PsiElement, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()
    val EXPOSED_FUNCTION_RETURN_TYPE by error3<FirSourceElement, PsiElement, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()
    val EXPOSED_RECEIVER_TYPE by error3<FirSourceElement, KtTypeReference, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()
    val EXPOSED_PROPERTY_TYPE by error3<FirSourceElement, PsiElement, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()
    val EXPOSED_PARAMETER_TYPE by error3<FirSourceElement, KtParameter, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()
    val EXPOSED_SUPER_INTERFACE by error3<FirSourceElement, KtSuperTypeListEntry, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()
    val EXPOSED_SUPER_CLASS by error3<FirSourceElement, KtSuperTypeListEntry, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()
    val EXPOSED_TYPE_PARAMETER_BOUND by error3<FirSourceElement, KtTypeParameter, FirEffectiveVisibility, FirMemberDeclaration, FirEffectiveVisibility>()

    // Modifiers
    val INAPPLICABLE_INFIX_MODIFIER by existing1<FirSourceElement, PsiElement, String>()
    val REPEATED_MODIFIER by error1<FirSourceElement, PsiElement, KtModifierKeywordToken>()
    val REDUNDANT_MODIFIER by error2<FirSourceElement, PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()
    val DEPRECATED_MODIFIER_PAIR by error2<FirSourceElement, PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()
    val INCOMPATIBLE_MODIFIERS by error2<FirSourceElement, PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()

    // Applicability
    val NONE_APPLICABLE by error1<FirSourceElement, PsiElement, Collection<AbstractFirBasedSymbol<*>>>()
    val INAPPLICABLE_CANDIDATE by error1<FirSourceElement, PsiElement, AbstractFirBasedSymbol<*>>()
    val INAPPLICABLE_LATEINIT_MODIFIER by error1<FirSourceElement, PsiElement, String>()

    // Ambiguity
    val AMBIGUITY by error1<FirSourceElement, PsiElement, Collection<AbstractFirBasedSymbol<*>>>()
    val ASSIGN_OPERATOR_AMBIGUITY by error1<FirSourceElement, PsiElement, Collection<AbstractFirBasedSymbol<*>>>()

    // Types & type parameters
    val TYPE_MISMATCH by error2<FirSourceElement, PsiElement, ConeKotlinType, ConeKotlinType>()
    val RECURSION_IN_IMPLICIT_TYPES by error0<FirSourceElement, PsiElement>()
    val INFERENCE_ERROR by error0<FirSourceElement, PsiElement>()
    val PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT by error0<FirSourceElement, PsiElement>()
    val UPPER_BOUND_VIOLATED by error2<FirSourceElement, PsiElement, FirTypeParameterSymbol, ConeKotlinType>()
    val TYPE_ARGUMENTS_NOT_ALLOWED by error0<FirSourceElement, PsiElement>()
    val WRONG_NUMBER_OF_TYPE_ARGUMENTS by error2<FirSourceElement, PsiElement, Int, FirRegularClassSymbol>()
    val NO_TYPE_FOR_TYPE_PARAMETER by error0<FirSourceElement, PsiElement>()
    val TYPE_PARAMETERS_IN_OBJECT by error0<FirSourceElement, PsiElement>()
    val ILLEGAL_PROJECTION_USAGE by error0<FirSourceElement, PsiElement>()
    val TYPE_PARAMETERS_IN_ENUM by error0<FirSourceElement, PsiElement>()
    val CONFLICTING_PROJECTION by error1<FirSourceElement, PsiElement, String>()
    val VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED by error0<FirSourceElement, PsiElement>()
    val RETURN_TYPE_MISMATCH_ON_OVERRIDE by error2<FirSourceElement, PsiElement, String, FirMemberDeclaration>()
    val PROPERTY_TYPE_MISMATCH_ON_OVERRIDE by error2<FirSourceElement, PsiElement, String, FirMemberDeclaration>()
    val VAR_TYPE_MISMATCH_ON_OVERRIDE by error2<FirSourceElement, PsiElement, String, FirMemberDeclaration>()

    // Redeclarations
    val MANY_COMPANION_OBJECTS by error0<FirSourceElement, PsiElement>()
    val CONFLICTING_OVERLOADS by error1<FirSourceElement, PsiElement, String>()
    val REDECLARATION by error1<FirSourceElement, PsiElement, String>()
    val ANY_METHOD_IMPLEMENTED_IN_INTERFACE by error0<FirSourceElement, PsiElement>()

    // Invalid local declarations
    val LOCAL_OBJECT_NOT_ALLOWED by error1<FirSourceElement, PsiElement, Name>()
    val LOCAL_INTERFACE_NOT_ALLOWED by error1<FirSourceElement, PsiElement, Name>()

    // Control flow diagnostics
    val UNINITIALIZED_VARIABLE by error1<FirSourceElement, PsiElement, FirPropertySymbol>()
    val WRONG_INVOCATION_KIND by warning3<FirSourceElement, PsiElement, AbstractFirBasedSymbol<*>, EventOccurrencesRange, EventOccurrencesRange>()
    val LEAKED_IN_PLACE_LAMBDA by error1<FirSourceElement, PsiElement, AbstractFirBasedSymbol<*>>()
    val WRONG_IMPLIES_CONDITION by error0<FirSourceElement, PsiElement>()

    // Extended checkers group
    val REDUNDANT_VISIBILITY_MODIFIER by warning0<FirSourceElement, PsiElement>()
    val REDUNDANT_MODALITY_MODIFIER by warning0<FirSourceElement, PsiElement>()
    val REDUNDANT_RETURN_UNIT_TYPE by warning0<FirSourceElement, PsiTypeElement>()
    val REDUNDANT_EXPLICIT_TYPE by warning0<FirSourceElement, PsiElement>()
    val REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE by warning0<FirSourceElement, PsiElement>()
    val CAN_BE_VAL by warning0<FirSourceElement, PsiElement>()
    val CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT by warning0<FirSourceElement, PsiElement>()
    val REDUNDANT_CALL_OF_CONVERSION_METHOD by warning0<FirSourceElement, PsiElement>()
    val ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS by warning0<FirSourceElement, PsiElement>()
    val EMPTY_RANGE by warning0<FirSourceElement, PsiElement>()
    val REDUNDANT_SETTER_PARAMETER_TYPE by warning0<FirSourceElement, PsiElement>()
    val UNUSED_VARIABLE by warning0<FirSourceElement, PsiElement>()
    val ASSIGNED_VALUE_IS_NEVER_READ by warning0<FirSourceElement, PsiElement>()
    val VARIABLE_INITIALIZER_IS_REDUNDANT by warning0<FirSourceElement, PsiElement>()
    val VARIABLE_NEVER_READ by warning0<FirSourceElement, PsiElement>()
    val USELESS_CALL_ON_NOT_NULL by warning0<FirSourceElement, PsiElement>()
}
