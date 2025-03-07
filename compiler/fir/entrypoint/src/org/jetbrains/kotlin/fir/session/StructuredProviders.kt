/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider

class StructuredProviders(
    // source, generated, IC
    val sourceProviders: List<FirSymbolProvider>,
    // klib/jar deserialized providers
    val librariesProviders: List<FirSymbolProvider>,
    // buitlins, etc
    val sharedProvider: FirSymbolProvider
) : FirSessionComponent {
    fun withSourceProviders(sourceProviders: List<FirSymbolProvider>): StructuredProviders = StructuredProviders(
        this.sourceProviders + sourceProviders,
        librariesProviders,
        sharedProvider,
    )

    operator fun plus(other: StructuredProviders): StructuredProviders = StructuredProviders(
        sourceProviders + other.sourceProviders,
        librariesProviders + other.librariesProviders,
        sharedProvider,
    ).also {
        check(sharedProvider == other.sharedProvider) {
            "Shared providers should be the same"
        }
    }
}

val FirSession.structuredProviders: StructuredProviders by FirSession.sessionComponentAccessor()
