/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.WarningLevel
import kotlin.reflect.KClass

@RequiresOptIn("Please use DiagnosticReporter.reportOn method if possible")
annotation class InternalDiagnosticFactoryMethod

sealed class AbstractKtDiagnosticFactory(
    val name: String,
    val severity: Severity,
    val defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    val psiType: KClass<*>
) {
    abstract val ktRenderer: KtDiagnosticRenderer

    protected fun getEffectiveSeverity(languageVersionSettings: LanguageVersionSettings): Severity? {
        return when (languageVersionSettings.getFlag(AnalysisFlags.warningLevels)[name]) {
            WarningLevel.Error -> Severity.ERROR
            WarningLevel.Warning -> Severity.FIXED_WARNING
            WarningLevel.Disabled -> null
            null -> severity
        }
    }

    override fun toString(): String {
        return name
    }
}

class KtDiagnosticFactory0(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>
) : AbstractKtDiagnosticFactory(name, severity, defaultPositioningStrategy, psiType) {
    override val ktRenderer: KtDiagnosticRenderer = SimpleKtDiagnosticRenderer("")

    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractKtSourceElement,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        languageVersionSettings: LanguageVersionSettings,
    ): KtSimpleDiagnostic? {
        val effectiveSeverity = getEffectiveSeverity(languageVersionSettings) ?: return null
        return when (element) {
            is KtPsiSourceElement -> KtPsiSimpleDiagnostic(
                element, effectiveSeverity, this, positioningStrategy ?: defaultPositioningStrategy
            )
            is KtLightSourceElement -> KtLightSimpleDiagnostic(
                element,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy
            )
            else -> KtOffsetsOnlySimpleDiagnostic(element, effectiveSeverity, this, positioningStrategy ?: defaultPositioningStrategy)
        }
    }
}

class KtDiagnosticFactory1<A>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>
) : AbstractKtDiagnosticFactory(name, severity, defaultPositioningStrategy, psiType) {
    override val ktRenderer: KtDiagnosticRenderer = KtDiagnosticWithParameters1Renderer(
        "{0}",
        KtDiagnosticRenderers.TO_STRING
    )

    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractKtSourceElement,
        a: A,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        languageVersionSettings: LanguageVersionSettings,
    ): KtDiagnosticWithParameters1<A>? {
        val effectiveSeverity = getEffectiveSeverity(languageVersionSettings) ?: return null
        return when (element) {
            is KtPsiSourceElement -> KtPsiDiagnosticWithParameters1(
                element, a, effectiveSeverity, this, positioningStrategy ?: defaultPositioningStrategy
            )
            is KtLightSourceElement -> KtLightDiagnosticWithParameters1(
                element,
                a,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy
            )
            else -> KtOffsetsOnlyDiagnosticWithParameters1(
                element, a, effectiveSeverity, this, positioningStrategy ?: defaultPositioningStrategy
            )
        }
    }
}

class KtDiagnosticFactory2<A, B>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>
) : AbstractKtDiagnosticFactory(name, severity, defaultPositioningStrategy, psiType) {
    override val ktRenderer: KtDiagnosticRenderer = KtDiagnosticWithParameters2Renderer(
        "{0}, {1}",
        KtDiagnosticRenderers.TO_STRING,
        KtDiagnosticRenderers.TO_STRING
    )

    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractKtSourceElement,
        a: A,
        b: B,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        languageVersionSettings: LanguageVersionSettings,
    ): KtDiagnosticWithParameters2<A, B>? {
        val effectiveSeverity = getEffectiveSeverity(languageVersionSettings) ?: return null
        return when (element) {
            is KtPsiSourceElement -> KtPsiDiagnosticWithParameters2(
                element, a, b, effectiveSeverity, this, positioningStrategy ?: defaultPositioningStrategy
            )
            is KtLightSourceElement -> KtLightDiagnosticWithParameters2(
                element,
                a,
                b,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy
            )
            else -> KtOffsetsOnlyDiagnosticWithParameters2(
                element, a, b, effectiveSeverity, this, positioningStrategy ?: defaultPositioningStrategy
            )
        }
    }
}

