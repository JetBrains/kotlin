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
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.WasmStandardClassIds

object FirWasmImportAnnotationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotation: FirAnnotation =
            declaration.annotations.getAnnotationByClassId(WasmStandardClassIds.Annotations.WasmImport, context.session) ?: return

        if (!context.isTopLevel) {
            reporter.reportOn(annotation.source, FirWasmErrors.NESTED_WASM_IMPORT, context)
        }

        if (!declaration.symbol.isEffectivelyExternal(context.session)) {
            reporter.reportOn(annotation.source, FirWasmErrors.WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION, context)
        }

        if (declaration is FirFunction) {
            checkWasmInteropSignature(declaration, context, reporter)
        }
    }
}

fun checkWasmInteropSignature(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
    for (parameter in declaration.valueParameters) {
        val type = parameter.returnTypeRef.coneType
        if (parameter.defaultValue != null) {
            reporter.reportOn(parameter.source, FirWasmErrors.WASM_IMPORT_EXPORT_PARAMETER_DEFAULT_VALUE, context)
        }
        if (parameter.isVararg) {
            reporter.reportOn(parameter.source, FirWasmErrors.WASM_IMPORT_EXPORT_VARARG_PARAMETER, context)
        }
        if (!isTypeSupportedInWasmInterop(type, false, context.session)) {
            reporter.reportOn(parameter.source, FirWasmErrors.WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE, type, context)
        }
    }

    val returnType = declaration.returnTypeRef.coneType
    if (!isTypeSupportedInWasmInterop(returnType, true, context.session)) {
        reporter.reportOn(declaration.source, FirWasmErrors.WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE, returnType, context)
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
