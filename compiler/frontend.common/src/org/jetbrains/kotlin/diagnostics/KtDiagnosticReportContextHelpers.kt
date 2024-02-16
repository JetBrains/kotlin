/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.AbstractKtSourceElement

context(DiagnosticContext)
fun DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory0,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory, this@DiagnosticContext, positioningStrategy)
}

context(DiagnosticContext)
fun <A> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory1<A>,
    a: A,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory, a, this@DiagnosticContext, positioningStrategy)
}

context(DiagnosticContext)
fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory2<A, B>,
    a: A,
    b: B,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory, a, b, this@DiagnosticContext, positioningStrategy)
}

context(DiagnosticContext)
fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory3<A, B, C>,
    a: A,
    b: B,
    c: C,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory, a, b, c, this@DiagnosticContext, positioningStrategy)
}

context(DiagnosticContext)
fun <A, B, C, D> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactory4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory, a, b, c, d, this@DiagnosticContext, positioningStrategy)
}

context(DiagnosticContext)
fun DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation0,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory, this@DiagnosticContext, positioningStrategy)
}

context(DiagnosticContext)
fun <A> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation1<A>,
    a: A,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory, a, this@DiagnosticContext, positioningStrategy)
}

context(DiagnosticContext)
fun <A, B> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation2<A, B>,
    a: A,
    b: B,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory, a, b, this@DiagnosticContext, positioningStrategy)
}

context(DiagnosticContext)
fun <A, B, C> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation3<A, B, C>,
    a: A,
    b: B,
    c: C,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory, a, b, c, this@DiagnosticContext, positioningStrategy)
}

context(DiagnosticContext)
fun <A, B, C, D> DiagnosticReporter.reportOn(
    source: AbstractKtSourceElement?,
    factory: KtDiagnosticFactoryForDeprecation4<A, B, C, D>,
    a: A,
    b: B,
    c: C,
    d: D,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
) {
    reportOn(source, factory, a, b, c, d, this@DiagnosticContext, positioningStrategy)
}
