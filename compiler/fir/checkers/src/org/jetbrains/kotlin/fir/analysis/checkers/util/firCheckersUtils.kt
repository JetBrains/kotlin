/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.util

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitor

fun FirElement.checkChildrenWithCustomVisitor(
    parentContext: CheckerContext,
    visitorVoid: FirVisitor<Unit, CheckerContext>
) {
    val collectingVisitor = object : AbstractDiagnosticCollectorVisitor(parentContext as PersistentCheckerContext) {
        override fun checkElement(element: FirElement) {
            element.accept(visitorVoid, context)
        }
    }
    this.accept(collectingVisitor, null)
}
