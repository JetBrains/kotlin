/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.CheckerRunningDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.util.checkCanceled

internal open class FirIdeDiagnosticVisitor(
    context: PersistentCheckerContext,
    components: List<AbstractDiagnosticCollectorComponent>
) : CheckerRunningDiagnosticCollectorVisitor(context, components) {
    override fun beforeRunningSingleComponentOnElement(element: FirElement) {
        checkCanceled()
    }

    override fun goToNestedElements(element: FirElement) {
        if (element is FirDeclaration) {
            session.beforeElementDiagnosticCollectionHandler?.beforeGoingNestedDeclaration(element, context)
        }
        super.goToNestedElements(element)
    }

    override fun beforeRunningAllComponentsOnElement(element: FirElement) {
        session.beforeElementDiagnosticCollectionHandler?.beforeCollectingForElement(element)
    }
}