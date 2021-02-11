/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypeElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirEffectiveVisibility
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.WhenMissingCase
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*


@Suppress("UNUSED_VARIABLE", "LocalVariableName")
@OptIn(PrivateForInline::class)
val DIAGNOSTICS_LIST = DiagnosticListBuilder.buildDiagnosticList {
    group("Miscellaneous") {
        val SYNTAX by error<FirSourceElement, PsiElement>()
        val OTHER_ERROR by error<FirSourceElement, PsiElement>()
    }

    group("General syntax") {
        val ILLEGAL_CONST_EXPRESSION by error<FirSourceElement, PsiElement>()
        val ILLEGAL_UNDERSCORE by error<FirSourceElement, PsiElement>()
        val EXPRESSION_REQUIRED by error<FirSourceElement, PsiElement>()
        val BREAK_OR_CONTINUE_OUTSIDE_A_LOOP by error<FirSourceElement, PsiElement>()
        val NOT_A_LOOP_LABEL by error<FirSourceElement, PsiElement>()
        val VARIABLE_EXPECTED by error<FirSourceElement, PsiElement>()
        val RETURN_NOT_ALLOWED by error<FirSourceElement, PsiElement>()
        val DELEGATION_IN_INTERFACE by error<FirSourceElement, PsiElement>()
    }

    group("Unresolved") {
        val HIDDEN by error<FirSourceElement, PsiElement> {
            parameter<AbstractFirBasedSymbol<*>>("hidden")
        }
        val UNRESOLVED_REFERENCE by error<FirSourceElement, PsiElement> {
            parameter<String>("reference")
        }
        val UNRESOLVED_LABEL by error<FirSourceElement, PsiElement>()
        val DESERIALIZATION_ERROR by error<FirSourceElement, PsiElement>()
        val ERROR_FROM_JAVA_RESOLUTION by error<FirSourceElement, PsiElement>()
        val UNKNOWN_CALLABLE_KIND by error<FirSourceElement, PsiElement>()
        val MISSING_STDLIB_CLASS by error<FirSourceElement, PsiElement>()
        val NO_THIS by error<FirSourceElement, PsiElement>()
    }

    group("Super") {
        val SUPER_IS_NOT_AN_EXPRESSION by error<FirSourceElement, PsiElement>()
        val SUPER_NOT_AVAILABLE by error<FirSourceElement, PsiElement>()
        val ABSTRACT_SUPER_CALL by error<FirSourceElement, PsiElement>()
        val INSTANCE_ACCESS_BEFORE_SUPER_CALL by error<FirSourceElement, PsiElement> {
            parameter<String>("target")
        }
    }

    group("Supertypes") {
        val TYPE_PARAMETER_AS_SUPERTYPE by error<FirSourceElement, PsiElement>()
        val ENUM_AS_SUPERTYPE by error<FirSourceElement, PsiElement>()
        val RECURSION_IN_SUPERTYPES by error<FirSourceElement, PsiElement>()
        val NOT_A_SUPERTYPE by error<FirSourceElement, PsiElement>()
        val SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE by error<FirSourceElement, PsiElement>()
        val QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE by error<FirSourceElement, PsiElement> {
            parameter<FirClass<*>>("otherSuperType")
        }
        val SUPERTYPE_INITIALIZED_IN_INTERFACE by error<FirSourceElement, PsiElement>()
        val INTERFACE_WITH_SUPERCLASS by error<FirSourceElement, PsiElement>()
        val CLASS_IN_SUPERTYPE_FOR_ENUM by error<FirSourceElement, PsiElement>()
        val SEALED_SUPERTYPE by error<FirSourceElement, PsiElement>()
        val SEALED_SUPERTYPE_IN_LOCAL_CLASS by error<FirSourceElement, PsiElement>()
    }

    group(" Constructor problems") {
        val CONSTRUCTOR_IN_OBJECT by error<FirSourceElement, KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val CONSTRUCTOR_IN_INTERFACE by error<FirSourceElement, KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val NON_PRIVATE_CONSTRUCTOR_IN_ENUM by error<FirSourceElement, PsiElement>()
        val NON_PRIVATE_CONSTRUCTOR_IN_SEALED by error<FirSourceElement, PsiElement>()
        val CYCLIC_CONSTRUCTOR_DELEGATION_CALL by warning<FirSourceElement, PsiElement>()
        val PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED by warning<FirSourceElement, PsiElement>(PositioningStrategy.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)
        val SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR by warning<FirSourceElement, PsiElement>()
        val DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR by warning<FirSourceElement, PsiElement>()
        val PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS by warning<FirSourceElement, PsiElement>()
        val EXPLICIT_DELEGATION_CALL_REQUIRED by warning<FirSourceElement, PsiElement>(PositioningStrategy.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)
        val SEALED_CLASS_CONSTRUCTOR_CALL by error<FirSourceElement, PsiElement>()
    }

    group("Annotations") {
        val ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR by error<FirSourceElement, KtExpression>()
        val ANNOTATION_ARGUMENT_MUST_BE_CONST by error<FirSourceElement, KtExpression>()
        val ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST by error<FirSourceElement, KtExpression>()
        val ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL by error<FirSourceElement, KtExpression>()
        val ANNOTATION_CLASS_MEMBER by error<FirSourceElement, PsiElement>()
        val ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT by error<FirSourceElement, KtExpression>()
        val INVALID_TYPE_OF_ANNOTATION_MEMBER by error<FirSourceElement, KtTypeReference>()
        val LOCAL_ANNOTATION_CLASS_ERROR by error<FirSourceElement, KtClassOrObject>()
        val MISSING_VAL_ON_ANNOTATION_PARAMETER by error<FirSourceElement, KtParameter>()
        val NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION by error<FirSourceElement, KtExpression>()
        val NOT_AN_ANNOTATION_CLASS by error<FirSourceElement, PsiElement> {
            parameter<String>("annotationName")
        }
        val NULLABLE_TYPE_OF_ANNOTATION_MEMBER by error<FirSourceElement, KtTypeReference>()
        val VAR_ANNOTATION_PARAMETER by error<FirSourceElement, KtParameter>(PositioningStrategy.VAL_OR_VAR_NODE)
    }

    group("Exposed visibility group") {
        val EXPOSED_TYPEALIAS_EXPANDED_TYPE by exposedVisibilityError<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val EXPOSED_FUNCTION_RETURN_TYPE by exposedVisibilityError<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)

        val EXPOSED_RECEIVER_TYPE by exposedVisibilityError<KtTypeReference>()
        val EXPOSED_PROPERTY_TYPE by exposedVisibilityError<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val EXPOSED_PARAMETER_TYPE by exposedVisibilityError<KtParameter>(/* // NB: for parameter FE 1.0 reports not on a name for some reason */)
        val EXPOSED_SUPER_INTERFACE by exposedVisibilityError<KtTypeReference>()
        val EXPOSED_SUPER_CLASS by exposedVisibilityError<KtTypeReference>()
        val EXPOSED_TYPE_PARAMETER_BOUND by exposedVisibilityError<KtTypeReference>()
    }

    group("Modifiers") {
        val INAPPLICABLE_INFIX_MODIFIER by error<FirSourceElement, PsiElement>()
        val REPEATED_MODIFIER by error<FirSourceElement, PsiElement> {
            parameter<KtModifierKeywordToken>("modifier")
        }
        val REDUNDANT_MODIFIER by error<FirSourceElement, PsiElement> {
            parameter<KtModifierKeywordToken>("redundantModifier")
            parameter<KtModifierKeywordToken>("conflictingModifier")
        }
        val DEPRECATED_MODIFIER_PAIR by error<FirSourceElement, PsiElement> {
            parameter<KtModifierKeywordToken>("deprecatedModifier")
            parameter<KtModifierKeywordToken>("conflictingModifier")
        }
        val INCOMPATIBLE_MODIFIERS by error<FirSourceElement, PsiElement> {
            parameter<KtModifierKeywordToken>("modifier1")
            parameter<KtModifierKeywordToken>("modifier2")
        }
        val REDUNDANT_OPEN_IN_INTERFACE by warning<FirSourceElement, KtModifierListOwner>(PositioningStrategy.OPEN_MODIFIER)
    }

    group("Applicability") {
        val NONE_APPLICABLE by error<FirSourceElement, PsiElement> {
            parameter<Collection<AbstractFirBasedSymbol<*>>>("candidates")
        }

        val INAPPLICABLE_CANDIDATE by error<FirSourceElement, PsiElement> {
            parameter<AbstractFirBasedSymbol<*>>("candidate")
        }
        val INAPPLICABLE_LATEINIT_MODIFIER by error<FirSourceElement, PsiElement> {
            parameter<String>("reason")
        }
    }

    group("Ambiguity") {
        val AMBIGUITY by error<FirSourceElement, PsiElement> {
            parameter<Collection<AbstractFirBasedSymbol<*>>>("candidates")
        }
        val ASSIGN_OPERATOR_AMBIGUITY by error<FirSourceElement, PsiElement> {
            parameter<Collection<AbstractFirBasedSymbol<*>>>("candidates")
        }
    }

    group("Types & type parameters") {
        val TYPE_MISMATCH by error<FirSourceElement, PsiElement> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }
        val RECURSION_IN_IMPLICIT_TYPES by error<FirSourceElement, PsiElement>()
        val INFERENCE_ERROR by error<FirSourceElement, PsiElement>()
        val PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT by error<FirSourceElement, PsiElement>()
        val UPPER_BOUND_VIOLATED by error<FirSourceElement, PsiElement> {
            parameter<FirTypeParameterSymbol>("typeParameter")
            parameter<ConeKotlinType>("violatedType")
        }
        val TYPE_ARGUMENTS_NOT_ALLOWED by error<FirSourceElement, PsiElement>()
        val WRONG_NUMBER_OF_TYPE_ARGUMENTS by error<FirSourceElement, PsiElement> {
            parameter<Int>("expectedCount")
            parameter<FirClassLikeSymbol<*>>("classifier")
        }
        val NO_TYPE_FOR_TYPE_PARAMETER by error<FirSourceElement, PsiElement>()
        val TYPE_PARAMETERS_IN_OBJECT by error<FirSourceElement, PsiElement>()
        val ILLEGAL_PROJECTION_USAGE by error<FirSourceElement, PsiElement>()
        val TYPE_PARAMETERS_IN_ENUM by error<FirSourceElement, PsiElement>()
        val CONFLICTING_PROJECTION by error<FirSourceElement, PsiElement> {
            parameter<String>("type") // TODO use ConeType instead of String
        }
        val VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED by error<FirSourceElement, KtTypeParameter>(PositioningStrategy.VARIANCE_MODIFIER)

        val CATCH_PARAMETER_WITH_DEFAULT_VALUE by error<FirSourceElement, PsiElement>()
        val REIFIED_TYPE_IN_CATCH_CLAUSE by error<FirSourceElement, PsiElement>()
        val TYPE_PARAMETER_IN_CATCH_CLAUSE by error<FirSourceElement, PsiElement>()
        val GENERIC_THROWABLE_SUBCLASS by error<FirSourceElement, KtTypeParameterList>()
        val INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS by error<FirSourceElement, KtClassOrObject>(PositioningStrategy.DECLARATION_NAME)
    }

    group("overrides") {
        val NOTHING_TO_OVERRIDE by error<FirSourceElement, KtModifierListOwner>(PositioningStrategy.OVERRIDE_MODIFIER) {
            parameter<FirMemberDeclaration>("declaration")
        }

        val CANNOT_WEAKEN_ACCESS_PRIVILEGE by error<FirSourceElement, KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER) {
            parameter<Visibility>("overridingVisibility")
            parameter<FirCallableDeclaration<*>>("overridden")
            parameter<Name>("containingClassName")
        }
        val CANNOT_CHANGE_ACCESS_PRIVILEGE by error<FirSourceElement, KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER) {
            parameter<Visibility>("overridingVisibility")
            parameter<FirCallableDeclaration<*>>("overridden")
            parameter<Name>("containingClassName")
        }

        val OVERRIDING_FINAL_MEMBER by error<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.OVERRIDE_MODIFIER) {
            parameter<FirCallableDeclaration<*>>("overriddenDeclaration")
            parameter<Name>("containingClassName")
        }

        val RETURN_TYPE_MISMATCH_ON_OVERRIDE by error<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.DECLARATION_RETURN_TYPE) {
            parameter<FirMemberDeclaration>("function")
            parameter<FirMemberDeclaration>("superFunction")
        }
        val PROPERTY_TYPE_MISMATCH_ON_OVERRIDE by error<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.DECLARATION_RETURN_TYPE) {
            parameter<FirMemberDeclaration>("property")
            parameter<FirMemberDeclaration>("superProperty")
        }
        val VAR_TYPE_MISMATCH_ON_OVERRIDE by error<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.DECLARATION_RETURN_TYPE) {
            parameter<FirMemberDeclaration>("variable")
            parameter<FirMemberDeclaration>("superVariable")
        }
        val VAR_OVERRIDDEN_BY_VAL by error<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.VAL_OR_VAR_NODE) {
            parameter<FirMemberDeclaration>("overridingDeclaration")
            parameter<FirMemberDeclaration>("overriddenDeclaration")
        }
    }

    group("Redeclarations") {
        val MANY_COMPANION_OBJECTS by error<FirSourceElement, PsiElement>()
        val CONFLICTING_OVERLOADS by error<FirSourceElement, PsiElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<String>("conflictingOverloads") // TODO use Collection<Symbol> instead of String
        }
        val REDECLARATION by error<FirSourceElement, PsiElement>() {
            parameter<String>("conflictingDeclaration") // TODO use Collection<Symbol> instead of String
        }
        val ANY_METHOD_IMPLEMENTED_IN_INTERFACE by error<FirSourceElement, PsiElement>()
    }

    group("Invalid local declarations") {
        val LOCAL_OBJECT_NOT_ALLOWED by error<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<Name>("objectName")
        }
        val LOCAL_INTERFACE_NOT_ALLOWED by error<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<Name>("interfaceName")
        }
    }

    group("Functions") {
        val ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS by error<FirSourceElement, KtFunction>(PositioningStrategy.MODALITY_MODIFIER) {
            parameter<FirMemberDeclaration>("function")
            parameter<FirMemberDeclaration>("containingClass") // TODO use FirClass instead of FirMemberDeclaration
        }
        val ABSTRACT_FUNCTION_WITH_BODY by error<FirSourceElement, KtFunction>(PositioningStrategy.MODALITY_MODIFIER) {
            parameter<FirMemberDeclaration>("function")
        }
        val NON_ABSTRACT_FUNCTION_WITH_NO_BODY by error<FirSourceElement, KtFunction>(PositioningStrategy.DECLARATION_SIGNATURE) {
            parameter<FirMemberDeclaration>("function")
        }
        val PRIVATE_FUNCTION_WITH_NO_BODY by error<FirSourceElement, KtFunction>(PositioningStrategy.VISIBILITY_MODIFIER) {
            parameter<FirMemberDeclaration>("function")
        }

        val NON_MEMBER_FUNCTION_NO_BODY by error<FirSourceElement, KtFunction>(PositioningStrategy.DECLARATION_SIGNATURE) {
            parameter<FirMemberDeclaration>("function")
        }

        val FUNCTION_DECLARATION_WITH_NO_NAME by error<FirSourceElement, KtFunction>(PositioningStrategy.DECLARATION_SIGNATURE)

        // TODO: val ANONYMOUS_FUNCTION_WITH_NAME by error1<FirSourceElement, PsiElement, Name>(SourceElementPositioningStrategies.DECLARATION_NAME)
        val ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE by error<FirSourceElement, KtParameter>(PositioningStrategy.PARAMETER_DEFAULT_VALUE)
        val USELESS_VARARG_ON_PARAMETER by warning<FirSourceElement, KtParameter>()
    }

    group("Properties & accessors") {
        val ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS by error<FirSourceElement, KtModifierListOwner>(PositioningStrategy.MODALITY_MODIFIER) {
            parameter<FirMemberDeclaration>("property")
            parameter<FirMemberDeclaration>("containingClass") // TODO use FirClass instead of FirMemberDeclaration
        }
        val PRIVATE_PROPERTY_IN_INTERFACE by error<FirSourceElement, KtProperty>(PositioningStrategy.VISIBILITY_MODIFIER)

        val ABSTRACT_PROPERTY_WITH_INITIALIZER by error<FirSourceElement, KtExpression>()
        val PROPERTY_INITIALIZER_IN_INTERFACE by error<FirSourceElement, KtExpression>()
        val PROPERTY_WITH_NO_TYPE_NO_INITIALIZER by error<FirSourceElement, KtProperty>(PositioningStrategy.DECLARATION_SIGNATURE)

        val BACKING_FIELD_IN_INTERFACE by error<FirSourceElement, KtProperty>(PositioningStrategy.DECLARATION_SIGNATURE)
        val EXTENSION_PROPERTY_WITH_BACKING_FIELD by error<FirSourceElement, KtExpression>()
        val PROPERTY_INITIALIZER_NO_BACKING_FIELD by error<FirSourceElement, KtExpression>()

        val ABSTRACT_DELEGATED_PROPERTY by error<FirSourceElement, KtPropertyDelegate>()
        val DELEGATED_PROPERTY_IN_INTERFACE by error<FirSourceElement, KtPropertyDelegate>()
        // TODO: val ACCESSOR_FOR_DELEGATED_PROPERTY by error1<FirSourceElement, PsiElement, FirPropertyAccessorSymbol>()

        val ABSTRACT_PROPERTY_WITH_GETTER by error<FirSourceElement, KtPropertyAccessor>()
        val ABSTRACT_PROPERTY_WITH_SETTER by error<FirSourceElement, KtPropertyAccessor>()
        val PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY by error<FirSourceElement, PsiElement>()
        val PRIVATE_SETTER_FOR_OPEN_PROPERTY by error<FirSourceElement, PsiElement>()
        val EXPECTED_PRIVATE_DECLARATION by error<FirSourceElement, KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER)
    }

    group("Multi-platform projects") {
        val EXPECTED_DECLARATION_WITH_BODY by error<FirSourceElement, KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val EXPECTED_PROPERTY_INITIALIZER by error<FirSourceElement, KtExpression>()
        // TODO: need to cover `by` as well as delegate expression
        val EXPECTED_DELEGATED_PROPERTY by error<FirSourceElement, KtPropertyDelegate>()
    }

    group("Destructuring declaration") {
        val INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION by error<FirSourceElement, KtDestructuringDeclaration>()
        val COMPONENT_FUNCTION_MISSING by error<FirSourceElement, PsiElement> {
            parameter<Name>("missingFunctionName")
            parameter<ConeKotlinType>("destructingType")
        }
        val COMPONENT_FUNCTION_AMBIGUITY by error<FirSourceElement, PsiElement> {
            parameter<Name>("functionWithAmbiguityName")
            parameter<Collection<AbstractFirBasedSymbol<*>>>("candidates")
        }

        val COMPONENT_FUNCTION_ON_NULLABLE by error<FirSourceElement, KtExpression> {
            parameter<Name>("componentFunctionName")
        }

        // TODO: val COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH by ...
    }

    group("Control flow diagnostics") {
        val UNINITIALIZED_VARIABLE by error<FirSourceElement, PsiElement> {
            parameter<FirPropertySymbol>("variable")
        }
        val WRONG_INVOCATION_KIND by warning<FirSourceElement, PsiElement> {
            parameter<AbstractFirBasedSymbol<*>>("declaration")
            parameter<EventOccurrencesRange>("requiredRange")
            parameter<EventOccurrencesRange>("actualRange")
        }
        val LEAKED_IN_PLACE_LAMBDA by error<FirSourceElement, PsiElement> {
            parameter<AbstractFirBasedSymbol<*>>("lambda")
        }
        val WRONG_IMPLIES_CONDITION by warning<FirSourceElement, PsiElement>()
    }

    group("Nullability") {
        val UNSAFE_CALL by error<FirSourceElement, PsiElement>(PositioningStrategy.DOT_BY_SELECTOR) {
            parameter<ConeKotlinType>("receiverType")
        }
        val UNSAFE_IMPLICIT_INVOKE_CALL by error<FirSourceElement, PsiElement> {
            parameter<ConeKotlinType>("receiverType")
        }
        val UNSAFE_INFIX_CALL by error<FirSourceElement, KtExpression> {
            parameter<FirExpression>("lhs")
            parameter<String>("operator")
            parameter<FirExpression>("rhs")
        }
        val UNSAFE_OPERATOR_CALL by error<FirSourceElement, KtExpression> {
            parameter<FirExpression>("lhs")
            parameter<String>("operator")
            parameter<FirExpression>("rhs")
        }
        // TODO: val UNEXPECTED_SAFE_CALL by ...
    }

    group("When expressions") {
        val NO_ELSE_IN_WHEN by error<FirSourceElement, KtWhenExpression>(PositioningStrategy.WHEN_EXPRESSION) {
            parameter<List<WhenMissingCase>>("missingWhenCases")
        }
        val INVALID_IF_AS_EXPRESSION by error<FirSourceElement, KtIfExpression>(PositioningStrategy.IF_EXPRESSION)
    }

    group("Function contracts") {
        val ERROR_IN_CONTRACT_DESCRIPTION by error<FirSourceElement, KtElement> {
            parameter<String>("reason")
        }
    }

    group("Extended checkers") {
        val REDUNDANT_VISIBILITY_MODIFIER by warning<FirSourceElement, KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER)
        val REDUNDANT_MODALITY_MODIFIER by warning<FirSourceElement, KtModifierListOwner>(PositioningStrategy.MODALITY_MODIFIER)
        val REDUNDANT_RETURN_UNIT_TYPE by warning<FirSourceElement, PsiTypeElement>()
        val REDUNDANT_EXPLICIT_TYPE by warning<FirSourceElement, PsiElement>()
        val REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE by warning<FirSourceElement, PsiElement>()
        val CAN_BE_VAL by warning<FirSourceElement, KtDeclaration>(PositioningStrategy.VAL_OR_VAR_NODE)
        val CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT by warning<FirSourceElement, KtExpression>(PositioningStrategy.OPERATOR)
        val REDUNDANT_CALL_OF_CONVERSION_METHOD by warning<FirSourceElement, PsiElement>()
        val ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS by warning<FirSourceElement, KtExpression>(PositioningStrategy.OPERATOR)
        val EMPTY_RANGE by warning<FirSourceElement, PsiElement>()
        val REDUNDANT_SETTER_PARAMETER_TYPE by warning<FirSourceElement, PsiElement>()
        val UNUSED_VARIABLE by warning<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val ASSIGNED_VALUE_IS_NEVER_READ by warning<FirSourceElement, PsiElement>()
        val VARIABLE_INITIALIZER_IS_REDUNDANT by warning<FirSourceElement, PsiElement>()
        val VARIABLE_NEVER_READ by warning<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val USELESS_CALL_ON_NOT_NULL by warning<FirSourceElement, PsiElement>()
    }
}

private inline fun <reified P : PsiElement> DiagnosticListBuilder.exposedVisibilityError(
    positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT
) = error<FirSourceElement, P>(positioningStrategy) {
    parameter<FirEffectiveVisibility>("elementVisibility")
    parameter<FirMemberDeclaration>("restrictingDeclaration")
    parameter<FirEffectiveVisibility>("restrictingVisibility")
}
