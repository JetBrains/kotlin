/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.analysis.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

object FirErrors {
    // Meta-errors
    val UNSUPPORTED by error1<PsiElement, String>()
    val UNSUPPORTED_FEATURE by error1<PsiElement, Pair<LanguageFeature, LanguageVersionSettings>>()

    // Miscellaneous
    val SYNTAX by error0<PsiElement>()
    val OTHER_ERROR by error0<PsiElement>()

    // General syntax
    val ILLEGAL_CONST_EXPRESSION by error0<PsiElement>()
    val ILLEGAL_UNDERSCORE by error0<PsiElement>()
    val EXPRESSION_EXPECTED by error0<PsiElement>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)
    val ASSIGNMENT_IN_EXPRESSION_CONTEXT by error0<KtBinaryExpression>()
    val BREAK_OR_CONTINUE_OUTSIDE_A_LOOP by error0<PsiElement>()
    val NOT_A_LOOP_LABEL by error0<PsiElement>()
    val VARIABLE_EXPECTED by error0<PsiElement>()
    val DELEGATION_IN_INTERFACE by error0<PsiElement>()
    val NESTED_CLASS_NOT_ALLOWED by error1<KtNamedDeclaration, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val INCORRECT_CHARACTER_LITERAL by error0<PsiElement>()
    val EMPTY_CHARACTER_LITERAL by error0<PsiElement>()
    val TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL by error0<PsiElement>()
    val ILLEGAL_ESCAPE by error0<PsiElement>()
    val INT_LITERAL_OUT_OF_RANGE by error0<PsiElement>()
    val FLOAT_LITERAL_OUT_OF_RANGE by error0<PsiElement>()
    val WRONG_LONG_SUFFIX by error0<KtElement>(SourceElementPositioningStrategies.LONG_LITERAL_SUFFIX)
    val DIVISION_BY_ZERO by warning0<KtExpression>()

    // Unresolved
    val INVISIBLE_REFERENCE by error1<PsiElement, AbstractFirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val UNRESOLVED_REFERENCE by error1<PsiElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val UNRESOLVED_LABEL by error0<PsiElement>()
    val DESERIALIZATION_ERROR by error0<PsiElement>()
    val ERROR_FROM_JAVA_RESOLUTION by error0<PsiElement>()
    val UNKNOWN_CALLABLE_KIND by error0<PsiElement>()
    val MISSING_STDLIB_CLASS by error0<PsiElement>()
    val NO_THIS by error0<PsiElement>()

    // Call resolution
    val CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS by error0<KtExpression>()

    // Super
    val SUPER_IS_NOT_AN_EXPRESSION by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val SUPER_NOT_AVAILABLE by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val ABSTRACT_SUPER_CALL by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val INSTANCE_ACCESS_BEFORE_SUPER_CALL by error1<PsiElement, String>()

    // Supertypes
    val ENUM_AS_SUPERTYPE by error0<PsiElement>()
    val RECURSION_IN_SUPERTYPES by error0<PsiElement>()
    val NOT_A_SUPERTYPE by error0<PsiElement>()
    val SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE by error0<PsiElement>()
    val QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE by error1<PsiElement, FirClass<*>>()
    val SUPERTYPE_INITIALIZED_IN_INTERFACE by error0<PsiElement>()
    val INTERFACE_WITH_SUPERCLASS by error0<PsiElement>()
    val CLASS_IN_SUPERTYPE_FOR_ENUM by error0<PsiElement>()
    val SEALED_SUPERTYPE by error0<PsiElement>()
    val SEALED_SUPERTYPE_IN_LOCAL_CLASS by error0<PsiElement>()
    val SUPERTYPE_NOT_A_CLASS_OR_INTERFACE by error1<KtElement, String>()

    // Constructor problems
    val CONSTRUCTOR_IN_OBJECT by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val CONSTRUCTOR_IN_INTERFACE by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val NON_PRIVATE_CONSTRUCTOR_IN_ENUM by error0<PsiElement>()
    val NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED by error0<PsiElement>()
    val CYCLIC_CONSTRUCTOR_DELEGATION_CALL by warning0<PsiElement>()
    val PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED by warning0<PsiElement>(SourceElementPositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)
    val SUPERTYPE_NOT_INITIALIZED by error0<KtTypeReference>()
    val SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR by error0<PsiElement>()
    val DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR by warning0<PsiElement>()
    val PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS by error0<KtNamedDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val EXPLICIT_DELEGATION_CALL_REQUIRED by error0<PsiElement>(SourceElementPositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)
    val SEALED_CLASS_CONSTRUCTOR_CALL by error0<PsiElement>()
    val DATA_CLASS_WITHOUT_PARAMETERS by error0<KtPrimaryConstructor>()
    val DATA_CLASS_VARARG_PARAMETER by error0<KtParameter>()
    val DATA_CLASS_NOT_PROPERTY_PARAMETER by error0<KtParameter>()

    // Annotations
    val ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR by error0<KtExpression>()
    val ANNOTATION_ARGUMENT_MUST_BE_CONST by error0<KtExpression>()
    val ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST by error0<KtExpression>()
    val ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL by error0<KtExpression>()
    val ANNOTATION_CLASS_MEMBER by error0<PsiElement>()
    val ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT by error0<KtExpression>()
    val INVALID_TYPE_OF_ANNOTATION_MEMBER by error0<KtTypeReference>()
    val LOCAL_ANNOTATION_CLASS_ERROR by error0<KtClassOrObject>()
    val MISSING_VAL_ON_ANNOTATION_PARAMETER by error0<KtParameter>()
    val NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION by error0<KtExpression>()
    val NOT_AN_ANNOTATION_CLASS by error1<PsiElement, String>()
    val NULLABLE_TYPE_OF_ANNOTATION_MEMBER by error0<KtTypeReference>()
    val VAR_ANNOTATION_PARAMETER by error0<KtParameter>(SourceElementPositioningStrategies.VAL_OR_VAR_NODE)
    val SUPERTYPES_FOR_ANNOTATION_CLASS by error0<KtClass>(SourceElementPositioningStrategies.SUPERTYPES_LIST)
    val ANNOTATION_USED_AS_ANNOTATION_ARGUMENT by error0<KtAnnotation>()

    // Exposed visibility
    val EXPOSED_TYPEALIAS_EXPANDED_TYPE by error3<KtNamedDeclaration, EffectiveVisibility, FirMemberDeclaration, EffectiveVisibility>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val EXPOSED_FUNCTION_RETURN_TYPE by error3<KtNamedDeclaration, EffectiveVisibility, FirMemberDeclaration, EffectiveVisibility>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val EXPOSED_RECEIVER_TYPE by error3<KtTypeReference, EffectiveVisibility, FirMemberDeclaration, EffectiveVisibility>()
    val EXPOSED_PROPERTY_TYPE by error3<KtNamedDeclaration, EffectiveVisibility, FirMemberDeclaration, EffectiveVisibility>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR by warning3<KtNamedDeclaration, EffectiveVisibility, FirMemberDeclaration, EffectiveVisibility>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val EXPOSED_PARAMETER_TYPE by error3<KtParameter, EffectiveVisibility, FirMemberDeclaration, EffectiveVisibility>()
    val EXPOSED_SUPER_INTERFACE by error3<KtTypeReference, EffectiveVisibility, FirMemberDeclaration, EffectiveVisibility>()
    val EXPOSED_SUPER_CLASS by error3<KtTypeReference, EffectiveVisibility, FirMemberDeclaration, EffectiveVisibility>()
    val EXPOSED_TYPE_PARAMETER_BOUND by error3<KtTypeReference, EffectiveVisibility, FirMemberDeclaration, EffectiveVisibility>()

    // Modifiers
    val INAPPLICABLE_INFIX_MODIFIER by error0<PsiElement>()
    val REPEATED_MODIFIER by error1<PsiElement, KtModifierKeywordToken>()
    val REDUNDANT_MODIFIER by error2<PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()
    val DEPRECATED_MODIFIER_PAIR by error2<PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()
    val INCOMPATIBLE_MODIFIERS by error2<PsiElement, KtModifierKeywordToken, KtModifierKeywordToken>()
    val REDUNDANT_OPEN_IN_INTERFACE by warning0<KtModifierListOwner>(SourceElementPositioningStrategies.OPEN_MODIFIER)
    val WRONG_MODIFIER_TARGET by error2<PsiElement, KtModifierKeywordToken, String>()
    val OPERATOR_MODIFIER_REQUIRED by error2<PsiElement, FirNamedFunctionSymbol, String>()

    // Inline classes
    val INLINE_CLASS_NOT_TOP_LEVEL by error0<KtDeclaration>(SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER)
    val INLINE_CLASS_NOT_FINAL by error0<KtDeclaration>(SourceElementPositioningStrategies.MODALITY_MODIFIER)
    val ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_INLINE_CLASS by error0<KtDeclaration>(SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER)
    val INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE by error0<KtElement>()
    val INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER by error0<KtParameter>()
    val PROPERTY_WITH_BACKING_FIELD_INSIDE_INLINE_CLASS by error0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val DELEGATED_PROPERTY_INSIDE_INLINE_CLASS by error0<PsiElement>()
    val INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE by error1<KtTypeReference, ConeKotlinType>()
    val INLINE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION by error0<PsiElement>()
    val INLINE_CLASS_CANNOT_EXTEND_CLASSES by error0<KtTypeReference>()
    val INLINE_CLASS_CANNOT_BE_RECURSIVE by error0<KtTypeReference>()
    val RESERVED_MEMBER_INSIDE_INLINE_CLASS by error1<KtFunction, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS by error0<PsiElement>()
    val INNER_CLASS_INSIDE_INLINE_CLASS by error0<KtDeclaration>(SourceElementPositioningStrategies.INNER_MODIFIER)
    val VALUE_CLASS_CANNOT_BE_CLONEABLE by error0<KtDeclaration>(SourceElementPositioningStrategies.INLINE_OR_VALUE_MODIFIER)

    // Applicability
    val NONE_APPLICABLE by error1<PsiElement, Collection<AbstractFirBasedSymbol<*>>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val INAPPLICABLE_CANDIDATE by error1<PsiElement, AbstractFirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val ARGUMENT_TYPE_MISMATCH by error2<PsiElement, ConeKotlinType, ConeKotlinType>()
    val NULL_FOR_NONNULL_TYPE by error0<PsiElement>()
    val INAPPLICABLE_LATEINIT_MODIFIER by error1<KtModifierListOwner, String>(SourceElementPositioningStrategies.LATEINIT_MODIFIER)
    val VARARG_OUTSIDE_PARENTHESES by error0<KtExpression>()
    val NAMED_ARGUMENTS_NOT_ALLOWED by error1<KtValueArgument, ForbiddenNamedArgumentsTarget>(SourceElementPositioningStrategies.NAME_OF_NAMED_ARGUMENT)
    val NON_VARARG_SPREAD by error0<LeafPsiElement>()
    val ARGUMENT_PASSED_TWICE by error0<KtValueArgument>(SourceElementPositioningStrategies.NAME_OF_NAMED_ARGUMENT)
    val TOO_MANY_ARGUMENTS by error1<PsiElement, FirCallableDeclaration<*>>()
    val NO_VALUE_FOR_PARAMETER by error1<KtElement, FirValueParameter>(SourceElementPositioningStrategies.VALUE_ARGUMENTS)
    val NAMED_PARAMETER_NOT_FOUND by error1<KtValueArgument, String>(SourceElementPositioningStrategies.NAME_OF_NAMED_ARGUMENT)
    val MANY_LAMBDA_EXPRESSION_ARGUMENTS by error0<KtValueArgument>()

    // Ambiguity
    val OVERLOAD_RESOLUTION_AMBIGUITY by error1<PsiElement, Collection<AbstractFirBasedSymbol<*>>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val ASSIGN_OPERATOR_AMBIGUITY by error1<PsiElement, Collection<AbstractFirBasedSymbol<*>>>()
    val ITERATOR_AMBIGUITY by error1<PsiElement, Collection<AbstractFirBasedSymbol<*>>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val HAS_NEXT_FUNCTION_AMBIGUITY by error1<PsiElement, Collection<AbstractFirBasedSymbol<*>>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val NEXT_AMBIGUITY by error1<PsiElement, Collection<AbstractFirBasedSymbol<*>>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)

    // Types & type parameters
    val TYPE_MISMATCH by error2<PsiElement, ConeKotlinType, ConeKotlinType>()
    val RECURSION_IN_IMPLICIT_TYPES by error0<PsiElement>()
    val INFERENCE_ERROR by error0<PsiElement>()
    val PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT by error0<PsiElement>()
    val UPPER_BOUND_VIOLATED by error1<PsiElement, ConeKotlinType>()
    val TYPE_ARGUMENTS_NOT_ALLOWED by error0<PsiElement>()
    val WRONG_NUMBER_OF_TYPE_ARGUMENTS by error2<PsiElement, Int, FirClassLikeSymbol<*>>()
    val NO_TYPE_ARGUMENTS_ON_RHS by error2<PsiElement, Int, FirClassLikeSymbol<*>>()
    val TYPE_PARAMETERS_IN_OBJECT by error0<PsiElement>()
    val ILLEGAL_PROJECTION_USAGE by error0<PsiElement>()
    val TYPE_PARAMETERS_IN_ENUM by error0<PsiElement>()
    val CONFLICTING_PROJECTION by error1<PsiElement, String>()
    val VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED by error0<KtTypeParameter>(SourceElementPositioningStrategies.VARIANCE_MODIFIER)
    val CATCH_PARAMETER_WITH_DEFAULT_VALUE by error0<PsiElement>()
    val REIFIED_TYPE_IN_CATCH_CLAUSE by error0<PsiElement>()
    val TYPE_PARAMETER_IN_CATCH_CLAUSE by error0<PsiElement>()
    val GENERIC_THROWABLE_SUBCLASS by error0<KtTypeParameterList>()
    val INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS by error0<KtClassOrObject>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE by error1<KtNamedDeclaration, FirTypeParameterSymbol>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val TYPE_PARAMETER_AS_REIFIED by error1<PsiElement, FirTypeParameterSymbol>()
    val FINAL_UPPER_BOUND by warning1<KtTypeReference, ConeKotlinType>()
    val UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE by error0<KtTypeReference>()
    val BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER by error0<KtElement>()
    val ONLY_ONE_CLASS_BOUND_ALLOWED by error0<KtTypeReference>()
    val REPEATED_BOUND by error0<KtTypeReference>()
    val CONFLICTING_UPPER_BOUNDS by error1<KtNamedDeclaration, FirTypeParameterSymbol>()
    val NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER by error2<KtSimpleNameExpression, Name, FirDeclaration>()
    val BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED by error0<KtTypeReference>()
    val REIFIED_TYPE_PARAMETER_NO_INLINE by error0<KtTypeParameter>(SourceElementPositioningStrategies.REIFIED_MODIFIER)
    val TYPE_PARAMETERS_NOT_ALLOWED by error0<KtDeclaration>(SourceElementPositioningStrategies.TYPE_PARAMETERS_LIST)
    val TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER by error0<KtTypeParameter>()
    val RETURN_TYPE_MISMATCH by error2<KtExpression, ConeKotlinType, ConeKotlinType>(SourceElementPositioningStrategies.WHOLE_ELEMENT)
    val CYCLIC_GENERIC_UPPER_BOUND by error0<PsiElement>()
    val DEPRECATED_TYPE_PARAMETER_SYNTAX by error0<KtDeclaration>(SourceElementPositioningStrategies.TYPE_PARAMETERS_LIST)
    val MISPLACED_TYPE_PARAMETER_CONSTRAINTS by warning0<KtTypeParameter>()
    val DYNAMIC_UPPER_BOUND by error0<KtTypeReference>()

    // Reflection
    val EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED by error1<KtExpression, FirCallableDeclaration<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val CALLABLE_REFERENCE_LHS_NOT_A_CLASS by error0<KtExpression>()
    val CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR by error0<KtExpression>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val CLASS_LITERAL_LHS_NOT_A_CLASS by error0<KtExpression>()
    val NULLABLE_TYPE_IN_CLASS_LITERAL_LHS by error0<KtExpression>()
    val EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS by error1<PsiElement, ConeKotlinType>()

    // overrides
    val NOTHING_TO_OVERRIDE by error1<KtModifierListOwner, FirMemberDeclaration>(SourceElementPositioningStrategies.OVERRIDE_MODIFIER)
    val CANNOT_WEAKEN_ACCESS_PRIVILEGE by error3<KtModifierListOwner, Visibility, FirCallableDeclaration<*>, Name>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val CANNOT_CHANGE_ACCESS_PRIVILEGE by error3<KtModifierListOwner, Visibility, FirCallableDeclaration<*>, Name>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val OVERRIDING_FINAL_MEMBER by error2<KtNamedDeclaration, FirCallableDeclaration<*>, Name>(SourceElementPositioningStrategies.OVERRIDE_MODIFIER)
    val ABSTRACT_MEMBER_NOT_IMPLEMENTED by error2<KtClassOrObject, FirClass<*>, FirCallableDeclaration<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED by error2<KtClassOrObject, FirClass<*>, FirCallableDeclaration<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER by error2<KtClassOrObject, FirClass<*>, FirCallableDeclaration<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_WARNING by warning2<KtClassOrObject, FirClass<*>, FirCallableDeclaration<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val MANY_IMPL_MEMBER_NOT_IMPLEMENTED by error2<KtClassOrObject, FirClass<*>, FirCallableDeclaration<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED by error2<KtClassOrObject, FirClass<*>, FirCallableDeclaration<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val OVERRIDING_FINAL_MEMBER_BY_DELEGATION by error2<KtClassOrObject, FirCallableDeclaration<*>, FirCallableDeclaration<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE by warning2<KtClassOrObject, FirCallableDeclaration<*>, FirCallableDeclaration<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val RETURN_TYPE_MISMATCH_ON_OVERRIDE by error2<KtNamedDeclaration, FirMemberDeclaration, FirMemberDeclaration>(SourceElementPositioningStrategies.DECLARATION_RETURN_TYPE)
    val PROPERTY_TYPE_MISMATCH_ON_OVERRIDE by error2<KtNamedDeclaration, FirMemberDeclaration, FirMemberDeclaration>(SourceElementPositioningStrategies.DECLARATION_RETURN_TYPE)
    val VAR_TYPE_MISMATCH_ON_OVERRIDE by error2<KtNamedDeclaration, FirMemberDeclaration, FirMemberDeclaration>(SourceElementPositioningStrategies.DECLARATION_RETURN_TYPE)
    val VAR_OVERRIDDEN_BY_VAL by error2<KtNamedDeclaration, FirMemberDeclaration, FirMemberDeclaration>(SourceElementPositioningStrategies.VAL_OR_VAR_NODE)
    val NON_FINAL_MEMBER_IN_FINAL_CLASS by warning0<KtNamedDeclaration>(SourceElementPositioningStrategies.OPEN_MODIFIER)
    val NON_FINAL_MEMBER_IN_OBJECT by warning0<KtNamedDeclaration>(SourceElementPositioningStrategies.OPEN_MODIFIER)

    // Redeclarations
    val MANY_COMPANION_OBJECTS by error0<KtObjectDeclaration>(SourceElementPositioningStrategies.COMPANION_OBJECT)
    val CONFLICTING_OVERLOADS by error1<PsiElement, Collection<AbstractFirBasedSymbol<*>>>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val REDECLARATION by error1<PsiElement, Collection<AbstractFirBasedSymbol<*>>>()
    val METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE by error0<PsiElement>()

    // Invalid local declarations
    val LOCAL_OBJECT_NOT_ALLOWED by error1<KtNamedDeclaration, Name>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val LOCAL_INTERFACE_NOT_ALLOWED by error1<KtNamedDeclaration, Name>(SourceElementPositioningStrategies.DECLARATION_NAME)

    // Functions
    val ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS by error2<KtFunction, FirMemberDeclaration, FirMemberDeclaration>(SourceElementPositioningStrategies.MODALITY_MODIFIER)
    val ABSTRACT_FUNCTION_WITH_BODY by error1<KtFunction, FirMemberDeclaration>(SourceElementPositioningStrategies.MODALITY_MODIFIER)
    val NON_ABSTRACT_FUNCTION_WITH_NO_BODY by error1<KtFunction, FirMemberDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val PRIVATE_FUNCTION_WITH_NO_BODY by error1<KtFunction, FirMemberDeclaration>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val NON_MEMBER_FUNCTION_NO_BODY by error1<KtFunction, FirMemberDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val FUNCTION_DECLARATION_WITH_NO_NAME by error0<KtFunction>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val ANONYMOUS_FUNCTION_WITH_NAME by error0<KtFunction>()
    val ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE by error0<KtParameter>(SourceElementPositioningStrategies.PARAMETER_DEFAULT_VALUE)
    val USELESS_VARARG_ON_PARAMETER by warning0<KtParameter>()
    val MULTIPLE_VARARG_PARAMETERS by error0<KtParameter>(SourceElementPositioningStrategies.PARAMETER_VARARG_MODIFIER)
    val FORBIDDEN_VARARG_PARAMETER_TYPE by error1<KtParameter, ConeKotlinType>(SourceElementPositioningStrategies.PARAMETER_VARARG_MODIFIER)
    val VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION by error0<KtParameter>()
    val CANNOT_INFER_PARAMETER_TYPE by error0<KtParameter>()

    // Fun interfaces
    val FUN_INTERFACE_CONSTRUCTOR_REFERENCE by error0<KtExpression>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS by error0<KtClass>(SourceElementPositioningStrategies.FUN_MODIFIER)
    val FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES by error0<KtDeclaration>(SourceElementPositioningStrategies.FUN_INTERFACE)
    val FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS by error0<KtDeclaration>(SourceElementPositioningStrategies.FUN_INTERFACE)
    val FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE by error0<KtDeclaration>(SourceElementPositioningStrategies.FUN_INTERFACE)
    val FUN_INTERFACE_WITH_SUSPEND_FUNCTION by error0<KtDeclaration>(SourceElementPositioningStrategies.FUN_INTERFACE)

    // Properties & accessors
    val ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS by error2<KtModifierListOwner, FirMemberDeclaration, FirMemberDeclaration>(SourceElementPositioningStrategies.MODALITY_MODIFIER)
    val PRIVATE_PROPERTY_IN_INTERFACE by error0<KtProperty>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val ABSTRACT_PROPERTY_WITH_INITIALIZER by error0<KtExpression>()
    val PROPERTY_INITIALIZER_IN_INTERFACE by error0<KtExpression>()
    val PROPERTY_WITH_NO_TYPE_NO_INITIALIZER by error0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val MUST_BE_INITIALIZED by error0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val MUST_BE_INITIALIZED_OR_BE_ABSTRACT by error0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT by error0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val UNNECESSARY_LATEINIT by warning0<KtProperty>(SourceElementPositioningStrategies.LATEINIT_MODIFIER)
    val BACKING_FIELD_IN_INTERFACE by error0<KtProperty>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val EXTENSION_PROPERTY_WITH_BACKING_FIELD by error0<KtExpression>()
    val PROPERTY_INITIALIZER_NO_BACKING_FIELD by error0<KtExpression>()
    val ABSTRACT_DELEGATED_PROPERTY by error0<KtPropertyDelegate>()
    val DELEGATED_PROPERTY_IN_INTERFACE by error0<KtPropertyDelegate>()
    val ABSTRACT_PROPERTY_WITH_GETTER by error0<KtPropertyAccessor>()
    val ABSTRACT_PROPERTY_WITH_SETTER by error0<KtPropertyAccessor>()
    val PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY by error0<KtModifierListOwner>(SourceElementPositioningStrategies.PRIVATE_MODIFIER)
    val PRIVATE_SETTER_FOR_OPEN_PROPERTY by error0<KtModifierListOwner>(SourceElementPositioningStrategies.PRIVATE_MODIFIER)
    val EXPECTED_PRIVATE_DECLARATION by error0<KtModifierListOwner>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val VAL_WITH_SETTER by error0<KtPropertyAccessor>()
    val CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT by error0<KtProperty>(SourceElementPositioningStrategies.CONST_MODIFIER)
    val CONST_VAL_WITH_GETTER by error0<KtProperty>()
    val CONST_VAL_WITH_DELEGATE by error0<KtPropertyDelegate>()
    val TYPE_CANT_BE_USED_FOR_CONST_VAL by error1<KtProperty, ConeKotlinType>(SourceElementPositioningStrategies.CONST_MODIFIER)
    val CONST_VAL_WITHOUT_INITIALIZER by error0<KtProperty>(SourceElementPositioningStrategies.CONST_MODIFIER)
    val CONST_VAL_WITH_NON_CONST_INITIALIZER by error0<KtExpression>()
    val WRONG_SETTER_PARAMETER_TYPE by error2<KtTypeReference, ConeKotlinType, ConeKotlinType>()
    val INITIALIZER_TYPE_MISMATCH by error2<KtProperty, ConeKotlinType, ConeKotlinType>(SourceElementPositioningStrategies.ASSIGNMENT_VALUE)
    val GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY by error0<KtModifierListOwner>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val WRONG_SETTER_RETURN_TYPE by error0<KtProperty>()

    // Multi-platform projects
    val EXPECTED_DECLARATION_WITH_BODY by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val EXPECTED_PROPERTY_INITIALIZER by error0<KtExpression>()
    val EXPECTED_DELEGATED_PROPERTY by error0<KtPropertyDelegate>()
    val EXPECTED_LATEINIT_PROPERTY by error0<KtModifierListOwner>(SourceElementPositioningStrategies.LATEINIT_MODIFIER)

    // Destructuring declaration
    val INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION by error0<KtDestructuringDeclaration>()
    val COMPONENT_FUNCTION_MISSING by error2<PsiElement, Name, ConeKotlinType>()
    val COMPONENT_FUNCTION_AMBIGUITY by error2<PsiElement, Name, Collection<AbstractFirBasedSymbol<*>>>()
    val COMPONENT_FUNCTION_ON_NULLABLE by error1<KtExpression, Name>()
    val COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH by error3<KtExpression, Name, ConeKotlinType, ConeKotlinType>()

    // Control flow diagnostics
    val UNINITIALIZED_VARIABLE by error1<KtSimpleNameExpression, FirPropertySymbol>()
    val UNINITIALIZED_ENUM_ENTRY by error1<KtSimpleNameExpression, FirVariableSymbol<FirEnumEntry>>()
    val UNINITIALIZED_ENUM_COMPANION by error1<KtSimpleNameExpression, FirRegularClassSymbol>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val VAL_REASSIGNMENT by error1<KtExpression, FirVariableSymbol<*>>()
    val VAL_REASSIGNMENT_VIA_BACKING_FIELD by warning1<KtExpression, FirPropertySymbol>()
    val VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR by error1<KtExpression, FirPropertySymbol>()
    val WRONG_INVOCATION_KIND by warning3<PsiElement, AbstractFirBasedSymbol<*>, EventOccurrencesRange, EventOccurrencesRange>()
    val LEAKED_IN_PLACE_LAMBDA by error1<PsiElement, AbstractFirBasedSymbol<*>>()
    val WRONG_IMPLIES_CONDITION by warning0<PsiElement>()

    // Nullability
    val UNSAFE_CALL by error1<PsiElement, ConeKotlinType>(SourceElementPositioningStrategies.DOT_BY_QUALIFIED)
    val UNSAFE_IMPLICIT_INVOKE_CALL by error1<PsiElement, ConeKotlinType>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val UNSAFE_INFIX_CALL by error3<KtExpression, FirExpression, String, FirExpression>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val UNSAFE_OPERATOR_CALL by error3<KtExpression, FirExpression, String, FirExpression>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val ITERATOR_ON_NULLABLE by error0<KtExpression>()
    val UNNECESSARY_SAFE_CALL by warning1<PsiElement, ConeKotlinType>(SourceElementPositioningStrategies.SAFE_ACCESS)
    val UNEXPECTED_SAFE_CALL by error0<PsiElement>(SourceElementPositioningStrategies.SAFE_ACCESS)
    val UNNECESSARY_NOT_NULL_ASSERTION by warning1<KtExpression, ConeKotlinType>(SourceElementPositioningStrategies.OPERATOR)
    val NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION by warning0<KtExpression>(SourceElementPositioningStrategies.OPERATOR)
    val NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE by warning0<KtExpression>(SourceElementPositioningStrategies.OPERATOR)
    val USELESS_ELVIS by warning1<KtBinaryExpression, ConeKotlinType>(SourceElementPositioningStrategies.USELESS_ELVIS)
    val USELESS_ELVIS_RIGHT_IS_NULL by warning0<KtBinaryExpression>(SourceElementPositioningStrategies.USELESS_ELVIS)

    // Casts and is-checks
    val USELESS_CAST by warning0<KtBinaryExpressionWithTypeRHS>(SourceElementPositioningStrategies.AS_TYPE)
    val USELESS_IS_CHECK by warning1<KtElement, Boolean>()

    // When expressions
    val NO_ELSE_IN_WHEN by error1<KtWhenExpression, List<WhenMissingCase>>(SourceElementPositioningStrategies.WHEN_EXPRESSION)
    val INVALID_IF_AS_EXPRESSION by error0<KtIfExpression>(SourceElementPositioningStrategies.IF_EXPRESSION)
    val ELSE_MISPLACED_IN_WHEN by error0<KtWhenEntry>(SourceElementPositioningStrategies.ELSE_ENTRY)

    // Context tracking
    val TYPE_PARAMETER_IS_NOT_AN_EXPRESSION by error1<KtSimpleNameExpression, FirTypeParameterSymbol>()
    val TYPE_PARAMETER_ON_LHS_OF_DOT by error1<KtSimpleNameExpression, FirTypeParameterSymbol>()
    val NO_COMPANION_OBJECT by error1<KtSimpleNameExpression, FirRegularClassSymbol>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)
    val EXPRESSION_EXPECTED_PACKAGE_FOUND by error0<KtSimpleNameExpression>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)

    // Function contracts
    val ERROR_IN_CONTRACT_DESCRIPTION by error1<KtElement, String>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)

    // Conventions
    val NO_GET_METHOD by error0<KtArrayAccessExpression>(SourceElementPositioningStrategies.ARRAY_ACCESS)
    val NO_SET_METHOD by error0<KtArrayAccessExpression>(SourceElementPositioningStrategies.ARRAY_ACCESS)
    val ITERATOR_MISSING by error0<KtExpression>()
    val HAS_NEXT_MISSING by error0<KtExpression>()
    val NEXT_MISSING by error0<KtExpression>()
    val HAS_NEXT_FUNCTION_NONE_APPLICABLE by error1<KtExpression, Collection<AbstractFirBasedSymbol<*>>>()
    val NEXT_NONE_APPLICABLE by error1<KtExpression, Collection<AbstractFirBasedSymbol<*>>>()
    val DELEGATE_SPECIAL_FUNCTION_MISSING by error3<KtExpression, String, ConeKotlinType, String>()
    val DELEGATE_SPECIAL_FUNCTION_AMBIGUITY by error2<KtExpression, String, Collection<AbstractFirBasedSymbol<*>>>()
    val DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE by error2<KtExpression, String, Collection<AbstractFirBasedSymbol<*>>>()
    val DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH by error3<KtExpression, String, ConeKotlinType, ConeKotlinType>()
    val UNDERSCORE_IS_RESERVED by error0<KtExpression>(SourceElementPositioningStrategies.RESERVED_UNDERSCORE)
    val UNDERSCORE_USAGE_WITHOUT_BACKTICKS by error0<KtExpression>(SourceElementPositioningStrategies.RESERVED_UNDERSCORE)

    // Type alias
    val TOPLEVEL_TYPEALIASES_ONLY by error0<KtTypeAlias>()

    // Extended checkers
    val REDUNDANT_VISIBILITY_MODIFIER by warning0<KtModifierListOwner>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val REDUNDANT_MODALITY_MODIFIER by warning0<KtModifierListOwner>(SourceElementPositioningStrategies.MODALITY_MODIFIER)
    val REDUNDANT_RETURN_UNIT_TYPE by warning0<PsiTypeElement>()
    val REDUNDANT_EXPLICIT_TYPE by warning0<PsiElement>()
    val REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE by warning0<PsiElement>()
    val CAN_BE_VAL by warning0<KtDeclaration>(SourceElementPositioningStrategies.VAL_OR_VAR_NODE)
    val CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT by warning0<KtExpression>(SourceElementPositioningStrategies.OPERATOR)
    val REDUNDANT_CALL_OF_CONVERSION_METHOD by warning0<PsiElement>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)
    val ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS by warning0<KtExpression>(SourceElementPositioningStrategies.OPERATOR)
    val EMPTY_RANGE by warning0<PsiElement>()
    val REDUNDANT_SETTER_PARAMETER_TYPE by warning0<PsiElement>()
    val UNUSED_VARIABLE by warning0<KtNamedDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val ASSIGNED_VALUE_IS_NEVER_READ by warning0<PsiElement>()
    val VARIABLE_INITIALIZER_IS_REDUNDANT by warning0<PsiElement>()
    val VARIABLE_NEVER_READ by warning0<KtNamedDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val USELESS_CALL_ON_NOT_NULL by warning0<PsiElement>(SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED)

    // Returns
    val RETURN_NOT_ALLOWED by error0<KtReturnExpression>(SourceElementPositioningStrategies.RETURN_WITH_LABEL)
    val RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY by error0<KtReturnExpression>(SourceElementPositioningStrategies.RETURN_WITH_LABEL)

    // Inline
    val USAGE_IS_NOT_INLINABLE by error1<KtElement, AbstractFirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val NON_LOCAL_RETURN_NOT_ALLOWED by error1<KtElement, AbstractFirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val RECURSION_IN_INLINE by error1<KtElement, AbstractFirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val NON_PUBLIC_CALL_FROM_PUBLIC_INLINE by error2<KtElement, AbstractFirBasedSymbol<*>, AbstractFirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE by error2<KtElement, AbstractFirBasedSymbol<*>, AbstractFirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR by error2<KtElement, AbstractFirBasedSymbol<*>, AbstractFirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val PROTECTED_CALL_FROM_PUBLIC_INLINE by warning2<KtElement, AbstractFirBasedSymbol<*>, AbstractFirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val PRIVATE_CLASS_MEMBER_FROM_INLINE by error2<KtElement, AbstractFirBasedSymbol<*>, AbstractFirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val SUPER_CALL_FROM_PUBLIC_INLINE by warning1<KtElement, AbstractFirBasedSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)

}
