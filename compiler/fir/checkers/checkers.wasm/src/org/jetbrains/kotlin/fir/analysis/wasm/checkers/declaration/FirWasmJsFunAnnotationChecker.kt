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
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.reportIncompatibleAnnotations
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.name.WasmStandardClassIds
import org.jetbrains.kotlin.name.WebCommonStandardClassIds

object FirWasmJsFunAnnotationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    // The function is implemented by the given JavaScript code, so it is not looked up in JavaScript.
    // '@WasmImport' takes precedence over '@JsFun' and is reported by its own checker.
    private val incompatibleAnnotations = setOf(
        WebCommonStandardClassIds.Annotations.JsModule,
        WebCommonStandardClassIds.Annotations.JsQualifier,
        WebCommonStandardClassIds.Annotations.JsName,
    )

    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        val annotation: FirAnnotation =
            declaration.annotations.getAnnotationByClassId(WasmStandardClassIds.Annotations.JsFun, context.session) ?: return

        if (!context.isTopLevel || !declaration.symbol.isEffectivelyExternal(context.session)) {
            reporter.reportOn(annotation.source, FirWasmErrors.WRONG_JS_FUN_TARGET)
        }

        reportIncompatibleAnnotations(declaration, incompatibleAnnotations, FirWasmErrors.INCOMPATIBLE_ANNOTATION_WITH_JS_FUN)
    }
}
