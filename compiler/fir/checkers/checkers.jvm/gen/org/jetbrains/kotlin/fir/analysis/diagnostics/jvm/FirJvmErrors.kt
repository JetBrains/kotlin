/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.jvm

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature.DefaultMethodsCallFromJava6TargetError
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitConcurrentHashMapContains
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitSpreadOnSignaturePolymorphicCall
import org.jetbrains.kotlin.config.LanguageFeature.RepeatableAnnotationContainerConstraints
import org.jetbrains.kotlin.config.LanguageFeature.SynchronizedSuspendError
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

object FirJvmErrors {
    // Declarations
    val CONFLICTING_JVM_DECLARATIONS by error0<PsiElement>()
    val OVERRIDE_CANNOT_BE_STATIC by error0<PsiElement>()
    val JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION by error0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION by error0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val JVM_STATIC_ON_NON_PUBLIC_MEMBER by error0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val JVM_STATIC_ON_CONST_OR_JVM_FIELD by error0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val JVM_STATIC_ON_EXTERNAL_IN_INTERFACE by error0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val INAPPLICABLE_JVM_NAME by error0<PsiElement>()
    val ILLEGAL_JVM_NAME by error0<PsiElement>()
    val FUNCTION_DELEGATE_MEMBER_NAME_CLASH by error0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION by error0<PsiElement>()
    val JVM_INLINE_WITHOUT_VALUE_CLASS by error0<PsiElement>()

    // Types
    val JAVA_TYPE_MISMATCH by error2<KtExpression, ConeKotlinType, ConeKotlinType>()

    // Type parameters
    val UPPER_BOUND_CANNOT_BE_ARRAY by error0<PsiElement>()

    // annotations
    val STRICTFP_ON_CLASS by error0<KtAnnotationEntry>()
    val VOLATILE_ON_VALUE by error0<KtAnnotationEntry>()
    val VOLATILE_ON_DELEGATE by error0<KtAnnotationEntry>()
    val SYNCHRONIZED_ON_ABSTRACT by error0<KtAnnotationEntry>()
    val SYNCHRONIZED_IN_INTERFACE by error0<KtAnnotationEntry>()
    val SYNCHRONIZED_ON_INLINE by warning0<KtAnnotationEntry>()
    val SYNCHRONIZED_ON_SUSPEND by deprecationError0<KtAnnotationEntry>(SynchronizedSuspendError)
    val OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS by warning0<KtAnnotationEntry>()
    val OVERLOADS_ABSTRACT by error0<KtAnnotationEntry>()
    val OVERLOADS_INTERFACE by error0<KtAnnotationEntry>()
    val OVERLOADS_LOCAL by error0<KtAnnotationEntry>()
    val OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR by deprecationError0<KtAnnotationEntry>(ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses)
    val OVERLOADS_PRIVATE by warning0<KtAnnotationEntry>()
    val DEPRECATED_JAVA_ANNOTATION by warning1<KtAnnotationEntry, FqName>()
    val JVM_PACKAGE_NAME_CANNOT_BE_EMPTY by error0<KtAnnotationEntry>()
    val JVM_PACKAGE_NAME_MUST_BE_VALID_NAME by error0<KtAnnotationEntry>()
    val JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES by error0<KtAnnotationEntry>()
    val POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION by error0<KtExpression>()