class KtDiagnosticFactory3<A, B, C>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>
) : AbstractKtDiagnosticFactory(name, severity, defaultPositioningStrategy, psiType) {
    override val ktRenderer: KtDiagnosticRenderer = KtDiagnosticWithParameters3Renderer(
        "{0}, {1}, {2}",
        KtDiagnosticRenderers.TO_STRING,
        KtDiagnosticRenderers.TO_STRING,
        KtDiagnosticRenderers.TO_STRING
    )

    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractKtSourceElement,
        a: A,
        b: B,
        c: C,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        languageVersionSettings: LanguageVersionSettings,
    ): KtDiagnosticWithParameters3<A, B, C>? {
        val effectiveSeverity = getEffectiveSeverity(languageVersionSettings) ?: return null
        return when (element) {
            is KtPsiSourceElement -> KtPsiDiagnosticWithParameters3(
                element, a, b, c, effectiveSeverity, this, positioningStrategy ?: defaultPositioningStrategy
            )
            is KtLightSourceElement -> KtLightDiagnosticWithParameters3(
                element,
                a,
                b,
                c,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy
            )
            else -> KtOffsetsOnlyDiagnosticWithParameters3(
                element, a, b, c, effectiveSeverity, this, positioningStrategy ?: defaultPositioningStrategy
            )
        }
    }
}

class KtDiagnosticFactory4<A, B, C, D>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>
) : AbstractKtDiagnosticFactory(name, severity, defaultPositioningStrategy, psiType) {
    override val ktRenderer: KtDiagnosticRenderer = KtDiagnosticWithParameters4Renderer(
        "{0}, {1}, {2}, {3}",
        KtDiagnosticRenderers.TO_STRING,
        KtDiagnosticRenderers.TO_STRING,
        KtDiagnosticRenderers.TO_STRING,
        KtDiagnosticRenderers.TO_STRING
    )

    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractKtSourceElement,
        a: A,
        b: B,
        c: C,
        d: D,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        languageVersionSettings: LanguageVersionSettings,
    ): KtDiagnosticWithParameters4<A, B, C, D>? {
        val effectiveSeverity = getEffectiveSeverity(languageVersionSettings) ?: return null
        return when (element) {
            is KtPsiSourceElement -> KtPsiDiagnosticWithParameters4(
                element, a, b, c, d, effectiveSeverity, this, positioningStrategy ?: defaultPositioningStrategy
            )
            is KtLightSourceElement -> KtLightDiagnosticWithParameters4(
                element,
                a,
                b,
                c,
                d,
                effectiveSeverity,
                this,
                positioningStrategy ?: defaultPositioningStrategy
            )
            else -> KtOffsetsOnlyDiagnosticWithParameters4(
                element, a, b, c, d, effectiveSeverity, this, positioningStrategy ?: defaultPositioningStrategy
            )
        }
    }
}

// ------------------------------ factories for deprecation ------------------------------

sealed class KtDiagnosticFactoryForDeprecation<F : AbstractKtDiagnosticFactory>(
    val name: String,
    val deprecatingFeature: LanguageFeature,
    val warningFactory: F,
    val errorFactory: F
)

private const val WARNING = "_WARNING"
private const val ERROR = "_ERROR"

class KtDiagnosticFactoryForDeprecation0(
    name: String,
    featureForError: LanguageFeature,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>
) : KtDiagnosticFactoryForDeprecation<KtDiagnosticFactory0>(
    name,
    featureForError,
    KtDiagnosticFactory0("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType),
    KtDiagnosticFactory0("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType)
)

class KtDiagnosticFactoryForDeprecation1<A>(
    name: String,
    featureForError: LanguageFeature,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>
) : KtDiagnosticFactoryForDeprecation<KtDiagnosticFactory1<A>>(
    name,
    featureForError,
    KtDiagnosticFactory1("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType),
    KtDiagnosticFactory1("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType)
)

class KtDiagnosticFactoryForDeprecation2<A, B>(
    name: String,
    featureForError: LanguageFeature,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>
) : KtDiagnosticFactoryForDeprecation<KtDiagnosticFactory2<A, B>>(
    name,
    featureForError,
    KtDiagnosticFactory2("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType),
    KtDiagnosticFactory2("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType)
)

class KtDiagnosticFactoryForDeprecation3<A, B, C>(
    name: String,
    featureForError: LanguageFeature,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>
) : KtDiagnosticFactoryForDeprecation<KtDiagnosticFactory3<A, B, C>>(
    name,
    featureForError,
    KtDiagnosticFactory3("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType),
    KtDiagnosticFactory3("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType)
)

class KtDiagnosticFactoryForDeprecation4<A, B, C, D>(
    name: String,
    featureForError: LanguageFeature,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>
) : KtDiagnosticFactoryForDeprecation<KtDiagnosticFactory4<A, B, C, D>>(
    name,
    featureForError,
    KtDiagnosticFactory4("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType),
    KtDiagnosticFactory4("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType)
)
