/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.WarningLevel
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import kotlin.reflect.KClass

@RequiresOptIn("Please use DiagnosticReporter.reportOn method if possible")
annotation class InternalDiagnosticFactoryMethod

sealed class AbstractKtDiagnosticFactory(
    val name: String,
    val severity: Severity,
    val rendererFactory: BaseDiagnosticRendererFactory
) {
    val ktRenderer: KtDiagnosticRenderer
        get() = rendererFactory.MAP[this]
            ?: error("Renderer is not found for factory $this inside ${rendererFactory.MAP.name} renderer map")

    fun getEffectiveSeverity(languageVersionSettings: LanguageVersionSettings): Severity? {
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

class KtSourcelessDiagnosticFactory(
    name: String,
    severity: Severity,
    rendererFactory: BaseDiagnosticRendererFactory,
) : AbstractKtDiagnosticFactory(name, severity, rendererFactory) {
    fun create(message: String, location: CompilerMessageSourceLocation?, context: DiagnosticBaseContext): KtDiagnosticWithoutSource? {
        val effectiveSeverity = getEffectiveSeverity(context.languageVersionSettings) ?: return null
        return KtDiagnosticWithoutSource(message, location, effectiveSeverity, this, context)
    }
}

sealed class KtDiagnosticFactoryN(
    name: String,
    severity: Severity,
    val defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    val psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory
) : AbstractKtDiagnosticFactory(name, severity, rendererFactory)

class KtDiagnosticFactory0(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : KtDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractKtSourceElement,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        context: DiagnosticBaseContext,
    ): KtSimpleDiagnostic? {
        val effectiveSeverity = getEffectiveSeverity(context.languageVersionSettings) ?: return null
        return context.extraSourceToDiagnosticInstanceMapper?.createDiagnostic0(
            element, effectiveSeverity, this, positioningStrategy ?: defaultPositioningStrategy, context
        ) ?: KtRegularSimpleDiagnostic(
            element,
            effectiveSeverity,
            this,
            positioningStrategy ?: defaultPositioningStrategy,
            context,
        )
    }
}

class KtDiagnosticFactory1<A>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : KtDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractKtSourceElement,
        a: A,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        context: DiagnosticBaseContext,
    ): KtDiagnosticWithParameters1<A>? {
        val effectiveSeverity = getEffectiveSeverity(context.languageVersionSettings) ?: return null
        return context.extraSourceToDiagnosticInstanceMapper?.createDiagnostic1(
            element, effectiveSeverity, this, a, positioningStrategy ?: defaultPositioningStrategy, context
        ) ?: KtRegularDiagnosticWithParameters1(
            element,
            a,
            effectiveSeverity,
            this,
            positioningStrategy ?: defaultPositioningStrategy,
            context,
        )
    }
}

class KtDiagnosticFactory2<A, B>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : KtDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractKtSourceElement,
        a: A,
        b: B,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        context: DiagnosticBaseContext,
    ): KtDiagnosticWithParameters2<A, B>? {
        val effectiveSeverity = getEffectiveSeverity(context.languageVersionSettings) ?: return null
        return context.extraSourceToDiagnosticInstanceMapper?.createDiagnostic2(
            element, effectiveSeverity, this, a, b, positioningStrategy ?: defaultPositioningStrategy, context
        ) ?: KtRegularDiagnosticWithParameters2(
            element,
            a,
            b,
            effectiveSeverity,
            this,
            positioningStrategy ?: defaultPositioningStrategy,
            context,
        )
    }
}

class KtDiagnosticFactory3<A, B, C>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : KtDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractKtSourceElement,
        a: A,
        b: B,
        c: C,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        context: DiagnosticBaseContext,
    ): KtDiagnosticWithParameters3<A, B, C>? {
        val effectiveSeverity = getEffectiveSeverity(context.languageVersionSettings) ?: return null
        return context.extraSourceToDiagnosticInstanceMapper?.createDiagnostic3(
            element, effectiveSeverity, this, a, b, c, positioningStrategy ?: defaultPositioningStrategy, context
        ) ?: KtRegularDiagnosticWithParameters3(
            element,
            a,
            b,
            c,
            effectiveSeverity,
            this,
            positioningStrategy ?: defaultPositioningStrategy,
            context,
        )
    }
}

class KtDiagnosticFactory4<A, B, C, D>(
    name: String,
    severity: Severity,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : KtDiagnosticFactoryN(name, severity, defaultPositioningStrategy, psiType, rendererFactory) {
    @InternalDiagnosticFactoryMethod
    fun on(
        element: AbstractKtSourceElement,
        a: A,
        b: B,
        c: C,
        d: D,
        positioningStrategy: AbstractSourceElementPositioningStrategy?,
        context: DiagnosticBaseContext,
    ): KtDiagnosticWithParameters4<A, B, C, D>? {
        val effectiveSeverity = getEffectiveSeverity(context.languageVersionSettings) ?: return null
        return context.extraSourceToDiagnosticInstanceMapper?.createDiagnostic4(
            element, effectiveSeverity, this, a, b, c, d, positioningStrategy ?: defaultPositioningStrategy, context
        ) ?: KtRegularDiagnosticWithParameters4(
            element,
            a,
            b,
            c,
            d,
            effectiveSeverity,
            this,
            positioningStrategy ?: defaultPositioningStrategy,
            context,
        )
    }
}

// ------------------------------ factories for deprecation ------------------------------

sealed class KtDiagnosticFactoryForDeprecation<F : KtDiagnosticFactoryN>(
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
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : KtDiagnosticFactoryForDeprecation<KtDiagnosticFactory0>(
    name,
    featureForError,
    KtDiagnosticFactory0("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType, rendererFactory),
    KtDiagnosticFactory0("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType, rendererFactory)
)

class KtDiagnosticFactoryForDeprecation1<A>(
    name: String,
    featureForError: LanguageFeature,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : KtDiagnosticFactoryForDeprecation<KtDiagnosticFactory1<A>>(
    name,
    featureForError,
    KtDiagnosticFactory1("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType, rendererFactory),
    KtDiagnosticFactory1("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType, rendererFactory)
)

class KtDiagnosticFactoryForDeprecation2<A, B>(
    name: String,
    featureForError: LanguageFeature,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : KtDiagnosticFactoryForDeprecation<KtDiagnosticFactory2<A, B>>(
    name,
    featureForError,
    KtDiagnosticFactory2("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType, rendererFactory),
    KtDiagnosticFactory2("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType, rendererFactory)
)

class KtDiagnosticFactoryForDeprecation3<A, B, C>(
    name: String,
    featureForError: LanguageFeature,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : KtDiagnosticFactoryForDeprecation<KtDiagnosticFactory3<A, B, C>>(
    name,
    featureForError,
    KtDiagnosticFactory3("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType, rendererFactory),
    KtDiagnosticFactory3("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType, rendererFactory)
)

class KtDiagnosticFactoryForDeprecation4<A, B, C, D>(
    name: String,
    featureForError: LanguageFeature,
    defaultPositioningStrategy: AbstractSourceElementPositioningStrategy,
    psiType: KClass<*>,
    rendererFactory: BaseDiagnosticRendererFactory,
) : KtDiagnosticFactoryForDeprecation<KtDiagnosticFactory4<A, B, C, D>>(
    name,
    featureForError,
    KtDiagnosticFactory4("$name$WARNING", Severity.WARNING, defaultPositioningStrategy, psiType, rendererFactory),
    KtDiagnosticFactory4("$name$ERROR", Severity.ERROR, defaultPositioningStrategy, psiType, rendererFactory)
)
