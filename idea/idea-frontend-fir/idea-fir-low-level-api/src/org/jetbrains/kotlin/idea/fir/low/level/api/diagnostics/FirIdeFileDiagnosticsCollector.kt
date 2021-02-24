/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirFile

internal class FirIdeFileDiagnosticsCollector private constructor(
    session: FirSession,
    useExtendedCheckers: Boolean,
) : AbstractFirIdeDiagnosticsCollector(
    session,
    useExtendedCheckers,
) {
    private val result = mutableListOf<FirPsiDiagnostic<*>>()

    override fun onDiagnostic(diagnostic: FirPsiDiagnostic<*>) {
        result += diagnostic
    }

    companion object {
        fun collectForFile(firFile: FirFile, useExtendedCheckers: Boolean): List<FirPsiDiagnostic<*>> =
            FirIdeFileDiagnosticsCollector(firFile.session, useExtendedCheckers).let { collector ->
                collector.collectDiagnostics(firFile)
                collector.result
            }
    }
}
