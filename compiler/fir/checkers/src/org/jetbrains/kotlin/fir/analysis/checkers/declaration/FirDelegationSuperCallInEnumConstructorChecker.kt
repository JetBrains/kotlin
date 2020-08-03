/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

object FirDelegationSuperCallInEnumConstructorChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirRegularClass || declaration.classKind != ClassKind.ENUM_CLASS) {
            return
        }

        for (it in declaration.declarations) {
            // checking offsets is needed because we
            // still have a real source element ("") here,
            // even if the user hasn't written anything
            if (
                it is FirConstructor && !it.isPrimary &&
                it.delegatedConstructor?.isThis == false &&
                it.delegatedConstructor?.source?.startOffset != it.delegatedConstructor?.source?.endOffset
            ) {
                reporter.report(it.delegatedConstructor?.source)
            }
        }
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let { report(FirErrors.DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR.on(it)) }
    }
}