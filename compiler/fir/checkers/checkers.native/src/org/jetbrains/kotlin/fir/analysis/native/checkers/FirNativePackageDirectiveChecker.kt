/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.KtNodeTypes.REFERENCE_EXPRESSION
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFileChecker
import org.jetbrains.kotlin.fir.analysis.forEachChildOfType
import org.jetbrains.kotlin.fir.analysis.native.checkers.FirNativeIdentifierChecker.checkNameAndReport
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.text

object FirNativePackageDirectiveChecker : FirFileChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFile) {
        declaration.packageDirective.source?.forEachChildOfType(setOf(REFERENCE_EXPRESSION), depth = -1) {
            checkNameAndReport(
                Name.identifier(it.text.toString()),
                it
            )
        }
    }
}
