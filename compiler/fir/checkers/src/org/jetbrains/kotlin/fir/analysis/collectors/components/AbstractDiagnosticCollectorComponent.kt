/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class AbstractDiagnosticCollectorComponent(private val collector: AbstractDiagnosticCollector) : FirVisitor<Unit, CheckerContext>() {
    protected val session: FirSession = collector.session

    override fun visitElement(element: FirElement, data: CheckerContext) {}

    protected val reporter: DiagnosticReporter
        get() = collector.reporter
}
