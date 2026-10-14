/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.reportIncompatibleAnnotations
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.WasmStandardClassIds
import org.jetbrains.kotlin.name.WebCommonStandardClassIds

object FirWasmImportAnnotationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    // The function is imported from the given Wasm module, so it is neither looked up in JavaScript
    // nor implemented by the JavaScript code of '@JsFun'.
    private val incompatibleAnnotations = setOf(
        WebCommonStandardClassIds.Annotations.JsModule,
        WebCommonStandardClassIds.Annotations.JsQualifier,
        WebCommonStandardClassIds.Annotations.JsName,
        WasmStandardClassIds.Annotations.JsFun,
    )

    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        val annotation: FirAnnotation =
            declaration.annotations.getAnnotationByClassId(WasmStandardClassIds.Annotations.WasmImport, context.session) ?: return

        if (!context.isTopLevel) {
            reporter.reportOn(annotation.source, FirWasmErrors.NESTED_WASM_IMPORT)
        }

        if (!declaration.symbol.isEffectivelyExternal(context.session)) {
            reporter.reportOn(annotation.source, FirWasmErrors.WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION)
        }

        reportIncompatibleAnnotations(declaration, incompatibleAnnotations, FirWasmErrors.INCOMPATIBLE_ANNOTATION_WITH_WASM_IMPORT)

        if (declaration is FirFunction) {
            checkWasmInteropSignature(declaration)
        }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkWasmInteropSignature(declaration: FirFunction) {
    for (parameter in declaration.valueParameters) {
        val type = parameter.returnTypeRef.coneType
        if (parameter.defaultValue != null) {
            reporter.reportOn(parameter.source, FirWasmErrors.WASM_IMPORT_EXPORT_PARAMETER_DEFAULT_VALUE)
        }
        if (parameter.isVararg) {
            reporter.reportOn(parameter.source, FirWasmErrors.WASM_IMPORT_EXPORT_VARARG_PARAMETER)
        }
        if (!isTypeSupportedInWasmInterop(type, false, context.session)) {
            reporter.reportOn(parameter.source, FirWasmErrors.WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE, type)
        }
    }

    val returnType = declaration.returnTypeRef.coneType
    if (!isTypeSupportedInWasmInterop(returnType, true, context.session)) {
        reporter.reportOn(declaration.source, FirWasmErrors.WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE, returnType)
    }
}

private fun isTypeSupportedInWasmInterop(
    unexpandedType: ConeKotlinType,
    isInFunctionReturnPosition: Boolean,
    session: FirSession,
): Boolean {
    val type = unexpandedType.fullyExpandedType(session)

    if (type.isUnit) {
        return isInFunctionReturnPosition
    }

    // Primitive numbers and Boolean are supported
    return (type.isPrimitive && !type.isChar) || type.isUnsignedType
}
