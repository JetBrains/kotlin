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
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
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
object FirJvmErrors {
    // Declarations
    val OVERRIDE_CANNOT_BE_STATIC: KtDiagnosticFactory0 by error0<PsiElement>()
    val JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val JVM_STATIC_ON_NON_PUBLIC_MEMBER: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val JVM_STATIC_ON_CONST_OR_JVM_FIELD: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val JVM_STATIC_ON_EXTERNAL_IN_INTERFACE: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val INAPPLICABLE_JVM_NAME: KtDiagnosticFactory0 by error0<PsiElement>()
    val ILLEGAL_JVM_NAME: KtDiagnosticFactory0 by error0<PsiElement>()
    val FUNCTION_DELEGATE_MEMBER_NAME_CLASH: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION: KtDiagnosticFactory0 by error0<PsiElement>()
    val JVM_INLINE_WITHOUT_VALUE_CLASS: KtDiagnosticFactory0 by error0<PsiElement>()
    val WRONG_NULLABILITY_FOR_JAVA_OVERRIDE: KtDiagnosticFactory2<FirCallableSymbol<*>, FirCallableSymbol<*>> by warning2<PsiElement, FirCallableSymbol<*>, FirCallableSymbol<*>>(SourceElementPositioningStrategies.OVERRIDE_MODIFIER)
    val ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE: KtDiagnosticFactory3<FirNamedFunctionSymbol, String, FirNamedFunctionSymbol> by error3<KtNamedFunction, FirNamedFunctionSymbol, String, FirNamedFunctionSymbol>(SourceElementPositioningStrategies.DECLARATION_NAME)

    // Types
    val JAVA_TYPE_MISMATCH: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by error2<KtExpression, ConeKotlinType, ConeKotlinType>()
    val RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, String> by warning3<PsiElement, ConeKotlinType, ConeKotlinType, String>()
    val NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS: KtDiagnosticFactory3<ConeKotlinType, ConeKotlinType, String> by warning3<PsiElement, ConeKotlinType, ConeKotlinType, String>()

    // Type parameters
    val UPPER_BOUND_CANNOT_BE_ARRAY: KtDiagnosticFactory0 by error0<PsiElement>()
    val UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by warning2<PsiElement, ConeKotlinType, ConeKotlinType>()
    val UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_BASED_ON_JAVA_ANNOTATIONS: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by warning2<PsiElement, ConeKotlinType, ConeKotlinType>()

    // annotations
    val STRICTFP_ON_CLASS: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val SYNCHRONIZED_ON_ABSTRACT: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val SYNCHRONIZED_IN_INTERFACE: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val SYNCHRONIZED_ON_INLINE: KtDiagnosticFactory0 by warning0<KtAnnotationEntry>()
    val SYNCHRONIZED_ON_SUSPEND: KtDiagnosticFactoryForDeprecation0 by deprecationError0<KtAnnotationEntry>(SynchronizedSuspendError)
    val OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS: KtDiagnosticFactory0 by warning0<KtAnnotationEntry>()
    val OVERLOADS_ABSTRACT: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val OVERLOADS_INTERFACE: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val OVERLOADS_LOCAL: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR: KtDiagnosticFactoryForDeprecation0 by deprecationError0<KtAnnotationEntry>(ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses)
    val OVERLOADS_PRIVATE: KtDiagnosticFactory0 by warning0<KtAnnotationEntry>()
    val DEPRECATED_JAVA_ANNOTATION: KtDiagnosticFactory1<FqName> by warning1<KtAnnotationEntry, FqName>()
    val JVM_PACKAGE_NAME_CANNOT_BE_EMPTY: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val JVM_PACKAGE_NAME_MUST_BE_VALID_NAME: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION: KtDiagnosticFactory0 by error0<KtExpression>()
    val REDUNDANT_REPEATABLE_ANNOTATION: KtDiagnosticFactory2<FqName, FqName> by warning2<KtAnnotationEntry, FqName, FqName>()

