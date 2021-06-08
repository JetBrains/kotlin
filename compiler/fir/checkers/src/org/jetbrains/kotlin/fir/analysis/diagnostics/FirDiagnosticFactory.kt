/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticRenderer
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement

@RequiresOptIn("Please use DiagnosticReporter.reportOn method if possible")
annotation class InternalDiagnosticFactoryMethod

sealed class AbstractFirDiagnosticFactory<D : FirDiagnostic<*>, P : PsiElement>(
    override val name: String,
    override val severity: Severity,
    val positioningStrategy: SourceElementPositioningStrategy<P>,
) : DiagnosticFactory<D>(name, severity) {
    abstract val firRenderer: FirDiagnosticRenderer<D>

    override var defaultRenderer: DiagnosticRenderer<D>?
        get() = firRenderer
        set(_) {
        }

    fun getTextRanges(diagnostic: FirDiagnostic<*>): List<TextRange> =
        positioningStrategy.markDiagnostic(diagnostic)

    fun isValid(diagnostic: FirDiagnostic<*>): Boolean {
        val element = diagnostic.element
        return positioningStrategy.isValid(element)
    }
}

class FirDiagnosticFactory0<P : PsiElement>(
    name: String,
    severity: Severity,
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT,
) : AbstractFirDiagnosticFactory<FirSimpleDiagnostic<*>, P>(name, severity, positioningStrategy) {
    override val firRenderer: FirDiagnosticRenderer<FirSimpleDiagnostic<*>> = SimpleFirDiagnosticRenderer("")

    @InternalDiagnosticFactoryMethod
    fun on(element: FirSourceElement): FirSimpleDiagnostic<*> {
        return when (element) {
            is FirPsiSourceElement<*> -> FirPsiSimpleDiagnostic(
                element as FirPsiSourceElement<P>, severity, this
            )
            is FirLightSourceElement -> FirLightSimpleDiagnostic(element, severity, this)
            else -> incorrectElement(element)
        }
    }
}

class FirDiagnosticFactory1<P : PsiElement, A>(
    name: String,
    severity: Severity,
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT,
) : AbstractFirDiagnosticFactory<FirDiagnosticWithParameters1<*, A>, P>(name, severity, positioningStrategy) {
    override val firRenderer: FirDiagnosticRenderer<FirDiagnosticWithParameters1<*, A>> = FirDiagnosticWithParameters1Renderer(
        "{0}",
        FirDiagnosticRenderers.TO_STRING
    )

    @InternalDiagnosticFactoryMethod
    fun on(element: FirSourceElement, a: A): FirDiagnosticWithParameters1<*, A> {
        return when (element) {
            is FirPsiSourceElement<*> -> FirPsiDiagnosticWithParameters1(
                element as FirPsiSourceElement<P>, a, severity, this
            )
            is FirLightSourceElement -> FirLightDiagnosticWithParameters1(element, a, severity, this)
            else -> incorrectElement(element)
        }
    }
}

class FirDiagnosticFactory2<P : PsiElement, A, B>(
    name: String,
    severity: Severity,
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT,
) : AbstractFirDiagnosticFactory<FirDiagnosticWithParameters2<*, A, B>, P>(name, severity, positioningStrategy) {
    override val firRenderer: FirDiagnosticRenderer<FirDiagnosticWithParameters2<*, A, B>> = FirDiagnosticWithParameters2Renderer(
        "{0}, {1}",
        FirDiagnosticRenderers.TO_STRING,
        FirDiagnosticRenderers.TO_STRING
    )

    @InternalDiagnosticFactoryMethod
    fun on(element: FirSourceElement, a: A, b: B): FirDiagnosticWithParameters2<*, A, B> {
        return when (element) {
            is FirPsiSourceElement<*> -> FirPsiDiagnosticWithParameters2(
                element as FirPsiSourceElement<P>, a, b, severity, this
            )
            is FirLightSourceElement -> FirLightDiagnosticWithParameters2(element, a, b, severity, this)
            else -> incorrectElement(element)
        }
    }
}

