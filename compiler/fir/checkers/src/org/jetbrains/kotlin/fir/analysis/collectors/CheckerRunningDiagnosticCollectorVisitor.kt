/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.components.AbstractDiagnosticCollectorComponent

open class CheckerRunningDiagnosticCollectorVisitor(
    context: PersistentCheckerContext,
    protected val components: List<AbstractDiagnosticCollectorComponent>
) : AbstractDiagnosticCollectorVisitor(context) {

    override fun checkElement(element: FirElement) {
        components.forEach {
            element.accept(it, context)
        }
    }
}
