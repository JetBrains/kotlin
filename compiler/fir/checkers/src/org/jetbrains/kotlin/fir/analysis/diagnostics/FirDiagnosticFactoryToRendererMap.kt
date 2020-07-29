/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticRenderer

class FirDiagnosticFactoryToRendererMap(val name: String) {
    private val diagnosticsMap: MutableMap<AbstractFirDiagnosticFactory<*, *>, DiagnosticRenderer<*>> = mutableMapOf()
    val psiDiagnosticMap: DiagnosticFactoryToRendererMap =
        DiagnosticFactoryToRendererMap()

    operator fun get(factory: AbstractFirDiagnosticFactory<*, *>): DiagnosticRenderer<*>? = diagnosticsMap[factory]

    fun put(factory: FirDiagnosticFactory0<*, *>, message: String) {
        psiDiagnosticMap.put(factory.psiDiagnosticFactory, message)
        putToFirMap(factory)
    }

    fun <A : Any> put(
        factory: FirDiagnosticFactory1<*, *, A>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?
    ) {
        psiDiagnosticMap.put(factory.psiDiagnosticFactory, message, rendererA)
        putToFirMap(factory)
    }

    fun <A : Any, B : Any> put(
        factory: FirDiagnosticFactory2<*, *, A, B>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?
    ) {
        psiDiagnosticMap.put(factory.psiDiagnosticFactory, message, rendererA, rendererB)
        putToFirMap(factory)
    }

    fun <A : Any, B : Any, C : Any> put(
        factory: FirDiagnosticFactory3<*, *, A, B, C>,
        message: String,
        rendererA: DiagnosticParameterRenderer<A>?,
        rendererB: DiagnosticParameterRenderer<B>?,
        rendererC: DiagnosticParameterRenderer<C>?
    ) {
        psiDiagnosticMap.put(factory.psiDiagnosticFactory, message, rendererA, rendererB, rendererC)
        putToFirMap(factory)
    }

    private fun putToFirMap(factory: AbstractFirDiagnosticFactory<*, *>) {
        psiDiagnosticMap[factory.psiDiagnosticFactory]?.let {
            diagnosticsMap[factory] = it
        }
    }
}