    // Super
    val SUPER_CALL_WITH_DEFAULT_PARAMETERS by error1<PsiElement, String>()
    val INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)

    // JVM Records
    val LOCAL_JVM_RECORD by error0<PsiElement>()
    val NON_FINAL_JVM_RECORD by error0<PsiElement>(SourceElementPositioningStrategies.NON_FINAL_MODIFIER_OR_NAME)
    val ENUM_JVM_RECORD by error0<PsiElement>(SourceElementPositioningStrategies.ENUM_MODIFIER)
    val JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS by error0<PsiElement>()
    val NON_DATA_CLASS_JVM_RECORD by error0<PsiElement>()
    val JVM_RECORD_NOT_VAL_PARAMETER by error0<PsiElement>()
    val JVM_RECORD_NOT_LAST_VARARG_PARAMETER by error0<PsiElement>()
    val INNER_JVM_RECORD by error0<PsiElement>(SourceElementPositioningStrategies.INNER_MODIFIER)
    val FIELD_IN_JVM_RECORD by error0<PsiElement>()
    val DELEGATION_BY_IN_JVM_RECORD by error0<PsiElement>()
    val JVM_RECORD_EXTENDS_CLASS by error1<PsiElement, ConeKotlinType>(SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME)
    val ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE by error0<PsiElement>()

    // JVM Default
    val JVM_DEFAULT_NOT_IN_INTERFACE by error0<PsiElement>()
    val JVM_DEFAULT_IN_JVM6_TARGET by error1<PsiElement, String>()
    val JVM_DEFAULT_REQUIRED_FOR_OVERRIDE by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val JVM_DEFAULT_IN_DECLARATION by error1<KtElement, String>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val JVM_DEFAULT_WITH_COMPATIBILITY_IN_DECLARATION by error0<KtElement>()
    val JVM_DEFAULT_WITH_COMPATIBILITY_NOT_ON_INTERFACE by error0<KtElement>()
    val NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT by warning0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)

    // External Declaration
    val EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT by error0<KtDeclaration>(SourceElementPositioningStrategies.ABSTRACT_MODIFIER)
    val EXTERNAL_DECLARATION_CANNOT_HAVE_BODY by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val EXTERNAL_DECLARATION_IN_INTERFACE by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val EXTERNAL_DECLARATION_CANNOT_BE_INLINED by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)

    // Repeatable Annotations
    val NON_SOURCE_REPEATED_ANNOTATION by error0<KtAnnotationEntry>()
    val REPEATED_ANNOTATION_TARGET6 by error0<KtAnnotationEntry>()
    val REPEATED_ANNOTATION_WITH_CONTAINER by error2<KtAnnotationEntry, ClassId, ClassId>()
    val REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY by deprecationError2<KtAnnotationEntry, ClassId, ClassId>(RepeatableAnnotationContainerConstraints)
    val REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER by deprecationError2<KtAnnotationEntry, ClassId, Name>(RepeatableAnnotationContainerConstraints)
    val REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION by deprecationError4<KtAnnotationEntry, ClassId, String, ClassId, String>(RepeatableAnnotationContainerConstraints)
    val REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET by deprecationError2<KtAnnotationEntry, ClassId, ClassId>(RepeatableAnnotationContainerConstraints)
    val REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER by deprecationError0<KtAnnotationEntry>(RepeatableAnnotationContainerConstraints)

    // Suspension Point
    val SUSPENSION_POINT_INSIDE_CRITICAL_SECTION by error1<PsiElement, FirCallableSymbol<*>>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)

    // Misc
    val INAPPLICABLE_JVM_FIELD by error1<KtAnnotationEntry, String>()
    val INAPPLICABLE_JVM_FIELD_WARNING by warning1<KtAnnotationEntry, String>()
    val JVM_SYNTHETIC_ON_DELEGATE by error0<KtAnnotationEntry>()
    val DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET by deprecationError0<PsiElement>(DefaultMethodsCallFromJava6TargetError, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET by deprecationError0<PsiElement>(DefaultMethodsCallFromJava6TargetError, SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC by error0<PsiElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val CONCURRENT_HASH_MAP_CONTAINS_OPERATOR by deprecationError0<PsiElement>(ProhibitConcurrentHashMapContains)
    val SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL by deprecationError0<PsiElement>(ProhibitSpreadOnSignaturePolymorphicCall, SourceElementPositioningStrategies.SPREAD_OPERATOR)
    val JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE by error0<PsiElement>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FirJvmErrorsDefaultMessages)
    }
}
