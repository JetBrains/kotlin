/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.languageVersionSettings

abstract class DiagnosticReporter {
    abstract fun report(diagnostic: FirDiagnostic?, context: CheckerContext)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: FirDiagnosticFactory0,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    source?.let { report(factory.on(it, positioningStrategy), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any> DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: FirDiagnosticFactory1<A>,
    a: A,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    source?.let { report(factory.on(it, a, positioningStrategy), context) }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any, B : Any> DiagnosticReporter.reportOn(
    source: KtSourceElement?,
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
    source: KtSourceElement?,
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
    source: KtSourceElement?,
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

@OptIn(InternalDiagnosticFactoryMethod::class)
fun DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: FirDiagnosticFactoryForDeprecation0,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), context, positioningStrategy)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any> DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: FirDiagnosticFactoryForDeprecation1<A>,
    a: A,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, context, positioningStrategy)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any, B : Any> DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: FirDiagnosticFactoryForDeprecation2<A, B>,
    a: A,
    b: B,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, context, positioningStrategy)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any, B : Any, C : Any> DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: FirDiagnosticFactoryForDeprecation3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, c, context, positioningStrategy)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
fun <A : Any, B : Any, C : Any, D : Any> DiagnosticReporter.reportOn(
    source: KtSourceElement?,
    factory: FirDiagnosticFactoryForDeprecation4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, c, d, context, positioningStrategy)
}

fun <F : AbstractFirDiagnosticFactory> FirDiagnosticFactoryForDeprecation<F>.chooseFactory(context: CheckerContext): F {
    return if (context.session.languageVersionSettings.supportsFeature(deprecatingFeature)) {
        errorFactory
    } else {
        warningFactory
    }
}

fun DiagnosticReporter.reportOnWithSuppression(
    element: FirAnnotationContainer,
    factory: FirDiagnosticFactory0,
    context: CheckerContext,
    positioningStrategy: SourceElementPositioningStrategy? = null
) {
    withSuppressedDiagnostics(element, context) {
        reportOn(element.source, factory, it, positioningStrategy)
    }
}

fun <A : Any> DiagnosticReporter.reportOnWithSuppression(
    element: FirAnnotationContainer,
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
    element: FirAnnotationContainer,
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
    element: FirAnnotationContainer,
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
    element: FirAnnotationContainer,
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

inline fun withSuppressedDiagnostics(
    annotationContainer: FirAnnotationContainer,
    context: CheckerContext,
    f: (CheckerContext) -> Unit
) {
    val arguments = AbstractDiagnosticCollector.getDiagnosticsSuppressedForContainer(annotationContainer)
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

