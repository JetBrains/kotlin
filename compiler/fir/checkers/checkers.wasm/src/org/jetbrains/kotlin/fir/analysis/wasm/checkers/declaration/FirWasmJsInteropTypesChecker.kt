/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.hasValidJsCodeBody
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.isJsExportedDeclaration
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.WasmStandardClassIds

object FirWasmJsInteropTypesChecker : FirBasicDeclarationChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session

        fun isExternalJsInteropDeclaration(): Boolean {
            val isEffectivelyExternal = declaration.symbol.isEffectivelyExternal(session)
            val hasWasmImportAnnotation = declaration.annotations.hasAnnotation(WasmStandardClassIds.Annotations.WasmImport, session)
            return isEffectivelyExternal && !hasWasmImportAnnotation
        }

        fun isJsCodeDeclaration(): Boolean {
            return when (declaration) {
                is FirSimpleFunction -> declaration.hasValidJsCodeBody()
                is FirProperty -> declaration.hasValidJsCodeBody()
                else -> false
            }
        }

        // Interop type restriction rules apply uniformly to external, js("code"), and @JsExport declarations.
        if (
            !isExternalJsInteropDeclaration() &&
            !isJsCodeDeclaration() &&
            !isJsExportedDeclaration(declaration, session)
        ) {
            return
        }

        // filter out compiler-generated declarations (data/enum methods, property accessors, primary constructors, etc.) to prevent
        // 1) reporting excessive diagnostics
        //    (e.g. on generated methods of external enum classes (which are handled by another checker))
        // 2) reporting duplicate diagnostics
        //    (e.g. on properties with generated accessors and type parameters of classes with generated primary constructors)
        if (declaration.source is KtFakeSourceElement) return

        fun ConeKotlinType.isSupportedInJsInterop(position: Position): Boolean {
            if (isUnit || isNothing) {
                // Unit and Nothing are supported in return type positions and unsupported in other type positions
                return position == Position.RETURN_TYPE || position == Position.FUNCTION_TYPE_RETURN_TYPE
            }

            // primitive types, unsigned types, and String are supported (regardless of nullability)
            if (isPrimitiveOrNullablePrimitive) return true
            if (isUnsignedTypeOrNullableUnsignedType) return true
            if (isString || isNullableString) return true

            // type parameters' upper bounds should be checked separately on declaration-site
            if (this is ConeTypeParameterType) return true

            // function types themselves are supported
            // (and their parameter and return types should be checked separately)
            if (isBasicFunctionType(session)) return true

            // aside from the aforementioned cases, only external types are supported
            return toRegularClassSymbol(session)?.isEffectivelyExternal(session) == true
        }

        fun FirTypeRef.checkSupportInJsInterop(position: Position, fallbackSource: KtSourceElement?) {
            val type = coneType.let {
                val unexpandedType = if (position == Position.VARARG_VALUE_PARAMETER_TYPE) it.varargElementType() else it
                unexpandedType.fullyExpandedType(session)
            }

            if (!type.isSupportedInJsInterop(position)) {
                reporter.reportOn(
                    source ?: fallbackSource,
                    FirWasmErrors.WRONG_JS_INTEROP_TYPE,
                    type,
                    position.description,
                    context
                )
                return
            }

            // although function types themselves are supported, their parameter and return types should be checked separately
            val functionTypeRef = (this as? FirResolvedTypeRef)?.delegatedTypeRef as? FirFunctionTypeRef ?: return
            with(functionTypeRef) {
                parameters.map(FirFunctionTypeParameter::returnTypeRef).forEach { parameterTypeRef ->
                    parameterTypeRef.checkSupportInJsInterop(Position.FUNCTION_TYPE_PARAMETER_TYPE, fallbackSource = functionTypeRef.source)
                }
                returnTypeRef.checkSupportInJsInterop(Position.FUNCTION_TYPE_RETURN_TYPE, fallbackSource = functionTypeRef.source)
            }
        }

        if (declaration is FirTypeParameterRefsOwner) {
            for (typeParameter in declaration.typeParameters) {
                for (upperBound in typeParameter.symbol.resolvedBounds) {
                    upperBound.checkSupportInJsInterop(Position.TYPE_PARAMETER_UPPER_BOUND, fallbackSource = typeParameter.source)
                }
            }
        }

        when (declaration) {
            is FirProperty -> {
                declaration.returnTypeRef.checkSupportInJsInterop(Position.PROPERTY_TYPE, fallbackSource = declaration.source)
            }
            is FirFunction -> {
                for (valueParameter in declaration.valueParameters) {
                    val position = if (valueParameter.isVararg) Position.VARARG_VALUE_PARAMETER_TYPE else Position.VALUE_PARAMETER_TYPE
                    valueParameter.returnTypeRef.checkSupportInJsInterop(position, fallbackSource = valueParameter.source)
                }
                declaration.returnTypeRef.checkSupportInJsInterop(Position.RETURN_TYPE, fallbackSource = declaration.source)
            }
            else -> Unit
        }
    }

    private enum class Position(val description: String) {
        TYPE_PARAMETER_UPPER_BOUND("upper bound of JS interop type parameter"),
        PROPERTY_TYPE("type of JS interop property"),
        VALUE_PARAMETER_TYPE("value parameter type of JS interop function"),
        VARARG_VALUE_PARAMETER_TYPE("value parameter type of JS interop function"),
        RETURN_TYPE("return type of JS interop function"),
        FUNCTION_TYPE_PARAMETER_TYPE("parameter type of JS interop function type"),
        FUNCTION_TYPE_RETURN_TYPE("return type of JS interop function type"),
    }
}
