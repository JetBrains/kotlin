/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DeprecatedCallableAddReplaceWith", "unused")

package org.jetbrains.kotlin.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature

private const val message = "PSI element is no longer required for defining diagnostic factories. Use an overload without type parameter instead."

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedWarning0")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement> warning0(
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DiagnosticFactory0DelegateProvider {
    return DiagnosticFactory0DelegateProvider(WARNING, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedStrongWarning0")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement> strongWarning0(
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DiagnosticFactory0DelegateProvider {
    return DiagnosticFactory0DelegateProvider(STRONG_WARNING, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedStrongWarning1")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A> strongWarning1(
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DiagnosticFactory1DelegateProvider<A> {
    return DiagnosticFactory1DelegateProvider(STRONG_WARNING, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedWarning1")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A> warning1(
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DiagnosticFactory1DelegateProvider<A> {
    return DiagnosticFactory1DelegateProvider(WARNING, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedStrongWarning2")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A, B> strongWarning2(
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DiagnosticFactory2DelegateProvider<A, B> {
    return DiagnosticFactory2DelegateProvider(STRONG_WARNING, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedWarning2")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A, B> warning2(
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DiagnosticFactory2DelegateProvider<A, B> {
    return DiagnosticFactory2DelegateProvider(WARNING, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedWarning3")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A, B, C> warning3(
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DiagnosticFactory3DelegateProvider<A, B, C> {
    return DiagnosticFactory3DelegateProvider(WARNING, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedWarning4")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A, B, C, D> warning4(
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DiagnosticFactory4DelegateProvider<A, B, C, D> {
    return DiagnosticFactory4DelegateProvider(WARNING, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedError0")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement> error0(
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DiagnosticFactory0DelegateProvider {
    return DiagnosticFactory0DelegateProvider(ERROR, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedError1")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A> error1(
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DiagnosticFactory1DelegateProvider<A> {
    return DiagnosticFactory1DelegateProvider(ERROR, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedError2")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A, B> error2(
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DiagnosticFactory2DelegateProvider<A, B> {
    return DiagnosticFactory2DelegateProvider(ERROR, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedError3")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A, B, C> error3(
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DiagnosticFactory3DelegateProvider<A, B, C> {
    return DiagnosticFactory3DelegateProvider(ERROR, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedError4")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A, B, C, D> error4(
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DiagnosticFactory4DelegateProvider<A, B, C, D> {
    return DiagnosticFactory4DelegateProvider(ERROR, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedDeprecationError0")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement> deprecationError0(
    featureForError: LanguageFeature,
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DeprecationDiagnosticFactory0DelegateProvider {
    return DeprecationDiagnosticFactory0DelegateProvider(featureForError, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedDeprecationError1")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A> deprecationError1(
    featureForError: LanguageFeature,
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DeprecationDiagnosticFactory1DelegateProvider<A> {
    return DeprecationDiagnosticFactory1DelegateProvider(featureForError, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedDeprecationError2")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A, B> deprecationError2(
    featureForError: LanguageFeature,
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DeprecationDiagnosticFactory2DelegateProvider<A, B> {
    return DeprecationDiagnosticFactory2DelegateProvider(featureForError, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedDeprecationError3")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A, B, C> deprecationError3(
    featureForError: LanguageFeature,
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DeprecationDiagnosticFactory3DelegateProvider<A, B, C> {
    return DeprecationDiagnosticFactory3DelegateProvider(featureForError, positioningStrategyKind, container)
}

@Deprecated(message, level = DeprecationLevel.WARNING)
@JvmName("deprecatedDeprecationError4")
context(container: KtDiagnosticsContainer)
inline fun <reified P : PsiElement, A, B, C, D> deprecationError4(
    featureForError: LanguageFeature,
    positioningStrategyKind: PositioningStrategyKind = PositioningStrategyKinds.DEFAULT
): DeprecationDiagnosticFactory4DelegateProvider<A, B, C, D> {
    return DeprecationDiagnosticFactory4DelegateProvider(featureForError, positioningStrategyKind, container)
}