    // Super
    val INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)

    // JVM Records
    val LOCAL_JVM_RECORD: KtDiagnosticFactory0 by error0<PsiElement>()
    val NON_FINAL_JVM_RECORD: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.NON_FINAL_MODIFIER_OR_NAME)
    val ENUM_JVM_RECORD: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.ENUM_MODIFIER)
    val JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS: KtDiagnosticFactory0 by error0<PsiElement>()
    val NON_DATA_CLASS_JVM_RECORD: KtDiagnosticFactory0 by error0<PsiElement>()
    val JVM_RECORD_NOT_VAL_PARAMETER: KtDiagnosticFactory0 by error0<PsiElement>()
    val JVM_RECORD_NOT_LAST_VARARG_PARAMETER: KtDiagnosticFactory0 by error0<PsiElement>()
    val INNER_JVM_RECORD: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.INNER_MODIFIER)
    val FIELD_IN_JVM_RECORD: KtDiagnosticFactory0 by error0<PsiElement>()
    val DELEGATION_BY_IN_JVM_RECORD: KtDiagnosticFactory0 by error0<PsiElement>()
    val JVM_RECORD_EXTENDS_CLASS: KtDiagnosticFactory1<ConeKotlinType> by error1<PsiElement, ConeKotlinType>(SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME)
    val ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE: KtDiagnosticFactory0 by error0<PsiElement>()

    // JVM Default
    val JVM_DEFAULT_IN_DECLARATION: KtDiagnosticFactory1<String> by error1<KtElement, String>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val JVM_DEFAULT_WITH_COMPATIBILITY_IN_DECLARATION: KtDiagnosticFactory0 by error0<KtElement>()
    val JVM_DEFAULT_WITH_COMPATIBILITY_NOT_ON_INTERFACE: KtDiagnosticFactory0 by error0<KtElement>()

    // External Declaration
    val EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.ABSTRACT_MODIFIER)
    val EXTERNAL_DECLARATION_CANNOT_HAVE_BODY: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val EXTERNAL_DECLARATION_IN_INTERFACE: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val EXTERNAL_DECLARATION_CANNOT_BE_INLINED: KtDiagnosticFactory0 by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)

    // Repeatable Annotations
    val NON_SOURCE_REPEATED_ANNOTATION: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val REPEATED_ANNOTATION_WITH_CONTAINER: KtDiagnosticFactory2<ClassId, ClassId> by error2<KtAnnotationEntry, ClassId, ClassId>()
    val REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY: KtDiagnosticFactoryForDeprecation2<ClassId, ClassId> by deprecationError2<KtAnnotationEntry, ClassId, ClassId>(RepeatableAnnotationContainerConstraints)
    val REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER: KtDiagnosticFactoryForDeprecation2<ClassId, Name> by deprecationError2<KtAnnotationEntry, ClassId, Name>(RepeatableAnnotationContainerConstraints)
    val REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION: KtDiagnosticFactoryForDeprecation4<ClassId, String, ClassId, String> by deprecationError4<KtAnnotationEntry, ClassId, String, ClassId, String>(RepeatableAnnotationContainerConstraints)
    val REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET: KtDiagnosticFactoryForDeprecation2<ClassId, ClassId> by deprecationError2<KtAnnotationEntry, ClassId, ClassId>(RepeatableAnnotationContainerConstraints)
    val REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER: KtDiagnosticFactoryForDeprecation0 by deprecationError0<KtAnnotationEntry>(RepeatableAnnotationContainerConstraints)

    // Suspension Point
    val SUSPENSION_POINT_INSIDE_CRITICAL_SECTION: KtDiagnosticFactory1<FirCallableSymbol<*>> by error1<PsiElement, FirCallableSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)

    // Misc
    val INAPPLICABLE_JVM_FIELD: KtDiagnosticFactory1<String> by error1<KtAnnotationEntry, String>()
    val INAPPLICABLE_JVM_FIELD_WARNING: KtDiagnosticFactory1<String> by warning1<KtAnnotationEntry, String>()
    val JVM_SYNTHETIC_ON_DELEGATE: KtDiagnosticFactory0 by error0<KtAnnotationEntry>()
    val SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC: KtDiagnosticFactory0 by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val CONCURRENT_HASH_MAP_CONTAINS_OPERATOR: KtDiagnosticFactoryForDeprecation0 by deprecationError0<PsiElement>(ProhibitConcurrentHashMapContains)
    val SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL: KtDiagnosticFactoryForDeprecation0 by deprecationError0<PsiElement>(ProhibitSpreadOnSignaturePolymorphicCall, SourceElementPositioningStrategies.SPREAD_OPERATOR)
    val JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE: KtDiagnosticFactory0 by error0<PsiElement>()
    val NO_REFLECTION_IN_CLASS_PATH: KtDiagnosticFactory0 by warning0<PsiElement>()
    val SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN: KtDiagnosticFactory2<FirNamedFunctionSymbol, Name> by warning2<PsiElement, FirNamedFunctionSymbol, Name>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)

    init {
        RootDiagnosticRendererFactory.registerFactory(FirJvmErrorsDefaultMessages)
    }
}
