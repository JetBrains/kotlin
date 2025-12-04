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

    fun registerFactories(factories: List<BaseDiagnosticRendererFactory>) {
        this.factories += factories
    }

    val allDiagnosticFactories: List<AbstractKtDiagnosticFactory>
        get() = factories.flatMap { it.MAP.factories }
}
