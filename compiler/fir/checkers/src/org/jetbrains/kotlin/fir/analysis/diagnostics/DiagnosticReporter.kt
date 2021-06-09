/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector

abstract class DiagnosticReporter {
    abstract fun report(diagnostic: FirDiagnostic?, context: CheckerContext)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun DiagnosticReporter.reportOn(
    source: FirSourceElement?,
    factory: FirDiagnosticFactory0,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    source?.let { report(factory.on(it, positioningStrategy), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any> DiagnosticReporter.reportOn(
    source: FirSourceElement?,
    factory: FirDiagnosticFactory1<A>,
    a: A,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    source?.let { report(factory.on(it, a, positioningStrategy), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any, B : Any> DiagnosticReporter.reportOn(
    source: FirSourceElement?,
    factory: FirDiagnosticFactory2<A, B>,
    a: A,
    b: B,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    source?.let { report(factory.on(it, a, b, positioningStrategy), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any, B : Any, C : Any> DiagnosticReporter.reportOn(
    source: FirSourceElement?,
    factory: FirDiagnosticFactory3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    source?.let { report(factory.on(it, a, b, c, positioningStrategy), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any, B : Any, C : Any, D : Any> DiagnosticReporter.reportOn(
    source: FirSourceElement?,
    factory: FirDiagnosticFactory4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    source?.let { report(factory.on(it, a, b, c, d, positioningStrategy), context) }
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

fun DiagnosticReporter.reportOnWithSuppression(
    element: FirElement,
    factory: FirDiagnosticFactory0,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, it, positioningStrategy)
    }
}

fun <A : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirElement,
    factory: FirDiagnosticFactory1<A>,
    a: A,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, a, it, positioningStrategy)
    }
}

fun <A : Any, B : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirElement,
    factory: FirDiagnosticFactory2<A, B>,
    a: A,
    b: B,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, a, b, it, positioningStrategy)
    }
}

fun <A : Any, B : Any, C : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirElement,
    factory: FirDiagnosticFactory3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, a, b, c, it, positioningStrategy)
    }
}

fun <A : Any, B : Any, C : Any, D : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirElement,
    factory: FirDiagnosticFactory4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, a, b, c, d, it, positioningStrategy)
    }
}

