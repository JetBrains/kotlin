/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirMemberDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.lexer.KtTokens

object FirJvmExternalDeclarationChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isExternal) return
        val source = declaration.source ?: return
        if (source.kind is FirFakeSourceElementKind) return

        // WRONG_MODIFIER_TARGET on external constructor is intentionally NOT covered in this checker.
        if (declaration !is FirFunction<*>) {
            val target = when (declaration) {
                is FirProperty -> "property"
                is FirRegularClass -> "class"
                else -> "non-function declaration"
            }
            val externalModifier = declaration.getModifier(KtTokens.EXTERNAL_KEYWORD)
            externalModifier?.let {
                reporter.reportOn(it.source, FirErrors.WRONG_MODIFIER_TARGET, it.token, target, context)
            }
        }

        // TODO: Implement checkers for these JVM-specific errors (see ExternalFunChecker in FE1.0):
        // - EXTERNAL_DECLARATION_IN_INTERFACE
        // - EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT
        // - EXTERNAL_DECLARATION_CANNOT_HAVE_BODY
        // - EXTERNAL_DECLARATION_CANNOT_BE_INLINED
    }
}
