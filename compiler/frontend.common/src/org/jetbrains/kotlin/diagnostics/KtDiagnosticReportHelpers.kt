/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalDiagnosticFactoryMethod::class)

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.AbstractKtSourceElement

fun DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory0,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), positioningStrategy), context)
}

fun <A : Any> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory1<A>,
    a: A,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, positioningStrategy), context)
}

fun <A : Any, B : Any> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory2<A, B>,
    a: A,
    b: B,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, positioningStrategy), context)
}

fun <A : Any, B : Any, C : Any> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, c, positioningStrategy), context)
}

fun <A : Any, B : Any, C : Any, D : Any> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    report(factory.on(source.requireNotNull(), a, b, c, d, positioningStrategy), context)
}

fun AbstractKtSourceElement?.requireNotNull(): AbstractKtSourceElement =
    requireNotNull(this) { "source must not be null" }

fun DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation0,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), context, positioningStrategy)
}

fun <A : Any> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation1<A>,
    a: A,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, context, positioningStrategy)
}

fun <A : Any, B : Any> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation2<A, B>,
    a: A,
    b: B,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, context, positioningStrategy)
}

fun <A : Any, B : Any, C : Any> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, c, context, positioningStrategy)
}

fun <A : Any, B : Any, C : Any, D : Any> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
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
