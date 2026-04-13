/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), positioningStrategy, context), context)
}

context(context: DiagnosticContext)
fun DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory0,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), positioningStrategy, context), context)
}

// #### KtDiagnosticFactory1 ####

fun <A> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory1<A>,
    a: A,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, positioningStrategy, context), context)
}

context(context: DiagnosticContext)
fun <A> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory1<A>,
    a: A,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, positioningStrategy, context), context)
}

// #### KtDiagnosticFactory2 ####

fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory2<A, B>,
    a: A,
    b: B,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, positioningStrategy, context), context)
}

context(context: DiagnosticContext)
fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory2<A, B>,
    a: A,
    b: B,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, positioningStrategy, context), context)
}

// #### KtDiagnosticFactory3 ####

fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, c, positioningStrategy, context), context)
}

context(context: DiagnosticContext)
fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory3<A, B, C>,
    a: A,
    b: B,
    c: C,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, c, positioningStrategy, context), context)
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
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, c, d, positioningStrategy, context), context)
}

context(context: DiagnosticContext)
fun <A, B, C, D> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, c, d, positioningStrategy, context), context)
}

fun AbstractKtSourceElement?.requireNotNull(): AbstractKtSourceElement =
    requireNotNull(this) { "source must not be null" }

// #### KtDiagnosticFactoryForDeprecation0 ####

fun DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation0,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    with(context) { reportOn(source, factory.chooseFactory(), positioningStrategy) }
}

context(context: DiagnosticContext)
fun DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation0,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(), positioningStrategy)
}

// #### KtDiagnosticFactoryForDeprecation1 ####

fun <A> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation1<A>,
    a: A,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    with(context) { reportOn(source, factory.chooseFactory(), a, positioningStrategy) }
}

context(context: DiagnosticContext)
fun <A> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation1<A>,
    a: A,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(), a, positioningStrategy)
}

// #### KtDiagnosticFactoryForDeprecation2 ####

fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation2<A, B>,
    a: A,
    b: B,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    with(context) { reportOn(source, factory.chooseFactory(), a, b, positioningStrategy) }
}

context(context: DiagnosticContext)
fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation2<A, B>,
    a: A,
    b: B,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(), a, b, positioningStrategy)
}

// #### KtDiagnosticFactoryForDeprecation3 ####

fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    with(context) { reportOn(source, factory.chooseFactory(), a, b, c, positioningStrategy) }
}

context(context: DiagnosticContext)
fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation3<A, B, C>,
    a: A,
    b: B,
    c: C,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(), a, b, c, positioningStrategy)
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
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    with(context) { reportOn(source, factory.chooseFactory(), a, b, c, d, positioningStrategy) }
}

context(context: DiagnosticContext)
fun <A, B, C, D> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(), a, b, c, d, positioningStrategy)
}

context(context: DiagnosticContext)
fun <F : KtDiagnosticFactoryN> KtDiagnosticFactoryForDeprecation<F>.chooseFactory(): F {
    return if (context.languageVersionSettings.supportsFeature(deprecatingFeature)) {
        errorFactory
    } else {
        warningFactory
    }
}
