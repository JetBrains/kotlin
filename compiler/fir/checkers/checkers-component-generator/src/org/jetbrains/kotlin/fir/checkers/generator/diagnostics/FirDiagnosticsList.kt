/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.RelationToType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.*
import org.jetbrains.kotlin.fir.declarations.FirDeprecationInfo
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.PrivateForInline
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

@Suppress("UNUSED_VARIABLE", "LocalVariableName", "ClassName", "unused")
@OptIn(PrivateForInline::class)
object DIAGNOSTICS_LIST : DiagnosticList("FirErrors") {
    val MetaErrors by object : DiagnosticGroup("Meta-errors") {
        val UNSUPPORTED by warning<PsiElement> {
            parameter<String>("unsupported")
        }
        val UNSUPPORTED_FEATURE by warning<PsiElement> {
            parameter<Pair<LanguageFeature, LanguageVersionSettings>>("unsupportedFeature")
        }
        val UNSUPPORTED_SUSPEND_TEST by warning<PsiElement>()
        val NEW_INFERENCE_ERROR by warning<PsiElement> {
            parameter<String>("error")
        }
    }

    val Miscellaneous by object : DiagnosticGroup("Miscellaneous") {
        val OTHER_ERROR by warning<PsiElement>()

        val OTHER_ERROR_WITH_REASON by warning<PsiElement> {
            parameter<String>("reason")
        }
    }

