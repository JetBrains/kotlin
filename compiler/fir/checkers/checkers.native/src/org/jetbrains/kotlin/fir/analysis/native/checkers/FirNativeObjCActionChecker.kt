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
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.types.UnexpandedTypeCheck
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.name.NativeStandardInteropNames.objCActionClassId

object FirNativeObjCActionChecker : FirClassChecker(MppCheckerKind.Common) {
    @OptIn(UnexpandedTypeCheck::class)
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session

        fun checkCanGenerateFunctionImp(function: FirSimpleFunction) {
            if (function.valueParameters.size > 2)
                reporter.reportOn(function.source, FirNativeErrors.TWO_OR_LESS_PARAMETERS_ARE_SUPPORTED_HERE, context)
        }

        fun checkCanGenerateActionImp(function: FirSimpleFunction) {
            val action = "@${objCActionClassId.asFqNameString()}"

            function.receiverParameter?.let {
                reporter.reportOn(it.source, MUST_NOT_HAVE_EXTENSION_RECEIVER, "$action method", context)
            }

            function.valueParameters.forEach {
                val kotlinType = it.returnTypeRef
                if (!kotlinType.isObjCObjectType(session))
                    reporter.reportOn(it.source, MUST_BE_OBJC_OBJECT_TYPE, "$action method parameter type", kotlinType.coneType, context)
            }

            val returnType = function.returnTypeRef
            if (!returnType.isUnit)
                reporter.reportOn(function.source, MUST_BE_UNIT_TYPE, "$action method return type", returnType.coneType, context)

            checkCanGenerateFunctionImp(function)
        }

        fun checkKotlinObjCClass(firClass: FirClass) {
            for (decl in firClass.declarations) {
                if (decl is FirSimpleFunction && decl.annotations.hasAnnotation(objCActionClassId, session))
                    checkCanGenerateActionImp(decl)
            }
        }

        if (!declaration.isExpect && declaration.symbol.isKotlinObjCClass(context.session)) {
            checkKotlinObjCClass(declaration)
        }
    }
}
