/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.jvm

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitConcurrentHashMapContains
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitSpreadOnSignaturePolymorphicCall
import org.jetbrains.kotlin.config.LanguageFeature.RepeatableAnnotationContainerConstraints
import org.jetbrains.kotlin.config.LanguageFeature.SynchronizedSuspendError
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation4
import org.jetbrains.kotlin.diagnostics.Severity.ERROR
import org.jetbrains.kotlin.diagnostics.Severity.WARNING
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Generated from: [org.jetbrains.kotlin.fir.checkers.generator.diagnostics.JVM_DIAGNOSTICS_LIST]
 */
@Suppress("IncorrectFormatting")
object FirJvmErrors {
    // Declarations
    val OVERRIDE_CANNOT_BE_STATIC: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERRIDE_CANNOT_BE_STATIC", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val JVM_STATIC_ON_NON_PUBLIC_MEMBER: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_STATIC_ON_NON_PUBLIC_MEMBER", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val JVM_STATIC_ON_CONST_OR_JVM_FIELD: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_STATIC_ON_CONST_OR_JVM_FIELD", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val JVM_STATIC_ON_EXTERNAL_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_STATIC_ON_EXTERNAL_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val INAPPLICABLE_JVM_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_JVM_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val ILLEGAL_JVM_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("ILLEGAL_JVM_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val FUNCTION_DELEGATE_MEMBER_NAME_CLASH: KtDiagnosticFactory0 = KtDiagnosticFactory0("FUNCTION_DELEGATE_MEMBER_NAME_CLASH", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, PsiElement::class)
    val VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val JVM_INLINE_WITHOUT_VALUE_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_INLINE_WITHOUT_VALUE_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val WRONG_NULLABILITY_FOR_JAVA_OVERRIDE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("WRONG_NULLABILITY_FOR_JAVA_OVERRIDE", WARNING, SourceElementPositioningStrategies.OVERRIDE_MODIFIER, PsiElement::class)
    val ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE: KtDiagnosticFactory3<FirNamedFunctionSymbol, String, FirNamedFunctionSymbol> = KtDiagnosticFactory3("ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedFunction::class)
    val NOT_YET_SUPPORTED_LOCAL_INLINE_FUNCTION: KtDiagnosticFactory0 = KtDiagnosticFactory0("NOT_YET_SUPPORTED_LOCAL_INLINE_FUNCTION", ERROR, SourceElementPositioningStrategies.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT, KtDeclaration::class)

    // Types
    val JAVA_TYPE_MISMATCH: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("JAVA_TYPE_MISMATCH", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class)
    val RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, String> = KtDiagnosticFactory3("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, String> = KtDiagnosticFactory3("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)

    // Type parameters
    val UPPER_BOUND_CANNOT_BE_ARRAY: KtDiagnosticFactory0 = KtDiagnosticFactory0("UPPER_BOUND_CANNOT_BE_ARRAY", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)

    // annotations
    val STRICTFP_ON_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("STRICTFP_ON_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val SYNCHRONIZED_ON_ABSTRACT: KtDiagnosticFactory0 = KtDiagnosticFactory0("SYNCHRONIZED_ON_ABSTRACT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val SYNCHRONIZED_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("SYNCHRONIZED_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val SYNCHRONIZED_ON_INLINE: KtDiagnosticFactory0 = KtDiagnosticFactory0("SYNCHRONIZED_ON_INLINE", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val SYNCHRONIZED_ON_SUSPEND: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("SYNCHRONIZED_ON_SUSPEND", SynchronizedSuspendError, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val OVERLOADS_ABSTRACT: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERLOADS_ABSTRACT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val OVERLOADS_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERLOADS_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val OVERLOADS_LOCAL: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERLOADS_LOCAL", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR", ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val OVERLOADS_PRIVATE: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERLOADS_PRIVATE", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val DEPRECATED_JAVA_ANNOTATION: KtDiagnosticFactory1<FqName> = KtDiagnosticFactory1("DEPRECATED_JAVA_ANNOTATION", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val JVM_PACKAGE_NAME_CANNOT_BE_EMPTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_PACKAGE_NAME_CANNOT_BE_EMPTY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val JVM_PACKAGE_NAME_MUST_BE_VALID_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_PACKAGE_NAME_MUST_BE_VALID_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class)
    val REDUNDANT_REPEATABLE_ANNOTATION: KtDiagnosticFactory2<FqName, FqName> = KtDiagnosticFactory2("REDUNDANT_REPEATABLE_ANNOTATION", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)

    // Super
    val INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER: KtDiagnosticFactory0 = KtDiagnosticFactory0("INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class)

    // JVM Records
    val LOCAL_JVM_RECORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("LOCAL_JVM_RECORD", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val NON_FINAL_JVM_RECORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_FINAL_JVM_RECORD", ERROR, SourceElementPositioningStrategies.NON_FINAL_MODIFIER_OR_NAME, PsiElement::class)
    val ENUM_JVM_RECORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("ENUM_JVM_RECORD", ERROR, SourceElementPositioningStrategies.ENUM_MODIFIER, PsiElement::class)
    val JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val NON_DATA_CLASS_JVM_RECORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_DATA_CLASS_JVM_RECORD", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val JVM_RECORD_NOT_VAL_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_RECORD_NOT_VAL_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val JVM_RECORD_NOT_LAST_VARARG_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_RECORD_NOT_LAST_VARARG_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val INNER_JVM_RECORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("INNER_JVM_RECORD", ERROR, SourceElementPositioningStrategies.INNER_MODIFIER, PsiElement::class)
    val FIELD_IN_JVM_RECORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("FIELD_IN_JVM_RECORD", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val DELEGATION_BY_IN_JVM_RECORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("DELEGATION_BY_IN_JVM_RECORD", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val JVM_RECORD_EXTENDS_CLASS: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("JVM_RECORD_EXTENDS_CLASS", ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, PsiElement::class)
    val ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)

    // JVM Modules
    val JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE: KtDiagnosticFactory2<String, String> = KtDiagnosticFactory2("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)

    // JVM Default
    val JVM_DEFAULT_IN_DECLARATION: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("JVM_DEFAULT_IN_DECLARATION", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class)
    val JVM_DEFAULT_WITH_COMPATIBILITY_IN_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_DEFAULT_WITH_COMPATIBILITY_IN_DECLARATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val JVM_DEFAULT_WITH_COMPATIBILITY_NOT_ON_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_DEFAULT_WITH_COMPATIBILITY_NOT_ON_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)

    // External Declaration
    val EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT", ERROR, SourceElementPositioningStrategies.ABSTRACT_MODIFIER, KtDeclaration::class)
    val EXTERNAL_DECLARATION_CANNOT_HAVE_BODY: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTERNAL_DECLARATION_CANNOT_HAVE_BODY", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, KtDeclaration::class)
    val EXTERNAL_DECLARATION_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTERNAL_DECLARATION_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, KtDeclaration::class)
    val EXTERNAL_DECLARATION_CANNOT_BE_INLINED: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTERNAL_DECLARATION_CANNOT_BE_INLINED", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, KtDeclaration::class)

    // Repeatable Annotations
    val NON_SOURCE_REPEATED_ANNOTATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_SOURCE_REPEATED_ANNOTATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val REPEATED_ANNOTATION_WITH_CONTAINER: KtDiagnosticFactory2<ClassId, ClassId> = KtDiagnosticFactory2("REPEATED_ANNOTATION_WITH_CONTAINER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY: KtDiagnosticFactoryForDeprecation2<ClassId, ClassId> = KtDiagnosticFactoryForDeprecation2("REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY", RepeatableAnnotationContainerConstraints, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER: KtDiagnosticFactoryForDeprecation2<ClassId, Name> = KtDiagnosticFactoryForDeprecation2("REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER", RepeatableAnnotationContainerConstraints, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION: KtDiagnosticFactoryForDeprecation4<ClassId, String, ClassId, String> = KtDiagnosticFactoryForDeprecation4("REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION", RepeatableAnnotationContainerConstraints, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET: KtDiagnosticFactoryForDeprecation2<ClassId, ClassId> = KtDiagnosticFactoryForDeprecation2("REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET", RepeatableAnnotationContainerConstraints, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER", RepeatableAnnotationContainerConstraints, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)

    // Suspension Point
    val SUSPENSION_POINT_INSIDE_CRITICAL_SECTION: KtDiagnosticFactory1<FirCallableSymbol<*>> = KtDiagnosticFactory1("SUSPENSION_POINT_INSIDE_CRITICAL_SECTION", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class)

    // Inline
    val INLINE_FROM_HIGHER_PLATFORM: KtDiagnosticFactory2<String, String> = KtDiagnosticFactory2("INLINE_FROM_HIGHER_PLATFORM", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)

    // Misc
    val INAPPLICABLE_JVM_FIELD: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INAPPLICABLE_JVM_FIELD", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val INAPPLICABLE_JVM_FIELD_WARNING: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INAPPLICABLE_JVM_FIELD_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val JVM_SYNTHETIC_ON_DELEGATE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_SYNTHETIC_ON_DELEGATE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class)
    val SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class)
    val CONCURRENT_HASH_MAP_CONTAINS_OPERATOR: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("CONCURRENT_HASH_MAP_CONTAINS_OPERATOR", ProhibitConcurrentHashMapContains, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL", ProhibitSpreadOnSignaturePolymorphicCall, SourceElementPositioningStrategies.SPREAD_OPERATOR, PsiElement::class)
    val JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val NO_REFLECTION_IN_CLASS_PATH: KtDiagnosticFactory0 = KtDiagnosticFactory0("NO_REFLECTION_IN_CLASS_PATH", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN: KtDiagnosticFactory2<FirNamedFunctionSymbol, Name> = KtDiagnosticFactory2("SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN", WARNING, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class)
    val JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY: KtDiagnosticFactory1<FirPropertySymbol> = KtDiagnosticFactory1("JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class)
    val MISSING_BUILT_IN_DECLARATION: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("MISSING_BUILT_IN_DECLARATION", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class)

    init {
        RootDiagnosticRendererFactory.registerFactory(FirJvmErrorsDefaultMessages)
    }
}
