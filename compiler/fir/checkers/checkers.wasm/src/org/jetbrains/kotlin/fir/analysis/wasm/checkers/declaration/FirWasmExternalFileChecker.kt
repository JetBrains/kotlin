/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.WasmStandardClassIds

object FirWasmExternalFileChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.isTopLevel || declaration.symbol.isEffectivelyExternal(context.session)) {
            return
        }

        val targetAnnotations = context.containingFile
            ?.annotations
            ?.firstOrNull { it.toAnnotationClassId(context.session) in WasmStandardClassIds.Annotations.annotationsRequiringExternal }

        if (targetAnnotations != null) {
            reporter.reportOn(
                declaration.source,
                FirWasmErrors.NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE,
                targetAnnotations.resolvedType,
                context
            )
        }
    }
}
