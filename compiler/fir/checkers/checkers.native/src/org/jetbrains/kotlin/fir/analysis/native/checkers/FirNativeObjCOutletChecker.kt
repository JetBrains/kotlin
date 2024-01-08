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
import org.jetbrains.kotlin.fir.backend.native.interop.isKotlinObjCClass
import org.jetbrains.kotlin.fir.backend.native.interop.isObjCObjectType
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.NativeStandardInteropNames.objCOutletClassId

object FirNativeObjCOutletChecker : FirClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session

        fun checkCanGenerateFunctionImp(setter: FirPropertyAccessor) {
            if (setter.valueParameters.size > 2)
                reporter.reportOn(setter.source, FirNativeErrors.TWO_OR_LESS_PARAMETERS_ARE_SUPPORTED_HERE, context)
        }

        fun checkCanGenerateOutletSetterImp(property: FirProperty) {
            if (!property.isVar) {
                reporter.reportOn(property.source, FirNativeErrors.PROPERTY_MUST_BE_VAR, objCOutletClassId.asSingleFqName(), context)
                return
            }

            property.receiverParameter?.let {
                reporter.reportOn(it.source, FirNativeErrors.MUST_NOT_HAVE_EXTENSION_RECEIVER, "@${objCOutletClassId.asFqNameString()}", context)
            }

            val type = property.returnTypeRef
            if (!type.isObjCObjectType(session))
                reporter.reportOn(
                    property.returnTypeRef.source,
                    FirNativeErrors.MUST_BE_OBJC_OBJECT_TYPE,
                    "@${objCOutletClassId.asSingleFqName()} type",
                    type.coneType,
                    context
                )

            checkCanGenerateFunctionImp(property.setter!!)
        }

        fun checkKotlinObjCClass(firClass: FirClass) {
            for (decl in firClass.declarations) {
                if (decl is FirProperty && decl.annotations.hasAnnotation(objCOutletClassId, session))
                    checkCanGenerateOutletSetterImp(decl)
            }
        }

        if (!declaration.isExpect && declaration.symbol.isKotlinObjCClass(context.session)) {
            checkKotlinObjCClass(declaration)
        }
    }
}
