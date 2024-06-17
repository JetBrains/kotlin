/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.web.common

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.Severity.ERROR
import org.jetbrains.kotlin.diagnostics.Severity.WARNING
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter

/**
 * Generated from: [org.jetbrains.kotlin.fir.checkers.generator.diagnostics.WEB_COMMON_DIAGNOSTICS_LIST]
 */
object FirWebCommonErrors {
    // Annotations
    val WRONG_JS_QUALIFIER: KtDiagnosticFactory0 = KtDiagnosticFactory0("WRONG_JS_QUALIFIER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)

    // Externals
    val NESTED_EXTERNAL_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("NESTED_EXTERNAL_DECLARATION", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtExpression::class)
    val WRONG_EXTERNAL_DECLARATION: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("WRONG_EXTERNAL_DECLARATION", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtExpression::class)
    val NESTED_CLASS_IN_EXTERNAL_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("NESTED_CLASS_IN_EXTERNAL_INTERFACE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtExpression::class)
    val INLINE_EXTERNAL_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("INLINE_EXTERNAL_DECLARATION", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtDeclaration::class)
    val NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtExpression::class)
    val EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtParameter::class)
    val EXTERNAL_ANONYMOUS_INITIALIZER: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTERNAL_ANONYMOUS_INITIALIZER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtAnonymousInitializer::class)
    val EXTERNAL_DELEGATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTERNAL_DELEGATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val EXTERNAL_DELEGATED_CONSTRUCTOR_CALL: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTERNAL_DELEGATED_CONSTRUCTOR_CALL", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val WRONG_BODY_OF_EXTERNAL_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("WRONG_BODY_OF_EXTERNAL_DECLARATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val CANNOT_CHECK_FOR_EXTERNAL_INTERFACE: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("CANNOT_CHECK_FOR_EXTERNAL_INTERFACE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val UNCHECKED_CAST_TO_EXTERNAL_INTERFACE: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val EXTERNAL_INTERFACE_AS_CLASS_LITERAL: KtDiagnosticFactory0 = KtDiagnosticFactory0("EXTERNAL_INTERFACE_AS_CLASS_LITERAL", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class)
    val NAMED_COMPANION_IN_EXTERNAL_INTERFACE: KtDiagnosticFactory0 = KtDiagnosticFactory0("NAMED_COMPANION_IN_EXTERNAL_INTERFACE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class)

    // Export
    val NESTED_JS_EXPORT: KtDiagnosticFactory0 = KtDiagnosticFactory0("NESTED_JS_EXPORT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)

    // JsCode
    val JSCODE_ARGUMENT_NON_CONST_EXPRESSION: KtDiagnosticFactory0 = KtDiagnosticFactory0("JSCODE_ARGUMENT_NON_CONST_EXPRESSION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)

    init {
        RootDiagnosticRendererFactory.registerFactory(FirWebCommonErrorsDefaultMessages)
    }
}
