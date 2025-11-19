/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.closestNonLocalWith
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeObject
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirJsExternalFileChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        val closestNonLocal = context.closestNonLocalWith(declaration) ?: return

        if (closestNonLocal.isNativeObject() || closestNonLocal is FirTypeAliasSymbol || !context.isTopLevel) {
            return
        }

        val targetAnnotations = context.containingFileSymbol?.resolvedAnnotationsWithClassIds?.firstOrNull {
            it.toAnnotationClassId(context.session) in JsStandardClassIds.Annotations.annotationsRequiringExternal
        } ?: return

        reporter.reportOn(
            declaration.source,
            FirWebCommonErrors.NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE,
            targetAnnotations.resolvedType
        )
    }
}
