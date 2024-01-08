/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.hasValidJsCodeBody
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.isJsExportedDeclaration
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.WasmStandardClassIds

object FirWasmJsInteropTypesChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session

        fun isExternalJsInteropDeclaration() =
            declaration.symbol.isEffectivelyExternal(session) &&
                    !declaration.annotations.hasAnnotation(WasmStandardClassIds.Annotations.WasmImport, session)

        fun isJsCodeDeclaration() =
            (declaration is FirSimpleFunction && declaration.hasValidJsCodeBody()) ||
                    (declaration is FirProperty && declaration.hasValidJsCodeBody())

        fun isJsExportDeclaration() =
            declaration is FirSimpleFunction && isJsExportedDeclaration(declaration, session)

        // Interop type restriction rules apply uniformly to external, js("code"), and @JsExport declarations
        if (
            !isExternalJsInteropDeclaration() &&
            !isJsCodeDeclaration() &&
            !isJsExportDeclaration()
        ) {
            return
        }

        // Skip enums to avoid reporting errors for synthetic static members with unsupported interop types.
        // External enums errors are reported in a separate checker.
        if (context.containingDeclarations.any { it is FirClass && it.isEnumClass }) {
            return
        }

        fun ConeKotlinType.checkJsInteropType(
            typePositionDescription: String,
            source: KtSourceElement?,
            isInFunctionReturnPosition: Boolean = false,
        ) {
            if (!isTypeSupportedInJsInterop(this, isInFunctionReturnPosition, session)) {
                reporter.reportOn(
                    source,
                    FirWasmErrors.WRONG_JS_INTEROP_TYPE,
                    typePositionDescription,
                    this,
                    context
                )
            }
        }

        fun FirTypeParameterRef.checkJsInteropTypeParameter() {
            for (upperBound in this.symbol.resolvedBounds) {
                upperBound.type.checkJsInteropType(
                    "JS interop type parameter upper bound",
                    upperBound.source ?: this.source
                )
            }
        }

        if (declaration is FirMemberDeclaration) {
            for (typeParameter in declaration.typeParameters) {
                typeParameter.checkJsInteropTypeParameter()
            }
        }

        when (declaration) {
            is FirProperty -> {
                declaration.returnTypeRef.coneType.checkJsInteropType(
                    "JS interop property",
                    declaration.source
                )
            }
            is FirFunction -> {
                for (parameter in declaration.valueParameters) {
                    val type = parameter.returnTypeRef.coneType
                    val varargElementTypeOrType = if (parameter.isVararg) type.varargElementType() else type
                    varargElementTypeOrType.checkJsInteropType(
                        "JS interop function parameter",
                        parameter.source
                    )
                }
                declaration.returnTypeRef.coneType.checkJsInteropType(
                    "JS interop function return",
                    declaration.source,
                    isInFunctionReturnPosition = true
                )
            }
            else -> {}
        }
    }
}

private fun isTypeSupportedInJsInterop(
    unexpandedType: ConeKotlinType,
    isInFunctionReturnPosition: Boolean,
    session: FirSession,
): Boolean {
    val type = unexpandedType.fullyExpandedType(session)

    if (type.isUnit || type.isNothing) {
        return isInFunctionReturnPosition
    }

    val nonNullable = type.withNullability(ConeNullability.NOT_NULL, session.typeContext)

    if (nonNullable.isPrimitive || nonNullable.isUnsignedType || nonNullable.isString) {
        return true
    }

    // Interop type parameters upper bounds should be checked
    // on declaration site separately
    if (nonNullable is ConeTypeParameterType) {
        return true
    }

    val regularClassSymbol = nonNullable.toRegularClassSymbol(session)
    if (regularClassSymbol?.isEffectivelyExternal(session) == true) {
        return true
    }

    if (type.isBasicFunctionType(session)) {
        val arguments = type.typeArguments
        for (i in 0 until arguments.lastIndex) {
            val argType = arguments[i].type ?: return false
            if (!isTypeSupportedInJsInterop(argType, isInFunctionReturnPosition = false, session)) {
                return false
            }
        }

        val returnType = arguments.last().type ?: return false
        return isTypeSupportedInJsInterop(
            returnType,
            isInFunctionReturnPosition = true,
            session
        )
    }

    return false
}
