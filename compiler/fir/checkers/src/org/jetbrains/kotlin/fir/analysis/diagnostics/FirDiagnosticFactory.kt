@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement

@RequiresOptIn("Please use DiagnosticReporter.reportOn method if possible")
annotation class InternalDiagnosticFactoryMethod

sealed class AbstractFirDiagnosticFactory<D : FirDiagnostic<*>, P : PsiElement>(
    val name: String,
    val severity: Severity,
    val defaultPositioningStrategy: SourceElementPositioningStrategy<P>,
) {
    abstract val firRenderer: FirDiagnosticRenderer<D>
}

class FirDiagnosticFactory0<P : PsiElement>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT,
) : AbstractFirDiagnosticFactory<FirSimpleDiagnostic<*>, P>(name, severity, defaultPositioningStrategy) {
    override val firRenderer: FirDiagnosticRenderer<FirSimpleDiagnostic<*>> = SimpleFirDiagnosticRenderer("")

    @InternalDiagnosticFactoryMethod
    fun on(
        element: FirSourceElement,
        positioningStrategy: SourceElementPositioningStrategy<P>?
    ): FirSimpleDiagnostic<*> {
        return when (element) {
            is FirPsiSourceElement<*> -> FirPsiSimpleDiagnostic(
                element as FirPsiSourceElement<P>, severity, this, positioningStrategy ?: defaultPositioningStrategy
            )
            is FirLightSourceElement -> FirLightSimpleDiagnostic(element, severity, this, positioningStrategy ?: defaultPositioningStrategy)
            else -> incorrectElement(element)
        }
    }
}

class FirDiagnosticFactory1<P : PsiElement, A>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT,
) : AbstractFirDiagnosticFactory<FirDiagnosticWithParameters1<*, A>, P>(name, severity, defaultPositioningStrategy) {
    override val firRenderer: FirDiagnosticRenderer<FirDiagnosticWithParameters1<*, A>> = FirDiagnosticWithParameters1Renderer(
        "{0}",
        FirDiagnosticRenderers.TO_STRING
    )

    @InternalDiagnosticFactoryMethod
    fun on(
        element: FirSourceElement,
        a: A,
        positioningStrategy: SourceElementPositioningStrategy<P>?
    ): FirDiagnosticWithParameters1<*, A> {
        return when (element) {
            is FirPsiSourceElement<*> -> FirPsiDiagnosticWithParameters1(
                element as FirPsiSourceElement<P>, a, severity, this, positioningStrategy ?: defaultPositioningStrategy
            )
            is FirLightSourceElement -> FirLightDiagnosticWithParameters1(
                element,
                a,
                severity,
                this,
                positioningStrategy ?: defaultPositioningStrategy
            )
            else -> incorrectElement(element)
        }
    }
}

class FirDiagnosticFactory2<P : PsiElement, A, B>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT,
) : AbstractFirDiagnosticFactory<FirDiagnosticWithParameters2<*, A, B>, P>(name, severity, defaultPositioningStrategy) {
    override val firRenderer: FirDiagnosticRenderer<FirDiagnosticWithParameters2<*, A, B>> = FirDiagnosticWithParameters2Renderer(
        "{0}, {1}",
        FirDiagnosticRenderers.TO_STRING,
        FirDiagnosticRenderers.TO_STRING
    )

    @InternalDiagnosticFactoryMethod
    fun on(
        element: FirSourceElement,
        a: A,
        b: B,
        positioningStrategy: SourceElementPositioningStrategy<P>?
    ): FirDiagnosticWithParameters2<*, A, B> {
        return when (element) {
            is FirPsiSourceElement<*> -> FirPsiDiagnosticWithParameters2(
                element as FirPsiSourceElement<P>, a, b, severity, this, positioningStrategy ?: defaultPositioningStrategy
            )
            is FirLightSourceElement -> FirLightDiagnosticWithParameters2(
                element,
                a,
                b,
                severity,
                this,
                positioningStrategy ?: defaultPositioningStrategy
            )
            else -> incorrectElement(element)
        }
    }
}

class FirDiagnosticFactory3<P : PsiElement, A, B, C>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT,
) : AbstractFirDiagnosticFactory<FirDiagnosticWithParameters3<*, A, B, C>, P>(name, severity, defaultPositioningStrategy) {
    override val firRenderer: FirDiagnosticRenderer<FirDiagnosticWithParameters3<*, A, B, C>> = FirDiagnosticWithParameters3Renderer(
        "{0}, {1}, {2}",
        FirDiagnosticRenderers.TO_STRING,
        FirDiagnosticRenderers.TO_STRING,
        FirDiagnosticRenderers.TO_STRING
    )

    @InternalDiagnosticFactoryMethod
    fun on(
        element: FirSourceElement,
        a: A,
        b: B,
        c: C,
        positioningStrategy: SourceElementPositioningStrategy<P>?
    ): FirDiagnosticWithParameters3<*, A, B, C> {
        return when (element) {
            is FirPsiSourceElement<*> -> FirPsiDiagnosticWithParameters3(
                element as FirPsiSourceElement<P>, a, b, c, severity, this, positioningStrategy ?: defaultPositioningStrategy
            )
            is FirLightSourceElement -> FirLightDiagnosticWithParameters3(
                element,
                a,
                b,
                c,
                severity,
                this,
                positioningStrategy ?: defaultPositioningStrategy
            )
            else -> incorrectElement(element)
        }
    }
}

class FirDiagnosticFactory4<P : PsiElement, A, B, C, D>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: SourceElementPositioningStrategy<P> = SourceElementPositioningStrategy.DEFAULT,
) : AbstractFirDiagnosticFactory<FirDiagnosticWithParameters4<*, A, B, C, D>, P>(name, severity, defaultPositioningStrategy) {
    override val firRenderer: FirDiagnosticRenderer<FirDiagnosticWithParameters4<*, A, B, C, D>> = FirDiagnosticWithParameters4Renderer(
        "{0}, {1}, {2}, {3}",
        FirDiagnosticRenderers.TO_STRING,
        FirDiagnosticRenderers.TO_STRING,
        FirDiagnosticRenderers.TO_STRING,
        FirDiagnosticRenderers.TO_STRING
    )

    @InternalDiagnosticFactoryMethod
    fun on(
        element: FirSourceElement,
        a: A,
        b: B,
        c: C,
        d: D,
        positioningStrategy: SourceElementPositioningStrategy<P>?
    ): FirDiagnosticWithParameters4<*, A, B, C, D> {
        return when (element) {
            is FirPsiSourceElement<*> -> FirPsiDiagnosticWithParameters4(
                element as FirPsiSourceElement<P>, a, b, c, d, severity, this, positioningStrategy ?: defaultPositioningStrategy
            )
            is FirLightSourceElement -> FirLightDiagnosticWithParameters4(
                element,
                a,
                b,
                c,
                d,
                severity,
                this,
                positioningStrategy ?: defaultPositioningStrategy
            )
            else -> incorrectElement(element)
        }
    }
}

private fun incorrectElement(element: FirSourceElement): Nothing {
    throw IllegalArgumentException("Unknown element type: ${element::class}")
}
