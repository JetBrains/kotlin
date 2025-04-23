/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.AbstractKtDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderer
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticRendererFactory
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.SessionConfiguration

class FirComposedDiagnosticRendererFactory : DiagnosticRendererFactory, FirSessionComponent {
    private val factories = mutableSetOf<BaseDiagnosticRendererFactory>()

    @SessionConfiguration
    fun registerFactories(factories: List<BaseDiagnosticRendererFactory>) {
        this.factories += factories
    }

    override fun invoke(diagnostic: KtDiagnostic): KtDiagnosticRenderer? {
        val diagnosticFactory = diagnostic.factory
        return factories.firstNotNullOfOrNull {
            it.MAP[diagnosticFactory]
        }
    }

    val allDiagnosticFactories: List<AbstractKtDiagnosticFactory>
        get() = factories.flatMap { it.MAP.factories }
}

val FirSession.diagnosticRendererFactory: FirComposedDiagnosticRendererFactory by FirSession.sessionComponentAccessor()
