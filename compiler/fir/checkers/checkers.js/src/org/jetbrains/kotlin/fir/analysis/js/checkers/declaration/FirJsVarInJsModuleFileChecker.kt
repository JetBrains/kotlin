/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation0
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isEitherModuleOrNonModule
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonVarInJsModuleFileChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty

object FirJsVarInJsModuleFileChecker : FirWebCommonVarInJsModuleFileChecker() {
    override val diagnostic: KtDiagnosticFactoryForDeprecation0 =
        FirJsErrors.JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE

    context(context: CheckerContext)
    override fun isNonWritableModuleImport(property: FirProperty): Boolean {
        // An own module annotation is already covered by JS_MODULE_PROHIBITED_ON_VAR and NESTED_JS_MODULE_PROHIBITED.
        if (property.symbol.isEitherModuleOrNonModule(context.session)) return false
        // The imported value is copied into a local variable, so a write never reaches the module,
        // no matter whether a qualifier is applied.
        return context.containingFileSymbol?.isEitherModuleOrNonModule(context.session) == true
    }
}
