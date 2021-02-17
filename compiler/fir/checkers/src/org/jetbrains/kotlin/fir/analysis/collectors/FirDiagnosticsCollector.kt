/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.ScopeSession

object FirDiagnosticsCollector {
    fun create(session: FirSession, scopeSession: ScopeSession): SimpleDiagnosticsCollector {
        val collector = SimpleDiagnosticsCollector(session, scopeSession)
        collector.registerAllComponents()
        return collector
    }
}
