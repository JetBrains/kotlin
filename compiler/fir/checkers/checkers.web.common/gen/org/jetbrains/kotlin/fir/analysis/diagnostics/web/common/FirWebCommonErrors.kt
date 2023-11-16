/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.web.common

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter

/**
 * Generated from: [org.jetbrains.kotlin.fir.checkers.generator.diagnostics.WEB_COMMON_DIAGNOSTICS_LIST]
 */
object FirWebCommonErrors {
    // Externals
    val NESTED_EXTERNAL_DECLARATION by error0<KtExpression>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val WRONG_EXTERNAL_DECLARATION by error1<KtExpression, String>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NESTED_CLASS_IN_EXTERNAL_INTERFACE by error0<KtExpression>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val INLINE_EXTERNAL_DECLARATION by error0<KtDeclaration>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE by error0<KtExpression>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER by error0<KtParameter>()
    val EXTERNAL_ANONYMOUS_INITIALIZER by error0<KtAnonymousInitializer>()
    val EXTERNAL_DELEGATION by error0<KtElement>()
    val EXTERNAL_DELEGATED_CONSTRUCTOR_CALL by error0<KtElement>()
    val WRONG_BODY_OF_EXTERNAL_DECLARATION by error0<KtElement>()
    val WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION by error0<KtElement>()
    val WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER by error0<KtElement>()

    // JsCode
    val JSCODE_ARGUMENT_NON_CONST_EXPRESSION by error0<KtElement>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FirWebCommonErrorsDefaultMessages)
    }
}