class FirDiagnosticFactory3<P : PsiElement, A, B, C>(
    name: String,
    severity: Severity,
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT,
) : AbstractFirDiagnosticFactory<FirDiagnosticWithParameters3<*, A, B, C>, P>(name, severity, positioningStrategy) {
    override val firRenderer: FirDiagnosticRenderer<FirDiagnosticWithParameters3<*, A, B, C>> = FirDiagnosticWithParameters3Renderer(
        "{0}, {1}, {2}",
        FirDiagnosticRenderers.TO_STRING,
        FirDiagnosticRenderers.TO_STRING,
        FirDiagnosticRenderers.TO_STRING
    )

    @InternalDiagnosticFactoryMethod
    fun on(element: FirSourceElement, a: A, b: B, c: C): FirDiagnosticWithParameters3<*, A, B, C> {
        return when (element) {
            is FirPsiSourceElement<*> -> FirPsiDiagnosticWithParameters3(
                element as FirPsiSourceElement<P>, a, b, c, severity, this
            )
            is FirLightSourceElement -> FirLightDiagnosticWithParameters3(element, a, b, c, severity, this)
            else -> incorrectElement(element)
        }
    }
}

class FirDiagnosticFactory4<P : PsiElement, A, B, C, D>(
    name: String,
    severity: Severity,
    positioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT,
) : AbstractFirDiagnosticFactory<FirDiagnosticWithParameters4<*, A, B, C, D>, P>(name, severity, positioningStrategy) {
    override val firRenderer: FirDiagnosticRenderer<FirDiagnosticWithParameters4<*, A, B, C, D>> = FirDiagnosticWithParameters4Renderer(
        "{0}, {1}, {2}, {3}",
        FirDiagnosticRenderers.TO_STRING,
        FirDiagnosticRenderers.TO_STRING,
        FirDiagnosticRenderers.TO_STRING,
        FirDiagnosticRenderers.TO_STRING
    )

    @InternalDiagnosticFactoryMethod
    fun on(element: FirSourceElement, a: A, b: B, c: C, d: D): FirDiagnosticWithParameters4<*, A, B, C, D> {
        return when (element) {
            is FirPsiSourceElement<*> -> FirPsiDiagnosticWithParameters4(
                element as FirPsiSourceElement<P>, a, b, c, d, severity, this
            )
            is FirLightSourceElement -> FirLightDiagnosticWithParameters4(element, a, b, c, d, severity, this)
            else -> incorrectElement(element)
        }
    }
}

private fun incorrectElement(element: FirSourceElement): Nothing {
    throw IllegalArgumentException("Unknown element type: ${element::class}")
}

@InternalDiagnosticFactoryMethod
fun <P : PsiElement> FirDiagnosticFactory0<P>.on(element: FirSourceElement?): FirSimpleDiagnostic<*>? {
    return element?.let { on(it) }
}

@InternalDiagnosticFactoryMethod
fun <P : PsiElement, A> FirDiagnosticFactory1<P, A>.on(
    element: FirSourceElement?, a: A
): FirDiagnosticWithParameters1<*, A>? {
    return element?.let { on(it, a) }
}

@InternalDiagnosticFactoryMethod
fun <P : PsiElement, A, B> FirDiagnosticFactory2<P, A, B>.on(
    element: FirSourceElement?,
    a: A,
    b: B
): FirDiagnosticWithParameters2<*, A, B>? {
    return element?.let { on(it, a, b) }
}

@InternalDiagnosticFactoryMethod
fun <P : PsiElement, A, B, C> FirDiagnosticFactory3<P, A, B, C>.on(
    element: FirSourceElement?,
    a: A,
    b: B,
    c: C
): FirDiagnosticWithParameters3<*, A, B, C>? {
    return element?.let { on(it, a, b, c) }
}

@InternalDiagnosticFactoryMethod
fun <P : PsiElement, A, B, C, D> FirDiagnosticFactory4<P, A, B, C, D>.on(
    element: FirSourceElement?,
    a: A,
    b: B,
    c: C,
    d: D
): FirDiagnosticWithParameters4<*, A, B, C, D>? {
    return element?.let { on(it, a, b, c, d) }
}
