/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalDiagnosticFactoryMethod::class)

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation

// #### KtSourcelessFactory ####

context(context: DiagnosticContext)
fun DiagnosticReporter.report(
    factory: KtSourcelessDiagnosticFactory,
    message: String,
    location: CompilerMessageSourceLocation? = null,
) {
    report(factory.create(message, location, context), context)
}

// #### KtDiagnosticFactory0 ####

fun DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory0,
    context: DiagnosticContext,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    report(factory.onOrFallback(source, positioningStrategyKind, context), context)
}

context(context: DiagnosticContext)
fun DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory0,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    report(factory.onOrFallback(source, positioningStrategyKind, context), context)
}

// #### KtDiagnosticFactory1 ####

fun <A> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory1<A>,
    a: A,
    context: DiagnosticContext,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    report(factory.onOrFallback(source, a, positioningStrategyKind, context), context)
}

context(context: DiagnosticContext)
fun <A> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory1<A>,
    a: A,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    report(factory.onOrFallback(source, a, positioningStrategyKind, context), context)
}

// #### KtDiagnosticFactory2 ####

fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory2<A, B>,
    a: A,
    b: B,
    context: DiagnosticContext,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    report(factory.onOrFallback(source, a, b, positioningStrategyKind, context), context)
}

context(context: DiagnosticContext)
fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory2<A, B>,
    a: A,
    b: B,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    report(factory.onOrFallback(source, a, b, positioningStrategyKind, context), context)
}

// #### KtDiagnosticFactory3 ####

fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: DiagnosticContext,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    report(factory.onOrFallback(source, a, b, c, positioningStrategyKind, context), context)
}

context(context: DiagnosticContext)
fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory3<A, B, C>,
    a: A,
    b: B,
    c: C,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    report(factory.onOrFallback(source, a, b, c, positioningStrategyKind, context), context)
}

// #### KtDiagnosticFactory4 ####

fun <A, B, C, D> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    context: DiagnosticContext,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    report(factory.onOrFallback(source, a, b, c, d, positioningStrategyKind, context), context)
}

context(context: DiagnosticContext)
fun <A, B, C, D> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    report(factory.onOrFallback(source, a, b, c, d, positioningStrategyKind, context), context)
}

// #### KtDiagnosticFactory5 ####

fun <A, B, C, D, E> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory5<A, B, C, D, E>,
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    context: DiagnosticContext,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    report(factory.onOrFallback(source, a, b, c, d, e, positioningStrategyKind, context), context)
}

context(context: DiagnosticContext)
fun <A, B, C, D, E> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory5<A, B, C, D, E>,
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    report(factory.onOrFallback(source, a, b, c, d, e, positioningStrategyKind, context), context)
}

// #### KtDiagnosticFactoryForDeprecation0 ####

fun DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation0,
    context: DiagnosticContext,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    with(context) { reportOn(source, factory.chooseFactory(), positioningStrategyKind) }
}

context(context: DiagnosticContext)
fun DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation0,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    reportOn(source, factory.chooseFactory(), positioningStrategyKind)
}

// #### KtDiagnosticFactoryForDeprecation1 ####

fun <A> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation1<A>,
    a: A,
    context: DiagnosticContext,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    with(context) { reportOn(source, factory.chooseFactory(), a, positioningStrategyKind) }
}

context(context: DiagnosticContext)
fun <A> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation1<A>,
    a: A,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    reportOn(source, factory.chooseFactory(), a, positioningStrategyKind)
}

// #### KtDiagnosticFactoryForDeprecation2 ####

fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation2<A, B>,
    a: A,
    b: B,
    context: DiagnosticContext,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    with(context) { reportOn(source, factory.chooseFactory(), a, b, positioningStrategyKind) }
}

context(context: DiagnosticContext)
fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation2<A, B>,
    a: A,
    b: B,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    reportOn(source, factory.chooseFactory(), a, b, positioningStrategyKind)
}

// #### KtDiagnosticFactoryForDeprecation3 ####

fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: DiagnosticContext,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    with(context) { reportOn(source, factory.chooseFactory(), a, b, c, positioningStrategyKind) }
}

context(context: DiagnosticContext)
fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation3<A, B, C>,
    a: A,
    b: B,
    c: C,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    reportOn(source, factory.chooseFactory(), a, b, c, positioningStrategyKind)
}

// #### KtDiagnosticFactoryForDeprecation4 ####

fun <A, B, C, D> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    context: DiagnosticContext,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    with(context) { reportOn(source, factory.chooseFactory(), a, b, c, d, positioningStrategyKind) }
}

context(context: DiagnosticContext)
fun <A, B, C, D> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    reportOn(source, factory.chooseFactory(), a, b, c, d, positioningStrategyKind)
}

// #### KtDiagnosticFactoryForDeprecation5 ####

fun <A, B, C, D, E> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation5<A, B, C, D, E>,
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    context: DiagnosticContext,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    with(context) { reportOn(source, factory.chooseFactory(), a, b, c, d, e, positioningStrategyKind) }
}

context(context: DiagnosticContext)
fun <A, B, C, D, E> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation5<A, B, C, D, E>,
    a: A,
    b: B,
    c: C,
    d: D,
    e: E,
    positioningStrategyKind: PositioningStrategyKind? = null
) {
    reportOn(source, factory.chooseFactory(), a, b, c, d, e, positioningStrategyKind)
}

context(context: DiagnosticContext)
fun <F : KtDiagnosticFactoryN> KtDiagnosticFactoryForDeprecation<F>.chooseFactory(): F {
    return if (context.languageVersionSettings.supportsFeature(deprecatingFeature)) {
        errorFactory
    } else {
        warningFactory
    }
}
