/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsImport
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsImportName
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsImportDefault
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsImportNamespace

object FirJsImportOptionsChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val hasJsImportName = declaration.hasAnnotation(JsImportName, context.session)
        val hasJsImportDefault = declaration.hasAnnotation(JsImportDefault, context.session)
        val hasJsImportNamespace = declaration.hasAnnotation(JsImportNamespace, context.session)

        if (!hasJsImportName && !hasJsImportDefault && !hasJsImportNamespace) return

        val thereIsMixOfOptions = listOf(hasJsImportName, hasJsImportDefault, hasJsImportNamespace).count { it } > 1
        if (thereIsMixOfOptions) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_IMPORT_DEFAULT_AND_NAMED, context)
        }

        if (hasJsImportNamespace && (declaration !is FirClass || !declaration.classKind.isObject)) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_IMPORT_NAMESPACE_ON_NON_OBJECT_DECLARATION, context)
        }

        val hasJsImport = declaration.hasAnnotation(JsImport, context.session)

        if (!hasJsImport && context.containingFile?.hasAnnotation(JsImport, context.session) != true) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_IMPORT_OPTION_WITHOUT_JS_IMPORT, context)
            return
        }

        if (hasJsImportNamespace && !hasJsImport) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_IMPORT_NAMESPACE_WITH_FILE_JS_IMPORT, context)
        }
    }
}
