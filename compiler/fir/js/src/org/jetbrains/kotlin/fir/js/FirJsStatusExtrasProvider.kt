/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.js

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirPlatformStatusProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.platformStatusProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsExport

data class FirJsStatus(
    val isExportedObject: Boolean = false,
)

class FirJsStatusProvider(val session: FirSession) : FirPlatformStatusProvider() {
    private val statusBySymbol = mutableMapOf<FirBasedSymbol<*>, FirJsStatus>()
    private var currentStatus = FirJsStatus()

    override fun <T> withCalculatedStatusOf(declaration: FirDeclaration, block: () -> T): T {
        return withCurrentJsStatus(doCalculateStatusFor(declaration)) { block() }
    }

    private inline fun <T> withCurrentJsStatus(status: FirJsStatus, block: () -> T): T {
        val oldStatus = currentStatus
        return try {
            currentStatus = status
            block()
        } finally {
            currentStatus = oldStatus
        }
    }

    override fun calculateStatusFor(declaration: FirDeclaration) {
        doCalculateStatusFor(declaration)
    }

    private fun doCalculateStatusFor(declaration: FirDeclaration): FirJsStatus {
        return currentStatus.copy(
            isExportedObject = currentStatus.isExportedObject || declaration.hasAnnotation(JsExport, session),
        ).also {
            statusBySymbol[declaration.symbol] = it
        }
    }

    fun getJsStatus(symbol: FirBasedSymbol<*>) = statusBySymbol[symbol] ?: FirJsStatus()
}

val FirSession.jsStatusProvider: FirJsStatusProvider get() = platformStatusProvider as FirJsStatusProvider
