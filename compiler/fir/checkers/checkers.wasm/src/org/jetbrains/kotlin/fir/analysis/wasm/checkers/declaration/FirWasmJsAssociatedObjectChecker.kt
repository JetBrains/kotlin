/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.ASSOCIATED_OBJECT_INVALID_BINDING
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal

object FirWasmJsAssociatedObjectChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirClass) return
        if (!declaration.symbol.isEffectivelyExternal(context.session)) return

        for (annotationCall in declaration.annotations) {
            val annotationSymbol = annotationCall.annotationTypeRef.coneType.toSymbol(context.session) ?: continue
            if (annotationSymbol.hasAnnotation(StandardClassIds.Annotations.AssociatedObjectKey, context.session)) {
                reporter.reportOn(annotationCall.source, ASSOCIATED_OBJECT_INVALID_BINDING, context)
            }
        }
    }
}
