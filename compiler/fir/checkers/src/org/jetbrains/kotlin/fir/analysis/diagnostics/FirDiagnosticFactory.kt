/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement

sealed class AbstractFirDiagnosticFactory<out E : FirSourceElement, D : FirDiagnostic<E>>(
    val name: String,
    val severity: Severity,
    val positioningStrategy: LightTreePositioningStrategy,
) {
    abstract val psiDiagnosticFactory: DiagnosticFactoryWithPsiElement<*, *>

    abstract val defaultRenderer: FirDiagnosticRenderer<*>

    fun getTextRanges(diagnostic: FirDiagnostic<*>): List<TextRange> =
        positioningStrategy.markDiagnostic(diagnostic)

    override fun toString(): String {
        return name
    }
}

class FirDiagnosticFactory0<E : FirSourceElement, P : PsiElement>(
    name: String,
    severity: Severity,
    override val psiDiagnosticFactory: DiagnosticFactory0<P>,
    positioningStrategy: LightTreePositioningStrategy = LightTreePositioningStrategy.DEFAULT,
) : AbstractFirDiagnosticFactory<E, FirSimpleDiagnostic<E>>(name, severity, positioningStrategy) {
    companion object {
        private val DefaultRenderer = SimpleFirDiagnosticRenderer("")
    }

    override val defaultRenderer: FirDiagnosticRenderer<*>
        get() = DefaultRenderer

    fun on(element: E): FirSimpleDiagnostic<E> {
        return when (element) {
            is FirPsiSourceElement<*> -> FirPsiSimpleDiagnostic(
                element as FirPsiSourceElement<P>, severity, this as FirDiagnosticFactory0<FirPsiSourceElement<P>, P>
            )
            is FirLightSourceElement -> FirLightSimpleDiagnostic(element, severity, this)
            else -> incorrectElement(element)
        } as FirSimpleDiagnostic<E>
    }
}

class FirDiagnosticFactory1<E : FirSourceElement, P : PsiElement, A : Any>(
    name: String,
    severity: Severity,
    override val psiDiagnosticFactory: DiagnosticFactory1<P, A>,
    positioningStrategy: LightTreePositioningStrategy = LightTreePositioningStrategy.DEFAULT,
) : AbstractFirDiagnosticFactory<E, FirDiagnosticWithParameters1<E, A>>(name, severity, positioningStrategy) {
    companion object {
        private val DefaultRenderer = FirDiagnosticWithParameters1Renderer(
            "{0}",
            FirDiagnosticRenderers.TO_STRING
        )
    }

    override val defaultRenderer: FirDiagnosticRenderer<*>
        get() = DefaultRenderer

    fun on(element: E, a: A): FirDiagnosticWithParameters1<E, A> {
        return when (element) {
            is FirPsiSourceElement<*> -> FirPsiDiagnosticWithParameters1(
                element as FirPsiSourceElement<P>, a, severity, this as FirDiagnosticFactory1<FirPsiSourceElement<P>, P, A>
            )
            is FirLightSourceElement -> FirLightDiagnosticWithParameters1(element, a, severity, this)
            else -> incorrectElement(element)
        } as FirDiagnosticWithParameters1<E, A>
    }
}

class FirDiagnosticFactory2<E : FirSourceElement, P : PsiElement, A : Any, B : Any>(
    name: String,
    severity: Severity,
    override val psiDiagnosticFactory: DiagnosticFactory2<P, A, B>,
    positioningStrategy: LightTreePositioningStrategy = LightTreePositioningStrategy.DEFAULT,
) : AbstractFirDiagnosticFactory<E, FirDiagnosticWithParameters2<E, A, B>>(name, severity, positioningStrategy) {
    companion object {
        private val DefaultRenderer = FirDiagnosticWithParameters2Renderer(
            "{0}, {1}",
            FirDiagnosticRenderers.TO_STRING,
            FirDiagnosticRenderers.TO_STRING
        )
    }

    override val defaultRenderer: FirDiagnosticRenderer<*>
        get() = DefaultRenderer

    fun on(element: E, a: A, b: B): FirDiagnosticWithParameters2<E, A, B> {
        return when (element) {
            is FirPsiSourceElement<*> -> FirPsiDiagnosticWithParameters2(
                element as FirPsiSourceElement<P>, a, b, severity, this as FirDiagnosticFactory2<FirPsiSourceElement<P>, P, A, B>
            )
            is FirLightSourceElement -> FirLightDiagnosticWithParameters2(element, a, b, severity, this)
            else -> incorrectElement(element)
        } as FirDiagnosticWithParameters2<E, A, B>
    }
}

class FirDiagnosticFactory3<E : FirSourceElement, P : PsiElement, A : Any, B : Any, C : Any>(
    name: String,
    severity: Severity,
    override val psiDiagnosticFactory: DiagnosticFactory3<P, A, B, C>,
    positioningStrategy: LightTreePositioningStrategy = LightTreePositioningStrategy.DEFAULT,
) : AbstractFirDiagnosticFactory<E, FirDiagnosticWithParameters3<E, A, B, C>>(name, severity, positioningStrategy) {
    companion object {
        private val DefaultRenderer = FirDiagnosticWithParameters3Renderer(
            "{0}, {1}, {2}",
            FirDiagnosticRenderers.TO_STRING,
            FirDiagnosticRenderers.TO_STRING,
            FirDiagnosticRenderers.TO_STRING
        )
    }

    override val defaultRenderer: FirDiagnosticRenderer<*>
        get() = DefaultRenderer

    fun on(element: E, a: A, b: B, c: C): FirDiagnosticWithParameters3<E, A, B, C> {
        return when (element) {
            is FirPsiSourceElement<*> -> FirPsiDiagnosticWithParameters3(
                element as FirPsiSourceElement<P>, a, b, c, severity, this as FirDiagnosticFactory3<FirPsiSourceElement<P>, P, A, B, C>
            )
            is FirLightSourceElement -> FirLightDiagnosticWithParameters3(element, a, b, c, severity, this)
            else -> incorrectElement(element)
        } as FirDiagnosticWithParameters3<E, A, B, C>
    }
}

private fun incorrectElement(element: FirSourceElement): Nothing {
    throw IllegalArgumentException("Unknown element type: ${element::class}")
}

fun <E : FirSourceElement, P : PsiElement> FirDiagnosticFactory0<E, P>.on(element: E?): FirSimpleDiagnostic<E>? {
    return element?.let { on(it) }
}

fun <E : FirSourceElement, P : PsiElement, A : Any> FirDiagnosticFactory1<E, P, A>.on(element: E?, a: A): FirDiagnosticWithParameters1<E, A>? {
    return element?.let { on(it, a) }
}

fun <E : FirSourceElement, P : PsiElement, A : Any, B : Any> FirDiagnosticFactory2<E, P, A, B>.on(
    element: E?,
    a: A,
    b: B
): FirDiagnosticWithParameters2<E, A, B>? {
    return element?.let { on(it, a, b) }
}

fun <E : FirSourceElement, P : PsiElement, A : Any, B : Any, C : Any> FirDiagnosticFactory3<E, P, A, B, C>.on(
    element: E?,
    a: A,
    b: B,
    c: C
): FirDiagnosticWithParameters3<E, A, B, C>? {
    return element?.let { on(it, a, b, c) }
}
