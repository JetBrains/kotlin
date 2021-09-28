/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector

fun DiagnosticReporter.reportOnWithSuppression(
    element: FirAnnotationContainer,
    factory: KtDiagnosticFactory0,
    context: MutableDiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, it, positioningStrategy)
    }
}

fun <A : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirAnnotationContainer,
    factory: KtDiagnosticFactory1<A>,
    a: A,
    context: MutableDiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, a, it, positioningStrategy)
    }
}

fun <A : Any, B : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirAnnotationContainer,
    factory: KtDiagnosticFactory2<A, B>,
    a: A,
    b: B,
    context: MutableDiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, a, b, it, positioningStrategy)
    }
}

fun <A : Any, B : Any, C : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirAnnotationContainer,
    factory: KtDiagnosticFactory3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: MutableDiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, a, b, c, it, positioningStrategy)
    }
}

fun <A : Any, B : Any, C : Any, D : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirAnnotationContainer,
    factory: KtDiagnosticFactory4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    context: MutableDiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, a, b, c, d, it, positioningStrategy)
    }
}

inline fun <reified C : MutableDiagnosticContext> withSuppressedDiagnostics(
    annotationContainer: FirAnnotationContainer,
    context: C,
    f: (C) -> Unit
) {
    val arguments = AbstractDiagnosticCollector.getDiagnosticsSuppressedForContainer(annotationContainer)
    if (arguments != null) {
        f(
            context.addSuppressedDiagnostics(
                arguments,
                allInfosSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_INFOS in arguments,
                allWarningsSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_WARNINGS in arguments,
                allErrorsSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_ERRORS in arguments
            ) as C
        )
        return
    }
    f(context)
}

