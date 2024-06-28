/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContextForProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile

open class CheckerRunningDiagnosticCollectorVisitor(
    context: CheckerContextForProvider,
    protected val components: DiagnosticCollectorComponents
) : AbstractDiagnosticCollectorVisitor(context) {

    override fun checkSettings() {
        components.regularComponents.forEach { it.checkSettings(context) }
    }

    override fun checkElement(element: FirElement) {
        components.regularComponents.forEach {
            element.accept(it, context)
        }
        element.accept(components.reportCommitter, context)
    }

    override fun onDeclarationExit(declaration: FirDeclaration) {
        if (declaration !is FirFile) return
        components.reportCommitter.endOfFile(declaration)
    }
}
