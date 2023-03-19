/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContextForProvider
import org.jetbrains.kotlin.fir.analysis.collectors.CheckerRunningDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorComponents
import org.jetbrains.kotlin.fir.declarations.FirDeclaration

internal open class LLFirDiagnosticVisitor(
    context: CheckerContextForProvider,
    components: DiagnosticCollectorComponents,
) : CheckerRunningDiagnosticCollectorVisitor(context, components) {
    private val beforeElementDiagnosticCollectionHandler = context.session.beforeElementDiagnosticCollectionHandler

    protected var useRegularComponents = true

    override fun visitNestedElements(element: FirElement) {
        if (element is FirDeclaration) {
            beforeElementDiagnosticCollectionHandler?.beforeGoingNestedDeclaration(element, context)
        }
        super.visitNestedElements(element)
    }

    override fun checkElement(element: FirElement) {
        if (useRegularComponents) {
            beforeElementDiagnosticCollectionHandler?.beforeCollectingForElement(element)
            components.regularComponents.forEach {
                checkCanceled()
                element.accept(it, context)
            }
        }
        checkCanceled()
        element.accept(components.reportCommitter, context)
    }
}
