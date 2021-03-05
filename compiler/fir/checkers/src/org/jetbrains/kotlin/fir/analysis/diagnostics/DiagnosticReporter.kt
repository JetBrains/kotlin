/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector

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

inline fun withSuppressedDiagnostics(
    element: FirElement,
    context: CheckerContext,
    f: (CheckerContext) -> Unit
) {
    val arguments = (element as? FirAnnotationContainer)?.let { AbstractDiagnosticCollector.getDiagnosticsSuppressedForContainer(it) }
    if (arguments != null) {
        f(
            context.addSuppressedDiagnostics(
                arguments,
                allInfosSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_INFOS in arguments,
                allWarningsSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_WARNINGS in arguments,
                allErrorsSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_ERRORS in arguments
            )
        )
        return
    }
    f(context)
}

inline fun <reified T : FirSourceElement, P : PsiElement> DiagnosticReporter.reportOnWithSuppression(
    element: FirElement,
    factory: FirDiagnosticFactory0<T, P>,
    context: CheckerContext
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source as? T, factory, it)
    }
}

inline fun <reified T : FirSourceElement, P : PsiElement, A : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirElement,
    factory: FirDiagnosticFactory1<T, P, A>,
    a: A,
    context: CheckerContext
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source as? T, factory, a, it)
    }
}

inline fun <reified T : FirSourceElement, P : PsiElement, A : Any, B : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirElement,
    factory: FirDiagnosticFactory2<T, P, A, B>,
    a: A,
    b: B,
    context: CheckerContext
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source as? T, factory, a, b, it)
    }
}

inline fun <reified T : FirSourceElement, P : PsiElement, A : Any, B : Any, C : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirElement,
    factory: FirDiagnosticFactory3<T, P, A, B, C>,
    a: A,
    b: B,
    c: C,
    context: CheckerContext
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source as? T, factory, a, b, c, it)
    }
}

