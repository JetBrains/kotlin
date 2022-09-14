/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.js

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

object FirJsErrors {
    // Annotations
    val WRONG_JS_QUALIFIER by error0<KtExpression>()
    val JS_MODULE_PROHIBITED_ON_VAR by error0<KtAnnotationEntry>()

    // Supertypes
    val WRONG_MULTIPLE_INHERITANCE by error1<KtElement, FirCallableSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

    // Fun Interfaces
    val IMPLEMENTING_FUNCTION_INTERFACE by error0<KtClassOrObject>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

    // External
    val OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS by error0<KtElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE by error1<KtElement, FirNamedFunctionSymbol>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

    // Dynamics
    val DELEGATION_BY_DYNAMIC by error0<KtElement>()

    // Debug Info
    val DYNAMIC by info0<KtElement>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FirJsErrorsDefaultMessages)
    }
}