    val GENERAL_SYNTAX by object : DiagnosticGroup("General syntax") {
        val ILLEGAL_CONST_EXPRESSION by warning<PsiElement>()
        val ILLEGAL_UNDERSCORE by warning<PsiElement>()
        val EXPRESSION_EXPECTED by warning<PsiElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
        val ASSIGNMENT_IN_EXPRESSION_CONTEXT by warning<KtBinaryExpression>()
        val BREAK_OR_CONTINUE_OUTSIDE_A_LOOP by warning<PsiElement>()
        val NOT_A_LOOP_LABEL by warning<PsiElement>()
        val BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY by warning<KtExpressionWithLabel>()
        val VARIABLE_EXPECTED by warning<PsiElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
        val DELEGATION_IN_INTERFACE by warning<PsiElement>()
        val DELEGATION_NOT_TO_INTERFACE by warning<PsiElement>()
        val NESTED_CLASS_NOT_ALLOWED by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<String>("declaration")
        }
        val NESTED_CLASS_NOT_ALLOWED_IN_LOCAL by deprecationError<KtNamedDeclaration>(
            LanguageFeature.ForbidCompanionInLocalInnerClass,
            PositioningStrategy.DECLARATION_NAME
        ) {
            parameter<String>("declaration")
        }
        val INCORRECT_CHARACTER_LITERAL by warning<PsiElement>()
        val EMPTY_CHARACTER_LITERAL by warning<PsiElement>()
        val TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL by warning<PsiElement>()
        val ILLEGAL_ESCAPE by warning<PsiElement>()
        val INT_LITERAL_OUT_OF_RANGE by warning<PsiElement>()
        val FLOAT_LITERAL_OUT_OF_RANGE by warning<PsiElement>()
        val WRONG_LONG_SUFFIX by warning<KtElement>(PositioningStrategy.LONG_LITERAL_SUFFIX)
        val UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH by warning<KtElement>()
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
        val INVISIBLE_SETTER by warning<PsiElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED) {
            parameter<FirPropertySymbol>("property")
            parameter<Visibility>("visibility")
            parameter<CallableId>("callableId")
        }
        val INNER_ON_TOP_LEVEL_SCRIPT_CLASS by deprecationError<PsiElement>(LanguageFeature.ProhibitScriptTopLevelInnerClasses)
        val ERROR_SUPPRESSION by warning<PsiElement> {
            parameter<String>("diagnosticName")
        }
        val MISSING_CONSTRUCTOR_KEYWORD by warning<PsiElement>()
        val REDUNDANT_INTERPOLATION_PREFIX by warning<PsiElement>()
        val WRAPPED_LHS_IN_ASSIGNMENT by deprecationError<PsiElement>(
            LanguageFeature.ForbidParenthesizedLhsInAssignments,
            PositioningStrategy.OUTERMOST_PARENTHESES_IN_ASSIGNMENT_LHS,
        )
    }

    val UNRESOLVED by object : DiagnosticGroup("Unresolved") {
        val INVISIBLE_REFERENCE by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("reference")
            parameter<Visibility>("visible")
            parameter<ClassId?>("containingDeclaration")
        }
        val UNRESOLVED_REFERENCE by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("reference")
            parameter<String?>("operator")
        }
        val UNRESOLVED_LABEL by warning<PsiElement>(PositioningStrategy.LABEL)
        val AMBIGUOUS_LABEL by warning<PsiElement>(PositioningStrategy.LABEL)
        val LABEL_NAME_CLASH by warning<PsiElement>(PositioningStrategy.LABEL)
        val DESERIALIZATION_ERROR by warning<PsiElement>()
        val ERROR_FROM_JAVA_RESOLUTION by warning<PsiElement>()
        val MISSING_STDLIB_CLASS by warning<PsiElement>()
        val NO_THIS by warning<PsiElement>()

        val DEPRECATION_ERROR by warning<PsiElement>(PositioningStrategy.DEPRECATION) {
            parameter<Symbol>("reference")
            parameter<String>("message")
            isSuppressible = true
        }

        val DEPRECATION by warning<PsiElement>(PositioningStrategy.DEPRECATION) {
            parameter<Symbol>("reference")
            parameter<String>("message")
        }

        val VERSION_REQUIREMENT_DEPRECATION_ERROR by warning<PsiElement>(PositioningStrategy.DEPRECATION) {
            parameter<Symbol>("reference")
            parameter<VersionRequirement.Version>("version")
            parameter<String>("currentVersion")
            parameter<String>("message")
        }

        val VERSION_REQUIREMENT_DEPRECATION by warning<PsiElement>(PositioningStrategy.DEPRECATION) {
            parameter<Symbol>("reference")
            parameter<VersionRequirement.Version>("version")
            parameter<String>("currentVersion")
            parameter<String>("message")
        }

        val TYPEALIAS_EXPANSION_DEPRECATION_ERROR by warning<PsiElement>(PositioningStrategy.DEPRECATION) {
            parameter<Symbol>("alias")
            parameter<Symbol>("reference")
            parameter<String>("message")
            isSuppressible = true
        }
        val TYPEALIAS_EXPANSION_DEPRECATION by warning<PsiElement>(PositioningStrategy.DEPRECATION) {
            parameter<Symbol>("alias")
            parameter<Symbol>("reference")
            parameter<String>("message")
        }

        val API_NOT_AVAILABLE by warning<PsiElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED) {
            parameter<ApiVersion>("sinceKotlinVersion")
            parameter<ApiVersion>("currentVersion")
        }

        val UNRESOLVED_REFERENCE_WRONG_RECEIVER by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Collection<Symbol>>("candidates")
        }
        val UNRESOLVED_IMPORT by warning<PsiElement>(PositioningStrategy.IMPORT_LAST_NAME) {
            parameter<String>("reference")
        }

        val PLACEHOLDER_PROJECTION_IN_QUALIFIER by warning<PsiElement>()

        val DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE by warning<PsiElement>()

        val MISSING_DEPENDENCY_CLASS by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<ConeKotlinType>("type")
        }
        val MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<ConeKotlinType>("type")
        }
        val MISSING_DEPENDENCY_SUPERCLASS by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<ConeKotlinType>("missingType")
            parameter<ConeKotlinType>("declarationType")
        }
        val MISSING_DEPENDENCY_SUPERCLASS_WARNING by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<ConeKotlinType>("missingType")
            parameter<ConeKotlinType>("declarationType")
        }
        val MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<ConeKotlinType>("missingType")
            parameter<ConeKotlinType>("declarationType")
        }
        val MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<ConeKotlinType>("type")
            parameter<Name>("parameterName")
        }
        val MISSING_DEPENDENCY_CLASS_IN_LAMBDA_RECEIVER by warning<PsiElement> {
            parameter<ConeKotlinType>("type")
        }
    }

    val CALL_RESOLUTION by object : DiagnosticGroup("Call resolution") {
        val CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS by warning<KtExpression>()
        val NO_CONSTRUCTOR by warning<PsiElement>(PositioningStrategy.VALUE_ARGUMENTS_LIST)
        val FUNCTION_CALL_EXPECTED by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("functionName")
            parameter<Boolean>("hasValueParameters")
        }
        val ILLEGAL_SELECTOR by warning<PsiElement>()
        val NO_RECEIVER_ALLOWED by warning<PsiElement>()
        val FUNCTION_EXPECTED by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("expression")
            parameter<ConeKotlinType>("type")
        }
        val INTERFACE_AS_FUNCTION by warning<PsiElement> {
            parameter<FirRegularClassSymbol>("classSymbol")
        }
        val EXPECT_CLASS_AS_FUNCTION by warning<PsiElement> {
            parameter<FirRegularClassSymbol>("classSymbol")
        }
        val INNER_CLASS_CONSTRUCTOR_NO_RECEIVER by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirRegularClassSymbol>("classSymbol")
        }
        val PLUGIN_AMBIGUOUS_INTERCEPTED_SYMBOL by warning<PsiElement> {
            parameter<List<String>>("names")
        }
        val RESOLUTION_TO_CLASSIFIER by warning<PsiElement> {
            parameter<FirRegularClassSymbol>("classSymbol")
        }
        val AMBIGUOUS_ALTERED_ASSIGN by warning<PsiElement> {
            parameter<List<String?>>("altererNames")
        }
        val SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR by warning<PsiElement>()
    }

    val SUPER by object : DiagnosticGroup("Super") {
        val SUPER_IS_NOT_AN_EXPRESSION by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val SUPER_NOT_AVAILABLE by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val ABSTRACT_SUPER_CALL by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val ABSTRACT_SUPER_CALL_WARNING by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val INSTANCE_ACCESS_BEFORE_SUPER_CALL by warning<PsiElement> {
            parameter<String>("target")
        }
        val SUPER_CALL_WITH_DEFAULT_PARAMETERS by warning<PsiElement> {
            parameter<String>("name")
        }
    }

    val SUPERTYPES by object : DiagnosticGroup("Supertypes") {
        val NOT_A_SUPERTYPE by warning<PsiElement>()
        val TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER by warning<KtElement>()
        val SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE by warning<PsiElement>()
        val SUPERTYPE_INITIALIZED_IN_INTERFACE by warning<KtElement>()
        val INTERFACE_WITH_SUPERCLASS by warning<KtElement>()
        val FINAL_SUPERTYPE by warning<KtElement>()
        val CLASS_CANNOT_BE_EXTENDED_DIRECTLY by warning<KtElement> {
            parameter<FirRegularClassSymbol>("classSymbol")
        }
        val SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE by warning<KtElement>()
        val SINGLETON_IN_SUPERTYPE by warning<KtElement>()
        val NULLABLE_SUPERTYPE by warning<KtElement>(PositioningStrategy.QUESTION_MARK_BY_TYPE)
        val NULLABLE_SUPERTYPE_THROUGH_TYPEALIAS by deprecationError<KtElement>(LanguageFeature.ProhibitNullableTypeThroughTypealias)
        val MANY_CLASSES_IN_SUPERTYPE_LIST by warning<KtElement>()
        val SUPERTYPE_APPEARS_TWICE by warning<KtElement>()
        val CLASS_IN_SUPERTYPE_FOR_ENUM by warning<KtElement>()
        val SEALED_SUPERTYPE by warning<KtElement>()
        val SEALED_SUPERTYPE_IN_LOCAL_CLASS by warning<KtElement> {
            parameter<String>("declarationType")
            parameter<ClassKind>("sealedClassKind")
        }
        val SEALED_INHERITOR_IN_DIFFERENT_PACKAGE by warning<KtElement>()
        val SEALED_INHERITOR_IN_DIFFERENT_MODULE by warning<KtElement>()
        val CLASS_INHERITS_JAVA_SEALED_CLASS by warning<KtElement>()
        val UNSUPPORTED_SEALED_FUN_INTERFACE by warning<PsiElement>()
        val SUPERTYPE_NOT_A_CLASS_OR_INTERFACE by warning<KtElement> {
            parameter<String>("reason")
        }
        val UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION by warning<PsiElement>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirBasedSymbol<*>>("symbol")
        }
        val CYCLIC_INHERITANCE_HIERARCHY by warning<PsiElement>()
        val EXPANDED_TYPE_CANNOT_BE_INHERITED by warning<KtTypeReference> {
            parameter<ConeKotlinType>("type")
        }
        val PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE by warning<KtModifierListOwner>(PositioningStrategy.VARIANCE_MODIFIER)
        val INCONSISTENT_TYPE_PARAMETER_VALUES by warning<KtClassOrObject>(PositioningStrategy.SUPERTYPES_LIST) {
            parameter<FirTypeParameterSymbol>("typeParameter")
            parameter<FirClassSymbol<*>>("type")
            parameter<Collection<ConeKotlinType>>("bounds")
        }
        val INCONSISTENT_TYPE_PARAMETER_BOUNDS by warning<PsiElement> {
            parameter<FirTypeParameterSymbol>("typeParameter")
            parameter<FirClassSymbol<*>>("type")
            parameter<Collection<ConeKotlinType>>("bounds")
        }
        val AMBIGUOUS_SUPER by warning<KtSuperExpression> {
            parameter<List<ConeKotlinType>>("candidates")
        }
    }

    val CONSTRUCTOR_PROBLEMS by object : DiagnosticGroup("Constructor problems") {
        val CONSTRUCTOR_IN_OBJECT by warning<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val CONSTRUCTOR_IN_INTERFACE by warning<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val NON_PRIVATE_CONSTRUCTOR_IN_ENUM by warning<PsiElement>()
        val NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED by warning<PsiElement>()
        val CYCLIC_CONSTRUCTOR_DELEGATION_CALL by warning<PsiElement>()
        val PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED by warning<PsiElement>(PositioningStrategy.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)
        val PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL by warning<KtExpression> {
            parameter<FirBasedSymbol<*>>("symbol")
        }

        // TODO: change it to KtSuperTypeEntry when possible (after re-targeter implementation)
        val SUPERTYPE_NOT_INITIALIZED by warning<KtElement>()
        val SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR by warning<PsiElement>()
        val DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR by warning<PsiElement>()
        val EXPLICIT_DELEGATION_CALL_REQUIRED by warning<PsiElement>(PositioningStrategy.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)
        val SEALED_CLASS_CONSTRUCTOR_CALL by warning<PsiElement>()

        val DATA_CLASS_CONSISTENT_COPY_AND_EXPOSED_COPY_ARE_INCOMPATIBLE_ANNOTATIONS by warning<KtAnnotationEntry>()
        val DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET by warning<KtAnnotationEntry>()

        val DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED by deprecationError<KtPrimaryConstructor>(
            LanguageFeature.ErrorAboutDataClassCopyVisibilityChange,
            PositioningStrategy.VISIBILITY_MODIFIER
        )

        val DATA_CLASS_INVISIBLE_COPY_USAGE by deprecationError<KtNameReferenceExpression>(
            LanguageFeature.ErrorAboutDataClassCopyVisibilityChange
        )

        // TODO: Consider creating a parameter list position strategy and report on the parameter list instead
        val DATA_CLASS_WITHOUT_PARAMETERS by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val DATA_CLASS_VARARG_PARAMETER by warning<KtParameter>()
        val DATA_CLASS_NOT_PROPERTY_PARAMETER by warning<KtParameter>()
    }

    val ANNOTATIONS by object : DiagnosticGroup("Annotations") {
        val ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR by warning<KtExpression>()
        val ANNOTATION_ARGUMENT_MUST_BE_CONST by warning<KtExpression>()
        val ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST by warning<KtExpression>()
        val ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL by warning<KtExpression>()
        val ANNOTATION_CLASS_MEMBER by warning<PsiElement>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT by warning<KtExpression>()
        val INVALID_TYPE_OF_ANNOTATION_MEMBER by warning<KtElement>()
        val PROJECTION_IN_TYPE_OF_ANNOTATION_MEMBER by deprecationError<KtTypeReference>(LanguageFeature.ForbidProjectionsInAnnotationProperties)
        val LOCAL_ANNOTATION_CLASS_ERROR by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME)
        val MISSING_VAL_ON_ANNOTATION_PARAMETER by warning<KtParameter>()
        val NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION by warning<KtExpression>()
        val CYCLE_IN_ANNOTATION_PARAMETER by deprecationError<KtParameter>(LanguageFeature.ProhibitCyclesInAnnotations)
        val ANNOTATION_CLASS_CONSTRUCTOR_CALL by warning<KtElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
        val ENUM_CLASS_CONSTRUCTOR_CALL by warning<KtElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
        val NOT_AN_ANNOTATION_CLASS by warning<PsiElement> {
            parameter<String>("annotationName")
        }
        val NULLABLE_TYPE_OF_ANNOTATION_MEMBER by warning<KtElement>()
        val VAR_ANNOTATION_PARAMETER by warning<KtParameter>(PositioningStrategy.VAL_OR_VAR_NODE)
        val SUPERTYPES_FOR_ANNOTATION_CLASS by warning<KtClass>(PositioningStrategy.SUPERTYPES_LIST)
        val ANNOTATION_USED_AS_ANNOTATION_ARGUMENT by warning<KtAnnotationEntry>()
        val ANNOTATION_ON_ANNOTATION_ARGUMENT by warning<KtAnnotationEntry>()
        val ILLEGAL_KOTLIN_VERSION_STRING_VALUE by warning<KtExpression>()
        val NEWER_VERSION_IN_SINCE_KOTLIN by warning<KtExpression> {
            parameter<String>("specifiedVersion")
        }
        val DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS by warning<PsiElement>()
        val DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS by warning<PsiElement>()
        val DEPRECATED_SINCE_KOTLIN_WITHOUT_DEPRECATED by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)

        val KOTLIN_ACTUAL_ANNOTATION_HAS_NO_EFFECT_IN_KOTLIN by warning<KtElement>()

        val OVERRIDE_DEPRECATION by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<Symbol>("overridenSymbol")
            parameter<FirDeprecationInfo>("deprecationInfo")
        }

        val REDUNDANT_ANNOTATION by warning<KtAnnotationEntry> {
            parameter<ClassId>("annotation")
        }

        val ANNOTATION_ON_SUPERCLASS_ERROR by warning<KtAnnotationEntry>()
        val RESTRICTED_RETENTION_FOR_EXPRESSION_ANNOTATION_ERROR by warning<PsiElement>()
        val WRONG_ANNOTATION_TARGET by warning<KtAnnotationEntry> {
            parameter<String>("actualTarget")
            parameter<Collection<KotlinTarget>>("allowedTargets")
        }
        val WRONG_ANNOTATION_TARGET_WARNING by warning<KtAnnotationEntry> {
            parameter<String>("actualTarget")
            parameter<Collection<KotlinTarget>>("allowedTargets")
        }
        val WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET by warning<KtAnnotationEntry> {
            parameter<String>("actualTarget")
            parameter<String>("useSiteTarget")
            parameter<Collection<KotlinTarget>>("allowedTargets")
        }
        val ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION by deprecationError<KtAnnotationEntry>(
            LanguageFeature.ForbidAnnotationsWithUseSiteTargetOnExpressions
        )
        val INAPPLICABLE_TARGET_ON_PROPERTY by warning<KtAnnotationEntry> {
            parameter<String>("useSiteDescription")
        }
        val INAPPLICABLE_TARGET_ON_PROPERTY_WARNING by warning<KtAnnotationEntry> {
            parameter<String>("useSiteDescription")
        }
        val INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE by warning<KtAnnotationEntry> {
            parameter<String>("useSiteDescription")
        }
        val INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE by warning<KtAnnotationEntry>()
        val INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD by warning<KtAnnotationEntry>()
        val INAPPLICABLE_PARAM_TARGET by warning<KtAnnotationEntry>()
        val REDUNDANT_ANNOTATION_TARGET by warning<KtAnnotationEntry> {
            parameter<String>("useSiteDescription")
        }
        val INAPPLICABLE_FILE_TARGET by warning<KtAnnotationEntry>(PositioningStrategy.ANNOTATION_USE_SITE)
        val INAPPLICABLE_ALL_TARGET by warning<KtAnnotationEntry>(PositioningStrategy.ANNOTATION_USE_SITE)
        val INAPPLICABLE_ALL_TARGET_IN_MULTI_ANNOTATION by warning<KtAnnotationEntry>()
        val REPEATED_ANNOTATION by warning<PsiElement>()
        val REPEATED_ANNOTATION_WARNING by warning<PsiElement>()
        val NOT_A_CLASS by warning<PsiElement>()
        val WRONG_EXTENSION_FUNCTION_TYPE by warning<KtAnnotationEntry>()
        val WRONG_EXTENSION_FUNCTION_TYPE_WARNING by warning<KtAnnotationEntry>()
        val ANNOTATION_IN_WHERE_CLAUSE_ERROR by warning<KtAnnotationEntry>()
        val ANNOTATION_IN_CONTRACT_ERROR by warning<KtElement>()

        val COMPILER_REQUIRED_ANNOTATION_AMBIGUITY by warning<PsiElement> {
            parameter<ConeKotlinType>("typeFromCompilerPhase")
            parameter<ConeKotlinType>("typeFromTypesPhase")
        }

        val AMBIGUOUS_ANNOTATION_ARGUMENT by warning<PsiElement> {
            parameter<List<FirBasedSymbol<*>>>("symbols")
        }

        val VOLATILE_ON_VALUE by warning<KtAnnotationEntry>()
        val VOLATILE_ON_DELEGATE by warning<KtAnnotationEntry>()

        val NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION by warning<KtAnnotationEntry>()

        val POTENTIALLY_NON_REPORTED_ANNOTATION by warning<KtAnnotationEntry>()

        val ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD by warning<KtAnnotationEntry> {
            parameter<String>("useSiteDescription")
        }
        val ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE by warning<PsiElement>()

        val IGNORABILITY_ANNOTATIONS_WITH_CHECKER_DISABLED by warning<KtAnnotationEntry>()

        val DSL_MARKER_PROPAGATES_TO_MANY by warning<KtAnnotationEntry>()
    }

    val OPT_IN by object : DiagnosticGroup("OptIn") {
        val OPT_IN_USAGE by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<ClassId>("optInMarkerClassId")
            parameter<String>("message")
        }
        val OPT_IN_USAGE_ERROR by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<ClassId>("optInMarkerClassId")
            parameter<String>("message")
            isSuppressible = true
        }
        val OPT_IN_TO_INHERITANCE by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<ClassId>("optInMarkerClassId")
            parameter<String>("message")
        }
        val OPT_IN_TO_INHERITANCE_ERROR by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<ClassId>("optInMarkerClassId")
            parameter<String>("message")
            isSuppressible = true
        }
        val OPT_IN_OVERRIDE by warning<PsiElement>(PositioningStrategy.DECLARATION_NAME) {
            parameter<ClassId>("optInMarkerClassId")
            parameter<String>("message")
        }
        val OPT_IN_OVERRIDE_ERROR by warning<PsiElement>(PositioningStrategy.DECLARATION_NAME) {
            parameter<ClassId>("optInMarkerClassId")
            parameter<String>("message")
            isSuppressible = true
        }

        val OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION by warning<PsiElement>()
        val OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN by warning<PsiElement>()

        val OPT_IN_WITHOUT_ARGUMENTS by warning<KtAnnotationEntry>()
        val OPT_IN_ARGUMENT_IS_NOT_MARKER by warning<KtClassLiteralExpression> {
            parameter<ClassId>("notMarkerClassId")
        }
        val OPT_IN_MARKER_WITH_WRONG_TARGET by warning<KtAnnotationEntry> {
            parameter<String>("target")
        }
        val OPT_IN_MARKER_WITH_WRONG_RETENTION by warning<KtAnnotationEntry>()

        val OPT_IN_MARKER_ON_WRONG_TARGET by warning<KtAnnotationEntry> {
            parameter<String>("target")
        }
        val OPT_IN_MARKER_ON_OVERRIDE by warning<KtAnnotationEntry>()
        val OPT_IN_MARKER_ON_OVERRIDE_WARNING by warning<KtAnnotationEntry>()

        val SUBCLASS_OPT_IN_INAPPLICABLE by warning<KtAnnotationEntry> {
            parameter<String>("target")
        }
        val SUBCLASS_OPT_IN_ARGUMENT_IS_NOT_MARKER by warning<KtClassLiteralExpression> {
            parameter<ClassId>("notMarkerClassId")
        }
    }

    val EXPOSED_VISIBILITY by object : DiagnosticGroup("Exposed visibility") {
        val EXPOSED_TYPEALIAS_EXPANDED_TYPE by exposedVisibilityError<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val EXPOSED_FUNCTION_RETURN_TYPE by exposedVisibilityError<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val EXPOSED_RECEIVER_TYPE by exposedVisibilityError<KtElement>()
        val EXPOSED_PROPERTY_TYPE by exposedVisibilityError<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR by exposedVisibilityError<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val EXPOSED_PARAMETER_TYPE by exposedVisibilityError<KtParameter>(/* // NB: for parameter FE 1.0 reports not on a name for some reason */)
        val EXPOSED_SUPER_INTERFACE by exposedVisibilityError<KtElement>()
        val EXPOSED_SUPER_CLASS by exposedVisibilityError<KtElement>()
        val EXPOSED_TYPE_PARAMETER_BOUND by exposedVisibilityError<KtElement>()
        val EXPOSED_TYPE_PARAMETER_BOUND_DEPRECATION_WARNING by exposedVisibilityWarning<KtElement>()
        val EXPOSED_PACKAGE_PRIVATE_TYPE_FROM_INTERNAL_WARNING by exposedVisibilityWarning<KtElement>()
    }

    val MODIFIERS by object : DiagnosticGroup("Modifiers") {
        val INAPPLICABLE_INFIX_MODIFIER by warning<PsiElement>(PositioningStrategy.INFIX_MODIFIER)
        val REPEATED_MODIFIER by warning<PsiElement> {
            parameter<KtModifierKeywordToken>("modifier")
        }
        val REDUNDANT_MODIFIER by warning<PsiElement> {
            parameter<KtModifierKeywordToken>("redundantModifier")
            parameter<KtModifierKeywordToken>("conflictingModifier")
        }
        val DEPRECATED_MODIFIER by warning<PsiElement> {
            parameter<KtModifierKeywordToken>("deprecatedModifier")
            parameter<KtModifierKeywordToken>("actualModifier")
        }
        val DEPRECATED_MODIFIER_PAIR by warning<PsiElement> {
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
        val INCOMPATIBLE_MODIFIERS by warning<PsiElement> {
            parameter<KtModifierKeywordToken>("modifier1")
            parameter<KtModifierKeywordToken>("modifier2")
        }
        val REDUNDANT_OPEN_IN_INTERFACE by warning<KtModifierListOwner>(PositioningStrategy.OPEN_MODIFIER)
        val WRONG_MODIFIER_TARGET by warning<PsiElement> {
            parameter<KtModifierKeywordToken>("modifier")
            parameter<String>("target")
        }
        val OPERATOR_MODIFIER_REQUIRED by warning<PsiElement> {
            parameter<FirNamedFunctionSymbol>("functionSymbol")
        }
        val OPERATOR_CALL_ON_CONSTRUCTOR by warning<PsiElement> {
            parameter<String>("name")
        }
        val INFIX_MODIFIER_REQUIRED by warning<PsiElement> {
            parameter<FirNamedFunctionSymbol>("functionSymbol")
        }
        val WRONG_MODIFIER_CONTAINING_DECLARATION by warning<PsiElement> {
            parameter<KtModifierKeywordToken>("modifier")
            parameter<String>("target")
        }
        val DEPRECATED_MODIFIER_CONTAINING_DECLARATION by warning<PsiElement> {
            parameter<KtModifierKeywordToken>("modifier")
            parameter<String>("target")
        }
        val INAPPLICABLE_OPERATOR_MODIFIER by warning<PsiElement>(PositioningStrategy.OPERATOR_MODIFIER) {
            parameter<String>("message")
        }
        val INAPPLICABLE_OPERATOR_MODIFIER_WARNING by warning<PsiElement>(PositioningStrategy.OPERATOR_MODIFIER) {
            parameter<String>("message")
        }

        val NO_EXPLICIT_VISIBILITY_IN_API_MODE by warning<KtDeclaration>(PositioningStrategy.DECLARATION_START_TO_NAME)
        val NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING by warning<KtDeclaration>(PositioningStrategy.DECLARATION_START_TO_NAME)

        val NO_EXPLICIT_RETURN_TYPE_IN_API_MODE by warning<KtDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING by warning<KtDeclaration>(PositioningStrategy.DECLARATION_NAME)

        val ANONYMOUS_SUSPEND_FUNCTION by warning<KtDeclaration>(PositioningStrategy.SUSPEND_MODIFIER)
    }

    val VALUE_CLASSES by object : DiagnosticGroup("Value classes") {
        val VALUE_CLASS_NOT_TOP_LEVEL by warning<KtDeclaration>(PositioningStrategy.INLINE_OR_VALUE_MODIFIER)
        val VALUE_CLASS_NOT_FINAL by warning<KtDeclaration>(PositioningStrategy.MODALITY_MODIFIER)
        val ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS by warning<KtDeclaration>(PositioningStrategy.INLINE_OR_VALUE_MODIFIER)
        val INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE by warning<KtElement>()
        val VALUE_CLASS_EMPTY_CONSTRUCTOR by warning<KtElement>()
        val VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER by warning<KtParameter>()
        val PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val DELEGATED_PROPERTY_INSIDE_VALUE_CLASS by warning<PsiElement>()
        val VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE by warning<KtElement> {
            parameter<ConeKotlinType>("type")
        }
        val VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION by warning<PsiElement>()
        val VALUE_CLASS_CANNOT_EXTEND_CLASSES by warning<KtElement>()
        val VALUE_CLASS_CANNOT_BE_RECURSIVE by warning<KtElement>()
        val MULTI_FIELD_VALUE_CLASS_PRIMARY_CONSTRUCTOR_DEFAULT_PARAMETER by warning<KtExpression>()
        val SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS by warning<PsiElement>()
        val RESERVED_MEMBER_INSIDE_VALUE_CLASS by warning<KtFunction>(PositioningStrategy.DECLARATION_NAME) {
            parameter<String>("name")
        }
        val RESERVED_MEMBER_FROM_INTERFACE_INSIDE_VALUE_CLASS by warning<KtClass>(PositioningStrategy.DECLARATION_NAME) {
            parameter<String>("interfaceName")
            parameter<String>("methodName")
        }
        val TYPE_ARGUMENT_ON_TYPED_VALUE_CLASS_EQUALS by warning<KtElement>()
        val INNER_CLASS_INSIDE_VALUE_CLASS by warning<KtDeclaration>(PositioningStrategy.INNER_MODIFIER)
        val VALUE_CLASS_CANNOT_BE_CLONEABLE by warning<KtDeclaration>(PositioningStrategy.INLINE_OR_VALUE_MODIFIER)
        val VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS by warning<KtDeclaration>(PositioningStrategy.CONTEXT_KEYWORD)
        val ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET by warning<KtAnnotationEntry> {
            parameter<String>("name")
        }
    }

    val APPLICABILITY by object : DiagnosticGroup("Applicability") {
        val NONE_APPLICABLE by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Collection<Symbol>>("candidates")
        }

        val INAPPLICABLE_CANDIDATE by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("candidate")
        }

        val TYPE_MISMATCH by warning<PsiElement> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
            parameter<Boolean>("isMismatchDueToNullability")
        }

        val TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR by warning<PsiElement>() {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }

        val THROWABLE_TYPE_MISMATCH by warning<PsiElement> {
            parameter<ConeKotlinType>("actualType")
            parameter<Boolean>("isMismatchDueToNullability")
        }

        val CONDITION_TYPE_MISMATCH by warning<PsiElement> {
            parameter<ConeKotlinType>("actualType")
            parameter<Boolean>("isMismatchDueToNullability")
        }

        val ARGUMENT_TYPE_MISMATCH by warning<PsiElement> {
            parameter<ConeKotlinType>("actualType")
            parameter<ConeKotlinType>("expectedType")
            parameter<Boolean>("isMismatchDueToNullability")
        }

        val MEMBER_PROJECTED_OUT by warning<PsiElement> {
            parameter<ConeKotlinType>("receiver")
            parameter<String>("projection")
            parameter<FirCallableSymbol<*>>("symbol")
        }

        val NULL_FOR_NONNULL_TYPE by warning<PsiElement> {
            parameter<ConeKotlinType>("expectedType")
        }

        val INAPPLICABLE_LATEINIT_MODIFIER by warning<KtModifierListOwner>(PositioningStrategy.LATEINIT_MODIFIER) {
            parameter<String>("reason")
        }

        // TODO: reset to KtExpression after fixsing lambda argument sources
        val VARARG_OUTSIDE_PARENTHESES by warning<KtElement>()

        val NAMED_ARGUMENTS_NOT_ALLOWED by warning<KtValueArgument>(PositioningStrategy.NAME_OF_NAMED_ARGUMENT) {
            parameter<ForbiddenNamedArgumentsTarget>("forbiddenNamedArgumentsTarget")
        }

        val NON_VARARG_SPREAD by warning<LeafPsiElement>()
        val ARGUMENT_PASSED_TWICE by warning<KtValueArgument>(PositioningStrategy.NAME_OF_NAMED_ARGUMENT)
        val TOO_MANY_ARGUMENTS by warning<PsiElement> {
            parameter<FirCallableSymbol<*>>("function")
        }
        val UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE by warning<PsiElement>()
        val NO_VALUE_FOR_PARAMETER by warning<KtElement>(PositioningStrategy.VALUE_ARGUMENTS) {
            parameter<Name>("violatedParameter")
        }

        val NAMED_PARAMETER_NOT_FOUND by warning<KtValueArgument>(PositioningStrategy.NAME_OF_NAMED_ARGUMENT) {
            parameter<String>("name")
        }
        val NAME_FOR_AMBIGUOUS_PARAMETER by warning<KtValueArgument>(PositioningStrategy.NAME_OF_NAMED_ARGUMENT)
        val MIXING_NAMED_AND_POSITIONAL_ARGUMENTS by warning<PsiElement>()

        val ASSIGNMENT_TYPE_MISMATCH by warning<KtExpression> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
            parameter<Boolean>("isMismatchDueToNullability")
        }

        val RESULT_TYPE_MISMATCH by warning<KtExpression> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }

        val MANY_LAMBDA_EXPRESSION_ARGUMENTS by warning<KtLambdaExpression>()

        val SPREAD_OF_NULLABLE by warning<PsiElement>(PositioningStrategy.SPREAD_OPERATOR)

        val ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION by deprecationError<KtExpression>(LanguageFeature.ProhibitAssigningSingleElementsToVarargsInNamedForm) {
            parameter<ConeKotlinType>("expectedArrayType")
        }
        val ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION by deprecationError<KtExpression>(LanguageFeature.ProhibitAssigningSingleElementsToVarargsInNamedForm)
        val REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION by warning<KtExpression>()
        val REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION by warning<KtExpression>()

        val NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE by warning<PsiElement> {
            parameter<FirClassLikeSymbol<*>>("symbol")
        }

        val COMPARE_TO_TYPE_MISMATCH by warning<KtExpression>(PositioningStrategy.OPERATOR) {
            parameter<ConeKotlinType>("actualType")
        }

        val HAS_NEXT_FUNCTION_TYPE_MISMATCH by warning<KtExpression> {
            parameter<ConeKotlinType>("actualType")
        }

        val ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_WARNING by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<ConeKotlinType>("type")
        }
    }

    val AMBIGUITY by object : DiagnosticGroup("Ambiguity") {
        val OVERLOAD_RESOLUTION_AMBIGUITY by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Collection<Symbol>>("candidates")
        }
        val ASSIGN_OPERATOR_AMBIGUITY by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Collection<Symbol>>("candidates")
        }
        val ITERATOR_AMBIGUITY by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Collection<FirBasedSymbol<*>>>("candidates")
        }
        val HAS_NEXT_FUNCTION_AMBIGUITY by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Collection<FirBasedSymbol<*>>>("candidates")
        }
        val NEXT_AMBIGUITY by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Collection<FirBasedSymbol<*>>>("candidates")
        }
        val AMBIGUOUS_FUNCTION_TYPE_KIND by warning<PsiElement> {
            parameter<Collection<FunctionTypeKind>>("kinds")
        }
    }

    val CONTEXT_PARAMETERS_RESOLUTION by object : DiagnosticGroup("Context parameters resolution") {
        val NO_CONTEXT_ARGUMENT by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirValueParameterSymbol>("symbol")
        }
        val AMBIGUOUS_CONTEXT_ARGUMENT by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirValueParameterSymbol>("symbol")
        }
        val AMBIGUOUS_CALL_WITH_IMPLICIT_CONTEXT_RECEIVER by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED)
        val UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL by warning<KtElement>(PositioningStrategy.NAME_IDENTIFIER)
        val SUBTYPING_BETWEEN_CONTEXT_RECEIVERS by warning<KtElement>(PositioningStrategy.DEFAULT)
        val CONTEXT_PARAMETERS_WITH_BACKING_FIELD by warning<KtElement>(PositioningStrategy.DEFAULT)
        val CONTEXT_RECEIVERS_DEPRECATED by warning<KtElement>(PositioningStrategy.CONTEXT_KEYWORD) {
            parameter<String>("message")
        }
        val CONTEXT_CLASS_OR_CONSTRUCTOR by warning<KtElement>(PositioningStrategy.CONTEXT_KEYWORD)

        val CONTEXT_PARAMETER_WITHOUT_NAME by warning<KtContextReceiver>()
        val CONTEXT_PARAMETER_WITH_DEFAULT by warning<KtElement>()
        val CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION by warning<KtElement>() {
            parameter<FirCallableSymbol<*>>("symbol")
        }
        val MULTIPLE_CONTEXT_LISTS by warning<KtElement>()
        val NAMED_CONTEXT_PARAMETER_IN_FUNCTION_TYPE by warning<KtElement>()
        val CONTEXTUAL_OVERLOAD_SHADOWED by warning<KtElement>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS) {
            parameter<Collection<FirBasedSymbol<*>>>("symbols")
        }
    }

    val TYPES_AND_TYPE_PARAMETERS by object : DiagnosticGroup("Types & type parameters") {
        val RECURSION_IN_IMPLICIT_TYPES by warning<PsiElement>()
        val INFERENCE_ERROR by warning<PsiElement>()
        val PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT by warning<PsiElement>()
        val UPPER_BOUND_VIOLATED by warning<PsiElement> {
            parameter<ConeKotlinType>("expectedUpperBound")
            parameter<ConeKotlinType>("actualUpperBound")
            parameter<String>("extraMessage")
        }
        val UPPER_BOUND_VIOLATED_DEPRECATION_WARNING by warning<PsiElement> {
            parameter<ConeKotlinType>("expectedUpperBound")
            parameter<ConeKotlinType>("actualUpperBound")
            parameter<String>("extraMessage")
        }
        val UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION by warning<PsiElement> {
            parameter<ConeKotlinType>("expectedUpperBound")
            parameter<ConeKotlinType>("actualUpperBound")
        }
        val UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_DEPRECATION_WARNING by warning<PsiElement> {
            parameter<ConeKotlinType>("expectedUpperBound")
            parameter<ConeKotlinType>("actualUpperBound")
        }
        val TYPE_ARGUMENTS_NOT_ALLOWED by warning<PsiElement> {
            parameter<String>("place")
        }
        val TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED by warning<PsiElement>()
        val WRONG_NUMBER_OF_TYPE_ARGUMENTS by warning<PsiElement>(PositioningStrategy.TYPE_ARGUMENT_LIST_OR_SELF) {
            parameter<Int>("expectedCount")
            parameter<FirBasedSymbol<*>>("owner")
        }
        val NO_TYPE_ARGUMENTS_ON_RHS by warning<PsiElement> {
            parameter<Int>("expectedCount")
            parameter<FirClassLikeSymbol<*>>("classifier")
        }
        val OUTER_CLASS_ARGUMENTS_REQUIRED by warning<PsiElement> {
            parameter<FirClassLikeSymbol<*>>("outer")
        }
        val TYPE_PARAMETERS_IN_OBJECT by warning<PsiElement>(PositioningStrategy.TYPE_PARAMETERS_LIST)
        val TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT by warning<PsiElement>(PositioningStrategy.TYPE_PARAMETERS_LIST)
        val ILLEGAL_PROJECTION_USAGE by warning<PsiElement>()
        val TYPE_PARAMETERS_IN_ENUM by warning<PsiElement>()
        val CONFLICTING_PROJECTION by warning<KtTypeProjection>(PositioningStrategy.VARIANCE_MODIFIER) {
            parameter<ConeKotlinType>("type")
        }
        val CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION by warning<KtElement>(PositioningStrategy.VARIANCE_MODIFIER) {
            parameter<ConeKotlinType>("type")
        }
        val REDUNDANT_PROJECTION by warning<KtTypeProjection>(PositioningStrategy.VARIANCE_MODIFIER) {
            parameter<ConeKotlinType>("type")
        }
        val VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED by warning<KtTypeParameter>(PositioningStrategy.VARIANCE_MODIFIER)

        val CATCH_PARAMETER_WITH_DEFAULT_VALUE by warning<PsiElement>()
        val TYPE_PARAMETER_IN_CATCH_CLAUSE by warning<PsiElement>()
        val GENERIC_THROWABLE_SUBCLASS by warning<KtTypeParameter>()
        val INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME)

        val KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }

        val TYPE_PARAMETER_AS_REIFIED by warning<PsiElement> {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }

        val TYPE_PARAMETER_AS_REIFIED_ARRAY_ERROR by warning<PsiElement> {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }

        val REIFIED_TYPE_FORBIDDEN_SUBSTITUTION by warning<PsiElement> {
            parameter<ConeKotlinType>("type")
        }
        val DEFINITELY_NON_NULLABLE_AS_REIFIED by warning<PsiElement>()
        val TYPE_INTERSECTION_AS_REIFIED by deprecationError<PsiElement>(LanguageFeature.ProhibitIntersectionReifiedTypeParameter) {
            parameter<FirTypeParameterSymbol>("typeParameter")
            parameter<Collection<ConeKotlinType>>("types")
        }

        val FINAL_UPPER_BOUND by warning<KtElement> {
            parameter<ConeKotlinType>("type")
        }

        val UPPER_BOUND_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE by warning<KtElement>()

        val BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER by warning<KtElement>()

        val ONLY_ONE_CLASS_BOUND_ALLOWED by warning<KtElement>()

        val REPEATED_BOUND by warning<KtElement>()

        val CONFLICTING_UPPER_BOUNDS by warning<KtNamedDeclaration> {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }

        val NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER by warning<KtSimpleNameExpression> {
            parameter<Name>("typeParameterName")
            parameter<Symbol>("typeParametersOwner")
        }

        val BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED by warning<KtElement>()

        val REIFIED_TYPE_PARAMETER_NO_INLINE by warning<KtTypeParameter>(PositioningStrategy.REIFIED_MODIFIER)

        val REIFIED_TYPE_PARAMETER_ON_ALIAS by deprecationError<KtTypeParameter>(
            LanguageFeature.ForbidReifiedTypeParametersOnTypeAliases, PositioningStrategy.REIFIED_MODIFIER
        )

        val TYPE_PARAMETERS_NOT_ALLOWED by warning<KtDeclaration>(PositioningStrategy.TYPE_PARAMETERS_LIST)

        val TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER by warning<KtTypeParameter>()

        val RETURN_TYPE_MISMATCH by warning<KtExpression>(PositioningStrategy.WHOLE_ELEMENT) {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
            parameter<FirFunction>("targetFunction")
            parameter<Boolean>("isMismatchDueToNullability")
        }

        val IMPLICIT_NOTHING_RETURN_TYPE by warning<PsiElement>(PositioningStrategy.NAME_IDENTIFIER)
        val IMPLICIT_NOTHING_PROPERTY_TYPE by warning<PsiElement>(PositioningStrategy.NAME_IDENTIFIER)

        val ABBREVIATED_NOTHING_RETURN_TYPE by warning<PsiElement>(PositioningStrategy.NAME_IDENTIFIER) {
            isSuppressible = true
        }
        val ABBREVIATED_NOTHING_PROPERTY_TYPE by warning<PsiElement>(PositioningStrategy.NAME_IDENTIFIER) {
            isSuppressible = true
        }

        val CYCLIC_GENERIC_UPPER_BOUND by warning<PsiElement> {
            parameter<List<FirTypeParameterSymbol>>("typeParameters")
        }

        val FINITE_BOUNDS_VIOLATION by warning<PsiElement>()
        val FINITE_BOUNDS_VIOLATION_IN_JAVA by warning<PsiElement>(PositioningStrategy.DECLARATION_NAME) {
            parameter<List<Symbol>>("containingTypes")
        }
        val EXPANSIVE_INHERITANCE by warning<PsiElement>()
        val EXPANSIVE_INHERITANCE_IN_JAVA by warning<PsiElement>(PositioningStrategy.DECLARATION_NAME) {
            parameter<List<Symbol>>("containingTypes")
        }

        val DEPRECATED_TYPE_PARAMETER_SYNTAX by warning<KtDeclaration>(PositioningStrategy.TYPE_PARAMETERS_LIST)

        val MISPLACED_TYPE_PARAMETER_CONSTRAINTS by warning<KtTypeParameter>()

        val DYNAMIC_SUPERTYPE by warning<KtElement>()

        val DYNAMIC_UPPER_BOUND by warning<KtElement>()

        val DYNAMIC_RECEIVER_NOT_ALLOWED by warning<KtElement>()

        val DYNAMIC_RECEIVER_EXPECTED_BUT_WAS_NON_DYNAMIC by warning<PsiElement> {
            parameter<ConeKotlinType>("actualType")
        }

        val INCOMPATIBLE_TYPES by warning<KtElement> {
            parameter<ConeKotlinType>("typeA")
            parameter<ConeKotlinType>("typeB")
        }

        val INCOMPATIBLE_TYPES_WARNING by warning<KtElement> {
            parameter<ConeKotlinType>("typeA")
            parameter<ConeKotlinType>("typeB")
        }

        val TYPE_VARIANCE_CONFLICT_ERROR by warning<PsiElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<FirTypeParameterSymbol>("typeParameter")
            parameter<Variance>("typeParameterVariance")
            parameter<Variance>("variance")
            parameter<ConeKotlinType>("containingType")
        }

        val TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE by warning<PsiElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<FirTypeParameterSymbol>("typeParameter")
            parameter<Variance>("typeParameterVariance")
            parameter<Variance>("variance")
            parameter<ConeKotlinType>("containingType")
        }

        val SMARTCAST_IMPOSSIBLE by warning<KtExpression> {
            parameter<ConeKotlinType>("desiredType")
            parameter<FirExpression>("subject")
            parameter<String>("description")
            parameter<Boolean>("isCastToNotNull")
        }

        val SMARTCAST_IMPOSSIBLE_ON_IMPLICIT_INVOKE_RECEIVER by warning<KtExpression> {
            parameter<ConeKotlinType>("desiredType")
            parameter<FirExpression>("subject")
            parameter<String>("description")
            parameter<Boolean>("isCastToNotNull")
        }

        val DEPRECATED_SMARTCAST_ON_DELEGATED_PROPERTY by warning<KtExpression> {
            parameter<ConeKotlinType>("desiredType")
            parameter<FirCallableSymbol<*>>("property")
        }

        val REDUNDANT_NULLABLE by warning<KtElement>(PositioningStrategy.REDUNDANT_NULLABLE)

        val PLATFORM_CLASS_MAPPED_TO_KOTLIN by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<ClassId>("kotlinClass")
        }

        val INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION by deprecationError<PsiElement>(
            LanguageFeature.ForbidInferringTypeVariablesIntoEmptyIntersection
        ) {
            parameter<String>("typeVariableDescription")
            parameter<Collection<ConeKotlinType>>("incompatibleTypes")
            parameter<String>("description")
            parameter<String>("causingTypes")
        }

        val INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION by warning<PsiElement> {
            parameter<String>("typeVariableDescription")
            parameter<Collection<ConeKotlinType>>("incompatibleTypes")
            parameter<String>("description")
            parameter<String>("causingTypes")
        }

        val INCORRECT_LEFT_COMPONENT_OF_INTERSECTION by warning<KtElement>()
        val INCORRECT_RIGHT_COMPONENT_OF_INTERSECTION by warning<KtElement>()
        val NULLABLE_ON_DEFINITELY_NOT_NULLABLE by warning<KtElement>()
        val INFERRED_INVISIBLE_REIFIED_TYPE_ARGUMENT by deprecationError<KtElement>(
            LanguageFeature.ForbidInferOfInvisibleTypeAsReifiedVarargOrReturnType
        ) {
            parameter<FirTypeParameterSymbol>("typeParameter")
            parameter<ConeKotlinType>("typeArgumentType")
        }
        val INFERRED_INVISIBLE_VARARG_TYPE_ARGUMENT by deprecationError<KtElement>(
            LanguageFeature.ForbidInferOfInvisibleTypeAsReifiedVarargOrReturnType
        ) {
            parameter<FirTypeParameterSymbol>("typeParameter")
            parameter<ConeKotlinType>("typeArgumentType")
            parameter<FirValueParameterSymbol>("valueParameter")
        }
        val INFERRED_INVISIBLE_RETURN_TYPE by deprecationError<KtElement>(
            LanguageFeature.ForbidInferOfInvisibleTypeAsReifiedVarargOrReturnType
        ) {
            parameter<FirBasedSymbol<*>>("calleeSymbol")
            parameter<ConeKotlinType>("returnType")
        }
        val GENERIC_QUALIFIER_ON_CONSTRUCTOR_CALL by deprecationError<PsiElement>(
            LanguageFeature.ProhibitGenericQualifiersOnConstructorCalls,
            PositioningStrategy.TYPE_ARGUMENT_LIST_OR_SELF
        )
        val ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY by warning<PsiElement> {
            parameter<ClassId>("atomicRef")
            parameter<ConeKotlinType>("argumentType")
            parameter<ClassId?>("suggestedType")
        }
    }

    val REFLECTION by object : DiagnosticGroup("Reflection") {
        val EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED by warning<KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirCallableSymbol<*>>("referencedDeclaration")
        }
        val CALLABLE_REFERENCE_LHS_NOT_A_CLASS by warning<KtExpression>()
        val CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR by warning<KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED)
        val ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE by warning<KtExpression>()

        val CLASS_LITERAL_LHS_NOT_A_CLASS by warning<KtExpression>()
        val NULLABLE_TYPE_IN_CLASS_LITERAL_LHS by warning<KtExpression>()
        val EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS by warning<PsiElement> {
            parameter<ConeKotlinType>("lhsType")
        }
        val UNSUPPORTED_CLASS_LITERALS_WITH_EMPTY_LHS by warning<KtElement>()
        val MUTABLE_PROPERTY_WITH_CAPTURED_TYPE by warning<PsiElement>()
    }

    val OVERRIDES by object : DiagnosticGroup("overrides") {
        val NOTHING_TO_OVERRIDE by warning<KtModifierListOwner>(PositioningStrategy.OVERRIDE_MODIFIER) {
            parameter<FirCallableSymbol<*>>("declaration")
            parameter<List<FirCallableSymbol<*>>>("candidates")
        }

        val CANNOT_OVERRIDE_INVISIBLE_MEMBER by warning<KtNamedDeclaration>(PositioningStrategy.OVERRIDE_MODIFIER) {
            parameter<FirCallableSymbol<*>>("overridingMember")
            parameter<FirCallableSymbol<*>>("baseMember")
        }

        val DATA_CLASS_OVERRIDE_CONFLICT by warning<KtClassOrObject>(PositioningStrategy.DATA_MODIFIER) {
            parameter<FirCallableSymbol<*>>("overridingMember")
            parameter<FirCallableSymbol<*>>("baseMember")
        }

        val DATA_CLASS_OVERRIDE_DEFAULT_VALUES by warning<KtElement>(PositioningStrategy.DATA_MODIFIER) {
            parameter<FirCallableSymbol<*>>("overridingMember")
            parameter<FirClassSymbol<*>>("baseType")
        }

        val CANNOT_WEAKEN_ACCESS_PRIVILEGE by warning<KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER) {
            parameter<Visibility>("overridingVisibility")
            parameter<FirCallableSymbol<*>>("overridden")
            parameter<Name>("containingClassName")
        }
        val CANNOT_WEAKEN_ACCESS_PRIVILEGE_WARNING by warning<KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER) {
            parameter<Visibility>("overridingVisibility")
            parameter<FirCallableSymbol<*>>("overridden")
            parameter<Name>("containingClassName")
        }
        val CANNOT_CHANGE_ACCESS_PRIVILEGE by warning<KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER) {
            parameter<Visibility>("overridingVisibility")
            parameter<FirCallableSymbol<*>>("overridden")
            parameter<Name>("containingClassName")
        }
        val CANNOT_CHANGE_ACCESS_PRIVILEGE_WARNING by warning<KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER) {
            parameter<Visibility>("overridingVisibility")
            parameter<FirCallableSymbol<*>>("overridden")
            parameter<Name>("containingClassName")
        }
        val CANNOT_INFER_VISIBILITY by warning<KtDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("callable")
        }
        val CANNOT_INFER_VISIBILITY_WARNING by warning<KtDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("callable")
        }

        val MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES by warning<KtElement>(PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT) {
            parameter<Name>("name")
            parameter<FirValueParameterSymbol>("valueParameter")
            parameter<List<FirCallableSymbol<*>>>("baseFunctions")
        }
        val MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE by warning<KtElement>(PositioningStrategy.DECLARATION_NAME) {
            parameter<Name>("name")
            parameter<FirValueParameterSymbol>("valueParameter")
            parameter<List<FirCallableSymbol<*>>>("baseFunctions")
        }
        val MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_DEPRECATION by deprecationError<KtElement>(
            LanguageFeature.ProhibitAllMultipleDefaultsInheritedFromSupertypes,
            PositioningStrategy.DECLARATION_SIGNATURE_OR_DEFAULT
        ) {
            parameter<Name>("name")
            parameter<FirValueParameterSymbol>("valueParameter")
            parameter<List<FirCallableSymbol<*>>>("baseFunctions")
        }
        val MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE_DEPRECATION by deprecationError<KtElement>(
            LanguageFeature.ProhibitAllMultipleDefaultsInheritedFromSupertypes,
            PositioningStrategy.DECLARATION_NAME
        ) {
            parameter<Name>("name")
            parameter<FirValueParameterSymbol>("valueParameter")
            parameter<List<FirCallableSymbol<*>>>("baseFunctions")
        }

        val TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS by warning<KtElement> {
            parameter<ConeKotlinType>("type")
        }

        val OVERRIDING_FINAL_MEMBER by warning<KtNamedDeclaration>(PositioningStrategy.OVERRIDE_MODIFIER) {
            parameter<FirCallableSymbol<*>>("overriddenDeclaration")
            parameter<Name>("containingClassName")
        }

        val RETURN_TYPE_MISMATCH_ON_INHERITANCE by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("conflictingDeclaration1")
            parameter<FirCallableSymbol<*>>("conflictingDeclaration2")
        }

        val PROPERTY_TYPE_MISMATCH_ON_INHERITANCE by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("conflictingDeclaration1")
            parameter<FirCallableSymbol<*>>("conflictingDeclaration2")
        }

        val VAR_TYPE_MISMATCH_ON_INHERITANCE by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("conflictingDeclaration1")
            parameter<FirCallableSymbol<*>>("conflictingDeclaration2")
        }

        val RETURN_TYPE_MISMATCH_BY_DELEGATION by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("delegateDeclaration")
            parameter<FirCallableSymbol<*>>("baseDeclaration")
        }

        val PROPERTY_TYPE_MISMATCH_BY_DELEGATION by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("delegateDeclaration")
            parameter<FirCallableSymbol<*>>("baseDeclaration")
        }

        val VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("delegateDeclaration")
            parameter<FirCallableSymbol<*>>("baseDeclaration")
        }

        val CONFLICTING_INHERITED_MEMBERS by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClassSymbol<*>>("owner")
            parameter<List<FirCallableSymbol<*>>>("conflictingDeclarations")
        }

        val ABSTRACT_MEMBER_NOT_IMPLEMENTED by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClassSymbol<*>>("classOrObject")
            parameter<List<FirCallableSymbol<*>>>("missingDeclarations")
        }
        val ABSTRACT_MEMBER_INCORRECTLY_DELEGATED by deprecationError<KtClassOrObject>(
            LanguageFeature.ForbidObjectDelegationToItself,
            PositioningStrategy.DECLARATION_NAME
        ) {
            parameter<FirClassSymbol<*>>("classOrObject")
            parameter<List<FirCallableSymbol<*>>>("missingDeclarations")
        }
        val ABSTRACT_MEMBER_NOT_IMPLEMENTED_BY_ENUM_ENTRY by warning<KtEnumEntry>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirEnumEntrySymbol>("enumEntry")
            parameter<List<FirCallableSymbol<*>>>("missingDeclarations")
        }
        val ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClassSymbol<*>>("classOrObject")
            parameter<List<FirCallableSymbol<*>>>("missingDeclarations")
        }
        val INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClassSymbol<*>>("classOrObject")
            parameter<List<FirCallableSymbol<*>>>("invisibleDeclarations")
        }
        val AMBIGUOUS_ANONYMOUS_TYPE_INFERRED by warning<KtDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<Collection<ConeKotlinType>>("superTypes")
        }
        val MANY_IMPL_MEMBER_NOT_IMPLEMENTED by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClassSymbol<*>>("classOrObject")
            parameter<FirCallableSymbol<*>>("missingDeclaration")
        }
        val MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirClassSymbol<*>>("classOrObject")
            parameter<FirCallableSymbol<*>>("missingDeclaration")
        }
        val OVERRIDING_FINAL_MEMBER_BY_DELEGATION by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("delegatedDeclaration")
            parameter<FirCallableSymbol<*>>("overriddenDeclaration")
        }
        val DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("delegatedDeclaration")
            parameter<FirCallableSymbol<*>>("overriddenDeclaration")
        }

        val RETURN_TYPE_MISMATCH_ON_OVERRIDE by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_RETURN_TYPE) {
            parameter<FirCallableSymbol<*>>("function")
            parameter<FirCallableSymbol<*>>("superFunction")
        }
        val PROPERTY_TYPE_MISMATCH_ON_OVERRIDE by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_RETURN_TYPE) {
            parameter<FirCallableSymbol<*>>("property")
            parameter<FirCallableSymbol<*>>("superProperty")
        }
        val VAR_TYPE_MISMATCH_ON_OVERRIDE by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_RETURN_TYPE) {
            parameter<FirCallableSymbol<*>>("variable")
            parameter<FirCallableSymbol<*>>("superVariable")
        }
        val VAR_OVERRIDDEN_BY_VAL by warning<KtNamedDeclaration>(PositioningStrategy.VAL_OR_VAR_NODE) {
            parameter<FirCallableSymbol<*>>("overridingDeclaration")
            parameter<FirCallableSymbol<*>>("overriddenDeclaration")
        }
        val VAR_IMPLEMENTED_BY_INHERITED_VAL by deprecationError<KtNamedDeclaration>(
            LanguageFeature.ProhibitImplementingVarByInheritedVal,
            PositioningStrategy.DECLARATION_NAME,
        ) {
            parameter<FirClassSymbol<*>>("classOrObject")
            parameter<FirCallableSymbol<*>>("overridingDeclaration")
            parameter<FirCallableSymbol<*>>("overriddenDeclaration")
        }
        val NON_FINAL_MEMBER_IN_FINAL_CLASS by warning<KtNamedDeclaration>(PositioningStrategy.OPEN_MODIFIER)
        val NON_FINAL_MEMBER_IN_OBJECT by warning<KtNamedDeclaration>(PositioningStrategy.OPEN_MODIFIER)
        val VIRTUAL_MEMBER_HIDDEN by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("declared")
            parameter<FirRegularClassSymbol>("overriddenContainer")
        }
        val PARAMETER_NAME_CHANGED_ON_OVERRIDE by warning<KtParameter>(PositioningStrategy.NAME_IDENTIFIER) {
            parameter<FirRegularClassSymbol>("superType")
            parameter<FirValueParameterSymbol>("conflictingParameter")
        }
        val DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES by warning<KtClassOrObject>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirValueParameterSymbol>("currentParameter")
            parameter<FirValueParameterSymbol>("conflictingParameter")
            parameter<Int>("parameterNumber")
            parameter<List<FirNamedFunctionSymbol>>("conflictingFunctions")
        }
        val SUSPEND_OVERRIDDEN_BY_NON_SUSPEND by warning<KtCallableDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("overridingDeclaration")
            parameter<FirCallableSymbol<*>>("overriddenDeclaration")
        }
        val NON_SUSPEND_OVERRIDDEN_BY_SUSPEND by warning<KtCallableDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("overridingDeclaration")
            parameter<FirCallableSymbol<*>>("overriddenDeclaration")
        }
    }

    val REDECLARATIONS by object : DiagnosticGroup("Redeclarations") {
        val MANY_COMPANION_OBJECTS by warning<KtObjectDeclaration>(PositioningStrategy.COMPANION_OBJECT)
        val CONFLICTING_OVERLOADS by warning<PsiElement>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS) {
            parameter<Collection<Symbol>>("conflictingOverloads")
        }
        val REDECLARATION by warning<KtNamedDeclaration>(PositioningStrategy.NAME_IDENTIFIER) {
            parameter<Collection<Symbol>>("conflictingDeclarations")
        }
        val CLASSIFIER_REDECLARATION by warning<KtNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Collection<Symbol>>("conflictingDeclarations")
        }
        val PACKAGE_CONFLICTS_WITH_CLASSIFIER by warning<KtPackageDirective>(PositioningStrategy.PACKAGE_DIRECTIVE_NAME_EXPRESSION) {
            parameter<ClassId>("conflictingClassId")
        }
        val EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE by warning<KtNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Symbol>("declaration")
        }
        val METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE by warning<PsiElement>(PositioningStrategy.DECLARATION_NAME)
        val EXTENSION_SHADOWED_BY_MEMBER by warning<PsiElement>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("member")
        }
        val EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE by warning<PsiElement>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirCallableSymbol<*>>("member")
            parameter<FirCallableSymbol<*>>("invokeOperator")
        }
    }

    val INVALID_LOCAL_DECLARATIONS by object : DiagnosticGroup("Invalid local declarations") {
        val LOCAL_OBJECT_NOT_ALLOWED by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<Name>("objectName")
        }
        val LOCAL_INTERFACE_NOT_ALLOWED by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME) {
            parameter<Name>("interfaceName")
        }
    }

    val FUNCTIONS by object : DiagnosticGroup("Functions") {
        val ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS by warning<KtFunction>(PositioningStrategy.MODALITY_MODIFIER) {
            parameter<FirCallableSymbol<*>>("function")
            parameter<FirClassSymbol<*>>("containingClass")
        }
        val ABSTRACT_FUNCTION_WITH_BODY by warning<KtFunction>(PositioningStrategy.MODALITY_MODIFIER) {
            parameter<FirCallableSymbol<*>>("function")
        }
        val NON_ABSTRACT_FUNCTION_WITH_NO_BODY by warning<KtFunction>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS) {
            parameter<FirCallableSymbol<*>>("function")
        }
        val PRIVATE_FUNCTION_WITH_NO_BODY by warning<KtFunction>(PositioningStrategy.VISIBILITY_MODIFIER) {
            parameter<FirCallableSymbol<*>>("function")
        }

        val NON_MEMBER_FUNCTION_NO_BODY by warning<KtFunction>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS) {
            parameter<FirCallableSymbol<*>>("function")
        }

        val FUNCTION_DECLARATION_WITH_NO_NAME by warning<KtFunction>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val ANONYMOUS_FUNCTION_WITH_NAME by warning<KtFunction>(PositioningStrategy.DECLARATION_NAME)
        val SINGLE_ANONYMOUS_FUNCTION_WITH_NAME by deprecationError<KtFunction>(
            LanguageFeature.ProhibitSingleNamedFunctionAsExpression,
            PositioningStrategy.DECLARATION_NAME
        )

        val ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE by warning<KtParameter>(PositioningStrategy.PARAMETER_DEFAULT_VALUE)
        val USELESS_VARARG_ON_PARAMETER by warning<KtParameter>()
        val MULTIPLE_VARARG_PARAMETERS by warning<KtParameter>(PositioningStrategy.PARAMETER_VARARG_MODIFIER)
        val FORBIDDEN_VARARG_PARAMETER_TYPE by warning<KtParameter>(PositioningStrategy.PARAMETER_VARARG_MODIFIER) {
            parameter<ConeKotlinType>("varargParameterType")
        }
        val VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE by warning<KtParameter>()

        val CANNOT_INFER_PARAMETER_TYPE by warning<KtElement> {
            parameter<FirTypeParameterSymbol>("parameter")
        }
        val CANNOT_INFER_VALUE_PARAMETER_TYPE by warning<KtElement> {
            parameter<FirValueParameterSymbol>("parameter")
        }
        val CANNOT_INFER_IT_PARAMETER_TYPE by warning<KtElement>()
        val CANNOT_INFER_RECEIVER_PARAMETER_TYPE by warning<KtElement>()

        val NO_TAIL_CALLS_FOUND by warning<KtNamedFunction>(PositioningStrategy.TAILREC_MODIFIER)
        val TAILREC_ON_VIRTUAL_MEMBER_ERROR by warning<KtNamedFunction>(PositioningStrategy.TAILREC_MODIFIER)
        val NON_TAIL_RECURSIVE_CALL by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val DATA_OBJECT_CUSTOM_EQUALS_OR_HASH_CODE by warning<KtNamedFunction>(PositioningStrategy.OVERRIDE_MODIFIER)
    }

    val PARAMETER_DEFAULT_VALUES by object : DiagnosticGroup("Parameter default values") {
        val DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE by warning<KtElement>()
    }

    val FUN_INTERFACES by object : DiagnosticGroup("Fun interfaces") {
        val FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS by warning<KtClass>(PositioningStrategy.FUN_MODIFIER)
        val FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES by warning<KtDeclaration>(PositioningStrategy.FUN_INTERFACE)
        val FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS by warning<KtDeclaration>(PositioningStrategy.FUN_INTERFACE)
        val FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE by warning<KtDeclaration>(PositioningStrategy.FUN_INTERFACE)
        val FUN_INTERFACE_WITH_SUSPEND_FUNCTION by warning<KtDeclaration>(PositioningStrategy.FUN_INTERFACE)
    }

    val PROPERTIES_AND_ACCESSORS by object : DiagnosticGroup("Properties & accessors") {
        val ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS by warning<KtModifierListOwner>(PositioningStrategy.MODALITY_MODIFIER) {
            parameter<FirCallableSymbol<*>>("property")
            parameter<FirClassSymbol<*>>("containingClass")
        }
        val PRIVATE_PROPERTY_IN_INTERFACE by warning<KtProperty>(PositioningStrategy.VISIBILITY_MODIFIER)

        val ABSTRACT_PROPERTY_WITH_INITIALIZER by warning<KtExpression>()
        val PROPERTY_INITIALIZER_IN_INTERFACE by warning<KtExpression>()
        val PROPERTY_WITH_NO_TYPE_NO_INITIALIZER by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val ABSTRACT_PROPERTY_WITHOUT_TYPE by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val LATEINIT_PROPERTY_WITHOUT_TYPE by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)

        val MUST_BE_INITIALIZED by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val MUST_BE_INITIALIZED_WARNING by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val MUST_BE_INITIALIZED_OR_BE_FINAL by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val MUST_BE_INITIALIZED_OR_BE_FINAL_WARNING by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val MUST_BE_INITIALIZED_OR_BE_ABSTRACT by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val MUST_BE_INITIALIZED_OR_BE_ABSTRACT_WARNING by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT_WARNING by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)

        val EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val UNNECESSARY_LATEINIT by warning<KtProperty>(PositioningStrategy.LATEINIT_MODIFIER)

        val BACKING_FIELD_IN_INTERFACE by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val EXTENSION_PROPERTY_WITH_BACKING_FIELD by warning<KtExpression>()
        val PROPERTY_INITIALIZER_NO_BACKING_FIELD by warning<KtExpression>()

        val ABSTRACT_DELEGATED_PROPERTY by warning<KtExpression>()
        val DELEGATED_PROPERTY_IN_INTERFACE by warning<KtExpression>()
        // TODO: val ACCESSOR_FOR_DELEGATED_PROPERTY by warning1<PsiElement, FirPropertyAccessorSymbol>()

        val ABSTRACT_PROPERTY_WITH_GETTER by warning<KtPropertyAccessor>()
        val ABSTRACT_PROPERTY_WITH_SETTER by warning<KtPropertyAccessor>()
        val PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY by warning<KtModifierListOwner>(PositioningStrategy.PRIVATE_MODIFIER)
        val PRIVATE_SETTER_FOR_OPEN_PROPERTY by warning<KtModifierListOwner>(PositioningStrategy.PRIVATE_MODIFIER)
        val VAL_WITH_SETTER by warning<KtPropertyAccessor>()
        val CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT by warning<KtElement>(PositioningStrategy.CONST_MODIFIER)
        val CONST_VAL_WITH_GETTER by warning<KtElement>()
        val CONST_VAL_WITH_DELEGATE by warning<KtExpression>()
        val TYPE_CANT_BE_USED_FOR_CONST_VAL by warning<KtProperty>(PositioningStrategy.CONST_MODIFIER) {
            parameter<ConeKotlinType>("constValType")
        }
        val CONST_VAL_WITHOUT_INITIALIZER by warning<KtProperty>(PositioningStrategy.CONST_MODIFIER)
        val CONST_VAL_WITH_NON_CONST_INITIALIZER by warning<KtExpression>()
        val WRONG_SETTER_PARAMETER_TYPE by warning<KtElement> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }
        val DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER by deprecationError<KtProperty>(
            LanguageFeature.ForbidUsingExtensionPropertyTypeParameterInDelegate,
            PositioningStrategy.PROPERTY_DELEGATE
        ) {
            parameter<FirTypeParameterSymbol>("usedTypeParameter")
        }
        // Type parameter is KtNamedDeclaration because PSI of FirProperty can be KtParameter in for loop
        val INITIALIZER_TYPE_MISMATCH by warning<KtNamedDeclaration>(PositioningStrategy.PROPERTY_INITIALIZER) {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
            parameter<Boolean>("isMismatchDueToNullability")
        }
        val GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY by warning<KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER)
        val SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY by warning<KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER)
        val WRONG_SETTER_RETURN_TYPE by warning<KtElement>()
        val WRONG_GETTER_RETURN_TYPE by warning<KtElement> {
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }
        val ACCESSOR_FOR_DELEGATED_PROPERTY by warning<KtPropertyAccessor>()
        val PROPERTY_INITIALIZER_WITH_EXPLICIT_FIELD_DECLARATION by warning<KtExpression>()
        val PROPERTY_FIELD_DECLARATION_MISSING_INITIALIZER by warning<KtBackingField>()
        val LATEINIT_PROPERTY_FIELD_DECLARATION_WITH_INITIALIZER by warning<KtBackingField>(PositioningStrategy.LATEINIT_MODIFIER)
        val LATEINIT_FIELD_IN_VAL_PROPERTY by warning<KtBackingField>(PositioningStrategy.LATEINIT_MODIFIER)
        val LATEINIT_NULLABLE_BACKING_FIELD by warning<KtBackingField>(PositioningStrategy.LATEINIT_MODIFIER)
        val BACKING_FIELD_FOR_DELEGATED_PROPERTY by warning<KtBackingField>(PositioningStrategy.FIELD_KEYWORD)
        val PROPERTY_MUST_HAVE_GETTER by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val PROPERTY_MUST_HAVE_SETTER by warning<KtProperty>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val EXPLICIT_BACKING_FIELD_IN_INTERFACE by warning<KtBackingField>(PositioningStrategy.FIELD_KEYWORD)
        val EXPLICIT_BACKING_FIELD_IN_ABSTRACT_PROPERTY by warning<KtBackingField>(PositioningStrategy.FIELD_KEYWORD)
        val EXPLICIT_BACKING_FIELD_IN_EXTENSION by warning<KtBackingField>(PositioningStrategy.FIELD_KEYWORD)
        val REDUNDANT_EXPLICIT_BACKING_FIELD by warning<KtBackingField>(PositioningStrategy.FIELD_KEYWORD)
        val ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS by warning<KtModifierListOwner>(PositioningStrategy.ABSTRACT_MODIFIER)
        val LOCAL_VARIABLE_WITH_TYPE_PARAMETERS_WARNING by warning<KtProperty>(PositioningStrategy.TYPE_PARAMETERS_LIST)
        val LOCAL_VARIABLE_WITH_TYPE_PARAMETERS by warning<KtProperty>(PositioningStrategy.TYPE_PARAMETERS_LIST)
        val EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS by warning<KtExpression>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("kind")
        }
        val SAFE_CALLABLE_REFERENCE_CALL by warning<KtExpression>()

        val LATEINIT_INTRINSIC_CALL_ON_NON_LITERAL by warning<PsiElement>()
        val LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT by warning<PsiElement>()
        val LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION by warning<PsiElement>()
        val LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY by warning<PsiElement>() {
            parameter<Symbol>("declaration")
        }

        val LOCAL_EXTENSION_PROPERTY by warning<PsiElement>()

        val UNNAMED_VAR_PROPERTY by warning<PsiElement>(PositioningStrategy.VAL_OR_VAR_NODE)
        val UNNAMED_DELEGATED_PROPERTY by warning<PsiElement>(PositioningStrategy.PROPERTY_DELEGATE_BY_KEYWORD)
    }

    val MPP_PROJECTS by object : DiagnosticGroup("Multi-platform projects") {
        val EXPECTED_DECLARATION_WITH_BODY by warning<KtDeclaration>(PositioningStrategy.DECLARATION_SIGNATURE)
        val EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL by warning<KtConstructorDelegationCall>()
        val EXPECTED_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER by warning<KtParameter>()
        val EXPECTED_ENUM_CONSTRUCTOR by warning<KtConstructor<*>>()
        val EXPECTED_ENUM_ENTRY_WITH_BODY by warning<KtEnumEntry>(PositioningStrategy.EXPECT_ACTUAL_MODIFIER)
        val EXPECTED_PROPERTY_INITIALIZER by warning<KtExpression>()

        // TODO: need to cover `by` as well as delegate expression
        val EXPECTED_DELEGATED_PROPERTY by warning<KtExpression>()
        val EXPECTED_LATEINIT_PROPERTY by warning<KtModifierListOwner>(PositioningStrategy.LATEINIT_MODIFIER)
        val SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS by warning<KtElement>(PositioningStrategy.SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS_DIAGNOSTIC)
        val EXPECTED_PRIVATE_DECLARATION by warning<KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER)
        val EXPECTED_EXTERNAL_DECLARATION by warning<KtModifierListOwner>(PositioningStrategy.EXTERNAL_MODIFIER)
        val EXPECTED_TAILREC_FUNCTION by warning<KtModifierListOwner>(PositioningStrategy.TAILREC_MODIFIER)
        val IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS by warning<KtDelegatedSuperTypeEntry>()

        val ACTUAL_TYPE_ALIAS_NOT_TO_CLASS by warning<KtTypeAlias>(PositioningStrategy.DECLARATION_SIGNATURE)
        val ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE by warning<KtTypeAlias>(PositioningStrategy.DECLARATION_SIGNATURE)
        val ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE by warning<KtTypeAlias>(PositioningStrategy.DECLARATION_SIGNATURE)
        val ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION by warning<KtTypeAlias>(PositioningStrategy.DECLARATION_SIGNATURE)
        val ACTUAL_TYPE_ALIAS_TO_NULLABLE_TYPE by warning<KtTypeAlias>(PositioningStrategy.DECLARATION_SIGNATURE)
        val ACTUAL_TYPE_ALIAS_TO_NOTHING by warning<KtTypeAlias>(PositioningStrategy.DECLARATION_SIGNATURE)
        val ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS by warning<KtFunction>(PositioningStrategy.PARAMETERS_WITH_DEFAULT_VALUE)
        val DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS by warning<KtTypeAlias> {
            parameter<FirClassSymbol<*>>("expectClassSymbol")
            parameter<Collection<FirCallableSymbol<*>>>("members")
        }
        val DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE by warning<KtClass>(PositioningStrategy.SUPERTYPES_LIST) {
            parameter<FirRegularClassSymbol>("expectClassSymbol")
            parameter<Collection<FirNamedFunctionSymbol>>("members")
        }
        val EXPECTED_FUNCTION_SOURCE_WITH_DEFAULT_ARGUMENTS_NOT_FOUND by warning<PsiElement>()

        val ACTUAL_WITHOUT_EXPECT by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME_ONLY) {
            parameter<Symbol>("declaration")
            parameter<Map<out ExpectActualMatchingCompatibility, Collection<Symbol>>>("compatibility")
        }

        val expectActualIncompatibilityError = error<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME_ONLY) {
            parameter<Symbol>("expectDeclaration")
            parameter<Symbol>("actualDeclaration")
            parameter<String>("reason")
        }

        val EXPECT_ACTUAL_INCOMPATIBLE_CLASS_TYPE_PARAMETER_COUNT by expectActualIncompatibilityError

        // Callables
        val EXPECT_ACTUAL_INCOMPATIBLE_RETURN_TYPE by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_PARAMETER_NAMES by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_CONTEXT_PARAMETER_NAMES by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_NAMES by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_VARARG by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_NOINLINE by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_CROSSINLINE by expectActualIncompatibilityError

        // Functions
        val EXPECT_ACTUAL_INCOMPATIBLE_FUNCTION_MODIFIERS_DIFFERENT by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_FUNCTION_MODIFIERS_NOT_SUBSET by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_PARAMETERS_WITH_DEFAULT_VALUES_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE by expectActualIncompatibilityError

        // Properties
        val EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_KIND by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_LATEINIT_MODIFIER by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_CONST_MODIFIER by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_PROPERTY_SETTER_VISIBILITY by expectActualIncompatibilityError

        // Classifiers
        val EXPECT_ACTUAL_INCOMPATIBLE_CLASS_KIND by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_FUN_INTERFACE_MODIFIER by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_NESTED_TYPE_ALIAS by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_ENUM_ENTRIES by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_ILLEGAL_REQUIRES_OPT_IN by expectActualIncompatibilityError

        // Common
        val EXPECT_ACTUAL_INCOMPATIBLE_MODALITY by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_VISIBILITY by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_CLASS_TYPE_PARAMETER_UPPER_BOUNDS by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_VARIANCE by expectActualIncompatibilityError
        val EXPECT_ACTUAL_INCOMPATIBLE_TYPE_PARAMETER_REIFIED by expectActualIncompatibilityError

        val EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE by warning<KtNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Symbol>("actualClass")
            parameter<Symbol>("expectMemberDeclaration")
            parameter<Symbol>("actualMemberDeclaration")
            parameter<String>("reason")
        }

        val EXPECT_REFINEMENT_ANNOTATION_WRONG_TARGET by warning<KtNamedDeclaration>(
            PositioningStrategy.DECLARATION_NAME
        )

        val AMBIGUOUS_EXPECTS by warning<KtNamedDeclaration>(PositioningStrategy.EXPECT_ACTUAL_MODIFIER) {
            parameter<Symbol>("declaration")
            parameter<Collection<FirModuleData>>("modules")
        }

        val NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS by warning<KtNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME) {
            parameter<Symbol>("declaration")
            parameter<List<Pair<Symbol, Map<out ExpectActualMatchingCompatibility.Mismatch, Collection<Symbol>>>>>("members")
        }

        val ACTUAL_MISSING by warning<KtNamedDeclaration>(PositioningStrategy.ACTUAL_DECLARATION_NAME)

        val EXPECT_REFINEMENT_ANNOTATION_MISSING by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)

        val EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING by warning<KtClassLikeDeclaration>(PositioningStrategy.EXPECT_ACTUAL_MODIFIER)

        val NOT_A_MULTIPLATFORM_COMPILATION by warning<PsiElement>()

        val EXPECT_ACTUAL_OPT_IN_ANNOTATION by warning<KtNamedDeclaration>(PositioningStrategy.EXPECT_ACTUAL_MODIFIER)

        val ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION by warning<KtTypeAlias>(PositioningStrategy.TYPEALIAS_TYPE_REFERENCE) {
            parameter<ClassId>("typealiasedClassId")
        }

        val ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT by warning<KtElement>(PositioningStrategy.DECLARATION_NAME_ONLY) {
            parameter<Symbol>("expectSymbol")
            parameter<Symbol>("actualSymbol")
            parameter<KtSourceElement?>("actualAnnotationTargetSourceElement")
            parameter<ExpectActualAnnotationsIncompatibilityType<FirAnnotation>>("incompatibilityType")
        }

        val OPTIONAL_DECLARATION_OUTSIDE_OF_ANNOTATION_ENTRY by warning<PsiElement>()

        val OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE by warning<PsiElement>()

        val OPTIONAL_EXPECTATION_NOT_ON_EXPECTED by warning<PsiElement>()
    }

    val DESTRUCTING_DECLARATION by object : DiagnosticGroup("Destructuring declaration") {
        val INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION by warning<KtDestructuringDeclaration>()
        val COMPONENT_FUNCTION_MISSING by warning<PsiElement> {
            parameter<Name>("missingFunctionName")
            parameter<ConeKotlinType>("destructingType")
        }
        val COMPONENT_FUNCTION_AMBIGUITY by warning<PsiElement> {
            parameter<Name>("functionWithAmbiguityName")
            parameter<Collection<Symbol>>("candidates")
            parameter<ConeKotlinType>("destructingType")
        }
        val COMPONENT_FUNCTION_ON_NULLABLE by warning<KtExpression> {
            parameter<Name>("componentFunctionName")
            parameter<ConeKotlinType>("destructingType")
        }
        val COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH by warning<KtExpression> {
            parameter<Name>("componentFunctionName")
            parameter<ConeKotlinType>("destructingType")
            parameter<ConeKotlinType>("expectedType")
        }
    }

    val CONTROL_FLOW by object : DiagnosticGroup("Control flow diagnostics") {
        val UNINITIALIZED_VARIABLE by warning<KtExpression> {
            parameter<FirPropertySymbol>("variable")
        }
        val UNINITIALIZED_PARAMETER by warning<KtSimpleNameExpression> {
            parameter<FirValueParameterSymbol>("parameter")
        }
        val UNINITIALIZED_ENUM_ENTRY by warning<KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirEnumEntrySymbol>("enumEntry")
        }
        val UNINITIALIZED_ENUM_COMPANION by warning<KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<FirRegularClassSymbol>("enumClass")
        }
        val VAL_REASSIGNMENT by warning<KtExpression>(PositioningStrategy.SELECTOR_BY_QUALIFIED) {
            parameter<FirVariableSymbol<*>>("variable")
        }
        val VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR by warning<KtExpression>(PositioningStrategy.SELECTOR_BY_QUALIFIED) {
            parameter<FirBackingFieldSymbol>("property")
        }
        val CAPTURED_VAL_INITIALIZATION by warning<KtExpression> {
            parameter<FirPropertySymbol>("property")
        }
        val CAPTURED_MEMBER_VAL_INITIALIZATION by warning<KtExpression> {
            parameter<FirPropertySymbol>("property")
        }
        val NON_INLINE_MEMBER_VAL_INITIALIZATION by warning<KtExpression> {
            parameter<FirPropertySymbol>("property")
        }
        val SETTER_PROJECTED_OUT by warning<KtBinaryExpression>(PositioningStrategy.SELECTOR_BY_QUALIFIED) {
            parameter<ConeKotlinType>("receiverType")
            parameter<String>("projection")
            parameter<FirPropertySymbol>("property")
        }
        val WRONG_INVOCATION_KIND by warning<PsiElement> {
            parameter<Symbol>("declaration")
            parameter<EventOccurrencesRange>("requiredRange")
            parameter<EventOccurrencesRange>("actualRange")
        }
        val LEAKED_IN_PLACE_LAMBDA by warning<PsiElement> {
            parameter<Symbol>("lambda")
        }
        val VARIABLE_WITH_NO_TYPE_NO_INITIALIZER by warning<KtVariableDeclaration>(PositioningStrategy.DECLARATION_NAME)

        val INITIALIZATION_BEFORE_DECLARATION by warning<KtExpression> {
            parameter<Symbol>("property")
        }
        val INITIALIZATION_BEFORE_DECLARATION_WARNING by warning<KtExpression> {
            parameter<Symbol>("property")
        }
        val UNREACHABLE_CODE by warning<KtElement>(PositioningStrategy.UNREACHABLE_CODE) {
            parameter<Set<KtSourceElement>>("reachable")
            parameter<Set<KtSourceElement>>("unreachable")
        }
        val SENSELESS_COMPARISON by warning<KtExpression> {
            parameter<Boolean>("compareResult")
        }
        val SENSELESS_NULL_IN_WHEN by warning<KtElement>()
        val TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM by warning<KtExpression>()

        val RETURN_VALUE_NOT_USED by warning<KtElement> {
            parameter<Name?>("functionName")
        }
    }

    val NULLABILITY by object : DiagnosticGroup("Nullability") {
        val UNSAFE_CALL by warning<PsiElement>(PositioningStrategy.DOT_BY_QUALIFIED) {
            parameter<ConeKotlinType>("receiverType")
            parameter<FirExpression?>("receiverExpression")
        }
        val UNSAFE_IMPLICIT_INVOKE_CALL by warning<PsiElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<ConeKotlinType>("receiverType")
        }
        val UNSAFE_INFIX_CALL by warning<KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<ConeKotlinType>("receiverType")
            parameter<FirExpression>("receiverExpression")
            parameter<String>("operator")
            parameter<FirExpression?>("argumentExpression")
        }
        val UNSAFE_OPERATOR_CALL by warning<KtExpression>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<ConeKotlinType>("receiverType")
            parameter<FirExpression>("receiverExpression")
            parameter<String>("operator")
            parameter<FirExpression?>("argumentExpression")
        }
        val ITERATOR_ON_NULLABLE by warning<KtExpression>()
        val UNNECESSARY_SAFE_CALL by warning<PsiElement>(PositioningStrategy.SAFE_ACCESS) {
            parameter<ConeKotlinType>("receiverType")
        }
        val UNEXPECTED_SAFE_CALL by warning<PsiElement>(PositioningStrategy.SAFE_ACCESS)
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
        val CANNOT_CHECK_FOR_ERASED by warning<PsiElement> {
            parameter<ConeKotlinType>("type")
        }
        val CAST_NEVER_SUCCEEDS by warning<KtBinaryExpressionWithTypeRHS>(PositioningStrategy.OPERATOR)
        val USELESS_CAST by warning<KtBinaryExpressionWithTypeRHS>(PositioningStrategy.AS_TYPE)
        val UNCHECKED_CAST by warning<KtBinaryExpressionWithTypeRHS>(PositioningStrategy.AS_TYPE) {
            parameter<ConeKotlinType>("originalType")
            parameter<ConeKotlinType>("targetType")
        }
        val USELESS_IS_CHECK by warning<KtElement> {
            parameter<Boolean>("compileTimeCheckResult")
        }
        val IS_ENUM_ENTRY by warning<KtElement>()
        val DYNAMIC_NOT_ALLOWED by warning<KtElement>()
        val ENUM_ENTRY_AS_TYPE by warning<KtElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
    }

    val WHEN_EXPRESSIONS by object : DiagnosticGroup("When expressions") {
        val EXPECTED_CONDITION by warning<KtWhenCondition>()
        val NO_ELSE_IN_WHEN by warning<KtWhenExpression>(PositioningStrategy.WHEN_EXPRESSION) {
            parameter<List<WhenMissingCase>>("missingWhenCases")
            parameter<String>("description")
        }
        val INVALID_IF_AS_EXPRESSION by warning<KtIfExpression>(PositioningStrategy.IF_EXPRESSION)
        val ELSE_MISPLACED_IN_WHEN by warning<KtWhenEntry>(PositioningStrategy.ELSE_ENTRY)
        val REDUNDANT_ELSE_IN_WHEN by warning<KtWhenEntry>(PositioningStrategy.ELSE_ENTRY)
        val ILLEGAL_DECLARATION_IN_WHEN_SUBJECT by warning<KtElement> {
            parameter<String>("illegalReason")
        }
        val COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT by warning<PsiElement>(PositioningStrategy.COMMAS)
        val DUPLICATE_BRANCH_CONDITION_IN_WHEN by warning<KtElement>()
        val CONFUSING_BRANCH_CONDITION by deprecationError<PsiElement>(LanguageFeature.ProhibitConfusingSyntaxInWhenBranches)
        val WRONG_CONDITION_SUGGEST_GUARD by warning<PsiElement>(PositioningStrategy.OPERATOR)

        val COMMA_IN_WHEN_CONDITION_WITH_WHEN_GUARD by warning<PsiElement>(PositioningStrategy.WHEN_GUARD)
        val WHEN_GUARD_WITHOUT_SUBJECT by warning<PsiElement>(PositioningStrategy.WHEN_GUARD)
        val INFERRED_INVISIBLE_WHEN_TYPE by deprecationError<KtElement>(
            LanguageFeature.ForbidInferOfInvisibleTypeAsReifiedVarargOrReturnType
        ) {
            parameter<ConeKotlinType>("whenType")
            parameter<String>("syntaxConstructionName")
        }
    }

    val CONTEXT_TRACKING by object : DiagnosticGroup("Context tracking") {
        val TYPE_PARAMETER_IS_NOT_AN_EXPRESSION by warning<KtSimpleNameExpression> {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }
        val TYPE_PARAMETER_ON_LHS_OF_DOT by warning<KtSimpleNameExpression> {
            parameter<FirTypeParameterSymbol>("typeParameter")
        }
        val NO_COMPANION_OBJECT by warning<KtExpression>(PositioningStrategy.SELECTOR_BY_QUALIFIED) {
            parameter<FirClassLikeSymbol<*>>("klass")
        }
        val EXPRESSION_EXPECTED_PACKAGE_FOUND by warning<KtExpression>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
    }

    val FUNCTION_CONTRACTS by object : DiagnosticGroup("Function contracts") {
        val ERROR_IN_CONTRACT_DESCRIPTION by warning<KtElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED) {
            parameter<String>("reason")
        }
        val CONTRACT_NOT_ALLOWED by warning<KtElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<String>("reason")
        }
    }

    val CONVENTIONS by object : DiagnosticGroup("Conventions") {
        val NO_GET_METHOD by warning<KtArrayAccessExpression>(PositioningStrategy.ARRAY_ACCESS)
        val NO_SET_METHOD by warning<KtArrayAccessExpression>(PositioningStrategy.ARRAY_ACCESS)
        val ITERATOR_MISSING by warning<KtExpression>()
        val HAS_NEXT_MISSING by warning<KtExpression>()
        val NEXT_MISSING by warning<KtExpression>()
        val HAS_NEXT_FUNCTION_NONE_APPLICABLE by warning<KtExpression> {
            parameter<Collection<FirBasedSymbol<*>>>("candidates")
        }
        val NEXT_NONE_APPLICABLE by warning<KtExpression> {
            parameter<Collection<FirBasedSymbol<*>>>("candidates")
        }
        val DELEGATE_SPECIAL_FUNCTION_MISSING by warning<KtExpression>(PositioningStrategy.PROPERTY_DELEGATE_BY_KEYWORD) {
            parameter<String>("expectedFunctionSignature")
            parameter<ConeKotlinType>("delegateType")
            parameter<String>("description")
        }
        val DELEGATE_SPECIAL_FUNCTION_AMBIGUITY by warning<KtExpression>(PositioningStrategy.PROPERTY_DELEGATE_BY_KEYWORD) {
            parameter<String>("expectedFunctionSignature")
            parameter<Collection<FirBasedSymbol<*>>>("candidates")
        }
        val DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE by warning<KtExpression>(PositioningStrategy.PROPERTY_DELEGATE_BY_KEYWORD) {
            parameter<String>("expectedFunctionSignature")
            parameter<Collection<FirBasedSymbol<*>>>("candidates")
        }
        val DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH by warning<KtExpression>(PositioningStrategy.PROPERTY_DELEGATE_BY_KEYWORD) {
            parameter<String>("delegateFunction")
            parameter<ConeKotlinType>("expectedType")
            parameter<ConeKotlinType>("actualType")
        }

        val UNDERSCORE_IS_RESERVED by warning<PsiElement>(PositioningStrategy.NAME_IDENTIFIER)
        val UNDERSCORE_USAGE_WITHOUT_BACKTICKS by warning<PsiElement>(PositioningStrategy.NAME_IDENTIFIER)
        val RESOLVED_TO_UNDERSCORE_NAMED_CATCH_PARAMETER by warning<KtNameReferenceExpression>()
        val INVALID_CHARACTERS by warning<PsiElement>(PositioningStrategy.NAME_IDENTIFIER) {
            parameter<String>("message")
        }
        val EQUALITY_NOT_APPLICABLE by warning<KtBinaryExpression> {
            parameter<String>("operator")
            parameter<ConeKotlinType>("leftType")
            parameter<ConeKotlinType>("rightType")
        }
        val EQUALITY_NOT_APPLICABLE_WARNING by warning<KtBinaryExpression> {
            parameter<String>("operator")
            parameter<ConeKotlinType>("leftType")
            parameter<ConeKotlinType>("rightType")
        }
        val INCOMPATIBLE_ENUM_COMPARISON_ERROR by warning<KtElement> {
            parameter<ConeKotlinType>("leftType")
            parameter<ConeKotlinType>("rightType")
        }
        val INCOMPATIBLE_ENUM_COMPARISON by warning<KtElement> {
            parameter<ConeKotlinType>("leftType")
            parameter<ConeKotlinType>("rightType")
        }
        val FORBIDDEN_IDENTITY_EQUALS by warning<KtElement> {
            parameter<ConeKotlinType>("leftType")
            parameter<ConeKotlinType>("rightType")
        }
        val FORBIDDEN_IDENTITY_EQUALS_WARNING by warning<KtElement> {
            parameter<ConeKotlinType>("leftType")
            parameter<ConeKotlinType>("rightType")
        }
        val DEPRECATED_IDENTITY_EQUALS by warning<KtElement> {
            parameter<ConeKotlinType>("leftType")
            parameter<ConeKotlinType>("rightType")
        }
        val IMPLICIT_BOXING_IN_IDENTITY_EQUALS by warning<KtElement> {
            parameter<ConeKotlinType>("leftType")
            parameter<ConeKotlinType>("rightType")
        }
        val INC_DEC_SHOULD_NOT_RETURN_UNIT by warning<KtExpression>(PositioningStrategy.OPERATOR)
        val ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT by warning<KtExpression>(PositioningStrategy.OPERATOR) {
            parameter<FirNamedFunctionSymbol>("functionSymbol")
            parameter<String>("operator")
        }
        val NOT_FUNCTION_AS_OPERATOR by warning<PsiElement>(PositioningStrategy.OPERATOR) {
            parameter<String>("elementName")
            parameter<FirBasedSymbol<*>>("elementSymbol")
        }
        val DSL_SCOPE_VIOLATION by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<FirBasedSymbol<*>>("calleeSymbol")
        }
    }

    val TYPE_ALIAS by object : DiagnosticGroup("Type alias") {
        val TOPLEVEL_TYPEALIASES_ONLY by warning<KtTypeAlias>()
        val RECURSIVE_TYPEALIAS_EXPANSION by warning<KtElement>()
        val TYPEALIAS_SHOULD_EXPAND_TO_CLASS by warning<KtElement> {
            parameter<ConeKotlinType>("expandedType")
        }
        val CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION by deprecationError<KtElement>(
            LanguageFeature.ProhibitConstructorAndSupertypeOnTypealiasWithTypeProjection
        )
        val TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS by warning<KtElement> {
            parameter<Set<FirTypeParameterSymbol>>("outerTypeParameters")
        }
    }

    val EXTRA_CHECKERS by object : DiagnosticGroup("Extra checkers") {
        val REDUNDANT_VISIBILITY_MODIFIER by warning<KtModifierListOwner>(PositioningStrategy.VISIBILITY_MODIFIER)
        val REDUNDANT_MODALITY_MODIFIER by warning<KtModifierListOwner>(PositioningStrategy.MODALITY_MODIFIER)
        val REDUNDANT_RETURN_UNIT_TYPE by warning<KtElement>()
        val REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE by warning<PsiElement>()
        val CAN_BE_VAL by warning<KtDeclaration>(PositioningStrategy.VAL_OR_VAR_NODE)
        val CAN_BE_VAL_LATEINIT by warning<KtDeclaration>(PositioningStrategy.VAL_OR_VAR_NODE)
        val CAN_BE_VAL_DELAYED_INITIALIZATION by warning<KtDeclaration>(PositioningStrategy.VAL_OR_VAR_NODE)
        val REDUNDANT_CALL_OF_CONVERSION_METHOD by warning<PsiElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
        val ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS by warning<KtExpression>(PositioningStrategy.OPERATOR)
        val EMPTY_RANGE by warning<PsiElement>()
        val REDUNDANT_SETTER_PARAMETER_TYPE by warning<PsiElement>()
        val UNUSED_VARIABLE by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val ASSIGNED_VALUE_IS_NEVER_READ by warning<PsiElement>()
        val VARIABLE_INITIALIZER_IS_REDUNDANT by warning<PsiElement>()
        val VARIABLE_NEVER_READ by warning<KtNamedDeclaration>(PositioningStrategy.DECLARATION_NAME)
        val USELESS_CALL_ON_NOT_NULL by warning<PsiElement>(PositioningStrategy.SELECTOR_BY_QUALIFIED)
        val UNUSED_ANONYMOUS_PARAMETER by warning<KtElement>(PositioningStrategy.DECLARATION_NAME) {
            parameter<FirValueParameterSymbol>("parameter")
        }
        val UNUSED_EXPRESSION by warning<PsiElement>()
        val UNUSED_LAMBDA_EXPRESSION by warning<PsiElement>()
    }

    val RETURNS by object : DiagnosticGroup("Returns") {
        val RETURN_NOT_ALLOWED by warning<KtReturnExpression>(PositioningStrategy.RETURN_WITH_LABEL)
        val NOT_A_FUNCTION_LABEL by warning<KtReturnExpression>(PositioningStrategy.RETURN_WITH_LABEL)
        val RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY by warning<KtReturnExpression>(PositioningStrategy.RETURN_WITH_LABEL)
        val RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_WARNING by warning<KtReturnExpression>(PositioningStrategy.RETURN_WITH_LABEL)
        val RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE by warning<KtReturnExpression>(PositioningStrategy.RETURN_WITH_LABEL)
        val NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY by warning<KtDeclarationWithBody>(PositioningStrategy.DECLARATION_WITH_BODY)
        val REDUNDANT_RETURN by warning<KtReturnExpression>(PositioningStrategy.RETURN_WITH_LABEL)

        val ANONYMOUS_INITIALIZER_IN_INTERFACE by warning<KtAnonymousInitializer>(PositioningStrategy.DECLARATION_SIGNATURE)
    }

    val INLINE by object : DiagnosticGroup("Inline") {
        val USAGE_IS_NOT_INLINABLE by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("parameter")
        }

        val NON_LOCAL_RETURN_NOT_ALLOWED by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("parameter")
        }

        val NOT_YET_SUPPORTED_IN_INLINE by warning<KtDeclaration>(PositioningStrategy.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT) {
            parameter<String>("message")
        }

        val NOTHING_TO_INLINE by warning<KtDeclaration>(PositioningStrategy.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT)

        val NULLABLE_INLINE_PARAMETER by warning<KtDeclaration>() {
            parameter<FirValueParameterSymbol>("parameter")
            parameter<Symbol>("function")
        }

        val RECURSION_IN_INLINE by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("symbol")
        }

        val NON_PUBLIC_CALL_FROM_PUBLIC_INLINE by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("inlineDeclaration")
            parameter<Symbol>("referencedDeclaration")
        }

        val NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("inlineDeclaration")
            parameter<Symbol>("referencedDeclaration")
        }

        val NON_PUBLIC_CALL_FROM_PUBLIC_INLINE_DEPRECATION by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("inlineDeclaration")
            parameter<Symbol>("referencedDeclaration")
        }

        val NON_PUBLIC_DATA_COPY_CALL_FROM_PUBLIC_INLINE by deprecationError<KtElement>(
            LanguageFeature.ErrorAboutDataClassCopyVisibilityChange,
            PositioningStrategy.REFERENCE_BY_QUALIFIED
        ) {
            parameter<Symbol>("inlineDeclaration")
        }

        val PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("inlineDeclaration")
            parameter<Symbol>("referencedDeclaration")
        }

        val PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("inlineDeclaration")
            parameter<Symbol>("referencedDeclaration")
        }

        val PRIVATE_CLASS_MEMBER_FROM_INLINE by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("inlineDeclaration")
            parameter<Symbol>("referencedDeclaration")
        }

        val SUPER_CALL_FROM_PUBLIC_INLINE by warning<KtElement>(PositioningStrategy.REFERENCE_BY_QUALIFIED) {
            parameter<Symbol>("symbol")
        }

        val DECLARATION_CANT_BE_INLINED by warning<KtDeclaration>(PositioningStrategy.INLINE_FUN_MODIFIER)
        val DECLARATION_CANT_BE_INLINED_DEPRECATION by deprecationError<KtDeclaration>(
            LanguageFeature.ProhibitInlineModifierOnPrimaryConstructorParameters,
            PositioningStrategy.INLINE_FUN_MODIFIER,
        )

        val OVERRIDE_BY_INLINE by warning<KtDeclaration>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)

        val NON_INTERNAL_PUBLISHED_API by warning<KtElement>()

        val INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE by warning<KtElement>() {
            parameter<FirValueParameterSymbol>("parameter")
        }

        val NOT_SUPPORTED_INLINE_PARAMETER_IN_INLINE_PARAMETER_DEFAULT_VALUE by warning<KtElement> {
            parameter<FirValueParameterSymbol>("parameter")
        }

        val REIFIED_TYPE_PARAMETER_IN_OVERRIDE by warning<KtElement>(PositioningStrategy.REIFIED_MODIFIER)

        val INLINE_PROPERTY_WITH_BACKING_FIELD by warning<KtDeclaration>(PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)
        val INLINE_PROPERTY_WITH_BACKING_FIELD_DEPRECATION by deprecationError<KtDeclaration>(
            LanguageFeature.ProhibitInlineModifierOnPrimaryConstructorParameters,
            PositioningStrategy.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS,
        )

        val ILLEGAL_INLINE_PARAMETER_MODIFIER by warning<KtElement>(PositioningStrategy.INLINE_PARAMETER_MODIFIER)

        val INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED by warning<KtParameter>()

        val INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS by warning<KtNamedFunction>(PositioningStrategy.DECLARATION_NAME) {
            parameter<ConeKotlinType>("type")
        }

        val INLINE_CLASS_DEPRECATED by warning<KtElement>(PositioningStrategy.INLINE_OR_VALUE_MODIFIER)

        val LESS_VISIBLE_TYPE_ACCESS_IN_INLINE by deprecationError<KtElement>(
            LanguageFeature.ForbidExposingLessVisibleTypesInInline,
            PositioningStrategy.REFERENCE_BY_QUALIFIED
        ) {
            parameter<EffectiveVisibility>("typeVisibility")
            parameter<ConeKotlinType>("type")
            parameter<EffectiveVisibility>("inlineVisibility")
        }

        val LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE by deprecationError<KtElement>(
            LanguageFeature.ForbidExposingLessVisibleTypesInInline,
            PositioningStrategy.REFERENCE_BY_QUALIFIED
        ) {
            parameter<FirBasedSymbol<*>>("symbol")
            parameter<EffectiveVisibility>("typeVisibility")
            parameter<ConeKotlinType>("type")
            parameter<EffectiveVisibility>("inlineVisibility")
        }

        val LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE by deprecationError<KtElement>(
            LanguageFeature.ForbidExposingLessVisibleTypesInInline,
            PositioningStrategy.REFERENCE_BY_QUALIFIED
        ) {
            parameter<FirBasedSymbol<*>>("symbol")
            parameter<EffectiveVisibility>("visibility")
            parameter<FirRegularClassSymbol>("containingClass")
            parameter<EffectiveVisibility>("inlineVisibility")
        }

        val CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE by deprecationError<KtElement>(
            LanguageFeature.ForbidExposingLessVisibleTypesInInline,
            PositioningStrategy.REFERENCE_BY_QUALIFIED
        ) {
            parameter<FirBasedSymbol<*>>("symbol")
            parameter<EffectiveVisibility>("visibility")
            parameter<EffectiveVisibility>("inlineVisibility")
        }
    }

    val IMPORTS by object : DiagnosticGroup("Imports") {
        val CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON by warning<KtImportDirective>(PositioningStrategy.IMPORT_LAST_NAME) {
            parameter<Name>("objectName")
        }

        val PACKAGE_CANNOT_BE_IMPORTED by warning<KtImportDirective>(PositioningStrategy.IMPORT_LAST_NAME)

        val CANNOT_BE_IMPORTED by warning<KtImportDirective>(PositioningStrategy.IMPORT_LAST_NAME) {
            parameter<Name>("name")
        }

        val CONFLICTING_IMPORT by warning<KtImportDirective>(PositioningStrategy.IMPORT_ALIAS) {
            parameter<Name>("name")
        }

        val OPERATOR_RENAMED_ON_IMPORT by warning<KtImportDirective>(PositioningStrategy.IMPORT_LAST_NAME)

        val TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT by deprecationError<KtImportDirective>(
            LanguageFeature.ProhibitTypealiasAsCallableQualifierInImport,
            PositioningStrategy.IMPORT_LAST_BUT_ONE_NAME,
        ) {
            parameter<Name>("typealiasName")
            parameter<Name>("originalClassName")
        }
    }

    val SUSPEND by object : DiagnosticGroup("Suspend errors") {
        val ILLEGAL_SUSPEND_FUNCTION_CALL by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Symbol>("suspendCallable")
        }
        val ILLEGAL_SUSPEND_PROPERTY_ACCESS by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED) {
            parameter<Symbol>("suspendCallable")
        }
        val NON_LOCAL_SUSPENSION_POINT by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND_FUN by deprecationError<PsiElement>(
            LanguageFeature.ModifierNonBuiltinSuspendFunError, PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED
        )
        val RETURN_FOR_BUILT_IN_SUSPEND by warning<KtReturnExpression>()
        val MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES by warning<PsiElement>(PositioningStrategy.SUPERTYPES_LIST)
        val MIXING_FUNCTIONAL_KINDS_IN_SUPERTYPES by warning<PsiElement>(PositioningStrategy.SUPERTYPES_LIST) {
            parameter<Set<FunctionTypeKind>>("kinds")
        }
    }

    val LABEL by object : DiagnosticGroup("label") {
        val REDUNDANT_LABEL_WARNING by warning<KtLabelReferenceExpression>(PositioningStrategy.LABEL)
        val MULTIPLE_LABELS_ARE_FORBIDDEN by warning<KtLabelReferenceExpression>(PositioningStrategy.LABEL)
    }

    val ENUM_ENTRIES_DEPRECATIONS by object : DiagnosticGroup("Enum.entries resolve deprecations") {
        val DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY by warning<PsiElement>()
        val DEPRECATED_ACCESS_TO_ENTRY_PROPERTY_FROM_ENUM by warning<PsiElement>()
        val DEPRECATED_ACCESS_TO_ENTRIES_PROPERTY by warning<PsiElement>()
        val DEPRECATED_ACCESS_TO_ENUM_ENTRY_PROPERTY_AS_REFERENCE by warning<PsiElement>(PositioningStrategy.REFERENCED_NAME_BY_QUALIFIED)
        val DEPRECATED_ACCESS_TO_ENTRIES_AS_QUALIFIER by warning<PsiElement>()
        val DECLARATION_OF_ENUM_ENTRY_ENTRIES by deprecationError<KtEnumEntry>(
            LanguageFeature.ForbidEnumEntryNamedEntries, PositioningStrategy.DECLARATION_NAME
        )
    }

    val COMPATIBILITY_ISSUES by object : DiagnosticGroup("Compatibility issues") {
        val INCOMPATIBLE_CLASS by warning<PsiElement> {
            parameter<String>("presentableString")
            parameter<IncompatibleVersionErrorData<*>>("incompatibility")
        }
        val PRE_RELEASE_CLASS by warning<PsiElement> {
            parameter<String>("presentableString")
            parameter<List<String>>("poisoningFeatures")
        }
        val IR_WITH_UNSTABLE_ABI_COMPILED_CLASS by warning<PsiElement> {
            parameter<String>("presentableString")
        }
    }

    val BUILDER_INFERENCE by object : DiagnosticGroup("Builder inference") {
        val BUILDER_INFERENCE_STUB_RECEIVER by warning<PsiElement> {
            parameter<Name>("typeParameterName")
            parameter<Name>("containingDeclarationName")
        }
        val BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION by warning<PsiElement> {
            parameter<Name>("typeParameterName")
            parameter<Name>("containingDeclarationName")
        }
    }
}

private val exposedVisibilityDiagnosticInit: DiagnosticBuilder.() -> Unit = {
    parameter<EffectiveVisibility>("elementVisibility")
    parameter<FirClassLikeSymbol<*>>("restrictingDeclaration")
    parameter<RelationToType>("relationToType")
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

private inline fun <reified P : PsiElement> AbstractDiagnosticGroup.exposedVisibilityDeprecationError(
    languageFeature: LanguageFeature,
    positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT
): PropertyDelegateProvider<Any?, ReadOnlyProperty<AbstractDiagnosticGroup, DeprecationDiagnosticData>> {
    return deprecationError<P>(languageFeature, positioningStrategy, exposedVisibilityDiagnosticInit)
}

typealias Symbol = FirBasedSymbol<*>
