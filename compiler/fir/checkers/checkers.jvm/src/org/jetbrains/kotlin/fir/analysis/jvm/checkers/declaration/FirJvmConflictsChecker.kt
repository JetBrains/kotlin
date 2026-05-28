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
import org.jetbrains.kotlin.fir.java.getJavaClassLikeSymbolByClassId

object FirJvmConflictsChecker : FirClassLikeChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClassLikeDeclaration) {
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
        // Stage 2 §6.1 indirect-caller audit (see compiler/java-direct/implDocs/DIRECT_INJECTION_STAGE_1_2026_05_20.md §6.1):
        // decouple from `JavaSymbolProvider` directly via a thin Java-targeted lookup helper. Today it wraps
        // `javaSymbolProvider`; after §6.2/§6.3 it transparently picks up binary `FirDeclarationOrigin.Java.Library`
        // results emitted by `JvmClassFileBasedSymbolProvider`. The composite `session.symbolProvider` cannot be used
        // here because its `firstNotNullOfOrNull` strategy hides the Java side when a Kotlin class shares the same
        // `classId` (which is exactly the redeclaration case this checker detects).
        val javaSymbol = context.session.getJavaClassLikeSymbolByClassId(declaration.classId) ?: return
        reporter.reportOn(
            declaration.source, FirErrors.CLASSIFIER_REDECLARATION, listOf(declaration.symbol, javaSymbol)
        )
    }
}
