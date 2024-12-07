/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getAnnotationStringParameter
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.getJsName
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirJsNameChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirProperty) {
            val accessors = listOfNotNull(declaration.getter?.symbol, declaration.setter?.symbol)
            val namedAccessorCount = accessors.count { it.getJsName(context.session) != null }
            if (namedAccessorCount > 0 && namedAccessorCount < accessors.size) {
                reporter.reportOn(declaration.source, FirJsErrors.JS_NAME_IS_NOT_ON_ALL_ACCESSORS, context)
            }
        }

        val jsName = declaration.getAnnotationByClassId(JsStandardClassIds.Annotations.JsName, context.session) ?: return
        val jsNameSource = jsName.source ?: declaration.source

        if (declaration.symbol.getAnnotationStringParameter(JsStandardClassIds.Annotations.JsNative, context.session) != null) {
            reporter.reportOn(jsNameSource, FirJsErrors.JS_NAME_PROHIBITED_FOR_NAMED_NATIVE, context)
        }

        if (
            declaration is FirCallableDeclaration && declaration.isOverride ||
            declaration is FirPropertyAccessor && declaration.propertySymbol.isOverride
        ) {
            reporter.reportOn(jsNameSource, FirJsErrors.JS_NAME_PROHIBITED_FOR_OVERRIDE, context)
        }

        when (declaration) {
            is FirConstructor -> {
                if (declaration.isPrimary) {
                    reporter.reportOn(jsNameSource, FirJsErrors.JS_NAME_ON_PRIMARY_CONSTRUCTOR_PROHIBITED, context)
                }
            }

            is FirPropertyAccessor -> {
                val property = declaration.propertySymbol
                if (property.getJsName(context.session) != null) {
                    reporter.reportOn(jsNameSource, FirJsErrors.JS_NAME_ON_ACCESSOR_AND_PROPERTY, context)
                }
            }

            is FirProperty -> {
                if (declaration.isExtension) {
                    reporter.reportOn(jsNameSource, FirJsErrors.JS_NAME_PROHIBITED_FOR_EXTENSION_PROPERTY, context)
                }
            }

            else -> {}
        }
    }
}
