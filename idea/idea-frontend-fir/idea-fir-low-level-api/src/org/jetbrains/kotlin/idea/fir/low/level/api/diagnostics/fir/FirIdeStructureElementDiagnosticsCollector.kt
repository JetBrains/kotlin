/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.CheckerRunningDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.AbstractFirIdeDiagnosticsCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.util.addValueFor

internal class FirIdeStructureElementDiagnosticsCollector(
    session: FirSession,
    createVisitor: (components: List<AbstractDiagnosticCollectorComponent>) -> CheckerRunningDiagnosticCollectorVisitor,
    useExtendedCheckers: Boolean,
) : AbstractFirIdeDiagnosticsCollector(
    session,
    useExtendedCheckers,
) {
    override val visitor: CheckerRunningDiagnosticCollectorVisitor = createVisitor(components)

    val result = mutableMapOf<PsiElement, MutableList<FirPsiDiagnostic<*>>>()

    override fun onDiagnostic(diagnostic: FirPsiDiagnostic<*>) {
        result.addValueFor(diagnostic.psiElement, diagnostic)
    }
}