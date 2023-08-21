/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.closestNonLocalWith
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeObject
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirJsExternalFileChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val closestNonLocal = context.closestNonLocalWith(declaration)?.symbol ?: return

        if (closestNonLocal.isNativeObject(context) || !context.isTopLevel) {
            return
        }

        val targetAnnotations = context.containingFile
            ?.annotations
            ?.firstOrNull { it.toAnnotationClassId(context.session) in JsStandardClassIds.Annotations.annotationsRequiringExternal }

        if (targetAnnotations != null) {
            reporter.reportOn(
                declaration.source,
                FirJsErrors.NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE,
                targetAnnotations.resolvedType,
                context
            )
        }
    }
}
