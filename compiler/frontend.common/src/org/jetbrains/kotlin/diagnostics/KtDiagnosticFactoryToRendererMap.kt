/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer

class KtDiagnosticFactoryToRendererMap(val name: String) {
    private val renderersMap: MutableMap<AbstractKtDiagnosticFactory, KtDiagnosticRenderer> = mutableMapOf()

    operator fun get(factory: AbstractKtDiagnosticFactory): KtDiagnosticRenderer? = renderersMap[factory]

    fun containsKey(factory: AbstractKtDiagnosticFactory): Boolean {
        return renderersMap.containsKey(factory)
    }

    fun put(factory: KtDiagnosticFactory0, message: String) {
        put(factory, SimpleKtDiagnosticRenderer(message))
    }

    fun <A> put(
        factory: KtDiagnosticFactory1<A>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?
    ) {
        put(factory, KtDiagnosticWithParameters1Renderer(message, rendererA))
    }

    fun <A, B> put(
        factory: KtDiagnosticFactory2<A, B>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?
    ) {
        put(factory, KtDiagnosticWithParameters2Renderer(message, rendererA, rendererB))
    }

    fun <A, B, C> put(
        factory: KtDiagnosticFactory3<A, B, C>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?,
        rendererC: DiagnosticParameterRenderer<C>?
    ) {
        put(factory, KtDiagnosticWithParameters3Renderer(message, rendererA, rendererB, rendererC))
    }

    fun <A, B, C, D> put(
        factory: KtDiagnosticFactory4<A, B, C, D>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?,
        rendererC: DiagnosticParameterRenderer<C>?,
        rendererD: DiagnosticParameterRenderer<D>?
    ) {
        put(factory, KtDiagnosticWithParameters4Renderer(message, rendererA, rendererB, rendererC, rendererD))
    }

    fun put(factory: KtDiagnosticFactoryForDeprecation0, message: String) {
        put(factory.errorFactory, SimpleKtDiagnosticRenderer(message))
        put(factory.warningFactory, SimpleKtDiagnosticRenderer(factory.warningMessage(message)))
    }

    fun <A> put(
        factory: KtDiagnosticFactoryForDeprecation1<A>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?
    ) {
        put(factory.errorFactory, KtDiagnosticWithParameters1Renderer(message, rendererA))
        put(factory.warningFactory, KtDiagnosticWithParameters1Renderer(factory.warningMessage(message), rendererA))
    }

    fun <A, B> put(
        factory: KtDiagnosticFactoryForDeprecation2<A, B>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?
    ) {
        put(factory.errorFactory, KtDiagnosticWithParameters2Renderer(message, rendererA, rendererB))
        put(factory.warningFactory, KtDiagnosticWithParameters2Renderer(factory.warningMessage(message), rendererA, rendererB))
    }

    fun <A, B, C> put(
        factory: KtDiagnosticFactoryForDeprecation3<A, B, C>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?,
        rendererC: DiagnosticParameterRenderer<C>?
    ) {
        put(factory.errorFactory, KtDiagnosticWithParameters3Renderer(message, rendererA, rendererB, rendererC))
        put(factory.warningFactory, KtDiagnosticWithParameters3Renderer(factory.warningMessage(message), rendererA, rendererB, rendererC))
    }

    fun <A, B, C, D> put(
        factory: KtDiagnosticFactoryForDeprecation4<A, B, C, D>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?,
        rendererC: DiagnosticParameterRenderer<C>?,
        rendererD: DiagnosticParameterRenderer<D>?
    ) {
        put(factory.errorFactory, KtDiagnosticWithParameters4Renderer(message, rendererA, rendererB, rendererC, rendererD))
        put(factory.warningFactory, KtDiagnosticWithParameters4Renderer(factory.warningMessage(message), rendererA, rendererB, rendererC, rendererD))
    }

    private fun put(factory: AbstractKtDiagnosticFactory, renderer: KtDiagnosticRenderer) {
        if (renderersMap.containsKey(factory)) {
            throw IllegalStateException("Diagnostic renderer is already initialized for $factory")
        }
        renderersMap[factory] = renderer
    }

    private fun KtDiagnosticFactoryForDeprecation<*>.warningMessage(errorMessage: String): String {
        return buildString {
            append(errorMessage)
            if (!errorMessage.endsWith(".")) append(".")
            append(" This will become an error")
            val sinceVersion = deprecatingFeature.sinceVersion
            if (sinceVersion != null) {
                append(" in Kotlin ")
                append(sinceVersion.versionString)
            } else {
                append(" in a future release")
            }
            append(".")
        }
    }
}
