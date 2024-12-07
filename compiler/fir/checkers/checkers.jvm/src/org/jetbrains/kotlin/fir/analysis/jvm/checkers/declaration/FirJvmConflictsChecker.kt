/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassLikeChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.java.javaSymbolProvider

object FirJvmConflictsChecker : FirClassLikeChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClassLikeDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val checkRedeclaration = when (declaration) {
            is FirAnonymousObject -> false
            // Java classes are allowed to "redeclare" expect classes. It's called Kotlin-to-Java direct actualization
            is FirRegularClass -> !declaration.isExpect
            // I'd say that even regular typealiases should conflict with Java, but it'd be a breaking change.
            // 'actual typealias' is just more important because it's a redeclaration in the "Kotlin-to-Java direct actualization" feature.
            is FirTypeAlias -> declaration.isActual
        }
        if (!checkRedeclaration) {
            return
        }
        val javaSymbol = context.session.javaSymbolProvider?.getClassLikeSymbolByClassId(declaration.classId) ?: return
        reporter.reportOn(
            declaration.source, FirErrors.CLASSIFIER_REDECLARATION, listOf(declaration.symbol, javaSymbol), context
        )
    }
}
