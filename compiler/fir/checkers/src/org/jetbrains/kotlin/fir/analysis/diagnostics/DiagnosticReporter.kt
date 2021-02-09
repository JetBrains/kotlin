/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext

abstract class DiagnosticReporter {
    abstract fun report(diagnostic: FirDiagnostic<*>?, context: CheckerContext)
}

inline fun <reified T : FirSourceElement, P : PsiElement> DiagnosticReporter.reportOn(
    source: T?,
    factory: FirDiagnosticFactory0<T, P>,
    context: CheckerContext
) {
    source?.let { report(factory.on(it), context) }
}

inline fun <reified T : FirSourceElement, P : PsiElement, A : Any> DiagnosticReporter.reportOn(
    source: T?,
    factory: FirDiagnosticFactory1<T, P, A>,
    a: A,
    context: CheckerContext
) {
    source?.let { report(factory.on(it, a), context) }
}

inline fun <reified T : FirSourceElement, P : PsiElement, A : Any, B : Any> DiagnosticReporter.reportOn(
    source: T?,
    factory: FirDiagnosticFactory2<T, P, A, B>,
    a: A,
    b: B,
    context: CheckerContext
) {
    source?.let { report(factory.on(it, a, b), context) }
}

inline fun <reified T : FirSourceElement, P : PsiElement, A : Any, B : Any, C : Any> DiagnosticReporter.reportOn(
    source: T?,
    factory: FirDiagnosticFactory3<T, P, A, B, C>,
    a: A,
    b: B,
    c: C,
    context: CheckerContext
) {
    source?.let { report(factory.on(it, a, b, c), context) }
}

