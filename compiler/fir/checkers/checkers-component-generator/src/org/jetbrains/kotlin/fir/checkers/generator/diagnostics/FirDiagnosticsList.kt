/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty


@Suppress("UNUSED_VARIABLE", "LocalVariableName", "ClassName", "unused")
@OptIn(PrivateForInline::class)
object DIAGNOSTICS_LIST : DiagnosticList() {
    val MetaErrors by object : DiagnosticGroup("Meta-errors") {
        val UNSUPPORTED by error<FirSourceElement, PsiElement> {
            parameter<String>("unsupported")
        }
        val UNSUPPORTED_FEATURE by error<FirSourceElement, PsiElement> {
            parameter<Pair<LanguageFeature, LanguageVersionSettings>>("unsupportedFeature")
        }
    }

    val Miscellaneous by object : DiagnosticGroup("Miscellaneous") {
        val SYNTAX by error<FirSourceElement, PsiElement>()
        val OTHER_ERROR by error<FirSourceElement, PsiElement>()
    }

    val GENERAL_SYNTAX by object : DiagnosticGroup("General syntax") {
        val ILLEGAL_CONST_EXPRESSION by error<FirSourceElement, PsiElement>()
        val ILLEGAL_UNDERSCORE by error<FirSourceElement, PsiElement>()
        val EXPRESSION_REQUIRED by error<FirSourceElement, PsiElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
        val BREAK_OR_CONTINUE_OUTSIDE_A_LOOP by error<FirSourceElement, PsiElement>()
        val NOT_A_LOOP_LABEL by error<FirSourceElement, PsiElement>()
        val VARIABLE_EXPECTED by error<FirSourceElement, PsiElement>()
        val DELEGATION_IN_INTERFACE by error<FirSourceElement, PsiElement>()
        val NESTED_CLASS_NOT_ALLOWED by error<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<String>("declaration")
        }
        val INCORRECT_CHARACTER_LITERAL by error<FirSourceElement, PsiElement>()
        val EMPTY_CHARACTER_LITERAL by error<FirSourceElement, PsiElement>()
        val TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL by error<FirSourceElement, PsiElement>()
        val ILLEGAL_ESCAPE by error<FirSourceElement, PsiElement>()
        val INT_LITERAL_OUT_OF_RANGE by error<FirSourceElement, PsiElement>()
        val FLOAT_LITERAL_OUT_OF_RANGE by error<FirSourceElement, PsiElement>()
        val WRONG_LONG_SUFFIX by error<FirSourceElement, KtElement>(PositioningStrategy.LONG_LITERAL_SUFFIX)
        val DIVISION_BY_ZERO by warning<FirSourceElement, KtExpression>()
    }

    val UNRESOLVED by object : DiagnosticGroup("Unresolved") {
        val HIDDEN by error<FirSourceElement, PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<AbstractFirBasedSymbol<*>>("hidden")
        }
        val UNRESOLVED_REFERENCE by error<FirSourceElement, PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("reference")
        }
        val UNRESOLVED_LABEL by error<FirSourceElement, PsiElement>()
        val DESERIALIZATION_ERROR by error<FirSourceElement, PsiElement>()
        val ERROR_FROM_JAVA_RESOLUTION by error<FirSourceElement, PsiElement>()
        val UNKNOWN_CALLABLE_KIND by error<FirSourceElement, PsiElement>()
        val MISSING_STDLIB_CLASS by error<FirSourceElement, PsiElement>()
        val NO_THIS by error<FirSourceElement, PsiElement>()
    }

    val SUPER by object : DiagnosticGroup("Super") {
        val SUPER_IS_NOT_AN_EXPRESSION by error<FirSourceElement, PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val SUPER_NOT_AVAILABLE by error<FirSourceElement, PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val ABSTRACT_SUPER_CALL by error<FirSourceElement, PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val INSTANCE_ACCESS_BEFORE_SUPER_CALL by error<FirSourceElement, PsiElement> {
            parameter<String>("target")
        }
    }

    val SUPERTYPES by object : DiagnosticGroup("Supertypes") {
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
        val SUPERTYPE_NOT_A_CLASS_OR_INTERFACE by error<FirSourceElement, KtElement> {
            parameter<String>("reason")
        }
    }

    val CONSTRUCTOR_PROBLEMS by object : DiagnosticGroup("Constructor problems") {
        val CONSTRUCTOR_IN_OBJECT by error<FirSourceElement, KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val CONSTRUCTOR_IN_INTERFACE by error<FirSourceElement, KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val NON_PRIVATE_CONSTRUCTOR_IN_ENUM by error<FirSourceElement, PsiElement>()
        val NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED by error<FirSourceElement, PsiElement>()
        val CYCLIC_CONSTRUCTOR_DELEGATION_CALL by warning<FirSourceElement, PsiElement>()
        val PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED by warning<FirSourceElement, PsiElement>(PositioningStrategy.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)

        // TODO: change it to KtSuperTypeEntry when possible (after re-targeter implementation)
        val SUPERTYPE_NOT_INITIALIZED by error<FirSourceElement, KtTypeReference>()
        val SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR by error<FirSourceElement, PsiElement>()
        val DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR by warning<FirSourceElement, PsiElement>()
        val PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS by error<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val EXPLICIT_DELEGATION_CALL_REQUIRED by error<FirSourceElement, PsiElement>(PositioningStrategy.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)
        val SEALED_CLASS_CONSTRUCTOR_CALL by error<FirSourceElement, PsiElement>()

        // TODO: Consider creating a parameter list position strategy and report on the parameter list instead
        val DATA_CLASS_WITHOUT_PARAMETERS by error<FirSourceElement, KtPrimaryConstructor>()
        val DATA_CLASS_VARARG_PARAMETER by error<FirSourceElement, KtParameter>()
        val DATA_CLASS_NOT_PROPERTY_PARAMETER by error<FirSourceElement, KtParameter>()
    }

    val ANNOTATIONS by object : DiagnosticGroup("Annotations") {
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
        val SUPERTYPES_FOR_ANNOTATION_CLASS by error<FirSourceElement, KtClass>(PositioningStrategy.SUPERTYPES_LIST)
    }

    val EXPOSED_VISIBILITY by object : DiagnosticGroup("Exposed visibility") {
        val EXPOSED_TYPEALIAS_EXPANDED_TYPE by exposedVisibilityError<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val EXPOSED_FUNCTION_RETURN_TYPE by exposedVisibilityError<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val EXPOSED_RECEIVER_TYPE by exposedVisibilityError<KtTypeReference>()
        val EXPOSED_PROPERTY_TYPE by exposedVisibilityError<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR by exposedVisibilityWarning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val EXPOSED_PARAMETER_TYPE by exposedVisibilityError<KtParameter>(/* // NB: for parameter FE 1.0 reports not on a name for some reason */)
        val EXPOSED_SUPER_INTERFACE by exposedVisibilityError<KtTypeReference>()
        val EXPOSED_SUPER_CLASS by exposedVisibilityError<KtTypeReference>()
        val EXPOSED_TYPE_PARAMETER_BOUND by exposedVisibilityError<KtTypeReference>()
    }

    val MODIFIERS by object : DiagnosticGroup("Modifiers") {
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
        val WRONG_MODIFIER_TARGET by error<FirSourceElement, PsiElement> {
            parameter<KtModifierKeywordToken>("modifier")
            parameter<String>("target")
        }
    }

    val INLINE_CLASSES by object : DiagnosticGroup("Inline classes") {
        val INLINE_CLASS_NOT_TOP_LEVEL by error<FirSourceElement, KtDeclaration>(PositioningStrategy.INLINE_OR_VALUE_MODIFIER)
        val INLINE_CLASS_NOT_FINAL by error<FirSourceElement, KtDeclaration>(PositioningStrategy.MODALITY_MODIFIER)
        val ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_INLINE_CLASS by error<FirSourceElement, KtDeclaration>(PositioningStrategy.INLINE_OR_VALUE_MODIFIER)
        val INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE by error<FirSourceElement, KtElement>()
        val INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER by error<FirSourceElement, KtParameter>()
        val PROPERTY_WITH_BACKING_FIELD_INSIDE_INLINE_CLASS by error<FirSourceElement, KtProperty>(PositioningStrategy.DECLARATION_SIGNATURE)
        val DELEGATED_PROPERTY_INSIDE_INLINE_CLASS by error<FirSourceElement, PsiElement>()
        val INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE by error<FirSourceElement, KtTypeReference> {
            parameter<ConeKotlinType>("type")
        }
        val INLINE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION by error<FirSourceElement, PsiElement>()
        val INLINE_CLASS_CANNOT_EXTEND_CLASSES by error<FirSourceElement, KtTypeReference>()
        val INLINE_CLASS_CANNOT_BE_RECURSIVE by error<FirSourceElement, KtTypeReference>()
        val RESERVED_MEMBER_INSIDE_INLINE_CLASS by error<FirSourceElement, KtFunction>(PositioningStrategy.DECLARATION_NAME) {
            parameter<String>("name")
        }
        val SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS by error<FirSourceElement, PsiElement>()
        val INNER_CLASS_INSIDE_INLINE_CLASS by error<FirSourceElement, KtDeclaration>(PositioningStrategy.INNER_MODIFIER)
        val VALUE_CLASS_CANNOT_BE_CLONEABLE by error<FirSourceElement, KtDeclaration>(PositioningStrategy.INLINE_OR_VALUE_MODIFIER)
    }

    val APPLICABILITY by object : DiagnosticGroup("Applicability") {
        val NONE_APPLICABLE by error<FirSourceElement, PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Collection<AbstractFirBasedSymbol<*>>>("candidates")
        }

        val INAPPLICABLE_CANDIDATE by error<FirSourceElement, PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<AbstractFirBasedSymbol<*>>("candidate")
        }

        val ARGUMENT_TYPE_MISMATCH by error<FirSourceElement, PsiElement> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }

        val INAPPLICABLE_LATEINIT_MODIFIER by error<FirSourceElement, KtModifierListOwner>(PositioningStrategy.LATEINIT_MODIFIER) {
            parameter<String>("reason")
        }

        val VARARG_OUTSIDE_PARENTHESES by error<FirSourceElement, KtExpression>()

        val NAMED_ARGUMENTS_NOT_ALLOWED by error<FirSourceElement, KtValueArgument>(PositioningStrategy.NAME_OF_NAMED_ARGUMENT) {
            parameter<ForbiddenNamedArgumentsTarget>("forbiddenNamedArgumentsTarget")
        }

        val NON_VARARG_SPREAD by error<FirSourceElement, LeafPsiElement>()
        val ARGUMENT_PASSED_TWICE by error<FirSourceElement, KtValueArgument>(PositioningStrategy.NAME_OF_NAMED_ARGUMENT)
        val TOO_MANY_ARGUMENTS by error<FirSourceElement, PsiElement> {
            parameter<FirCallableDeclaration<*>>("function")
        }
        val NO_VALUE_FOR_PARAMETER by error<FirSourceElement, KtElement>(PositioningStrategy.VALUE_ARGUMENTS) {
            parameter<FirValueParameter>("violatedParameter")
        }

        val NAMED_PARAMETER_NOT_FOUND by error<FirSourceElement, KtValueArgument>(PositioningStrategy.NAME_OF_NAMED_ARGUMENT) {
            parameter<String>("name")
        }
    }

    val AMBIGUITY by object : DiagnosticGroup("Ambiguity") {
        val OVERLOAD_RESOLUTION_AMBIGUITY by error<FirSourceElement, PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Collection<AbstractFirBasedSymbol<*>>>("candidates")
        }
        val ASSIGN_OPERATOR_AMBIGUITY by error<FirSourceElement, PsiElement> {
            parameter<Collection<AbstractFirBasedSymbol<*>>>("candidates")
        }
    }

    val TYPES_AND_TYPE_PARAMETERS by object : DiagnosticGroup("Types & type parameters") {
        val TYPE_MISMATCH by error<FirSourceElement, PsiElement> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }
        val RECURSION_IN_IMPLICIT_TYPES by error<FirSourceElement, PsiElement>()
        val INFERENCE_ERROR by error<FirSourceElement, PsiElement>()
        val PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT by error<FirSourceElement, PsiElement>()
        val UPPER_BOUND_VIOLATED by error<FirSourceElement, PsiElement> {
            parameter<ConeKotlinType>("upperBound")
        }
        val TYPE_ARGUMENTS_NOT_ALLOWED by error<FirSourceElement, PsiElement>()
        val WRONG_NUMBER_OF_TYPE_ARGUMENTS by error<FirSourceElement, PsiElement> {
            parameter<Int>("expectedCount")
            parameter<FirClassLikeSymbol<*>>("classifier")
        }
        val NO_TYPE_ARGUMENTS_ON_RHS by error<FirSourceElement, PsiElement> {
            parameter<Int>("expectedCount")
            parameter<FirClassLikeSymbol<*>>("classifier")
        }
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

        val KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE by error<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }

        val TYPE_PARAMETER_AS_REIFIED by error<FirSourceElement, PsiElement> {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }

        val FINAL_UPPER_BOUND by warning<FirSourceElement, KtTypeReference> {
            parameter<ConeKotlinType>("type")
        }

        val UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE by error<FirSourceElement, KtTypeReference>()

        val BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER by error<FirSourceElement, KtElement>()

        val ONLY_ONE_CLASS_BOUND_ALLOWED by error<FirSourceElement, KtTypeReference>()

        val REPEATED_BOUND by error<FirSourceElement, KtTypeReference>()

        val CONFLICTING_UPPER_BOUNDS by error<FirSourceElement, KtNamedDeclaration> {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }

        val NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER by error<FirSourceElement, KtSimpleNameExpression> {
            parameter<Name>("typeParameterName")
            parameter<FirDeclaration>("typeParametersOwner")
        }

        val BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED by error<FirSourceElement, KtTypeReference>()

        val REIFIED_TYPE_PARAMETER_NO_INLINE by error<FirSourceElement, KtTypeParameter>(PositioningStrategy.REIFIED_MODIFIER)

        val TYPE_PARAMETERS_NOT_ALLOWED by error<FirSourceElement, KtDeclaration>()

        val TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER by error<FirSourceElement, KtTypeParameter>()

        val RETURN_TYPE_MISMATCH by error<FirSourceElement, KtExpression>(PositioningStrategy.WHOLE_ELEMENT) {
            parameter<ConeKotlinType>("expected")
            parameter<ConeKotlinType>("actual")
        }

        val CYCLIC_GENERIC_UPPER_BOUND by error<FirSourceElement, PsiElement>()

        val DEPRECATED_TYPE_PARAMETER_SYNTAX by error<FirSourceElement, KtTypeParameterList>()

        val MISPLACED_TYPE_PARAMETER_CONSTRAINTS by warning<FirSourceElement, KtTypeParameter>()
        
        val DYNAMIC_UPPER_BOUND by error<FirSourceElement, KtTypeReference>()
    }

    val REFLECTION by object : DiagnosticGroup("Reflection") {
        val EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED by error<FirSourceElement, KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirCallableDeclaration<*>>("referencedDeclaration")
        }
        val CALLABLE_REFERENCE_LHS_NOT_A_CLASS by error<FirSourceElement, KtExpression>()
        val CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR by error<FirSourceElement, KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED)

        val CLASS_LITERAL_LHS_NOT_A_CLASS by error<FirSourceElement, KtExpression>()
        val NULLABLE_TYPE_IN_CLASS_LITERAL_LHS by error<FirSourceElement, KtExpression>()
        val EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS by error<FirSourceElement, PsiElement> {
            parameter<ConeKotlinType>("lhsType")
        }
    }

    val OVERRIDES by object : DiagnosticGroup("overrides") {
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

        val ABSTRACT_MEMBER_NOT_IMPLEMENTED by error<FirSourceElement, KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClass<*>>("classOrObject")
            parameter<FirCallableDeclaration<*>>("missingDeclaration")
        }
        val ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED by error<FirSourceElement, KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClass<*>>("classOrObject")
            parameter<FirCallableDeclaration<*>>("missingDeclaration")
        }
        val INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER by error<FirSourceElement, KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClass<*>>("classOrObject")
            parameter<FirCallableDeclaration<*>>("invisibleDeclaration")
        }
        val INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_WARNING by warning<FirSourceElement, KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClass<*>>("classOrObject")
            parameter<FirCallableDeclaration<*>>("invisibleDeclaration")
        }
        val MANY_IMPL_MEMBER_NOT_IMPLEMENTED by error<FirSourceElement, KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClass<*>>("classOrObject")
            parameter<FirCallableDeclaration<*>>("missingDeclaration")
        }
        val MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED by error<FirSourceElement, KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClass<*>>("classOrObject")
            parameter<FirCallableDeclaration<*>>("missingDeclaration")
        }
        val OVERRIDING_FINAL_MEMBER_BY_DELEGATION by error<FirSourceElement, KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableDeclaration<*>>("delegatedDeclaration")
            parameter<FirCallableDeclaration<*>>("overriddenDeclaration")
        }
        val DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE by warning<FirSourceElement, KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableDeclaration<*>>("delegatedDeclaration")
            parameter<FirCallableDeclaration<*>>("overriddenDeclaration")
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
        val NON_FINAL_MEMBER_IN_FINAL_CLASS by warning<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.OPEN_MODIFIER)
        val NON_FINAL_MEMBER_IN_OBJECT by warning<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.OPEN_MODIFIER)
    }

    val REDECLARATIONS by object : DiagnosticGroup("Redeclarations") {
        val MANY_COMPANION_OBJECTS by error<FirSourceElement, KtObjectDeclaration>(PositioningStrategy.COMPANION_OBJECT)
        val CONFLICTING_OVERLOADS by error<FirSourceElement, PsiElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<Collection<AbstractFirBasedSymbol<*>>>("conflictingOverloads")
        }
        val REDECLARATION by error<FirSourceElement, PsiElement> {
            parameter<Collection<AbstractFirBasedSymbol<*>>>("conflictingDeclarations")
        }
        val METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE by error<FirSourceElement, PsiElement>()
    }

    val INVALID_LOCAL_DECLARATIONS by object : DiagnosticGroup("Invalid local declarations") {
        val LOCAL_OBJECT_NOT_ALLOWED by error<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<Name>("objectName")
        }
        val LOCAL_INTERFACE_NOT_ALLOWED by error<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<Name>("interfaceName")
        }
    }

    val FUNCTIONS by object : DiagnosticGroup("Functions") {
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
        val MULTIPLE_VARARG_PARAMETERS by error<FirSourceElement, KtParameter>(PositioningStrategy.PARAMETER_VARARG_MODIFIER)
        val FORBIDDEN_VARARG_PARAMETER_TYPE by error<FirSourceElement, KtParameter>(PositioningStrategy.PARAMETER_VARARG_MODIFIER) {
            parameter<ConeKotlinType>("varargParameterType")
        }
        val VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION by error<FirSourceElement, KtParameter>()
        val CANNOT_INFER_PARAMETER_TYPE by error<FirSourceElement, KtParameter>()
    }

    val FUN_INTERFACES by object : DiagnosticGroup("Fun interfaces") {
        val FUN_INTERFACE_CONSTRUCTOR_REFERENCE by error<FirSourceElement, KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED)
    }

    val PROPERTIES_AND_ACCESSORS by object : DiagnosticGroup("Properties & accessors") {
        val ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS by error<FirSourceElement, KtModifierListOwner>(PositioningStrategy.MODALITY_MODIFIER) {
            parameter<FirMemberDeclaration>("property")
            parameter<FirMemberDeclaration>("containingClass") // TODO use FirClass instead of FirMemberDeclaration
        }
        val PRIVATE_PROPERTY_IN_INTERFACE by error<FirSourceElement, KtProperty>(PositioningStrategy.VISIBILITY_MODIFIER)

        val ABSTRACT_PROPERTY_WITH_INITIALIZER by error<FirSourceElement, KtExpression>()
        val PROPERTY_INITIALIZER_IN_INTERFACE by error<FirSourceElement, KtExpression>()
        val PROPERTY_WITH_NO_TYPE_NO_INITIALIZER by error<FirSourceElement, KtProperty>(PositioningStrategy.DECLARATION_SIGNATURE)

        val MUST_BE_INITIALIZED by error<FirSourceElement, KtProperty>(PositioningStrategy.DECLARATION_SIGNATURE)
        val MUST_BE_INITIALIZED_OR_BE_ABSTRACT by error<FirSourceElement, KtProperty>(PositioningStrategy.DECLARATION_SIGNATURE)
        val EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT by error<FirSourceElement, KtProperty>(PositioningStrategy.DECLARATION_SIGNATURE)
        val UNNECESSARY_LATEINIT by warning<FirSourceElement, KtProperty>(PositioningStrategy.LATEINIT_MODIFIER)

        val BACKING_FIELD_IN_INTERFACE by error<FirSourceElement, KtProperty>(PositioningStrategy.DECLARATION_SIGNATURE)
        val EXTENSION_PROPERTY_WITH_BACKING_FIELD by error<FirSourceElement, KtExpression>()
        val PROPERTY_INITIALIZER_NO_BACKING_FIELD by error<FirSourceElement, KtExpression>()

        val ABSTRACT_DELEGATED_PROPERTY by error<FirSourceElement, KtPropertyDelegate>()
        val DELEGATED_PROPERTY_IN_INTERFACE by error<FirSourceElement, KtPropertyDelegate>()
        // TODO: val ACCESSOR_FOR_DELEGATED_PROPERTY by error1<FirSourceElement, PsiElement, FirPropertyAccessorSymbol>()

        val ABSTRACT_PROPERTY_WITH_GETTER by error<FirSourceElement, KtPropertyAccessor>()
        val ABSTRACT_PROPERTY_WITH_SETTER by error<FirSourceElement, KtPropertyAccessor>()
        val PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY by error<FirSourceElement, KtModifierListOwner>(PositioningStrategy.PRIVATE_MODIFIER)
        val PRIVATE_SETTER_FOR_OPEN_PROPERTY by error<FirSourceElement, KtModifierListOwner>(PositioningStrategy.PRIVATE_MODIFIER)
        val EXPECTED_PRIVATE_DECLARATION by error<FirSourceElement, KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER)
        val VAL_WITH_SETTER by error<FirSourceElement, KtPropertyAccessor>()
        val CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT by error<FirSourceElement, KtProperty>(PositioningStrategy.CONST_MODIFIER)
        val CONST_VAL_WITH_GETTER by error<FirSourceElement, KtProperty>()
        val CONST_VAL_WITH_DELEGATE by error<FirSourceElement, KtPropertyDelegate>()
        val TYPE_CANT_BE_USED_FOR_CONST_VAL by error<FirSourceElement, KtProperty>(PositioningStrategy.CONST_MODIFIER) {
            parameter<ConeKotlinType>("constValType")
        }
        val CONST_VAL_WITHOUT_INITIALIZER by error<FirSourceElement, KtProperty>(PositioningStrategy.CONST_MODIFIER)
        val CONST_VAL_WITH_NON_CONST_INITIALIZER by error<FirSourceElement, KtExpression>()
        val WRONG_SETTER_PARAMETER_TYPE by error<FirSourceElement, KtTypeReference> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }
        val INITIALIZER_TYPE_MISMATCH by error<FirSourceElement, KtProperty>(PositioningStrategy.ASSIGNMENT_VALUE) {
            parameter<ConeKotlinType>("expected")
            parameter<ConeKotlinType>("actual")
        }
    }

    val MPP_PROJECTS by object : DiagnosticGroup("Multi-platform projects") {
        val EXPECTED_DECLARATION_WITH_BODY by error<FirSourceElement, KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val EXPECTED_PROPERTY_INITIALIZER by error<FirSourceElement, KtExpression>()

        // TODO: need to cover `by` as well as delegate expression
        val EXPECTED_DELEGATED_PROPERTY by error<FirSourceElement, KtPropertyDelegate>()
        val EXPECTED_LATEINIT_PROPERTY by error<FirSourceElement, KtModifierListOwner>(PositioningStrategy.LATEINIT_MODIFIER)
    }

    val DESTRUCTING_DECLARATION by object : DiagnosticGroup("Destructuring declaration") {
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
        val COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH by error<FirSourceElement, KtExpression> {
            parameter<Name>("componentFunctionName")
            parameter<ConeKotlinType>("destructingType")
            parameter<ConeKotlinType>("expectedType")
        }
    }

    val CONTROL_FLOW by object : DiagnosticGroup("Control flow diagnostics") {
        val UNINITIALIZED_VARIABLE by error<FirSourceElement, KtSimpleNameExpression> {
            parameter<FirPropertySymbol>("variable")
        }
        val UNINITIALIZED_ENUM_ENTRY by error<FirSourceElement, KtSimpleNameExpression> {
            parameter<FirVariableSymbol<FirEnumEntry>>("enumEntry")
        }
        val UNINITIALIZED_ENUM_COMPANION by error<FirSourceElement, KtSimpleNameExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirRegularClassSymbol>("enumClass")
        }
        val VAL_REASSIGNMENT by error<FirSourceElement, KtExpression> {
            parameter<FirVariableSymbol<*>>("variable")
        }
        val VAL_REASSIGNMENT_VIA_BACKING_FIELD by warning<FirSourceElement, KtExpression> {
            parameter<FirPropertySymbol>("property")
        }
        val VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR by error<FirSourceElement, KtExpression> {
            parameter<FirPropertySymbol>("property")
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

    val NULLABILITY by object : DiagnosticGroup("Nullability") {
        val UNSAFE_CALL by error<FirSourceElement, PsiElement>(PositioningStrategy.DOT_BY_QUALIFIED) {
            parameter<ConeKotlinType>("receiverType")
        }
        val UNSAFE_IMPLICIT_INVOKE_CALL by error<FirSourceElement, PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<ConeKotlinType>("receiverType")
        }
        val UNSAFE_INFIX_CALL by error<FirSourceElement, KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirExpression>("lhs")
            parameter<String>("operator")
            parameter<FirExpression>("rhs")
        }
        val UNSAFE_OPERATOR_CALL by error<FirSourceElement, KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirExpression>("lhs")
            parameter<String>("operator")
            parameter<FirExpression>("rhs")
        }
        val UNNECESSARY_SAFE_CALL by warning<FirSourceElement, PsiElement>(PositioningStrategy.SAFE_ACCESS) {
            parameter<ConeKotlinType>("receiverType")
        }
        val UNEXPECTED_SAFE_CALL by error<FirSourceElement, PsiElement>(PositioningStrategy.SAFE_ACCESS)
        val UNNECESSARY_NOT_NULL_ASSERTION by warning<FirSourceElement, KtExpression>(PositioningStrategy.OPERATOR) {
            parameter<ConeKotlinType>("receiverType")
        }
        val NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION by warning<FirSourceElement, KtExpression>(PositioningStrategy.OPERATOR)
        val NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE by warning<FirSourceElement, KtExpression>(PositioningStrategy.OPERATOR)
    }

    val WHEN_EXPRESSIONS by object : DiagnosticGroup("When expressions") {
        val NO_ELSE_IN_WHEN by error<FirSourceElement, KtWhenExpression>(PositioningStrategy.WHEN_EXPRESSION) {
            parameter<List<WhenMissingCase>>("missingWhenCases")
        }
        val INVALID_IF_AS_EXPRESSION by error<FirSourceElement, KtIfExpression>(PositioningStrategy.IF_EXPRESSION)
    }

    val CONTEXT_TRACKING by object : DiagnosticGroup("Context tracking") {
        val TYPE_PARAMETER_IS_NOT_AN_EXPRESSION by error<FirSourceElement, KtSimpleNameExpression> {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }
        val TYPE_PARAMETER_ON_LHS_OF_DOT by error<FirSourceElement, KtSimpleNameExpression> {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }
    }

    val FUNCTION_CONTRACTS by object : DiagnosticGroup("Function contracts") {
        val ERROR_IN_CONTRACT_DESCRIPTION by error<FirSourceElement, KtElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED) {
            parameter<String>("reason")
        }
    }

    val CONVENTIONS by object : DiagnosticGroup("Conventions") {
        val NO_GET_METHOD by error<FirSourceElement, KtArrayAccessExpression>(PositioningStrategy.ARRAY_ACCESS)
        val NO_SET_METHOD by error<FirSourceElement, KtArrayAccessExpression>(PositioningStrategy.ARRAY_ACCESS)
    }

    val TYPE_ALIAS by object : DiagnosticGroup("Type alias") {
        val TOPLEVEL_TYPEALIASES_ONLY by error<FirSourceElement, KtTypeAlias>()
    }

    val EXTENDED_CHECKERS by object : DiagnosticGroup("Extended checkers") {
        val REDUNDANT_VISIBILITY_MODIFIER by warning<FirSourceElement, KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER)
        val REDUNDANT_MODALITY_MODIFIER by warning<FirSourceElement, KtModifierListOwner>(PositioningStrategy.MODALITY_MODIFIER)
        val REDUNDANT_RETURN_UNIT_TYPE by warning<FirSourceElement, PsiTypeElement>()
        val REDUNDANT_EXPLICIT_TYPE by warning<FirSourceElement, PsiElement>()
        val REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE by warning<FirSourceElement, PsiElement>()
        val CAN_BE_VAL by warning<FirSourceElement, KtDeclaration>(PositioningStrategy.VAL_OR_VAR_NODE)
        val CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT by warning<FirSourceElement, KtExpression>(PositioningStrategy.OPERATOR)
        val REDUNDANT_CALL_OF_CONVERSION_METHOD by warning<FirSourceElement, PsiElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
        val ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS by warning<FirSourceElement, KtExpression>(PositioningStrategy.OPERATOR)
        val EMPTY_RANGE by warning<FirSourceElement, PsiElement>()
        val REDUNDANT_SETTER_PARAMETER_TYPE by warning<FirSourceElement, PsiElement>()
        val UNUSED_VARIABLE by warning<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val ASSIGNED_VALUE_IS_NEVER_READ by warning<FirSourceElement, PsiElement>()
        val VARIABLE_INITIALIZER_IS_REDUNDANT by warning<FirSourceElement, PsiElement>()
        val VARIABLE_NEVER_READ by warning<FirSourceElement, KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val USELESS_CALL_ON_NOT_NULL by warning<FirSourceElement, PsiElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
    }

    val RETURNS by object : DiagnosticGroup("Returns") {
        val RETURN_NOT_ALLOWED by error<FirSourceElement, KtReturnExpression>(PositioningStrategy.RETURN_WITH_LABEL)
        val RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY by error<FirSourceElement, KtReturnExpression>(PositioningStrategy.RETURN_WITH_LABEL)
    }
}

private val exposedVisibilityDiagnosticInit: DiagnosticBuilder.() -> Unit = {
    parameter<EffectiveVisibility>("elementVisibility")
    parameter<FirMemberDeclaration>("restrictingDeclaration")
    parameter<EffectiveVisibility>("restrictingVisibility")
}

private inline fun <reified P : PsiElement> DiagnosticGroup.exposedVisibilityError(
    positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT
): PropertyDelegateProvider<Any?, ReadOnlyProperty<DiagnosticGroup, DiagnosticData>> {
    return error<FirSourceElement, P>(positioningStrategy, exposedVisibilityDiagnosticInit)
}

private inline fun <reified P : PsiElement> DiagnosticGroup.exposedVisibilityWarning(
    positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT
): PropertyDelegateProvider<Any?, ReadOnlyProperty<DiagnosticGroup, DiagnosticData>> {
    return warning<FirSourceElement, P>(positioningStrategy, exposedVisibilityDiagnosticInit)
}
