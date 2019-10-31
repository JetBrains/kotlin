/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.diagnostics.collectors

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.resolve.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.components.AbstractDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.components.DeclarationCheckersDiagnosticComponent
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.components.ErrorNodeDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

abstract class AbstractDiagnosticCollector {
    fun collectDiagnostics(firFile: FirFile): Iterable<ConeDiagnostic> {
        if (!componentsInitialized) {
            throw IllegalStateException("Components are not initialized")
        }
        initializeCollector()
        firFile.accept(visitor)
        return getCollectedDiagnostics()
    }

    protected abstract fun initializeCollector()
    protected abstract fun getCollectedDiagnostics(): Iterable<ConeDiagnostic>
    internal abstract fun runCheck(block: (DiagnosticReporter) -> Unit)

    private val components: MutableList<AbstractDiagnosticCollectorComponent> = mutableListOf()
    private var componentsInitialized = false
    private val visitor = Visitor()

    fun initializeComponents(vararg components: AbstractDiagnosticCollectorComponent) {
        if (componentsInitialized) {
            throw IllegalStateException()
        }
        this.components += components
        componentsInitialized = true
    }

    private inner class Visitor : FirVisitorVoid() {
        private fun <T : FirElement> T.runComponents() {
            components.forEach {
                this.accept(it)
            }
        }

        override fun visitElement(element: FirElement) {
            element.runComponents()
            element.acceptChildren(this)
        }
    }
}

fun AbstractDiagnosticCollector.registerAllComponents() {
    initializeComponents(
        DeclarationCheckersDiagnosticComponent(this),
        ErrorNodeDiagnosticCollectorComponent(this)
    )
}