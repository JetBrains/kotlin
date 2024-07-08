/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContextForProvider
import org.jetbrains.kotlin.fir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.util.PrivateForInline

open class CheckerRunningDiagnosticCollectorVisitor(
    context: CheckerContextForProvider,
    protected val components: DiagnosticCollectorComponents
) : AbstractDiagnosticCollectorVisitor(context) {

    override fun checkSettings() {
        components.commonComponents.forEach { it.checkSettings(context) }
        components.expectComponents.forEach { it.checkSettings(context) }
        components.regularComponents.forEach { it.checkSettings(context) }
    }

    override fun checkElement(element: FirElement) {
        forEachComponentWithProperSession {
            element.accept(it, context)
        }
        element.accept(components.reportCommitter, context)
    }

    protected inline fun forEachComponentWithProperSession(block: (AbstractDiagnosticCollectorComponent) -> Unit) {
        if (components.commonComponents.isNotEmpty()) {
            withSessionHolderIfNotNullOrSkip(components.declarationSiteSessionHolder) {
                components.commonComponents.forEach(block)
            }
        }

        if (components.expectComponents.isNotEmpty()) {
            withDeclarationSiteSessionIfNeeded {
                components.expectComponents.forEach(block)
            }
        }

        components.regularComponents.forEach(block)
    }

    protected inline fun withDeclarationSiteSessionIfNeeded(block: () -> Unit) {
        return when (context.shouldUseDeclarationSiteSession) {
            false -> block()
            true -> withSessionHolderIfNotNullOrSkip(components.declarationSiteSessionHolder, block)
        }
    }

    protected inline fun withSessionHolderIfNotNullOrSkip(sessionHolder: SessionHolder?, block: () -> Unit) {
        if (sessionHolder != null) {
            withSessionHolder(sessionHolder, block)
        }
    }

    @OptIn(PrivateForInline::class)
    protected inline fun <T> withSessionHolder(sessionHolder: SessionHolder, block: () -> T): T {
        val oldSessionHolder = context.sessionHolder
        return try {
            context = context.setSessionHolder(sessionHolder)
            block()
        } finally {
            context = context.setSessionHolder(oldSessionHolder)
        }
    }

    override fun onDeclarationExit(declaration: FirDeclaration) {
        if (declaration !is FirFile) return
        components.reportCommitter.endOfFile(declaration)
    }
}
