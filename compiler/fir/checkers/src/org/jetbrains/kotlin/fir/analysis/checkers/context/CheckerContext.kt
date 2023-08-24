/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.context

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirInlineDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.ImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator

/**
 * This class is assumed to be read-only (all the modifications are assumed to happen on CheckerContextForProvider side)
 */
abstract class CheckerContext : DiagnosticContext {
    // Services
    abstract val sessionHolder: SessionHolder
    abstract val returnTypeCalculator: ReturnTypeCalculator

    // Context
    abstract val implicitReceiverStack: ImplicitReceiverStack
    abstract val containingDeclarations: List<FirDeclaration>

    /** Contains qualified access, annotation call, delegated constructor call, and variable assignment. */
    abstract val callsOrAssignments: List<FirStatement>
    abstract val getClassCalls: List<FirGetClassCall>
    abstract val annotationContainers: List<FirAnnotationContainer>
    abstract val containingElements: List<FirElement>
    abstract val isContractBody: Boolean
    abstract val inlineFunctionBodyContext: FirInlineDeclarationChecker.InlineFunctionBodyContext?

    // Suppress
    abstract val suppressedDiagnostics: Set<String>
    abstract val allInfosSuppressed: Boolean
    abstract val allWarningsSuppressed: Boolean
    abstract val allErrorsSuppressed: Boolean

    val session: FirSession
        get() = sessionHolder.session

    val scopeSession: ScopeSession
        get() = sessionHolder.scopeSession

    override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean {
        val factory = diagnostic.factory
        val name = factory.name

        if (name == FirErrors.ERROR_SUPPRESSION.name) {
            // Can't suppress warning about suppressed error
            return false
        }

        val suppressedByAll = when (factory.severity) {
            Severity.INFO -> allInfosSuppressed
            Severity.WARNING -> allWarningsSuppressed
            Severity.ERROR -> allErrorsSuppressed
        }

        return suppressedByAll || name in suppressedDiagnostics
    }

    override val languageVersionSettings: LanguageVersionSettings
        get() = session.languageVersionSettings

    abstract val containingFile: FirFile?

    override val containingFilePath: String?
        get() = containingFile?.sourceFile?.path
}

/**
 * Returns the closest to the end of context.containingDeclarations instance of type [T] or null if no such item could be found.
 * By specifying [check] you can filter which exact declaration should be found
 * E.g., property accessor is either getter or setter, but a type-based search could return, say,
 *   the closest setter, while we want to keep searching for a getter.
 */

inline fun <reified T : FirElement> CheckerContext.findClosest(check: (T) -> Boolean = { true }): T? {
    for (it in containingDeclarations.asReversed()) {
        return (it as? T)?.takeIf(check) ?: continue
    }

    return null
}
