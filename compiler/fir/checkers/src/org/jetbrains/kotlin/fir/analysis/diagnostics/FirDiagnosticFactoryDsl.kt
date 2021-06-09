/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Severity
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <reified P : PsiElement> warning0(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory0DelegateProvider<P> {
    return DiagnosticFactory0DelegateProvider(Severity.WARNING, positioningStrategy, P::class)
}

inline fun <reified P : PsiElement, A> warning1(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory1DelegateProvider<P, A> {
    return DiagnosticFactory1DelegateProvider(Severity.WARNING, positioningStrategy, P::class)
}

inline fun <reified P : PsiElement, A, B> warning2(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory2DelegateProvider<P, A, B> {
    return DiagnosticFactory2DelegateProvider(Severity.WARNING, positioningStrategy, P::class)
}

inline fun <reified P : PsiElement, A, B, C> warning3(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory3DelegateProvider<P, A, B, C> {
    return DiagnosticFactory3DelegateProvider(Severity.WARNING, positioningStrategy, P::class)
}

inline fun <reified P : PsiElement> error0(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory0DelegateProvider<P> {
    return DiagnosticFactory0DelegateProvider(Severity.ERROR, positioningStrategy, P::class)
}

inline fun <reified P : PsiElement, A> error1(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory1DelegateProvider<P, A> {
    return DiagnosticFactory1DelegateProvider(Severity.ERROR, positioningStrategy, P::class)
}

inline fun <reified P : PsiElement, A, B> error2(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory2DelegateProvider<P, A, B> {
    return DiagnosticFactory2DelegateProvider(Severity.ERROR, positioningStrategy, P::class)
}

inline fun <reified P : PsiElement, A, B, C> error3(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory3DelegateProvider<P, A, B, C> {
    return DiagnosticFactory3DelegateProvider(Severity.ERROR, positioningStrategy, P::class)
}

inline fun <reified P : PsiElement, A, B, C, D> error4(
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT
): DiagnosticFactory4DelegateProvider<P, A, B, C, D> {
    return DiagnosticFactory4DelegateProvider(Severity.ERROR, positioningStrategy, P::class)
}

// ------------------------------ Providers ------------------------------

class DiagnosticFactory0DelegateProvider<P : PsiElement>(
    private val severity: Severity,
    private val positioningStrategy: SourceElementPositioningStrategy<P>,
    private val psiType: KClass<*>
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory0<P>> {
        return DummyDelegate(FirDiagnosticFactory0(prop.name, severity, positioningStrategy, psiType))
    }
}

class DiagnosticFactory1DelegateProvider<P : PsiElement, A>(
    private val severity: Severity,
    private val positioningStrategy: SourceElementPositioningStrategy<P>,
    private val psiType: KClass<*>
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory1<P, A>> {
        return DummyDelegate(FirDiagnosticFactory1(prop.name, severity, positioningStrategy, psiType))
    }
}

class DiagnosticFactory2DelegateProvider<P : PsiElement, A, B>(
    private val severity: Severity,
    private val positioningStrategy: SourceElementPositioningStrategy<P>,
    private val psiType: KClass<*>
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory2<P, A, B>> {
        return DummyDelegate(FirDiagnosticFactory2(prop.name, severity, positioningStrategy, psiType))
    }
}

class DiagnosticFactory3DelegateProvider<P : PsiElement, A, B, C>(
    private val severity: Severity,
    private val positioningStrategy: SourceElementPositioningStrategy<P>,
    private val psiType: KClass<*>
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory3<P, A, B, C>> {
        return DummyDelegate(FirDiagnosticFactory3(prop.name, severity, positioningStrategy, psiType))
    }
}

class DiagnosticFactory4DelegateProvider<P : PsiElement, A, B, C, D>(
    private val severity: Severity,
    private val positioningStrategy: SourceElementPositioningStrategy<P>,
    private val psiType: KClass<*>
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory4<P, A, B, C, D>> {
        return DummyDelegate(FirDiagnosticFactory4(prop.name, severity, positioningStrategy, psiType))
    }
}


private class DummyDelegate<T>(val value: T) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }
}
