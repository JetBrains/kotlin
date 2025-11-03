/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalDiagnosticFactoryMethod::class)

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.AbstractKtSourceElement

// #### KtSourcelessFactory ####

fun DiagnosticReporter.report(
    factory: KtSourcelessDiagnosticFactory,
    message: String,
    context: DiagnosticContext,
) {
    report(factory.create(message, context), context)
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

@Deprecated(
    "When DiagnosticContext is available as context, use overload without context parameter.",
    replaceWith = ReplaceWith("reportOn(source, factory, positioningStrategy)")
)
context(_: DiagnosticContext)
fun DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory0,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null,
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

@Deprecated(
    "When DiagnosticContext is available as context, use overload without context parameter.",
    replaceWith = ReplaceWith("reportOn(source, factory, a, positioningStrategy)")
)
context(_: DiagnosticContext)
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

@Deprecated(
    "When DiagnosticContext is available as context, use overload without context parameter.",
    replaceWith = ReplaceWith("reportOn(source, factory, a, b, positioningStrategy)")
)
context(_: DiagnosticContext)
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

@Deprecated(
    "When DiagnosticContext is available as context, use overload without context parameter.",
    replaceWith = ReplaceWith("reportOn(source, factory, a, b, c, positioningStrategy)")
)
context(_: DiagnosticContext)
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

@Deprecated(
    "When DiagnosticContext is available as context, use overload without context parameter.",
    replaceWith = ReplaceWith("reportOn(source, factory, a, b, c, d, positioningStrategy)")
)
context(_: DiagnosticContext)
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
    reportOn(source, factory.chooseFactory(context), context, positioningStrategy)
}

@Deprecated(
    "When DiagnosticContext is available as context, use overload without context parameter.",
    replaceWith = ReplaceWith("reportOn(source, factory, positioningStrategy)")
)
context(_: DiagnosticContext)
fun DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation0,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), positioningStrategy)
}

context(context: DiagnosticContext)
fun DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation0,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), positioningStrategy)
}

// #### KtDiagnosticFactoryForDeprecation1 ####

fun <A> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation1<A>,
    a: A,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, context, positioningStrategy)
}

@Deprecated(
    "When DiagnosticContext is available as context, use overload without context parameter.",
    replaceWith = ReplaceWith("reportOn(source, factory, a, positioningStrategy)")
)
context(_: DiagnosticContext)
fun <A> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation1<A>,
    a: A,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, positioningStrategy)
}

context(context: DiagnosticContext)
fun <A> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation1<A>,
    a: A,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, positioningStrategy)
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
    reportOn(source, factory.chooseFactory(context), a, b, context, positioningStrategy)
}

@Deprecated(
    "When DiagnosticContext is available as context, use overload without context parameter.",
    replaceWith = ReplaceWith("reportOn(source, factory, a, b, positioningStrategy)")
)
context(_: DiagnosticContext)
fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation2<A, B>,
    a: A,
    b: B,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, positioningStrategy)
}

context(context: DiagnosticContext)
fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation2<A, B>,
    a: A,
    b: B,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, positioningStrategy)
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
    reportOn(source, factory.chooseFactory(context), a, b, c, context, positioningStrategy)
}

@Deprecated(
    "When DiagnosticContext is available as context, use overload without context parameter.",
    replaceWith = ReplaceWith("reportOn(source, factory, a, b, c, positioningStrategy)")
)
context(_: DiagnosticContext)
fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation3<A, B, C>,
    a: A,
    b: B,
    c: C,
    context: DiagnosticContext,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory.chooseFactory(context), a, b, c, positioningStrategy)
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
    reportOn(source, factory.chooseFactory(context), a, b, c, positioningStrategy)
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
    reportOn(source, factory.chooseFactory(context), a, b, c, d, context, positioningStrategy)
}

@Deprecated(
    "When DiagnosticContext is available as context, use overload without context parameter.",
    replaceWith = ReplaceWith("reportOn(source, factory, a, b, c, d, positioningStrategy)")
)
context(_: DiagnosticContext)
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
    reportOn(source, factory.chooseFactory(context), a, b, c, d, positioningStrategy)
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
    reportOn(source, factory.chooseFactory(context), a, b, c, d, positioningStrategy)
}

fun <F : KtDiagnosticFactoryN> KtDiagnosticFactoryForDeprecation<F>.chooseFactory(context: DiagnosticContext): F {
    return if (context.languageVersionSettings.supportsFeature(deprecatingFeature)) {
        errorFactory
    } else {
        warningFactory
    }
}
