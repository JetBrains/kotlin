@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement
import kotlin.reflect.KClass

@RequiresOptIn("Please use DiagnosticReporter.reportOn method if possible")
annotation class InternalDiagnosticFactoryMethod

sealed class AbstractFirDiagnosticFactory(
    val name: String,
    val severity: Severity,
    val defaultPositioningStrategy: SourceElementPositioningStrategy,
    val psiType: KClass<*>
) {
    abstract val firRenderer: FirDiagnosticRenderer

    override fun toString(): String {
        return name
    }

    protected fun checkMyDiagnostic(diagnostic: FirDiagnostic) {
        require(diagnostic.factory === this) { "$diagnostic should be of this factory" }
    }
}

class FirDiagnosticFactory0(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: SourceElementPositioningStrategy,
    psiType: KClass<*>
) : AbstractFirDiagnosticFactory(name, severity, defaultPositioningStrategy, psiType) {
    override val firRenderer: FirDiagnosticRenderer = SimpleFirDiagnosticRenderer("")

    @InternalDiagnosticFactoryMethod
    fun on(
        element: FirSourceElement,
        positioningStrategy: SourceElementPositioningStrategy?
    ): FirSimpleDiagnostic {
        return when (element) {
            is FirPsiSourceElement -> FirPsiSimpleDiagnostic(
                element, severity, this, positioningStrategy ?: defaultPositioningStrategy
            )
            is FirLightSourceElement -> FirLightSimpleDiagnostic(element, severity, this, positioningStrategy ?: defaultPositioningStrategy)
            else -> incorrectElement(element)
        }
    }
}

class FirDiagnosticFactory1<A>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: SourceElementPositioningStrategy,
    psiType: KClass<*>
) : AbstractFirDiagnosticFactory(name, severity, defaultPositioningStrategy, psiType) {
    override val firRenderer: FirDiagnosticRenderer = FirDiagnosticWithParameters1Renderer(
        "{0}",
        FirDiagnosticRenderers.TO_STRING
    )

    @InternalDiagnosticFactoryMethod
    fun on(
        element: FirSourceElement,
        a: A,
        positioningStrategy: SourceElementPositioningStrategy?
    ): FirDiagnosticWithParameters1<A> {
        return when (element) {
            is FirPsiSourceElement -> FirPsiDiagnosticWithParameters1(
                element, a, severity, this, positioningStrategy ?: defaultPositioningStrategy
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

    fun extractArgument(diagnostic: FirDiagnostic): A {
        checkMyDiagnostic(diagnostic)
        return (diagnostic as FirDiagnosticWithParameters1<A>).a
    }
}

class FirDiagnosticFactory2<A, B>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: SourceElementPositioningStrategy,
    psiType: KClass<*>
) : AbstractFirDiagnosticFactory(name, severity, defaultPositioningStrategy, psiType) {
    override val firRenderer: FirDiagnosticRenderer = FirDiagnosticWithParameters2Renderer(
        "{0}, {1}",
        FirDiagnosticRenderers.TO_STRING,
        FirDiagnosticRenderers.TO_STRING
    )

    @InternalDiagnosticFactoryMethod
    fun on(
        element: FirSourceElement,
        a: A,
        b: B,
        positioningStrategy: SourceElementPositioningStrategy?
    ): FirDiagnosticWithParameters2<A, B> {
        return when (element) {
            is FirPsiSourceElement -> FirPsiDiagnosticWithParameters2(
                element, a, b, severity, this, positioningStrategy ?: defaultPositioningStrategy
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

    fun extractArguments(diagnostic: FirDiagnostic): Pair<A, B> {
        checkMyDiagnostic(diagnostic)
        val typed = diagnostic as FirDiagnosticWithParameters2<A, B>
        return Pair(typed.a, typed.b)
    }
}

class FirDiagnosticFactory3<A, B, C>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: SourceElementPositioningStrategy,
    psiType: KClass<*>
) : AbstractFirDiagnosticFactory(name, severity, defaultPositioningStrategy, psiType) {
    override val firRenderer: FirDiagnosticRenderer = FirDiagnosticWithParameters3Renderer(
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
        positioningStrategy: SourceElementPositioningStrategy?
    ): FirDiagnosticWithParameters3<A, B, C> {
        return when (element) {
            is FirPsiSourceElement -> FirPsiDiagnosticWithParameters3(
                element, a, b, c, severity, this, positioningStrategy ?: defaultPositioningStrategy
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

class FirDiagnosticFactory4<A, B, C, D>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: SourceElementPositioningStrategy,
    psiType: KClass<*>
) : AbstractFirDiagnosticFactory(name, severity, defaultPositioningStrategy, psiType) {
    override val firRenderer: FirDiagnosticRenderer = FirDiagnosticWithParameters4Renderer(
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
        positioningStrategy: SourceElementPositioningStrategy?
    ): FirDiagnosticWithParameters4<A, B, C, D> {
        return when (element) {
            is FirPsiSourceElement -> FirPsiDiagnosticWithParameters4(
                element, a, b, c, d, severity, this, positioningStrategy ?: defaultPositioningStrategy
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

// ------------------------------ factories for deprecation ------------------------------

sealed class FirDiagnosticFactoryForDeprecation<F : AbstractFirDiagnosticFactory>(
    val deprecatingFeature: LanguageFeature,
    val warningFactory: F,
    val errorFactory: F
)

class FirDiagnosticFactoryForDeprecation0(
    featureForError: LanguageFeature,
    warningFactory: FirDiagnosticFactory0,
    errorFactory: FirDiagnosticFactory0
) : FirDiagnosticFactoryForDeprecation<FirDiagnosticFactory0>(featureForError, warningFactory, errorFactory)

class FirDiagnosticFactoryForDeprecation1<A>(
    featureForError: LanguageFeature,
    warningFactory: FirDiagnosticFactory1<A>,
    errorFactory: FirDiagnosticFactory1<A>
) : FirDiagnosticFactoryForDeprecation<FirDiagnosticFactory1<A>>(featureForError, warningFactory, errorFactory)

class FirDiagnosticFactoryForDeprecation2<A, B>(
    featureForError: LanguageFeature,
    warningFactory: FirDiagnosticFactory2<A, B>,
    errorFactory: FirDiagnosticFactory2<A, B>
) : FirDiagnosticFactoryForDeprecation<FirDiagnosticFactory2<A, B>>(featureForError, warningFactory, errorFactory)

class FirDiagnosticFactoryForDeprecation3<A, B, C>(
    featureForError: LanguageFeature,
    warningFactory: FirDiagnosticFactory3<A, B, C>,
    errorFactory: FirDiagnosticFactory3<A, B, C>
) : FirDiagnosticFactoryForDeprecation<FirDiagnosticFactory3<A, B, C>>(featureForError, warningFactory, errorFactory)

class FirDiagnosticFactoryForDeprecation4<A, B, C, D>(
    featureForError: LanguageFeature,
    warningFactory: FirDiagnosticFactory4<A, B, C, D>,
    errorFactory: FirDiagnosticFactory4<A, B, C, D>
) : FirDiagnosticFactoryForDeprecation<FirDiagnosticFactory4<A, B, C, D>>(featureForError, warningFactory, errorFactory)
