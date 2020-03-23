/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.psi

class FirDiagnostic(
    val diagnostic: Diagnostic,
    val source: FirSourceElement
)

inline fun <reified E : PsiElement> DiagnosticFactory0<E>.onSource(source: FirSourceElement): FirDiagnostic? {
    val psi = source.psi as? E
        ?: throw IllegalArgumentException("Incompatible PSI: expected ${E::class}, actual ${source.psi?.let { it::class }}")
    return FirDiagnostic(this.on(psi), source)
}

inline fun <reified E : PsiElement, A> DiagnosticFactory1<E, A>.onSource(source: FirSourceElement, a: A): FirDiagnostic? {
    val psi = source.psi as? E
        ?: throw IllegalArgumentException("Incompatible PSI: expected ${E::class}, actual ${source.psi?.let { it::class }}")
    return FirDiagnostic(this.on(psi, a), source)
}

inline fun <reified E : PsiElement, A, B> DiagnosticFactory2<E, A, B>.onSource(source: FirSourceElement, a: A, b: B): FirDiagnostic? {
    val psi = source.psi as? E
        ?: throw IllegalArgumentException("Incompatible PSI: expected ${E::class}, actual ${source.psi?.let { it::class }}")
    return FirDiagnostic(this.on(psi, a, b), source)
}

inline fun <reified E : PsiElement, A, B, C> DiagnosticFactory3<E, A, B, C>.onSource(source: FirSourceElement, a: A, b: B, c: C): FirDiagnostic? {
    val psi = source.psi as? E
        ?: throw IllegalArgumentException("Incompatible PSI: expected ${E::class}, actual ${source.psi?.let { it::class }}")
    return FirDiagnostic(this.on(psi, a, b, c), source)
}

inline fun <reified E : PsiElement, reified A> DiagnosticFactory1<E, A>.tryOnSource(
    source: FirSourceElement,
    a: Any?
): FirDiagnostic? {
    val aa = a as? A ?: throw IllegalArgumentException("Parameter passed to the factory is of incompatible type!")
    return onSource(source, aa)
}