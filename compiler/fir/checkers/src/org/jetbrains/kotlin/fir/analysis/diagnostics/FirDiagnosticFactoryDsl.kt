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

fun <E : FirSourceElement> warning0(): DiagnosticFactory0DelegateProvider<E> {
    return DiagnosticFactory0DelegateProvider(Severity.WARNING, null)
}

fun <E : FirSourceElement, A> warning1(): DiagnosticFactory1DelegateProvider<E, A> {
    return DiagnosticFactory1DelegateProvider(Severity.WARNING, null)
}

fun <E : FirSourceElement, A, B> warning2(): DiagnosticFactory2DelegateProvider<E, A, B> {
    return DiagnosticFactory2DelegateProvider(Severity.WARNING, null)
}

fun <E : FirSourceElement, A, B, C> warning3(): DiagnosticFactory3DelegateProvider<E, A, B, C> {
    return DiagnosticFactory3DelegateProvider(Severity.WARNING, null)
}

fun <E : FirSourceElement> error0(): DiagnosticFactory0DelegateProvider<E> {
    return DiagnosticFactory0DelegateProvider(Severity.ERROR, null)
}

fun <E : FirSourceElement, A> error1(): DiagnosticFactory1DelegateProvider<E, A> {
    return DiagnosticFactory1DelegateProvider(Severity.ERROR, null)
}

fun <E : FirSourceElement, A, B> error2(): DiagnosticFactory2DelegateProvider<E, A, B> {
    return DiagnosticFactory2DelegateProvider(Severity.ERROR, null)
}

fun <E : FirSourceElement, A, B, C> error3(): DiagnosticFactory3DelegateProvider<E, A, B, C> {
    return DiagnosticFactory3DelegateProvider(Severity.ERROR, null)
}

/**
 * Note that those functions can be applicable only for factories
 *   that takes `PsiElement` as first type parameter
 */
fun <E : FirSourceElement> existing(psiDiagnosticFactory: DiagnosticFactory0<PsiElement>): DiagnosticFactory0DelegateProvider<E> {
    return DiagnosticFactory0DelegateProvider(Severity.ERROR, psiDiagnosticFactory)
}

fun <E : FirSourceElement, A> existing(psiDiagnosticFactory: DiagnosticFactory1<PsiElement, A>): DiagnosticFactory1DelegateProvider<E, A> {
    return DiagnosticFactory1DelegateProvider(Severity.ERROR, psiDiagnosticFactory)
}

fun <E : FirSourceElement, A, B> existing(psiDiagnosticFactory: DiagnosticFactory2<PsiElement, A, B>): DiagnosticFactory2DelegateProvider<E, A, B> {
    return DiagnosticFactory2DelegateProvider(Severity.ERROR, psiDiagnosticFactory)
}

fun <E : FirSourceElement, A, B, C> existing(psiDiagnosticFactory: DiagnosticFactory3<PsiElement, A, B, C>): DiagnosticFactory3DelegateProvider<E, A, B, C> {
    return DiagnosticFactory3DelegateProvider(Severity.ERROR, psiDiagnosticFactory)
}

// ------------------------------ Providers ------------------------------

class DiagnosticFactory0DelegateProvider<E : FirSourceElement>(
    private val severity: Severity,
    private val psiDiagnosticFactory: DiagnosticFactory0<PsiElement>?
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory0<E>> {
        val psiFactory = psiDiagnosticFactory ?: DiagnosticFactory0.create<PsiElement>(severity).apply {
            initializeName(prop.name)
        }
        return DummyDelegate(FirDiagnosticFactory0(prop.name, severity, psiFactory))
    }
}

class DiagnosticFactory1DelegateProvider<E : FirSourceElement, A>(
    private val severity: Severity,
    private val psiDiagnosticFactory: DiagnosticFactory1<PsiElement, A>?
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory1<E, A>> {
        val psiFactory = psiDiagnosticFactory ?: DiagnosticFactory1.create<PsiElement, A>(severity).apply {
            initializeName(prop.name)
        }
        return DummyDelegate(FirDiagnosticFactory1(prop.name, severity, psiFactory))
    }
}

class DiagnosticFactory2DelegateProvider<E : FirSourceElement, A, B>(
    private val severity: Severity,
    private val psiDiagnosticFactory: DiagnosticFactory2<PsiElement, A, B>?
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory2<E, A, B>> {
        val psiFactory = psiDiagnosticFactory ?: DiagnosticFactory2.create<PsiElement, A, B>(severity).apply {
            initializeName(prop.name)
        }
        return DummyDelegate(FirDiagnosticFactory2(prop.name, severity, psiFactory))
    }
}

class DiagnosticFactory3DelegateProvider<E : FirSourceElement, A, B, C>(
    private val severity: Severity,
    private val psiDiagnosticFactory: DiagnosticFactory3<PsiElement, A, B, C>?
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory3<E, A, B, C>> {
        val psiFactory = psiDiagnosticFactory ?: DiagnosticFactory3.create<PsiElement, A, B, C>(severity).apply {
            initializeName(prop.name)
        }
        return DummyDelegate(FirDiagnosticFactory3(prop.name, severity, psiFactory))
    }
}

private class DummyDelegate<T>(val value: T) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }
}