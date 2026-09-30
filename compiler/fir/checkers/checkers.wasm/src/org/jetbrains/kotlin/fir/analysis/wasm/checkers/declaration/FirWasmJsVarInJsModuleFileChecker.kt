/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation0
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonVarInJsModuleFileChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.name.WebCommonStandardClassIds.Annotations.JsModule
import org.jetbrains.kotlin.name.WebCommonStandardClassIds.Annotations.JsQualifier

object FirWasmJsVarInJsModuleFileChecker : FirWebCommonVarInJsModuleFileChecker() {
    override val diagnostic: KtDiagnosticFactoryForDeprecation0 =
        FirWasmErrors.JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE

    context(context: CheckerContext)
    override fun isNonWritableModuleImport(property: FirProperty): Boolean {
        // An own module annotation is already covered by JS_MODULE_PROHIBITED_ON_VAR and NESTED_JS_MODULE_PROHIBITED.
        if (property.hasAnnotation(JsModule, context.session)) return false
        if (!context.containingFileSymbol.hasAnnotation(JsModule, context.session)) return false
        // A qualified declaration is referenced through its qualifier, so a write lands on a plain JS object.
        // Without a qualifier the declaration is a member of the module namespace object, which is never writable.
        return !property.hasAnnotation(JsQualifier, context.session) &&
                !context.containingFileSymbol.hasAnnotation(JsQualifier, context.session)
    }
}
