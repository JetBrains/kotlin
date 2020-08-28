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

fun <E : FirSourceElement, P : PsiElement> warning0(): DiagnosticFactory0DelegateProvider<E, P> {
    return DiagnosticFactory0DelegateProvider(Severity.WARNING, null)
}

fun <E : FirSourceElement, P : PsiElement, A : Any> warning1(): DiagnosticFactory1DelegateProvider<E, P, A> {
    return DiagnosticFactory1DelegateProvider(Severity.WARNING, null)
}

fun <E : FirSourceElement, P : PsiElement, A : Any, B : Any> warning2(): DiagnosticFactory2DelegateProvider<E, P, A, B> {
    return DiagnosticFactory2DelegateProvider(Severity.WARNING, null)
}

fun <E : FirSourceElement, P : PsiElement, A : Any, B : Any, C : Any> warning3(): DiagnosticFactory3DelegateProvider<E, P, A, B, C> {
    return DiagnosticFactory3DelegateProvider(Severity.WARNING, null)
}

fun <E : FirSourceElement, P : PsiElement> error0(): DiagnosticFactory0DelegateProvider<E, P> {
    return DiagnosticFactory0DelegateProvider(Severity.ERROR, null)
}

fun <E : FirSourceElement, P : PsiElement, A : Any> error1(): DiagnosticFactory1DelegateProvider<E, P, A> {
    return DiagnosticFactory1DelegateProvider(Severity.ERROR, null)
}

fun <E : FirSourceElement, P : PsiElement, A : Any, B : Any> error2(): DiagnosticFactory2DelegateProvider<E, P, A, B> {
    return DiagnosticFactory2DelegateProvider(Severity.ERROR, null)
}

fun <E : FirSourceElement, P : PsiElement, A : Any, B : Any, C : Any> error3(): DiagnosticFactory3DelegateProvider<E, P, A, B, C> {
    return DiagnosticFactory3DelegateProvider(Severity.ERROR, null)
}

/**
 * Note that those functions can be applicable only for factories
 *   that takes `PsiElement` as first type parameter
 */
fun <E : FirSourceElement, P : PsiElement> existing(
    psiDiagnosticFactory: DiagnosticFactory0<P>
): DiagnosticFactory0DelegateProvider<E, P> {
    return DiagnosticFactory0DelegateProvider(Severity.ERROR, psiDiagnosticFactory)
}

fun <E : FirSourceElement, P : PsiElement, A : Any> existing(
    psiDiagnosticFactory: DiagnosticFactory1<P, A>
): DiagnosticFactory1DelegateProvider<E, P, A> {
    return DiagnosticFactory1DelegateProvider(Severity.ERROR, psiDiagnosticFactory)
}

fun <E : FirSourceElement, P : PsiElement, A : Any, B : Any> existing(
    psiDiagnosticFactory: DiagnosticFactory2<P, A, B>
): DiagnosticFactory2DelegateProvider<E, P, A, B> {
    return DiagnosticFactory2DelegateProvider(Severity.ERROR, psiDiagnosticFactory)
}

fun <E : FirSourceElement, P : PsiElement, A : Any, B : Any, C : Any> existing(
    psiDiagnosticFactory: DiagnosticFactory3<P, A, B, C>
): DiagnosticFactory3DelegateProvider<E, P, A, B, C> {
    return DiagnosticFactory3DelegateProvider(Severity.ERROR, psiDiagnosticFactory)
}

// ------------------------------ Providers ------------------------------

class DiagnosticFactory0DelegateProvider<E : FirSourceElement, P : PsiElement>(
    private val severity: Severity,
    private val psiDiagnosticFactory: DiagnosticFactory0<P>?
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory0<E, P>> {
        val psiFactory = psiDiagnosticFactory ?: DiagnosticFactory0.create<P>(severity).apply {
            initializeName(prop.name)
        }
        return DummyDelegate(FirDiagnosticFactory0(prop.name, severity, psiFactory))
    }
}

class DiagnosticFactory1DelegateProvider<E : FirSourceElement, P : PsiElement, A : Any>(
    private val severity: Severity,
    private val psiDiagnosticFactory: DiagnosticFactory1<P, A>?
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory1<E, P, A>> {
        val psiFactory = psiDiagnosticFactory ?: DiagnosticFactory1.create<P, A>(severity).apply {
            initializeName(prop.name)
        }
        return DummyDelegate(FirDiagnosticFactory1(prop.name, severity, psiFactory))
    }
}

class DiagnosticFactory2DelegateProvider<E : FirSourceElement, P : PsiElement, A : Any, B : Any>(
    private val severity: Severity,
    private val psiDiagnosticFactory: DiagnosticFactory2<P, A, B>?
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory2<E, P, A, B>> {
        val psiFactory = psiDiagnosticFactory ?: DiagnosticFactory2.create<P, A, B>(severity).apply {
            initializeName(prop.name)
        }
        return DummyDelegate(FirDiagnosticFactory2(prop.name, severity, psiFactory))
    }
}

class DiagnosticFactory3DelegateProvider<E : FirSourceElement, P : PsiElement, A : Any, B : Any, C : Any>(
    private val severity: Severity,
    private val psiDiagnosticFactory: DiagnosticFactory3<P, A, B, C>?
) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, FirDiagnosticFactory3<E, P, A, B, C>> {
        val psiFactory = psiDiagnosticFactory ?: DiagnosticFactory3.create<P, A, B, C>(severity).apply {
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
