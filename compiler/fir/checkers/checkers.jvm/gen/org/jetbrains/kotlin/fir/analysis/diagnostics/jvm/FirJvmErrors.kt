/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.jvm

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtExpression

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

object FirJvmErrors {
    // Declarations
    val CONFLICTING_JVM_DECLARATIONS by error0<PsiElement>()

    // Types
    val JAVA_TYPE_MISMATCH by error2<KtExpression, ConeKotlinType, ConeKotlinType>()

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

}
