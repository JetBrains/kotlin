/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.jvm

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.FqName
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

    // Super
    val SUPER_CALL_WITH_DEFAULT_PARAMETERS by error1<PsiElement, String>()

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
    val JVM_DEFAULT_THROUGH_INHERITANCE by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL by error0<PsiElement>()
    val NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT by warning0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)

    // External Declaration
    val EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT by error0<KtDeclaration>(SourceElementPositioningStrategies.ABSTRACT_MODIFIER)
    val EXTERNAL_DECLARATION_CANNOT_HAVE_BODY by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val EXTERNAL_DECLARATION_IN_INTERFACE by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val EXTERNAL_DECLARATION_CANNOT_BE_INLINED by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)

    // Misc
    val INAPPLICABLE_JVM_FIELD by error1<KtAnnotationEntry, String>()
    val INAPPLICABLE_JVM_FIELD_WARNING by warning1<KtAnnotationEntry, String>()
    val JVM_SYNTHETIC_ON_DELEGATE by error0<KtAnnotationEntry>()

}
