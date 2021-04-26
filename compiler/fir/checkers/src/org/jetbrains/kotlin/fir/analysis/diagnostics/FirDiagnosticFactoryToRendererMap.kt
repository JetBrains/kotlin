/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer

class FirDiagnosticFactoryToRendererMap(val name: String) {
    private val renderersMap: MutableMap<AbstractFirDiagnosticFactory<*, *>, FirDiagnosticRenderer<*>> = mutableMapOf()
    val psiDiagnosticMap: DiagnosticFactoryToRendererMap = DiagnosticFactoryToRendererMap()

    operator fun get(factory: AbstractFirDiagnosticFactory<*, *>): FirDiagnosticRenderer<*>? = renderersMap[factory]

    fun put(factory: FirDiagnosticFactory0<*>, message: String) {
        put(factory, SimpleFirDiagnosticRenderer(message))
    }

    fun <A : Any> put(
        factory: FirDiagnosticFactory1<*, A>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?
    ) {
        put(factory, FirDiagnosticWithParameters1Renderer(message, rendererA))
    }

    fun <A : Any, B : Any> put(
        factory: FirDiagnosticFactory2<*, A, B>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?
    ) {
        put(factory, FirDiagnosticWithParameters2Renderer(message, rendererA, rendererB))
    }

    fun <A : Any, B : Any, C : Any> put(
        factory: FirDiagnosticFactory3<*, A, B, C>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?,
        rendererC: DiagnosticParameterRenderer<C>?
    ) {
        put(factory, FirDiagnosticWithParameters3Renderer(message, rendererA, rendererB, rendererC))
    }

    private fun put(factory: AbstractFirDiagnosticFactory<*, *>, renderer: FirDiagnosticRenderer<*>) {
        renderersMap[factory] = renderer
        psiDiagnosticMap.put(factory, renderer)
    }
}
