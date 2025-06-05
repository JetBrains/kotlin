/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.jvm

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature.ForbidImplementationByDelegationWithDifferentGenericSignature
import org.jetbrains.kotlin.config.LanguageFeature.ForbidJvmAnnotationsOnAnnotationParameters
import org.jetbrains.kotlin.config.LanguageFeature.ForbidJvmSerializableLambdaOnInlinedFunctionLiterals
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitSynchronizationByValueClassesAndPrimitives
import org.jetbrains.kotlin.config.LanguageFeature.SynchronizedSuspendError
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory4
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.Severity.ERROR
import org.jetbrains.kotlin.diagnostics.Severity.WARNING
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * Generated from: [org.jetbrains.kotlin.fir.checkers.generator.diagnostics.JVM_DIAGNOSTICS_LIST]
 */
@Suppress("IncorrectFormatting")
object FirJvmErrors : KtDiagnosticsContainer() {
    // Declarations
    val OVERRIDE_CANNOT_BE_STATIC: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERRIDE_CANNOT_BE_STATIC", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_STATIC_ON_NON_PUBLIC_MEMBER: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_STATIC_ON_NON_PUBLIC_MEMBER", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_STATIC_ON_CONST_OR_JVM_FIELD: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_STATIC_ON_CONST_OR_JVM_FIELD", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_STATIC_ON_EXTERNAL_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_STATIC_ON_EXTERNAL_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INAPPLICABLE_JVM_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_JVM_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ILLEGAL_JVM_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("ILLEGAL_JVM_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val FUNCTION_DELEGATE_MEMBER_NAME_CLASH: KtDiagnosticFactory0 = KtDiagnosticFactory0("FUNCTION_DELEGATE_MEMBER_NAME_CLASH", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, PsiElement::class, getRendererFactory())
    val VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_INLINE_WITHOUT_VALUE_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_INLINE_WITHOUT_VALUE_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val USELESS_JVM_EXPOSE_BOXED: KtDiagnosticFactory0 = KtDiagnosticFactory0("USELESS_JVM_EXPOSE_BOXED", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SUSPEND: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SUSPEND", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_EXPOSE_BOXED_REQUIRES_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_EXPOSE_BOXED_REQUIRES_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME_AS_JVM_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME_AS_JVM_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_EXPOSE_BOXED_CANNOT_EXPOSE_OPEN_ABSTRACT: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_EXPOSE_BOXED_CANNOT_EXPOSE_OPEN_ABSTRACT", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SYNTHETIC: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SYNTHETIC", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_EXPOSE_BOXED_CANNOT_EXPOSE_LOCALS: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_EXPOSE_BOXED_CANNOT_EXPOSE_LOCALS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_EXPOSE_BOXED_CANNOT_EXPOSE_REIFIED: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_EXPOSE_BOXED_CANNOT_EXPOSE_REIFIED", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val WRONG_NULLABILITY_FOR_JAVA_OVERRIDE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> = KtDiagnosticFactory2("WRONG_NULLABILITY_FOR_JAVA_OVERRIDE", WARNING, SourceElementPositioningStrategies.OVERRIDE_MODIFIER, PsiElement::class, getRendererFactory())
    val ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE: KtDiagnosticFactory3<FirNamedFunctionSymbol, String, FirNamedFunctionSymbol> = KtDiagnosticFactory3("ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE", ERROR, SourceElementPositioningStrategies.DECLARATION_NAME, KtNamedFunction::class, getRendererFactory())
    val IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE: KtDiagnosticFactoryForDeprecation2<FirNamedFunctionSymbol, FirNamedFunctionSymbol> = KtDiagnosticFactoryForDeprecation2("IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE", ForbidImplementationByDelegationWithDifferentGenericSignature, SourceElementPositioningStrategies.DEFAULT, KtTypeReference::class, getRendererFactory())
    val NOT_YET_SUPPORTED_LOCAL_INLINE_FUNCTION: KtDiagnosticFactory0 = KtDiagnosticFactory0("NOT_YET_SUPPORTED_LOCAL_INLINE_FUNCTION", ERROR, SourceElementPositioningStrategies.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT, KtDeclaration::class, getRendererFactory())
    val PROPERTY_HIDES_JAVA_FIELD: KtDiagnosticFactory1<FirFieldSymbol> = KtDiagnosticFactory1("PROPERTY_HIDES_JAVA_FIELD", WARNING, SourceElementPositioningStrategies.DECLARATION_NAME, KtCallableDeclaration::class, getRendererFactory())
    val CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Types
    val JAVA_TYPE_MISMATCH: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("JAVA_TYPE_MISMATCH", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, String> = KtDiagnosticFactory3("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, String> = KtDiagnosticFactory3("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NULLABILITY_MISMATCH_BASED_ON_EXPLICIT_TYPE_ARGUMENTS_FOR_JAVA: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, String> = KtDiagnosticFactory3("NULLABILITY_MISMATCH_BASED_ON_EXPLICIT_TYPE_ARGUMENTS_FOR_JAVA", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_CLASS_ON_COMPANION: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("JAVA_CLASS_ON_COMPANION", WARNING, SourceElementPositioningStrategies.SELECTOR_BY_QUALIFIED, PsiElement::class, getRendererFactory())

    // Type parameters
    val UPPER_BOUND_CANNOT_BE_ARRAY: KtDiagnosticFactory0 = KtDiagnosticFactory0("UPPER_BOUND_CANNOT_BE_ARRAY", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // annotations
    val STRICTFP_ON_CLASS: KtDiagnosticFactory0 = KtDiagnosticFactory0("STRICTFP_ON_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val SYNCHRONIZED_ON_ABSTRACT: KtDiagnosticFactory0 = KtDiagnosticFactory0("SYNCHRONIZED_ON_ABSTRACT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val SYNCHRONIZED_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("SYNCHRONIZED_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val SYNCHRONIZED_IN_ANNOTATION: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("SYNCHRONIZED_IN_ANNOTATION", ForbidJvmAnnotationsOnAnnotationParameters, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val SYNCHRONIZED_ON_INLINE: KtDiagnosticFactory0 = KtDiagnosticFactory0("SYNCHRONIZED_ON_INLINE", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val SYNCHRONIZED_ON_VALUE_CLASS: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("SYNCHRONIZED_ON_VALUE_CLASS", ProhibitSynchronizationByValueClassesAndPrimitives, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val SYNCHRONIZED_ON_SUSPEND: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("SYNCHRONIZED_ON_SUSPEND", SynchronizedSuspendError, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val OVERLOADS_ABSTRACT: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERLOADS_ABSTRACT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val OVERLOADS_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERLOADS_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val OVERLOADS_LOCAL: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERLOADS_LOCAL", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val OVERLOADS_PRIVATE: KtDiagnosticFactory0 = KtDiagnosticFactory0("OVERLOADS_PRIVATE", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val DEPRECATED_JAVA_ANNOTATION: KtDiagnosticFactory1<FqName> = KtDiagnosticFactory1("DEPRECATED_JAVA_ANNOTATION", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val JVM_PACKAGE_NAME_CANNOT_BE_EMPTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_PACKAGE_NAME_CANNOT_BE_EMPTY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val JVM_PACKAGE_NAME_MUST_BE_VALID_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_PACKAGE_NAME_MUST_BE_VALID_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtExpression::class, getRendererFactory())
    val REDUNDANT_REPEATABLE_ANNOTATION: KtDiagnosticFactory2<FqName, FqName> = KtDiagnosticFactory2("REDUNDANT_REPEATABLE_ANNOTATION", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val THROWS_IN_ANNOTATION: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("THROWS_IN_ANNOTATION", ForbidJvmAnnotationsOnAnnotationParameters, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val JVM_SERIALIZABLE_LAMBDA_ON_INLINED_FUNCTION_LITERALS: KtDiagnosticFactoryForDeprecation0 = KtDiagnosticFactoryForDeprecation0("JVM_SERIALIZABLE_LAMBDA_ON_INLINED_FUNCTION_LITERALS", ForbidJvmSerializableLambdaOnInlinedFunctionLiterals, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val INCOMPATIBLE_ANNOTATION_TARGETS: KtDiagnosticFactory2<Collection<String>, Collection<String>> = KtDiagnosticFactory2("INCOMPATIBLE_ANNOTATION_TARGETS", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val ANNOTATION_TARGETS_ONLY_IN_JAVA: KtDiagnosticFactory0 = KtDiagnosticFactory0("ANNOTATION_TARGETS_ONLY_IN_JAVA", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())

    // Super
    val INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER: KtDiagnosticFactory0 = KtDiagnosticFactory0("INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS: KtDiagnosticFactory2<ClassId, ConeKotlinType> = KtDiagnosticFactory2("JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())

    // JVM Records
    val LOCAL_JVM_RECORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("LOCAL_JVM_RECORD", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NON_FINAL_JVM_RECORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_FINAL_JVM_RECORD", ERROR, SourceElementPositioningStrategies.NON_FINAL_MODIFIER_OR_NAME, PsiElement::class, getRendererFactory())
    val ENUM_JVM_RECORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("ENUM_JVM_RECORD", ERROR, SourceElementPositioningStrategies.ENUM_MODIFIER, PsiElement::class, getRendererFactory())
    val JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NON_DATA_CLASS_JVM_RECORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_DATA_CLASS_JVM_RECORD", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_RECORD_NOT_VAL_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_RECORD_NOT_VAL_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_RECORD_NOT_LAST_VARARG_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_RECORD_NOT_LAST_VARARG_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INNER_JVM_RECORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("INNER_JVM_RECORD", ERROR, SourceElementPositioningStrategies.INNER_MODIFIER, PsiElement::class, getRendererFactory())
    val FIELD_IN_JVM_RECORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("FIELD_IN_JVM_RECORD", ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, PsiElement::class, getRendererFactory())
    val DELEGATION_BY_IN_JVM_RECORD: KtDiagnosticFactory0 = KtDiagnosticFactory0("DELEGATION_BY_IN_JVM_RECORD", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_RECORD_EXTENDS_CLASS: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("JVM_RECORD_EXTENDS_CLASS", ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, PsiElement::class, getRendererFactory())
    val ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE: KtDiagnosticFactory0 = KtDiagnosticFactory0("ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_RECORDS_ILLEGAL_BYTECODE_TARGET: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_RECORDS_ILLEGAL_BYTECODE_TARGET", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // JVM Modules
    val JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE: KtDiagnosticFactory2<String, String> = KtDiagnosticFactory2("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // JVM Default
    val JVM_DEFAULT_WITHOUT_COMPATIBILITY_NOT_IN_ENABLE_MODE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_DEFAULT_WITHOUT_COMPATIBILITY_NOT_IN_ENABLE_MODE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class, getRendererFactory())
    val JVM_DEFAULT_WITH_COMPATIBILITY_NOT_IN_NO_COMPATIBILITY_MODE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_DEFAULT_WITH_COMPATIBILITY_NOT_IN_NO_COMPATIBILITY_MODE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())

    // External Declaration
    val EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT", ERROR, SourceElementPositioningStrategies.ABSTRACT_MODIFIER, KtDeclaration::class, getRendererFactory())
    val EXTERNAL_DECLARATION_CANNOT_HAVE_BODY: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTERNAL_DECLARATION_CANNOT_HAVE_BODY", ERROR, SourceElementPositioningStrategies.EXTERNAL_MODIFIER, KtDeclaration::class, getRendererFactory())
    val EXTERNAL_DECLARATION_IN_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTERNAL_DECLARATION_IN_INTERFACE", ERROR, SourceElementPositioningStrategies.EXTERNAL_MODIFIER, KtDeclaration::class, getRendererFactory())
    val EXTERNAL_DECLARATION_CANNOT_BE_INLINED: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTERNAL_DECLARATION_CANNOT_BE_INLINED", ERROR, SourceElementPositioningStrategies.EXTERNAL_MODIFIER, KtDeclaration::class, getRendererFactory())

    // Repeatable Annotations
    val NON_SOURCE_REPEATED_ANNOTATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_SOURCE_REPEATED_ANNOTATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val REPEATED_ANNOTATION_WITH_CONTAINER: KtDiagnosticFactory2<ClassId, ClassId> = KtDiagnosticFactory2("REPEATED_ANNOTATION_WITH_CONTAINER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY_ERROR: KtDiagnosticFactory2<ClassId, ClassId> = KtDiagnosticFactory2("REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER_ERROR: KtDiagnosticFactory2<ClassId, Name> = KtDiagnosticFactory2("REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION_ERROR: KtDiagnosticFactory4<ClassId, String, ClassId, String> = KtDiagnosticFactory4("REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET_ERROR: KtDiagnosticFactory2<ClassId, ClassId> = KtDiagnosticFactory2("REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())

    // Suspension Point
    val SUSPENSION_POINT_INSIDE_CRITICAL_SECTION: KtDiagnosticFactory1<FirCallableSymbol<*>> = KtDiagnosticFactory1("SUSPENSION_POINT_INSIDE_CRITICAL_SECTION", ERROR, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED, PsiElement::class, getRendererFactory())

    // Inline
    val INLINE_FROM_HIGHER_PLATFORM: KtDiagnosticFactory2<String, String> = KtDiagnosticFactory2("INLINE_FROM_HIGHER_PLATFORM", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Misc
    val INAPPLICABLE_JVM_FIELD: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INAPPLICABLE_JVM_FIELD", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val INAPPLICABLE_JVM_FIELD_WARNING: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INAPPLICABLE_JVM_FIELD_WARNING", WARNING, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class, getRendererFactory())
    val SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE: KtDiagnosticFactoryForDeprecation1<ConeKotlinType> = KtDiagnosticFactoryForDeprecation1("SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE", ProhibitSynchronizationByValueClassesAndPrimitives, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JVM_SYNTHETIC_ON_DELEGATE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JVM_SYNTHETIC_ON_DELEGATE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnnotationEntry::class, getRendererFactory())
    val SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_ERROR: KtDiagnosticFactory0 = KtDiagnosticFactory0("SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL_ERROR", ERROR, SourceElementPositioningStrategies.SPREAD_OPERATOR, PsiElement::class, getRendererFactory())
    val JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE: KtDiagnosticFactory0 = KtDiagnosticFactory0("JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NO_REFLECTION_IN_CLASS_PATH: KtDiagnosticFactory0 = KtDiagnosticFactory0("NO_REFLECTION_IN_CLASS_PATH", WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN: KtDiagnosticFactory2<FirNamedFunctionSymbol, Name> = KtDiagnosticFactory2("SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN", WARNING, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY: KtDiagnosticFactory1<FirPropertySymbol> = KtDiagnosticFactory1("JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val MISSING_BUILT_IN_DECLARATION: KtDiagnosticFactory1<FirBasedSymbol<*>> = KtDiagnosticFactory1("MISSING_BUILT_IN_DECLARATION", ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val DANGEROUS_CHARACTERS: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("DANGEROUS_CHARACTERS", WARNING, SourceElementPositioningStrategies.NAME_IDENTIFIER, KtNamedDeclaration::class, getRendererFactory())

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = FirJvmErrorsDefaultMessages
}
