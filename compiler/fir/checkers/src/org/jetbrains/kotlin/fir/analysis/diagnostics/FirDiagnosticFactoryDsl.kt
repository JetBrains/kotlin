/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.FirSourceElement
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <E : FirSourceElement, P : PsiElement> warning0(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory0DelegateProvider<E, P> {
    return DiagnosticFactory0DelegateProvider(Severity.WARNING, positioningStrategy)
}

fun <E : FirSourceElement, P : PsiElement, A : Any> warning1(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory1DelegateProvider<E, P, A> {
    return DiagnosticFactory1DelegateProvider(Severity.WARNING, positioningStrategy)
}

fun <E : FirSourceElement, P : PsiElement, A : Any, B : Any> warning2(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory2DelegateProvider<E, P, A, B> {
    return DiagnosticFactory2DelegateProvider(Severity.WARNING, positioningStrategy)
}

fun <E : FirSourceElement, P : PsiElement, A : Any, B : Any, C : Any> warning3(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory3DelegateProvider<E, P, A, B, C> {
    return DiagnosticFactory3DelegateProvider(Severity.WARNING, positioningStrategy)
}

fun <E : FirSourceElement, P : PsiElement> error0(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory0DelegateProvider<E, P> {
    return DiagnosticFactory0DelegateProvider(Severity.ERROR, positioningStrategy)
}

fun <E : FirSourceElement, P : PsiElement, A : Any> error1(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory1DelegateProvider<E, P, A> {
    return DiagnosticFactory1DelegateProvider(Severity.ERROR, positioningStrategy)
}

fun <E : FirSourceElement, P : PsiElement, A : Any, B : Any> error2(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory2DelegateProvider<E, P, A, B> {
    return DiagnosticFactory2DelegateProvider(Severity.ERROR, positioningStrategy)
}

fun <E : FirSourceElement, P : PsiElement, A : Any, B : Any, C : Any> error3(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory3DelegateProvider<E, P, A, B, C> {
    return DiagnosticFactory3DelegateProvider(Severity.ERROR, positioningStrategy)
}

/**
 * Note that those functions can be applicable only for factories
 *   that takes `PsiElement` as first type parameter
 */
fun <E : FirSourceElement, P : PsiElement> existing0(): DiagnosticFactory0DelegateProvider<E, P> {
    return DiagnosticFactory0DelegateProvider(Severity.ERROR, SourceElementPositioningStrategy.DEFAULT)
}

fun <E : FirSourceElement, P : PsiElement, A : Any> existing1(): DiagnosticFactory1DelegateProvider<E, P, A> {
    return DiagnosticFactory1DelegateProvider(Severity.ERROR, SourceElementPositioningStrategy.DEFAULT)
}

fun <E : FirSourceElement, P : PsiElement, A : Any, B : Any> existing2(): DiagnosticFactory2DelegateProvider<E, P, A, B> {
    return DiagnosticFactory2DelegateProvider(Severity.ERROR, SourceElementPositioningStrategy.DEFAULT)
}

fun <E : FirSourceElement, P : PsiElement, A : Any, B : Any, C : Any> existing3(): DiagnosticFactory3DelegateProvider<E, P, A, B, C> {
    return DiagnosticFactory3DelegateProvider(Severity.ERROR, SourceElementPositioningStrategy.DEFAULT)
}

// ------------------------------ Providers ------------------------------

class DiagnosticFactory0DelegateProvider<E : FirSourceElement, P : PsiElement>(
    private val severity: Severity,
    private val positioningStrategy: SourceElementPositioningStrategy<P>
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory0<E, P>> {
        return DummyDelegate(FirDiagnosticFactory0(prop.name, severity, positioningStrategy))
    }
}

class DiagnosticFactory1DelegateProvider<E : FirSourceElement, P : PsiElement, A : Any>(
    private val severity: Severity,
    private val positioningStrategy: SourceElementPositioningStrategy<P>
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory1<E, P, A>> {
        return DummyDelegate(FirDiagnosticFactory1(prop.name, severity, positioningStrategy))
    }
}

class DiagnosticFactory2DelegateProvider<E : FirSourceElement, P : PsiElement, A : Any, B : Any>(
    private val severity: Severity,
    private val positioningStrategy: SourceElementPositioningStrategy<P>
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory2<E, P, A, B>> {
        return DummyDelegate(FirDiagnosticFactory2(prop.name, severity, positioningStrategy))
    }
}

class DiagnosticFactory3DelegateProvider<E : FirSourceElement, P : PsiElement, A : Any, B : Any, C : Any>(
    private val severity: Severity,
    private val positioningStrategy: SourceElementPositioningStrategy<P>
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory3<E, P, A, B, C>> {
        return DummyDelegate(FirDiagnosticFactory3(prop.name, severity, positioningStrategy))
    }
}

private class DummyDelegate<T>(val value: T) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }
}
