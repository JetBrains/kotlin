/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.MUST_BE_OBJC_OBJECT_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.MUST_BE_UNIT_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.MUST_NOT_HAVE_EXTENSION_RECEIVER
import org.jetbrains.kotlin.fir.backend.native.interop.isKotlinObjCClass
import org.jetbrains.kotlin.fir.backend.native.interop.isObjCObjectType
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.processAllDeclaredCallables
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.UnexpandedTypeCheck
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.name.NativeStandardInteropNames.objCActionClassId

object FirNativeObjCActionChecker : FirClassChecker(MppCheckerKind.Platform) {
    @OptIn(UnexpandedTypeCheck::class)
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session

        fun checkCanGenerateFunctionImp(function: FirNamedFunctionSymbol) {
            if (function.valueParameterSymbols.size > 2)
                reporter.reportOn(function.source, FirNativeErrors.TWO_OR_LESS_PARAMETERS_ARE_SUPPORTED_HERE, context)
        }

        fun checkCanGenerateActionImp(function: FirNamedFunctionSymbol) {
            val action = "@${objCActionClassId.asFqNameString()}"

            function.receiverParameterSymbol?.let {
                reporter.reportOn(it.source, MUST_NOT_HAVE_EXTENSION_RECEIVER, "$action method", context)
            }

            function.valueParameterSymbols.forEach {
                val kotlinType = it.resolvedReturnTypeRef
                if (!kotlinType.isObjCObjectType(session))
                    reporter.reportOn(it.source, MUST_BE_OBJC_OBJECT_TYPE, "$action method parameter type", kotlinType.coneType, context)
            }

            val returnType = function.resolvedReturnTypeRef
            if (!returnType.isUnit)
                reporter.reportOn(function.source, MUST_BE_UNIT_TYPE, "$action method return type", returnType.coneType, context)

            checkCanGenerateFunctionImp(function)
        }

        fun checkKotlinObjCClass(firClass: FirClass) {
            firClass.symbol.processAllDeclaredCallables(context.session) { symbol ->
                if (symbol is FirNamedFunctionSymbol && symbol.hasAnnotation(objCActionClassId, session))
                    checkCanGenerateActionImp(symbol)
            }
        }

        if (!declaration.isExpect && declaration.symbol.isKotlinObjCClass(context.session)) {
            checkKotlinObjCClass(declaration)
        }
    }
}
