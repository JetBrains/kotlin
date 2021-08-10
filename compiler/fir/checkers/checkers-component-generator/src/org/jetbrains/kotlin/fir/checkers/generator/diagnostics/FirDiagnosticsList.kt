/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.*
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.Incompatible
import org.jetbrains.kotlin.types.Variance
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

@Suppress("UNUSED_VARIABLE", "LocalVariableName", "ClassName", "unused")
@OptIn(PrivateForInline::class)
object DIAGNOSTICS_LIST : DiagnosticList("FirErrors") {
    val MetaErrors by object : DiagnosticGroup("Meta-errors") {
        val UNSUPPORTED by error<PsiElement> {
            parameter<String>("unsupported")
        }
        val UNSUPPORTED_FEATURE by error<PsiElement> {
            parameter<Pair<LanguageFeature, LanguageVersionSettings>>("unsupportedFeature")
        }
        val NEW_INFERENCE_ERROR by error<PsiElement> {
            parameter<String>("error")
        }
    }

    val Miscellaneous by object : DiagnosticGroup("Miscellaneous") {
        val SYNTAX by error<PsiElement>()
        val OTHER_ERROR by error<PsiElement>()
    }

    val GENERAL_SYNTAX by object : DiagnosticGroup("General syntax") {
        val ILLEGAL_CONST_EXPRESSION by error<PsiElement>()
        val ILLEGAL_UNDERSCORE by error<PsiElement>()
        val EXPRESSION_EXPECTED by error<PsiElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
        val ASSIGNMENT_IN_EXPRESSION_CONTEXT by error<KtBinaryExpression>()
        val BREAK_OR_CONTINUE_OUTSIDE_A_LOOP by error<PsiElement>()
        val NOT_A_LOOP_LABEL by error<PsiElement>()
        val BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY by error<KtExpressionWithLabel>()
        val VARIABLE_EXPECTED by error<PsiElement>(PositioningStrategy.ASSIGNMENT_LHS)
        val DELEGATION_IN_INTERFACE by error<PsiElement>()
        val DELEGATION_NOT_TO_INTERFACE by error<PsiElement>()
        val NESTED_CLASS_NOT_ALLOWED by error<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<String>("declaration")
        }
        val INCORRECT_CHARACTER_LITERAL by error<PsiElement>()
        val EMPTY_CHARACTER_LITERAL by error<PsiElement>()
        val TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL by error<PsiElement>()
        val ILLEGAL_ESCAPE by error<PsiElement>()
        val INT_LITERAL_OUT_OF_RANGE by error<PsiElement>()
        val FLOAT_LITERAL_OUT_OF_RANGE by error<PsiElement>()
        val WRONG_LONG_SUFFIX by error<KtElement>(PositioningStrategy.LONG_LITERAL_SUFFIX)
        val DIVISION_BY_ZERO by warning<KtExpression>()
        val VAL_OR_VAR_ON_LOOP_PARAMETER by warning<KtParameter>(PositioningStrategy.VAL_OR_VAR_NODE) {
            parameter<KtKeywordToken>("valOrVar")
        }
        val VAL_OR_VAR_ON_FUN_PARAMETER by warning<KtParameter>(PositioningStrategy.VAL_OR_VAR_NODE) {
            parameter<KtKeywordToken>("valOrVar")
        }
        val VAL_OR_VAR_ON_CATCH_PARAMETER by warning<KtParameter>(PositioningStrategy.VAL_OR_VAR_NODE) {
            parameter<KtKeywordToken>("valOrVar")
        }
        val VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER by warning<KtParameter>(PositioningStrategy.VAL_OR_VAR_NODE) {
            parameter<KtKeywordToken>("valOrVar")
        }
    }

    val UNRESOLVED by object : DiagnosticGroup("Unresolved") {
        val INVISIBLE_REFERENCE by error<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("reference")
        }
        val UNRESOLVED_REFERENCE by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("reference")
        }
        val UNRESOLVED_LABEL by error<PsiElement>(PositioningStrategy.LABEL)
        val DESERIALIZATION_ERROR by error<PsiElement>()
        val ERROR_FROM_JAVA_RESOLUTION by error<PsiElement>()
        val MISSING_STDLIB_CLASS by error<PsiElement>()
        val NO_THIS by error<PsiElement>()

        val DEPRECATION_ERROR by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Symbol>("reference")
            parameter<String>("message")
        }

        val DEPRECATION by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Symbol>("reference")
            parameter<String>("message")
        }

        val UNRESOLVED_REFERENCE_WRONG_RECEIVER by error<PsiElement> {
            parameter<Collection<Symbol>>("candidates")
        }
    }

    val CALL_RESOLUTION by object : DiagnosticGroup("Call resolution") {
        val CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS by error<KtExpression>()
        val FUNCTION_CALL_EXPECTED by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("functionName")
            parameter<Boolean>("hasValueParameters")
        }
        val ILLEGAL_SELECTOR by error<PsiElement>()
        val NO_RECEIVER_ALLOWED by error<PsiElement>()
        val FUNCTION_EXPECTED by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("expression")
            parameter<ConeKotlinType>("type")
        }
        val RESOLUTION_TO_CLASSIFIER by error<PsiElement> {
            parameter<FirRegularClassSymbol>("classSymbol")
        }
    }

    val SUPER by object : DiagnosticGroup("Super") {
        val SUPER_IS_NOT_AN_EXPRESSION by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val SUPER_NOT_AVAILABLE by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val ABSTRACT_SUPER_CALL by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val INSTANCE_ACCESS_BEFORE_SUPER_CALL by error<PsiElement> {
            parameter<String>("target")
        }
    }

    val SUPERTYPES by object : DiagnosticGroup("Supertypes") {
        val NOT_A_SUPERTYPE by error<PsiElement>()
        val TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER by warning<KtElement>()
        val SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE by error<PsiElement>()
        val QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE by error<KtTypeReference> {
            parameter<Symbol>("otherSuperType")
        }
        val SUPERTYPE_INITIALIZED_IN_INTERFACE by error<KtTypeReference>()
        val INTERFACE_WITH_SUPERCLASS by error<KtTypeReference>()
        val FINAL_SUPERTYPE by error<KtTypeReference>()
        val CLASS_CANNOT_BE_EXTENDED_DIRECTLY by error<KtTypeReference> {
            parameter<FirRegularClassSymbol>("classSymbol")
        }
        val SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE by error<KtTypeReference>()
        val SINGLETON_IN_SUPERTYPE by error<KtTypeReference>()
        val NULLABLE_SUPERTYPE by error<KtTypeReference>(PositioningStrategy.QUESTION_MARK_BY_TYPE)
        val MANY_CLASSES_IN_SUPERTYPE_LIST by error<KtTypeReference>()
        val SUPERTYPE_APPEARS_TWICE by error<KtTypeReference>()
        val CLASS_IN_SUPERTYPE_FOR_ENUM by error<KtTypeReference>()
        val SEALED_SUPERTYPE by error<KtTypeReference>()
        val SEALED_SUPERTYPE_IN_LOCAL_CLASS by error<KtTypeReference>()
        val SEALED_INHERITOR_IN_DIFFERENT_PACKAGE by error<KtTypeReference>()
        val SEALED_INHERITOR_IN_DIFFERENT_MODULE by error<KtTypeReference>()
        val CLASS_INHERITS_JAVA_SEALED_CLASS by error<KtTypeReference>()
        val SUPERTYPE_NOT_A_CLASS_OR_INTERFACE by error<KtElement> {
            parameter<String>("reason")
        }
        val CYCLIC_INHERITANCE_HIERARCHY by error<PsiElement>()
        val EXPANDED_TYPE_CANNOT_BE_INHERITED by error<KtTypeReference> {
            parameter<ConeKotlinType>("type")
        }
        val PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE by error<KtModifierListOwner>(PositioningStrategy.VARIANCE_MODIFIER)
        val INCONSISTENT_TYPE_PARAMETER_VALUES by error<KtClass>(PositioningStrategy.SUPERTYPES_LIST) {
            parameter<FirTypeParameterSymbol>("typeParameter")
            parameter<FirRegularClassSymbol>("type")
            parameter<Collection<ConeKotlinType>>("bounds")
        }
        val INCONSISTENT_TYPE_PARAMETER_BOUNDS by error<PsiElement> {
            parameter<FirTypeParameterSymbol>("typeParameter")
            parameter<FirRegularClassSymbol>("type")
            parameter<Collection<ConeKotlinType>>("bounds")
        }
        val AMBIGUOUS_SUPER by error<KtSuperExpression>()
    }

    val CONSTRUCTOR_PROBLEMS by object : DiagnosticGroup("Constructor problems") {
        val CONSTRUCTOR_IN_OBJECT by error<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val CONSTRUCTOR_IN_INTERFACE by error<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val NON_PRIVATE_CONSTRUCTOR_IN_ENUM by error<PsiElement>()
        val NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED by error<PsiElement>()
        val CYCLIC_CONSTRUCTOR_DELEGATION_CALL by error<PsiElement>()
        val PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED by warning<PsiElement>(PositioningStrategy.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)

        // TODO: change it to KtSuperTypeEntry when possible (after re-targeter implementation)
        val SUPERTYPE_NOT_INITIALIZED by error<KtTypeReference>()
        val SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR by error<PsiElement>()
        val DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR by error<PsiElement>()
        val PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS by error<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val EXPLICIT_DELEGATION_CALL_REQUIRED by error<PsiElement>(PositioningStrategy.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)
        val SEALED_CLASS_CONSTRUCTOR_CALL by error<PsiElement>()

        // TODO: Consider creating a parameter list position strategy and report on the parameter list instead
        val DATA_CLASS_WITHOUT_PARAMETERS by error<KtPrimaryConstructor>()
        val DATA_CLASS_VARARG_PARAMETER by error<KtParameter>()
        val DATA_CLASS_NOT_PROPERTY_PARAMETER by error<KtParameter>()
    }

    val ANNOTATIONS by object : DiagnosticGroup("Annotations") {
        val ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR by error<KtExpression>()
        val ANNOTATION_ARGUMENT_MUST_BE_CONST by error<KtExpression>()
        val ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST by error<KtExpression>()
        val ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL by error<KtExpression>()
        val ANNOTATION_CLASS_MEMBER by error<PsiElement>()
        val ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT by error<KtExpression>()
        val INVALID_TYPE_OF_ANNOTATION_MEMBER by error<KtTypeReference>()
        val LOCAL_ANNOTATION_CLASS_ERROR by error<KtClassOrObject>()
        val MISSING_VAL_ON_ANNOTATION_PARAMETER by error<KtParameter>()
        val NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION by error<KtExpression>()
        val ANNOTATION_CLASS_CONSTRUCTOR_CALL by error<KtCallExpression>()
        val NOT_AN_ANNOTATION_CLASS by error<PsiElement> {
            parameter<String>("annotationName")
        }
        val NULLABLE_TYPE_OF_ANNOTATION_MEMBER by error<KtTypeReference>()
        val VAR_ANNOTATION_PARAMETER by error<KtParameter>(PositioningStrategy.VAL_OR_VAR_NODE)
        val SUPERTYPES_FOR_ANNOTATION_CLASS by error<KtClass>(PositioningStrategy.SUPERTYPES_LIST)
        val ANNOTATION_USED_AS_ANNOTATION_ARGUMENT by error<KtAnnotationEntry>()
        val ILLEGAL_KOTLIN_VERSION_STRING_VALUE by error<KtExpression>()
        val NEWER_VERSION_IN_SINCE_KOTLIN by warning<KtExpression> {
            parameter<String>("specifiedVersion")
        }
        val DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS by error<PsiElement>()
        val DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS by error<PsiElement>()
        val DEPRECATED_SINCE_KOTLIN_WITHOUT_DEPRECATED by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)

        val ANNOTATION_ON_SUPERCLASS by deprecationError<KtAnnotationEntry>(LanguageFeature.ProhibitUseSiteTargetAnnotationsOnSuperTypes)
        val RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION by deprecationError<PsiElement>(LanguageFeature.RestrictRetentionForExpressionAnnotations)
        val WRONG_ANNOTATION_TARGET by error<KtAnnotationEntry> {
            parameter<String>("actualTarget")
        }
        val WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET by error<KtAnnotationEntry> {
            parameter<String>("actualTarget")
            parameter<String>("useSiteTarget")
        }
        val INAPPLICABLE_TARGET_ON_PROPERTY by error<KtAnnotationEntry> {
            parameter<String>("useSiteDescription")
        }
        val INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE by error<KtAnnotationEntry> {
            parameter<String>("useSiteDescription")
        }
        val INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE by error<KtAnnotationEntry>()
        val INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD by error<KtAnnotationEntry>()
        val INAPPLICABLE_PARAM_TARGET by error<KtAnnotationEntry>()
        val REDUNDANT_ANNOTATION_TARGET by warning<KtAnnotationEntry> {
            parameter<String>("useSiteDescription")
        }
        val INAPPLICABLE_FILE_TARGET by error<KtAnnotationEntry>(PositioningStrategy.ANNOTATION_USE_SITE)
    }

    val EXPERIMENTAL by object : DiagnosticGroup("OptIn-related") {
        val EXPERIMENTAL_API_USAGE by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FqName>("optInMarkerFqName")
            parameter<String>("message")
        }
        val EXPERIMENTAL_API_USAGE_ERROR by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FqName>("optInMarkerFqName")
            parameter<String>("message")
        }
        val EXPERIMENTAL_OVERRIDE by warning<PsiElement> {
            parameter<FqName>("optInMarkerFqName")
            parameter<String>("message")
        }
        val EXPERIMENTAL_OVERRIDE_ERROR by error<PsiElement> {
            parameter<FqName>("optInMarkerFqName")
            parameter<String>("message")
        }
        val EXPERIMENTAL_IS_NOT_ENABLED by warning<KtAnnotationEntry>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val EXPERIMENTAL_CAN_ONLY_BE_USED_AS_ANNOTATION by error<PsiElement>()
        val EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL by error<PsiElement>()
        val USE_EXPERIMENTAL_WITHOUT_ARGUMENTS by warning<KtAnnotationEntry>()
        val USE_EXPERIMENTAL_ARGUMENT_IS_NOT_MARKER by warning<KtAnnotationEntry> {
            parameter<FqName>("notMarkerFqName")
        }
        val EXPERIMENTAL_ANNOTATION_WITH_WRONG_TARGET by error<KtAnnotationEntry> {
            parameter<String>("target")
        }
        val EXPERIMENTAL_ANNOTATION_WITH_WRONG_RETENTION by error<KtAnnotationEntry>()
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
        val INAPPLICABLE_INFIX_MODIFIER by error<PsiElement>()
        val REPEATED_MODIFIER by error<PsiElement> {
            parameter<KtModifierKeywordToken>("modifier")
        }
        val REDUNDANT_MODIFIER by error<PsiElement> {
            parameter<KtModifierKeywordToken>("redundantModifier")
            parameter<KtModifierKeywordToken>("conflictingModifier")
        }
        val DEPRECATED_MODIFIER by warning<PsiElement> {
            parameter<KtModifierKeywordToken>("deprecatedModifier")
            parameter<KtModifierKeywordToken>("actualModifier")
        }
        val DEPRECATED_MODIFIER_PAIR by error<PsiElement> {
            parameter<KtModifierKeywordToken>("deprecatedModifier")
            parameter<KtModifierKeywordToken>("conflictingModifier")
        }
        val DEPRECATED_MODIFIER_FOR_TARGET by warning<PsiElement> {
            parameter<KtModifierKeywordToken>("deprecatedModifier")
            parameter<String>("target")
        }
        val REDUNDANT_MODIFIER_FOR_TARGET by warning<PsiElement> {
            parameter<KtModifierKeywordToken>("redundantModifier")
            parameter<String>("target")
        }
        val INCOMPATIBLE_MODIFIERS by error<PsiElement> {
            parameter<KtModifierKeywordToken>("modifier1")
            parameter<KtModifierKeywordToken>("modifier2")
        }
        val REDUNDANT_OPEN_IN_INTERFACE by warning<KtModifierListOwner>(PositioningStrategy.OPEN_MODIFIER)
        val WRONG_MODIFIER_TARGET by error<PsiElement> {
            parameter<KtModifierKeywordToken>("modifier")
            parameter<String>("target")
        }
        val OPERATOR_MODIFIER_REQUIRED by error<PsiElement> {
            parameter<FirNamedFunctionSymbol>("functionSymbol")
            parameter<String>("name")
        }
        val INFIX_MODIFIER_REQUIRED by error<PsiElement> {
            parameter<FirNamedFunctionSymbol>("functionSymbol")
        }
        val WRONG_MODIFIER_CONTAINING_DECLARATION by error<PsiElement> {
            parameter<KtModifierKeywordToken>("modifier")
            parameter<String>("target")
        }
        val DEPRECATED_MODIFIER_CONTAINING_DECLARATION by warning<PsiElement> {
            parameter<KtModifierKeywordToken>("modifier")
            parameter<String>("target")
        }
        val INAPPLICABLE_OPERATOR_MODIFIER by error<PsiElement>(PositioningStrategy.OPERATOR_MODIFIER) {
            parameter<String>("message")
        }
    }

    val INLINE_CLASSES by object : DiagnosticGroup("Inline classes") {
        val INLINE_CLASS_NOT_TOP_LEVEL by error<KtDeclaration>(PositioningStrategy.INLINE_OR_VALUE_MODIFIER)
        val INLINE_CLASS_NOT_FINAL by error<KtDeclaration>(PositioningStrategy.MODALITY_MODIFIER)
        val ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_INLINE_CLASS by error<KtDeclaration>(PositioningStrategy.INLINE_OR_VALUE_MODIFIER)
        val INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE by error<KtElement>()
        val INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER by error<KtParameter>()
        val PROPERTY_WITH_BACKING_FIELD_INSIDE_INLINE_CLASS by error<KtProperty>(PositioningStrategy.DECLARATION_SIGNATURE)
        val DELEGATED_PROPERTY_INSIDE_INLINE_CLASS by error<PsiElement>()
        val INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE by error<KtTypeReference> {
            parameter<ConeKotlinType>("type")
        }
        val INLINE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION by error<PsiElement>()
        val INLINE_CLASS_CANNOT_EXTEND_CLASSES by error<KtTypeReference>()
        val INLINE_CLASS_CANNOT_BE_RECURSIVE by error<KtTypeReference>()
        val RESERVED_MEMBER_INSIDE_INLINE_CLASS by error<KtFunction>(PositioningStrategy.DECLARATION_NAME) {
            parameter<String>("name")
        }
        val SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS by error<PsiElement>()
        val INNER_CLASS_INSIDE_INLINE_CLASS by error<KtDeclaration>(PositioningStrategy.INNER_MODIFIER)
        val VALUE_CLASS_CANNOT_BE_CLONEABLE by error<KtDeclaration>(PositioningStrategy.INLINE_OR_VALUE_MODIFIER)
    }

    val APPLICABILITY by object : DiagnosticGroup("Applicability") {
        val NONE_APPLICABLE by error<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Collection<Symbol>>("candidates")
        }

        val INAPPLICABLE_CANDIDATE by error<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("candidate")
        }

        val TYPE_MISMATCH by error<PsiElement> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }

        val THROWABLE_TYPE_MISMATCH by error<PsiElement> {
            parameter<ConeKotlinType>("actualType")
        }

        val CONDITION_TYPE_MISMATCH by error<PsiElement> {
            parameter<ConeKotlinType>("actualType")
        }

        val ARGUMENT_TYPE_MISMATCH by error<PsiElement> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
            parameter<Boolean>("isMismatchDueToNullability")
        }

        val NULL_FOR_NONNULL_TYPE by error<PsiElement> { }

        val INAPPLICABLE_LATEINIT_MODIFIER by error<KtModifierListOwner>(PositioningStrategy.LATEINIT_MODIFIER) {
            parameter<String>("reason")
        }

        // TODO: reset to KtExpression after fixsing lambda argument sources
        val VARARG_OUTSIDE_PARENTHESES by error<KtElement>()

        val NAMED_ARGUMENTS_NOT_ALLOWED by error<KtValueArgument>(PositioningStrategy.NAME_OF_NAMED_ARGUMENT) {
            parameter<ForbiddenNamedArgumentsTarget>("forbiddenNamedArgumentsTarget")
        }

        val NON_VARARG_SPREAD by error<LeafPsiElement>()
        val ARGUMENT_PASSED_TWICE by error<KtValueArgument>(PositioningStrategy.NAME_OF_NAMED_ARGUMENT)
        val TOO_MANY_ARGUMENTS by error<PsiElement> {
            parameter<FirCallableSymbol<*>>("function")
        }
        val NO_VALUE_FOR_PARAMETER by error<KtElement>(PositioningStrategy.VALUE_ARGUMENTS) {
            parameter<FirValueParameterSymbol>("violatedParameter")
        }

        val NAMED_PARAMETER_NOT_FOUND by error<KtValueArgument>(PositioningStrategy.NAME_OF_NAMED_ARGUMENT) {
            parameter<String>("name")
        }

        val ASSIGNMENT_TYPE_MISMATCH by error<KtExpression> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }

        val RESULT_TYPE_MISMATCH by error<KtExpression> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }

        val MANY_LAMBDA_EXPRESSION_ARGUMENTS by error<KtValueArgument>()

        val NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER by error<KtElement> {
            parameter<String>("name")
        }

        val SPREAD_OF_NULLABLE by error<PsiElement>(PositioningStrategy.SPREAD_OPERATOR)

        val ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION by deprecationError<KtExpression>(LanguageFeature.ProhibitAssigningSingleElementsToVarargsInNamedForm)
        val ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION by deprecationError<KtExpression>(LanguageFeature.ProhibitAssigningSingleElementsToVarargsInNamedForm)
    }

    val AMBIGUITY by object : DiagnosticGroup("Ambiguity") {
        val OVERLOAD_RESOLUTION_AMBIGUITY by error<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Collection<Symbol>>("candidates")
        }
        val ASSIGN_OPERATOR_AMBIGUITY by error<PsiElement> {
            parameter<Collection<Symbol>>("candidates")
        }
        val ITERATOR_AMBIGUITY by error<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Collection<FirBasedSymbol<*>>>("candidates")
        }
        val HAS_NEXT_FUNCTION_AMBIGUITY by error<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Collection<FirBasedSymbol<*>>>("candidates")
        }
        val NEXT_AMBIGUITY by error<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Collection<FirBasedSymbol<*>>>("candidates")
        }
    }

    val TYPES_AND_TYPE_PARAMETERS by object : DiagnosticGroup("Types & type parameters") {
        val RECURSION_IN_IMPLICIT_TYPES by error<PsiElement>()
        val INFERENCE_ERROR by error<PsiElement>()
        val PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT by error<PsiElement>()
        val UPPER_BOUND_VIOLATED by error<PsiElement> {
            parameter<ConeKotlinType>("expectedUpperBound")
            parameter<ConeKotlinType>("actualUpperBound")
        }
        val UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION by error<PsiElement> {
            parameter<ConeKotlinType>("expectedUpperBound")
            parameter<ConeKotlinType>("actualUpperBound")
        }
        val TYPE_ARGUMENTS_NOT_ALLOWED by error<PsiElement>()
        val WRONG_NUMBER_OF_TYPE_ARGUMENTS by error<PsiElement> {
            parameter<Int>("expectedCount")
            parameter<FirRegularClassSymbol>("classifier")
        }
        val NO_TYPE_ARGUMENTS_ON_RHS by error<PsiElement> {
            parameter<Int>("expectedCount")
            parameter<FirClassLikeSymbol<*>>("classifier")
        }
        val OUTER_CLASS_ARGUMENTS_REQUIRED by error<PsiElement> {
            parameter<FirRegularClassSymbol>("outer")
        }
        val TYPE_PARAMETERS_IN_OBJECT by error<PsiElement>()
        val ILLEGAL_PROJECTION_USAGE by error<PsiElement>()
        val TYPE_PARAMETERS_IN_ENUM by error<PsiElement>()
        val CONFLICTING_PROJECTION by error<KtTypeProjection>(PositioningStrategy.VARIANCE_MODIFIER) {
            parameter<ConeKotlinType>("type")
        }
        val CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION by error<KtElement>(PositioningStrategy.VARIANCE_MODIFIER) {
            parameter<ConeKotlinType>("type")
        }
        val REDUNDANT_PROJECTION by warning<KtTypeProjection>(PositioningStrategy.VARIANCE_MODIFIER) {
            parameter<ConeKotlinType>("type")
        }
        val VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED by error<KtTypeParameter>(PositioningStrategy.VARIANCE_MODIFIER)

        val CATCH_PARAMETER_WITH_DEFAULT_VALUE by error<PsiElement>()
        val REIFIED_TYPE_IN_CATCH_CLAUSE by error<PsiElement>()
        val TYPE_PARAMETER_IN_CATCH_CLAUSE by error<PsiElement>()
        val GENERIC_THROWABLE_SUBCLASS by error<KtTypeParameter>()
        val INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS by error<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME)

        val KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE by error<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }

        val TYPE_PARAMETER_AS_REIFIED by error<PsiElement> {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }

        val TYPE_PARAMETER_AS_REIFIED_ARRAY by deprecationError<PsiElement>(LanguageFeature.ProhibitNonReifiedArraysAsReifiedTypeArguments) {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }

        val REIFIED_TYPE_FORBIDDEN_SUBSTITUTION by error<PsiElement> {
            parameter<ConeKotlinType>("type")
        }

        val FINAL_UPPER_BOUND by warning<KtTypeReference> {
            parameter<ConeKotlinType>("type")
        }

        val UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE by error<KtTypeReference>()

        val BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER by error<KtElement>()

        val ONLY_ONE_CLASS_BOUND_ALLOWED by error<KtTypeReference>()

        val REPEATED_BOUND by error<KtTypeReference>()

        val CONFLICTING_UPPER_BOUNDS by error<KtNamedDeclaration> {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }

        val NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER by error<KtSimpleNameExpression> {
            parameter<Name>("typeParameterName")
            parameter<Symbol>("typeParametersOwner")
        }

        val BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED by error<KtTypeReference>()

        val REIFIED_TYPE_PARAMETER_NO_INLINE by error<KtTypeParameter>(PositioningStrategy.REIFIED_MODIFIER)

        val TYPE_PARAMETERS_NOT_ALLOWED by error<KtDeclaration>(PositioningStrategy.TYPE_PARAMETERS_LIST)

        val TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER by error<KtTypeParameter>()

        val RETURN_TYPE_MISMATCH by error<KtExpression>(PositioningStrategy.WHOLE_ELEMENT) {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
            parameter<FirSimpleFunction>("targetFunction")
        }

        val CYCLIC_GENERIC_UPPER_BOUND by error<PsiElement>()

        val DEPRECATED_TYPE_PARAMETER_SYNTAX by error<KtDeclaration>(PositioningStrategy.TYPE_PARAMETERS_LIST)

        val MISPLACED_TYPE_PARAMETER_CONSTRAINTS by warning<KtTypeParameter>()

        val DYNAMIC_UPPER_BOUND by error<KtTypeReference>()

        val INCOMPATIBLE_TYPES by error<KtElement> {
            parameter<ConeKotlinType>("typeA")
            parameter<ConeKotlinType>("typeB")
        }

        val INCOMPATIBLE_TYPES_WARNING by warning<KtElement> {
            parameter<ConeKotlinType>("typeA")
            parameter<ConeKotlinType>("typeB")
        }

        val TYPE_VARIANCE_CONFLICT by error<PsiElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<FirTypeParameterSymbol>("typeParameter")
            parameter<Variance>("typeParameterVariance")
            parameter<Variance>("variance")
            parameter<ConeKotlinType>("containingType")
        }

        val TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE by error<PsiElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<FirTypeParameterSymbol>("typeParameter")
            parameter<Variance>("typeParameterVariance")
            parameter<Variance>("variance")
            parameter<ConeKotlinType>("containingType")
        }

        val SMARTCAST_IMPOSSIBLE by error<KtExpression> {
            parameter<ConeKotlinType>("desiredType")
            parameter<FirExpression>("subject")
            parameter<String>("description")
        }
    }

    val REFLECTION by object : DiagnosticGroup("Reflection") {
        val EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED by error<KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirCallableSymbol<*>>("referencedDeclaration")
        }
        val CALLABLE_REFERENCE_LHS_NOT_A_CLASS by error<KtExpression>()
        val CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR by error<KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED)

        val CLASS_LITERAL_LHS_NOT_A_CLASS by error<KtExpression>()
        val NULLABLE_TYPE_IN_CLASS_LITERAL_LHS by error<KtExpression>()
        val EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS by error<PsiElement> {
            parameter<ConeKotlinType>("lhsType")
        }
    }

    val OVERRIDES by object : DiagnosticGroup("overrides") {
        val NOTHING_TO_OVERRIDE by error<KtModifierListOwner>(PositioningStrategy.OVERRIDE_MODIFIER) {
            parameter<FirCallableSymbol<*>>("declaration")
        }

        val CANNOT_OVERRIDE_INVISIBLE_MEMBER by error<KtNamedDeclaration>(PositioningStrategy.OVERRIDE_MODIFIER) {
            parameter<FirCallableSymbol<*>>("overridingMember")
            parameter<FirCallableSymbol<*>>("baseMember")
        }

        val DATA_CLASS_OVERRIDE_CONFLICT by error<KtClassOrObject>(PositioningStrategy.DATA_MODIFIER) {
            parameter<FirCallableSymbol<*>>("overridingMember")
            parameter<FirCallableSymbol<*>>("baseMember")
        }

        val CANNOT_WEAKEN_ACCESS_PRIVILEGE by error<KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER) {
            parameter<Visibility>("overridingVisibility")
            parameter<FirCallableSymbol<*>>("overridden")
            parameter<Name>("containingClassName")
        }
        val CANNOT_CHANGE_ACCESS_PRIVILEGE by error<KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER) {
            parameter<Visibility>("overridingVisibility")
            parameter<FirCallableSymbol<*>>("overridden")
            parameter<Name>("containingClassName")
        }

        val OVERRIDING_FINAL_MEMBER by error<KtNamedDeclaration>(PositioningStrategy.OVERRIDE_MODIFIER) {
            parameter<FirCallableSymbol<*>>("overriddenDeclaration")
            parameter<Name>("containingClassName")
        }

        val RETURN_TYPE_MISMATCH_ON_INHERITANCE by error<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("conflictingDeclaration1")
            parameter<FirCallableSymbol<*>>("conflictingDeclaration2")
        }

        val PROPERTY_TYPE_MISMATCH_ON_INHERITANCE by error<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("conflictingDeclaration1")
            parameter<FirCallableSymbol<*>>("conflictingDeclaration2")
        }

        val VAR_TYPE_MISMATCH_ON_INHERITANCE by error<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("conflictingDeclaration1")
            parameter<FirCallableSymbol<*>>("conflictingDeclaration2")
        }

        val RETURN_TYPE_MISMATCH_BY_DELEGATION by error<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("delegateDeclaration")
            parameter<FirCallableSymbol<*>>("baseDeclaration")
        }

        val PROPERTY_TYPE_MISMATCH_BY_DELEGATION by error<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("delegateDeclaration")
            parameter<FirCallableSymbol<*>>("baseDeclaration")
        }

        val VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION by error<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("delegateDeclaration")
            parameter<FirCallableSymbol<*>>("baseDeclaration")
        }

        val CONFLICTING_INHERITED_MEMBERS by error<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<List<FirCallableSymbol<*>>>("conflictingDeclarations")
        }

        val ABSTRACT_MEMBER_NOT_IMPLEMENTED by error<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClassSymbol<*>>("classOrObject")
            parameter<FirCallableSymbol<*>>("missingDeclaration")
        }
        val ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED by error<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClassSymbol<*>>("classOrObject")
            parameter<FirCallableSymbol<*>>("missingDeclaration")
        }
        val INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER by deprecationError<KtClassOrObject>(
            LanguageFeature.ProhibitInvisibleAbstractMethodsInSuperclasses,
            PositioningStrategy.DECLARATION_NAME
        ) {
            parameter<FirClassSymbol<*>>("classOrObject")
            parameter<FirCallableSymbol<*>>("invisibleDeclaration")
        }
        val MANY_IMPL_MEMBER_NOT_IMPLEMENTED by error<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClassSymbol<*>>("classOrObject")
            parameter<FirCallableSymbol<*>>("missingDeclaration")
        }
        val MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED by error<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClassSymbol<*>>("classOrObject")
            parameter<FirCallableSymbol<*>>("missingDeclaration")
        }
        val OVERRIDING_FINAL_MEMBER_BY_DELEGATION by error<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("delegatedDeclaration")
            parameter<FirCallableSymbol<*>>("overriddenDeclaration")
        }
        val DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("delegatedDeclaration")
            parameter<FirCallableSymbol<*>>("overriddenDeclaration")
        }

        val RETURN_TYPE_MISMATCH_ON_OVERRIDE by error<KtNamedDeclaration>(PositioningStrategy.DECLARATION_RETURN_TYPE) {
            parameter<FirCallableSymbol<*>>("function")
            parameter<FirCallableSymbol<*>>("superFunction")
        }
        val PROPERTY_TYPE_MISMATCH_ON_OVERRIDE by error<KtNamedDeclaration>(PositioningStrategy.DECLARATION_RETURN_TYPE) {
            parameter<FirCallableSymbol<*>>("property")
            parameter<FirCallableSymbol<*>>("superProperty")
        }
        val VAR_TYPE_MISMATCH_ON_OVERRIDE by error<KtNamedDeclaration>(PositioningStrategy.DECLARATION_RETURN_TYPE) {
            parameter<FirCallableSymbol<*>>("variable")
            parameter<FirCallableSymbol<*>>("superVariable")
        }
        val VAR_OVERRIDDEN_BY_VAL by error<KtNamedDeclaration>(PositioningStrategy.VAL_OR_VAR_NODE) {
            parameter<FirCallableSymbol<*>>("overridingDeclaration")
            parameter<FirCallableSymbol<*>>("overriddenDeclaration")
        }
        val NON_FINAL_MEMBER_IN_FINAL_CLASS by warning<KtNamedDeclaration>(PositioningStrategy.OPEN_MODIFIER)
        val NON_FINAL_MEMBER_IN_OBJECT by warning<KtNamedDeclaration>(PositioningStrategy.OPEN_MODIFIER)
        val VIRTUAL_MEMBER_HIDDEN by error<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("declared")
            parameter<FirRegularClassSymbol>("overriddenContainer")
        }
    }

    val REDECLARATIONS by object : DiagnosticGroup("Redeclarations") {
        val MANY_COMPANION_OBJECTS by error<KtObjectDeclaration>(PositioningStrategy.COMPANION_OBJECT)
        val CONFLICTING_OVERLOADS by error<PsiElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<Collection<Symbol>>("conflictingOverloads")
        }
        val REDECLARATION by error<KtNamedDeclaration>(PositioningStrategy.NAME_IDENTIFIER) {
            parameter<Collection<Symbol>>("conflictingDeclarations")
        }
        val PACKAGE_OR_CLASSIFIER_REDECLARATION by error<KtNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Collection<Symbol>>("conflictingDeclarations")
        }
        val METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE by error<PsiElement>()
    }

    val INVALID_LOCAL_DECLARATIONS by object : DiagnosticGroup("Invalid local declarations") {
        val LOCAL_OBJECT_NOT_ALLOWED by error<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<Name>("objectName")
        }
        val LOCAL_INTERFACE_NOT_ALLOWED by error<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<Name>("interfaceName")
        }
    }

    val FUNCTIONS by object : DiagnosticGroup("Functions") {
        val ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS by error<KtFunction>(PositioningStrategy.MODALITY_MODIFIER) {
            parameter<FirCallableSymbol<*>>("function")
            parameter<FirClassSymbol<*>>("containingClass")
        }
        val ABSTRACT_FUNCTION_WITH_BODY by error<KtFunction>(PositioningStrategy.MODALITY_MODIFIER) {
            parameter<FirCallableSymbol<*>>("function")
        }
        val NON_ABSTRACT_FUNCTION_WITH_NO_BODY by error<KtFunction>(PositioningStrategy.DECLARATION_SIGNATURE) {
            parameter<FirCallableSymbol<*>>("function")
        }
        val PRIVATE_FUNCTION_WITH_NO_BODY by error<KtFunction>(PositioningStrategy.VISIBILITY_MODIFIER) {
            parameter<FirCallableSymbol<*>>("function")
        }

        val NON_MEMBER_FUNCTION_NO_BODY by error<KtFunction>(PositioningStrategy.DECLARATION_SIGNATURE) {
            parameter<FirCallableSymbol<*>>("function")
        }

        val FUNCTION_DECLARATION_WITH_NO_NAME by error<KtFunction>(PositioningStrategy.DECLARATION_SIGNATURE)
        val ANONYMOUS_FUNCTION_WITH_NAME by error<KtFunction>()

        // TODO: val ANONYMOUS_FUNCTION_WITH_NAME by error1<PsiElement, Name>(SourceElementPositioningStrategies.DECLARATION_NAME)
        val ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE by error<KtParameter>(PositioningStrategy.PARAMETER_DEFAULT_VALUE)
        val USELESS_VARARG_ON_PARAMETER by warning<KtParameter>()
        val MULTIPLE_VARARG_PARAMETERS by error<KtParameter>(PositioningStrategy.PARAMETER_VARARG_MODIFIER)
        val FORBIDDEN_VARARG_PARAMETER_TYPE by error<KtParameter>(PositioningStrategy.PARAMETER_VARARG_MODIFIER) {
            parameter<ConeKotlinType>("varargParameterType")
        }
        val VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION by error<KtParameter>()

        // TODO: replace with KtParameter
        val CANNOT_INFER_PARAMETER_TYPE by error<KtElement>()
    }

    val FUN_INTERFACES by object : DiagnosticGroup("Fun interfaces") {
        val FUN_INTERFACE_CONSTRUCTOR_REFERENCE by error<KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED)
        val FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS by error<KtClass>(PositioningStrategy.FUN_MODIFIER)
        val FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES by error<KtDeclaration>(PositioningStrategy.FUN_INTERFACE)
        val FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS by error<KtDeclaration>(PositioningStrategy.FUN_INTERFACE)
        val FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE by error<KtDeclaration>(PositioningStrategy.FUN_INTERFACE)
        val FUN_INTERFACE_WITH_SUSPEND_FUNCTION by error<KtDeclaration>(PositioningStrategy.FUN_INTERFACE)
    }

    val PROPERTIES_AND_ACCESSORS by object : DiagnosticGroup("Properties & accessors") {
        val ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS by error<KtModifierListOwner>(PositioningStrategy.MODALITY_MODIFIER) {
            parameter<FirCallableSymbol<*>>("property")
            parameter<FirClassSymbol<*>>("containingClass")
        }
        val PRIVATE_PROPERTY_IN_INTERFACE by error<KtProperty>(PositioningStrategy.VISIBILITY_MODIFIER)

        val ABSTRACT_PROPERTY_WITH_INITIALIZER by error<KtExpression>()
        val PROPERTY_INITIALIZER_IN_INTERFACE by error<KtExpression>()
        val PROPERTY_WITH_NO_TYPE_NO_INITIALIZER by error<KtProperty>(PositioningStrategy.DECLARATION_SIGNATURE)

        val MUST_BE_INITIALIZED by error<KtProperty>(PositioningStrategy.DECLARATION_SIGNATURE)
        val MUST_BE_INITIALIZED_OR_BE_ABSTRACT by error<KtProperty>(PositioningStrategy.DECLARATION_SIGNATURE)
        val EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT by error<KtProperty>(PositioningStrategy.DECLARATION_SIGNATURE)
        val UNNECESSARY_LATEINIT by warning<KtProperty>(PositioningStrategy.LATEINIT_MODIFIER)

        val BACKING_FIELD_IN_INTERFACE by error<KtProperty>(PositioningStrategy.DECLARATION_SIGNATURE)
        val EXTENSION_PROPERTY_WITH_BACKING_FIELD by error<KtExpression>()
        val PROPERTY_INITIALIZER_NO_BACKING_FIELD by error<KtExpression>()

        val ABSTRACT_DELEGATED_PROPERTY by error<KtExpression>()
        val DELEGATED_PROPERTY_IN_INTERFACE by error<KtExpression>()
        // TODO: val ACCESSOR_FOR_DELEGATED_PROPERTY by error1<PsiElement, FirPropertyAccessorSymbol>()

        val ABSTRACT_PROPERTY_WITH_GETTER by error<KtPropertyAccessor>()
        val ABSTRACT_PROPERTY_WITH_SETTER by error<KtPropertyAccessor>()
        val PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY by error<KtModifierListOwner>(PositioningStrategy.PRIVATE_MODIFIER)
        val PRIVATE_SETTER_FOR_OPEN_PROPERTY by error<KtModifierListOwner>(PositioningStrategy.PRIVATE_MODIFIER)
        val VAL_WITH_SETTER by error<KtPropertyAccessor>()
        val CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT by error<KtElement>(PositioningStrategy.CONST_MODIFIER)
        val CONST_VAL_WITH_GETTER by error<KtElement>()
        val CONST_VAL_WITH_DELEGATE by error<KtExpression>()
        val TYPE_CANT_BE_USED_FOR_CONST_VAL by error<KtProperty>(PositioningStrategy.CONST_MODIFIER) {
            parameter<ConeKotlinType>("constValType")
        }
        val CONST_VAL_WITHOUT_INITIALIZER by error<KtProperty>(PositioningStrategy.CONST_MODIFIER)
        val CONST_VAL_WITH_NON_CONST_INITIALIZER by error<KtExpression>()
        val WRONG_SETTER_PARAMETER_TYPE by error<KtTypeReference> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }
        val INITIALIZER_TYPE_MISMATCH by error<KtProperty>(PositioningStrategy.PROPERTY_INITIALIZER) {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }
        val GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY by error<KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER)
        val SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY by error<KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER)
        val WRONG_SETTER_RETURN_TYPE by error<KtTypeReference>()
        val WRONG_GETTER_RETURN_TYPE by error<KtTypeReference> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }
        val ACCESSOR_FOR_DELEGATED_PROPERTY by error<KtPropertyAccessor>()
        val ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS by error<KtModifierListOwner>(PositioningStrategy.ABSTRACT_MODIFIER)
    }

    val MPP_PROJECTS by object : DiagnosticGroup("Multi-platform projects") {
        val EXPECTED_DECLARATION_WITH_BODY by error<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL by error<KtConstructorDelegationCall>()
        val EXPECTED_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER by error<KtParameter>()
        val EXPECTED_ENUM_CONSTRUCTOR by error<KtConstructor<*>>()
        val EXPECTED_ENUM_ENTRY_WITH_BODY by error<KtEnumEntry>()
        val EXPECTED_PROPERTY_INITIALIZER by error<KtExpression>()
        // TODO: need to cover `by` as well as delegate expression
        val EXPECTED_DELEGATED_PROPERTY by error<KtExpression>()
        val EXPECTED_LATEINIT_PROPERTY by error<KtModifierListOwner>(PositioningStrategy.LATEINIT_MODIFIER)
        val SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS by error<PsiElement>()
        val EXPECTED_PRIVATE_DECLARATION by error<KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER)
        val IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS by error<KtDelegatedSuperTypeEntry>()

        val ACTUAL_TYPE_ALIAS_NOT_TO_CLASS by error<KtTypeAlias>(PositioningStrategy.DECLARATION_SIGNATURE)
        val ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE by error<KtTypeAlias>(PositioningStrategy.DECLARATION_SIGNATURE)
        val ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE by error<KtTypeAlias>(PositioningStrategy.DECLARATION_SIGNATURE)
        val ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION by error<KtTypeAlias>(PositioningStrategy.DECLARATION_SIGNATURE)
        val ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS by error<PsiElement>()
        val ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE by error<PsiElement> {
            parameter<FirVariableSymbol<*>>("parameter")
        }

        val EXPECTED_FUNCTION_SOURCE_WITH_DEFAULT_ARGUMENTS_NOT_FOUND by error<PsiElement>()

        val NO_ACTUAL_FOR_EXPECT by error<KtNamedDeclaration>(PositioningStrategy.INCOMPATIBLE_DECLARATION) {
            parameter<Symbol>("declaration")
            parameter<FirModuleData>("module")
            parameter<Map<Incompatible<Symbol>, Collection<Symbol>>>("compatibility")
        }

        val ACTUAL_WITHOUT_EXPECT by error<KtNamedDeclaration> {
            parameter<Symbol>("declaration")
            parameter<Map<Incompatible<Symbol>, Collection<Symbol>>>("compatibility")
        }

        val AMBIGUOUS_ACTUALS by error<KtNamedDeclaration>(PositioningStrategy.INCOMPATIBLE_DECLARATION) {
            parameter<Symbol>("declaration")
            parameter<Collection<Symbol>>("candidates")
        }

        val AMBIGUOUS_EXPECTS by error<KtNamedDeclaration>(PositioningStrategy.INCOMPATIBLE_DECLARATION) {
            parameter<Symbol>("declaration")
            parameter<Collection<FirModuleData>>("modules")
        }

        val NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS by error<KtNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Symbol>("declaration")
            parameter<List<Pair<Symbol, Map<Incompatible<Symbol>, Collection<Symbol>>>>>("members")
        }

        val ACTUAL_MISSING by error<KtNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME)
    }

    val DESTRUCTING_DECLARATION by object : DiagnosticGroup("Destructuring declaration") {
        val INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION by error<KtDestructuringDeclaration>()
        val COMPONENT_FUNCTION_MISSING by error<PsiElement> {
            parameter<Name>("missingFunctionName")
            parameter<ConeKotlinType>("destructingType")
        }
        val COMPONENT_FUNCTION_AMBIGUITY by error<PsiElement> {
            parameter<Name>("functionWithAmbiguityName")
            parameter<Collection<Symbol>>("candidates")
        }
        val COMPONENT_FUNCTION_ON_NULLABLE by error<KtExpression> {
            parameter<Name>("componentFunctionName")
        }
        val COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH by error<KtExpression> {
            parameter<Name>("componentFunctionName")
            parameter<ConeKotlinType>("destructingType")
            parameter<ConeKotlinType>("expectedType")
        }
    }

    val CONTROL_FLOW by object : DiagnosticGroup("Control flow diagnostics") {
        val UNINITIALIZED_VARIABLE by error<KtSimpleNameExpression> {
            parameter<FirPropertySymbol>("variable")
        }
        val UNINITIALIZED_PARAMETER by error<KtSimpleNameExpression> {
            parameter<FirValueParameterSymbol>("parameter")
        }
        val UNINITIALIZED_ENUM_ENTRY by error<KtSimpleNameExpression> {
            parameter<FirEnumEntrySymbol>("enumEntry")
        }
        val UNINITIALIZED_ENUM_COMPANION by error<KtSimpleNameExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirRegularClassSymbol>("enumClass")
        }
        val VAL_REASSIGNMENT by error<KtExpression> {
            parameter<FirVariableSymbol<*>>("variable")
        }
        val VAL_REASSIGNMENT_VIA_BACKING_FIELD by deprecationError<KtExpression>(LanguageFeature.RestrictionOfValReassignmentViaBackingField) {
            parameter<FirBackingFieldSymbol>("property")
        }
        val CAPTURED_VAL_INITIALIZATION by error<KtExpression> {
            parameter<FirPropertySymbol>("property")
        }
        val CAPTURED_MEMBER_VAL_INITIALIZATION by error<KtExpression> {
            parameter<FirPropertySymbol>("property")
        }
        val SETTER_PROJECTED_OUT by error<KtBinaryExpression>(PositioningStrategy.ASSIGNMENT_LHS) {
            parameter<FirPropertySymbol>("property")
        }
        val WRONG_INVOCATION_KIND by warning<PsiElement> {
            parameter<Symbol>("declaration")
            parameter<EventOccurrencesRange>("requiredRange")
            parameter<EventOccurrencesRange>("actualRange")
        }
        val LEAKED_IN_PLACE_LAMBDA by error<PsiElement> {
            parameter<Symbol>("lambda")
        }
        val WRONG_IMPLIES_CONDITION by warning<PsiElement>()
        val VARIABLE_WITH_NO_TYPE_NO_INITIALIZER by error<KtVariableDeclaration>(PositioningStrategy.DECLARATION_NAME)

        val INITIALIZATION_BEFORE_DECLARATION by error<KtExpression>() {
            parameter<Symbol>("property")
        }
        val UNREACHABLE_CODE by warning<KtElement>(PositioningStrategy.UNREACHABLE_CODE) {
            parameter<Set<FirSourceElement>>("reachable")
            parameter<Set<FirSourceElement>>("unreachable")
        }
        val SENSELESS_COMPARISON by warning<KtExpression> {
            parameter<FirExpression>("expression")
            parameter<Boolean>("compareResult")
        }
        val SENSELESS_NULL_IN_WHEN by warning<KtElement>()
    }

    val NULLABILITY by object : DiagnosticGroup("Nullability") {
        val UNSAFE_CALL by error<PsiElement>(PositioningStrategy.DOT_BY_QUALIFIED) {
            parameter<ConeKotlinType>("receiverType")
            parameter<FirExpression?>("receiverExpression")
        }
        val UNSAFE_IMPLICIT_INVOKE_CALL by error<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<ConeKotlinType>("receiverType")
        }
        val UNSAFE_INFIX_CALL by error<KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirExpression>("receiverExpression")
            parameter<String>("operator")
            parameter<FirExpression>("argumentExpression")
        }
        val UNSAFE_OPERATOR_CALL by error<KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirExpression>("receiverExpression")
            parameter<String>("operator")
            parameter<FirExpression>("argumentExpression")
        }
        val ITERATOR_ON_NULLABLE by error<KtExpression>()
        val UNNECESSARY_SAFE_CALL by warning<PsiElement>(PositioningStrategy.SAFE_ACCESS) {
            parameter<ConeKotlinType>("receiverType")
        }
        val UNEXPECTED_SAFE_CALL by error<PsiElement>(PositioningStrategy.SAFE_ACCESS)
        val UNNECESSARY_NOT_NULL_ASSERTION by warning<KtExpression>(PositioningStrategy.OPERATOR) {
            parameter<ConeKotlinType>("receiverType")
        }
        val NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION by warning<KtExpression>(PositioningStrategy.OPERATOR)
        val NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE by warning<KtExpression>(PositioningStrategy.OPERATOR)
        val USELESS_ELVIS by warning<KtBinaryExpression>(PositioningStrategy.USELESS_ELVIS) {
            parameter<ConeKotlinType>("receiverType")
        }
        val USELESS_ELVIS_RIGHT_IS_NULL by warning<KtBinaryExpression>(PositioningStrategy.USELESS_ELVIS)
    }

    val CASTS_AND_IS_CHECKS by object : DiagnosticGroup("Casts and is-checks") {
        val USELESS_CAST by warning<KtBinaryExpressionWithTypeRHS>(PositioningStrategy.AS_TYPE)
        val USELESS_IS_CHECK by warning<KtElement> {
            parameter<Boolean>("compileTimeCheckResult")
        }
        val IS_ENUM_ENTRY by error<KtTypeReference>()
        val ENUM_ENTRY_AS_TYPE by error<KtTypeReference>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
    }

    val WHEN_EXPRESSIONS by object : DiagnosticGroup("When expressions") {
        val EXPECTED_CONDITION by error<KtWhenCondition>()
        val NO_ELSE_IN_WHEN by error<KtWhenExpression>(PositioningStrategy.WHEN_EXPRESSION) {
            parameter<List<WhenMissingCase>>("missingWhenCases")
        }
        val NON_EXHAUSTIVE_WHEN_STATEMENT by warning<KtWhenExpression>(PositioningStrategy.WHEN_EXPRESSION) {
            parameter<String>("type")
            parameter<List<WhenMissingCase>>("missingWhenCases")
        }
        val INVALID_IF_AS_EXPRESSION by error<KtIfExpression>(PositioningStrategy.IF_EXPRESSION)
        val ELSE_MISPLACED_IN_WHEN by error<KtWhenEntry>(PositioningStrategy.ELSE_ENTRY)
        val ILLEGAL_DECLARATION_IN_WHEN_SUBJECT by error<KtElement> {
            parameter<String>("illegalReason")
        }
        val COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT by error<PsiElement>(PositioningStrategy.COMMAS)
    }

    val CONTEXT_TRACKING by object : DiagnosticGroup("Context tracking") {
        val TYPE_PARAMETER_IS_NOT_AN_EXPRESSION by error<KtSimpleNameExpression> {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }
        val TYPE_PARAMETER_ON_LHS_OF_DOT by error<KtSimpleNameExpression> {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }
        val NO_COMPANION_OBJECT by error<KtExpression>(PositioningStrategy.SELECTOR_BY_QUALIFIED) {
            parameter<FirRegularClassSymbol>("klass")
        }
        val EXPRESSION_EXPECTED_PACKAGE_FOUND by error<KtExpression>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
    }

    val FUNCTION_CONTRACTS by object : DiagnosticGroup("Function contracts") {
        val ERROR_IN_CONTRACT_DESCRIPTION by error<KtElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED) {
            parameter<String>("reason")
        }
    }

    val CONVENTIONS by object : DiagnosticGroup("Conventions") {
        val NO_GET_METHOD by error<KtArrayAccessExpression>(PositioningStrategy.ARRAY_ACCESS)
        val NO_SET_METHOD by error<KtArrayAccessExpression>(PositioningStrategy.ARRAY_ACCESS)
        val ITERATOR_MISSING by error<KtExpression>()
        val HAS_NEXT_MISSING by error<KtExpression>()
        val NEXT_MISSING by error<KtExpression>()
        val HAS_NEXT_FUNCTION_NONE_APPLICABLE by error<KtExpression> {
            parameter<Collection<FirBasedSymbol<*>>>("candidates")
        }
        val NEXT_NONE_APPLICABLE by error<KtExpression> {
            parameter<Collection<FirBasedSymbol<*>>>("candidates")
        }
        val DELEGATE_SPECIAL_FUNCTION_MISSING by error<KtExpression> {
            parameter<String>("expectedFunctionSignature")
            parameter<ConeKotlinType>("delegateType")
            parameter<String>("description")
        }
        val DELEGATE_SPECIAL_FUNCTION_AMBIGUITY by error<KtExpression> {
            parameter<String>("expectedFunctionSignature")
            parameter<Collection<FirBasedSymbol<*>>>("candidates")
        }
        val DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE by error<KtExpression> {
            parameter<String>("expectedFunctionSignature")
            parameter<Collection<FirBasedSymbol<*>>>("candidates")
        }
        val DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH by error<KtExpression> {
            parameter<String>("delegateFunction")
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }

        val UNDERSCORE_IS_RESERVED by error<PsiElement>(PositioningStrategy.NAME_IDENTIFIER)
        val UNDERSCORE_USAGE_WITHOUT_BACKTICKS by error<PsiElement>(PositioningStrategy.NAME_IDENTIFIER)
        val RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER by warning<KtNameReferenceExpression>()
        val INVALID_CHARACTERS by error<KtNamedDeclaration>(PositioningStrategy.NAME_IDENTIFIER) {
            parameter<String>("message")
        }
        val DANGEROUS_CHARACTERS by warning<KtNamedDeclaration>(PositioningStrategy.NAME_IDENTIFIER) {
            parameter<String>("characters")
        }

        val EQUALITY_NOT_APPLICABLE by error<KtBinaryExpression> {
            parameter<String>("operator")
            parameter<ConeKotlinType>("leftType")
            parameter<ConeKotlinType>("rightType")
        }
        val EQUALITY_NOT_APPLICABLE_WARNING by warning<KtBinaryExpression> {
            parameter<String>("operator")
            parameter<ConeKotlinType>("leftType")
            parameter<ConeKotlinType>("rightType")
        }
        val INCOMPATIBLE_ENUM_COMPARISON_ERROR by error<KtElement> {
            parameter<ConeKotlinType>("leftType")
            parameter<ConeKotlinType>("rightType")
        }
        val INC_DEC_SHOULD_NOT_RETURN_UNIT by error<KtExpression>(PositioningStrategy.OPERATOR)
        val ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT by error<KtExpression>(PositioningStrategy.OPERATOR) {
            parameter<FirNamedFunctionSymbol>("functionSymbol")
            parameter<String>("operator")
        }
    }

    val TYPE_ALIAS by object : DiagnosticGroup("Type alias") {
        val TOPLEVEL_TYPEALIASES_ONLY by error<KtTypeAlias>()
        val RECURSIVE_TYPEALIAS_EXPANSION by error<KtElement>()
        val TYPEALIAS_SHOULD_EXPAND_TO_CLASS by error<KtElement> {
            parameter<ConeKotlinType>("expandedType")
        }
    }

    val EXTENDED_CHECKERS by object : DiagnosticGroup("Extended checkers") {
        val REDUNDANT_VISIBILITY_MODIFIER by warning<KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER)
        val REDUNDANT_MODALITY_MODIFIER by warning<KtModifierListOwner>(PositioningStrategy.MODALITY_MODIFIER)
        val REDUNDANT_RETURN_UNIT_TYPE by warning<KtTypeReference>()
        val REDUNDANT_EXPLICIT_TYPE by warning<PsiElement>()
        val REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE by warning<PsiElement>()
        val CAN_BE_VAL by warning<KtDeclaration>(PositioningStrategy.VAL_OR_VAR_NODE)
        val CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT by warning<KtExpression>(PositioningStrategy.OPERATOR)
        val REDUNDANT_CALL_OF_CONVERSION_METHOD by warning<PsiElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
        val ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS by warning<KtExpression>(PositioningStrategy.OPERATOR)
        val EMPTY_RANGE by warning<PsiElement>()
        val REDUNDANT_SETTER_PARAMETER_TYPE by warning<PsiElement>()
        val UNUSED_VARIABLE by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val ASSIGNED_VALUE_IS_NEVER_READ by warning<PsiElement>()
        val VARIABLE_INITIALIZER_IS_REDUNDANT by warning<PsiElement>()
        val VARIABLE_NEVER_READ by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val USELESS_CALL_ON_NOT_NULL by warning<PsiElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
    }

    val RETURNS by object : DiagnosticGroup("Returns") {
        val RETURN_NOT_ALLOWED by error<KtReturnExpression>(PositioningStrategy.RETURN_WITH_LABEL)
        val RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY by error<KtReturnExpression>(PositioningStrategy.RETURN_WITH_LABEL)
        val NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY by error<KtDeclarationWithBody>(PositioningStrategy.DECLARATION_WITH_BODY)

        val ANONYMOUS_INITIALIZER_IN_INTERFACE by error<KtAnonymousInitializer>(PositioningStrategy.DECLARATION_SIGNATURE)
    }

    val INLINE by object : DiagnosticGroup("Inline") {
        val USAGE_IS_NOT_INLINABLE by error<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("parameter")
        }

        val NON_LOCAL_RETURN_NOT_ALLOWED by error<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("parameter")
        }

        val NOT_YET_SUPPORTED_IN_INLINE by error<KtDeclaration>(PositioningStrategy.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT) {
            parameter<String>("message")
        }

        val NOTHING_TO_INLINE by warning<KtDeclaration>(PositioningStrategy.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT)

        val NULLABLE_INLINE_PARAMETER by error<KtDeclaration>() {
            parameter<FirValueParameterSymbol>("parameter")
            parameter<Symbol>("function")
        }

        val RECURSION_IN_INLINE by error<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("symbol")
        }

        val NON_PUBLIC_CALL_FROM_PUBLIC_INLINE by error<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("inlineDeclaration")
            parameter<Symbol>("referencedDeclaration")
        }

        val PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE by error<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("inlineDeclaration")
            parameter<Symbol>("referencedDeclaration")
        }

        val PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR by error<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("inlineDeclaration")
            parameter<Symbol>("referencedDeclaration")
        }

        val PROTECTED_CALL_FROM_PUBLIC_INLINE by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("inlineDeclaration")
            parameter<Symbol>("referencedDeclaration")
        }

        val PRIVATE_CLASS_MEMBER_FROM_INLINE by error<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("inlineDeclaration")
            parameter<Symbol>("referencedDeclaration")
        }

        val SUPER_CALL_FROM_PUBLIC_INLINE by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("symbol")
        }

        val DECLARATION_CANT_BE_INLINED by error<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)

        val OVERRIDE_BY_INLINE by warning<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)

        val NON_INTERNAL_PUBLISHED_API by error<KtElement>()

        val INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE by error<KtElement>() {
            parameter<FirExpression>("defaultValue")
            parameter<FirValueParameterSymbol>("parameter")
        }

        val REIFIED_TYPE_PARAMETER_IN_OVERRIDE by error<KtElement>(PositioningStrategy.REIFIED_MODIFIER)

        val INLINE_PROPERTY_WITH_BACKING_FIELD by error<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)

        val ILLEGAL_INLINE_PARAMETER_MODIFIER by error<KtElement>(PositioningStrategy.INLINE_PARAMETER_MODIFIER)

        val INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED by error<KtParameter>()

        val REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE by warning<KtElement>(PositioningStrategy.SUSPEND_MODIFIER)
    }

    val IMPORTS by object : DiagnosticGroup("Imports") {
        val CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON by error<KtImportDirective>(PositioningStrategy.IMPORT_LAST_NAME) {
            parameter<Name>("objectName")
        }

        val PACKAGE_CANNOT_BE_IMPORTED by error<KtImportDirective>(PositioningStrategy.IMPORT_LAST_NAME)

        val CANNOT_BE_IMPORTED by error<KtImportDirective>(PositioningStrategy.IMPORT_LAST_NAME) {
            parameter<Name>("name")
        }

        val CONFLICTING_IMPORT by error<KtImportDirective>(PositioningStrategy.IMPORT_LAST_NAME) {
            parameter<Name>("name")
        }

        val OPERATOR_RENAMED_ON_IMPORT by error<KtImportDirective>(PositioningStrategy.IMPORT_LAST_NAME)
    }

    val SUSPEND by object : DiagnosticGroup("Suspend errors") {
        val ILLEGAL_SUSPEND_FUNCTION_CALL by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Symbol>("suspendCallable")
        }
        val ILLEGAL_SUSPEND_PROPERTY_ACCESS by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Symbol>("suspendCallable")
        }
        val NON_LOCAL_SUSPENSION_POINT by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND by error<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val RETURN_FOR_BUILT_IN_SUSPEND by error<KtReturnExpression>()
    }
}

private val exposedVisibilityDiagnosticInit: DiagnosticBuilder.() -> Unit = {
    parameter<EffectiveVisibility>("elementVisibility")
    parameter<Symbol>("restrictingDeclaration")
    parameter<EffectiveVisibility>("restrictingVisibility")
}

private inline fun <reified P : PsiElement> AbstractDiagnosticGroup.exposedVisibilityError(
    positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT
): PropertyDelegateProvider<Any?, ReadOnlyProperty<AbstractDiagnosticGroup, RegularDiagnosticData>> {
    return error<P>(positioningStrategy, exposedVisibilityDiagnosticInit)
}

private inline fun <reified P : PsiElement> AbstractDiagnosticGroup.exposedVisibilityWarning(
    positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT
): PropertyDelegateProvider<Any?, ReadOnlyProperty<AbstractDiagnosticGroup, RegularDiagnosticData>> {
    return warning<P>(positioningStrategy, exposedVisibilityDiagnosticInit)
}

typealias Symbol = FirBasedSymbol<*>
