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

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <P : PsiElement> DiagnosticReporter.reportOn(
    source: FirSourceElement?,
    factory: FirDiagnosticFactory0<P>,
    context: CheckerContext
) {
    source?.let { report(factory.on(it), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <P : PsiElement, A : Any> DiagnosticReporter.reportOn(
    source: FirSourceElement?,
    factory: FirDiagnosticFactory1<P, A>,
    a: A,
    context: CheckerContext
) {
    source?.let { report(factory.on(it, a), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <P : PsiElement, A : Any, B : Any> DiagnosticReporter.reportOn(
    source: FirSourceElement?,
    factory: FirDiagnosticFactory2<P, A, B>,
    a: A,
    b: B,
    context: CheckerContext
) {
    source?.let { report(factory.on(it, a, b), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <P : PsiElement, A : Any, B : Any, C : Any> DiagnosticReporter.reportOn(
    source: FirSourceElement?,
    factory: FirDiagnosticFactory3<P, A, B, C>,
    a: A,
    b: B,
    c: C,
    context: CheckerContext
) {
    source?.let { report(factory.on(it, a, b, c), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <P : PsiElement, A : Any, B : Any, C : Any, D : Any> DiagnosticReporter.reportOn(
    source: FirSourceElement?,
    factory: FirDiagnosticFactory4<P, A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    context: CheckerContext
) {
    source?.let { report(factory.on(it, a, b, c, d), context) }
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

fun <P : PsiElement> DiagnosticReporter.reportOnWithSuppression(
    element: FirElement,
    factory: FirDiagnosticFactory0<P>,
    context: CheckerContext
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, it)
    }
}

fun <P : PsiElement, A : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirElement,
    factory: FirDiagnosticFactory1<P, A>,
    a: A,
    context: CheckerContext
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, a, it)
    }
}

fun <P : PsiElement, A : Any, B : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirElement,
    factory: FirDiagnosticFactory2<P, A, B>,
    a: A,
    b: B,
    context: CheckerContext
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, a, b, it)
    }
}

fun <P : PsiElement, A : Any, B : Any, C : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirElement,
    factory: FirDiagnosticFactory3<P, A, B, C>,
    a: A,
    b: B,
    c: C,
    context: CheckerContext
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, a, b, c, it)
    }
}

fun <P : PsiElement, A : Any, B : Any, C : Any, D : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirElement,
    factory: FirDiagnosticFactory4<P, A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    context: CheckerContext
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, a, b, c, d, it)
    }
}

