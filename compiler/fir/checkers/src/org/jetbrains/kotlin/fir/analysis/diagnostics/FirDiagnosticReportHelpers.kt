/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector

@OptIn(InternalDiagnosticFactoryMethod::class)
fun DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: KtDiagnosticFactory0,
    context: DiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    source?.let { report(factory.on(it, positioningStrategy), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any> DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: KtDiagnosticFactory1<A>,
    a: A,
    context: DiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    source?.let { report(factory.on(it, a, positioningStrategy), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any, B : Any> DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: KtDiagnosticFactory2<A, B>,
    a: A,
    b: B,
    context: DiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    source?.let { report(factory.on(it, a, b, positioningStrategy), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any, B : Any, C : Any> DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: KtDiagnosticFactory3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: DiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    source?.let { report(factory.on(it, a, b, c, positioningStrategy), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any, B : Any, C : Any, D : Any> DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: KtDiagnosticFactory4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    context: DiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    source?.let { report(factory.on(it, a, b, c, d, positioningStrategy), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation0,
    context: DiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), context, positioningStrategy)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any> DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation1<A>,
    a: A,
    context: DiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, context, positioningStrategy)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any, B : Any> DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation2<A, B>,
    a: A,
    b: B,
    context: DiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, context, positioningStrategy)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any, B : Any, C : Any> DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: DiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, c, context, positioningStrategy)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any, B : Any, C : Any, D : Any> DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    context: DiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, c, d, context, positioningStrategy)
}

fun <F : AbstractKtDiagnosticFactory> KtDiagnosticFactoryForDeprecation<F>.chooseFactory(context: DiagnosticContext): F {
    return if (context.languageVersionSettings.supportsFeature(deprecatingFeature)) {
        errorFactory
    } else {
        warningFactory
    }
}

fun DiagnosticReporter.reportOnWithSuppression(
    element: FirAnnotationContainer,
    factory: KtDiagnosticFactory0,
    context: DiagnosticContext,
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
    context: DiagnosticContext,
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
    context: DiagnosticContext,
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
    context: DiagnosticContext,
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
    context: DiagnosticContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, a, b, c, d, it, positioningStrategy)
    }
}

inline fun <reified C : DiagnosticContext> withSuppressedDiagnostics(
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

