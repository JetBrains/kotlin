/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.analysis.extensions.additionalCheckers
import org.jetbrains.kotlin.fir.extensions.extensionService

object FirDiagnosticsCollector {
    fun create(session: FirSession): AbstractDiagnosticCollector {
        session.registerAdditionalCheckers()
        val collector = SimpleDiagnosticsCollector(session)
        collector.registerAllComponents()
        return collector
    }

    // Use in CLI compiler
    @Suppress("unused")
    fun createParallel(session: FirSession): AbstractDiagnosticCollector {
        session.registerAdditionalCheckers()
        val collector = ParallelDiagnosticsCollector(session, numberOfThreads = 4)
        collector.registerAllComponents()
        return collector
    }

    private fun FirSession.registerAdditionalCheckers() {
        extensionService.additionalCheckers.forEach(checkersComponent::register)
    }
}