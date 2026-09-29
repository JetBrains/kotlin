/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.isNativeObject

/**
 * Reports a top-level 'var' that is imported from a JS module and therefore cannot be written to.
 *
 * Which declarations of a module file are affected depends on how a platform references an import,
 * so the decision is left to [isNonWritableModuleImport].
 */
abstract class FirWebCommonVarInJsModuleFileChecker : FirPropertyChecker(MppCheckerKind.Common) {
    protected abstract val diagnostic: KtDiagnosticFactoryForDeprecation0

    /**
     * Whether [property] is imported from the module declared by the annotations of its containing file
     * and a write to that import does not reach the module.
     */
    context(context: CheckerContext)
    protected abstract fun isNonWritableModuleImport(property: FirProperty): Boolean

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (!declaration.isVar || !context.isTopLevel) return
        // A non-external declaration is already covered by NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE
        // on JS and by JS_MODULE_PROHIBITED_ON_NON_EXTERNAL on Wasm.
        if (!declaration.symbol.isNativeObject(context.session)) return
        if (!isNonWritableModuleImport(declaration)) return

        reporter.reportOn(declaration.source, diagnostic)
    }
}
