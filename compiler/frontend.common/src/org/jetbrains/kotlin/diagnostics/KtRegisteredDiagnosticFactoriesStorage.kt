/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory

/**
 * Contains all diagnostic factories that could be used in the current compilation
 */
class KtRegisteredDiagnosticFactoriesStorage {
    private val factories = mutableSetOf<BaseDiagnosticRendererFactory>()

    fun registerDiagnosticContainers(vararg containers: KtDiagnosticsContainer) {
        registerDiagnosticContainers(containers.toList())
    }

    fun registerDiagnosticContainers(containers: List<KtDiagnosticsContainer>) {
        this.factories += containers.map { it.getRendererFactory() }
    }

    val allDiagnosticFactories: List<AbstractKtDiagnosticFactory>
        get() = factories.flatMap { it.MAP.factories }
}
