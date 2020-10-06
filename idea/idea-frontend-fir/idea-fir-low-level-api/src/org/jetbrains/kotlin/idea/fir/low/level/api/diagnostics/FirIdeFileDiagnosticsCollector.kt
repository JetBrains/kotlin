/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile

internal class FirIdeFileDiagnosticsCollector private constructor(
    session: FirSession,
) : AbstractFirIdeDiagnosticsCollector(
    session,
) {
    private val result = mutableListOf<Diagnostic>()

    override fun onDiagnostic(diagnostic: Diagnostic) {
        result += diagnostic
    }

    companion object {
        fun collectForFile(firFile: FirFile): List<Diagnostic> =
            FirIdeFileDiagnosticsCollector(firFile.session).let { collector ->
                collector.collectDiagnostics(firFile)
                collector.result
            }
    }
}
