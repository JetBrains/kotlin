/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.web.common

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.CANNOT_CHECK_FOR_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.EXTERNAL_ANONYMOUS_INITIALIZER
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.EXTERNAL_DELEGATED_CONSTRUCTOR_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.EXTERNAL_DELEGATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.EXTERNAL_INTERFACE_AS_CLASS_LITERAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.INLINE_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.JSCODE_ARGUMENT_NON_CONST_EXPRESSION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.NESTED_CLASS_IN_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.NESTED_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.NESTED_JS_EXPORT
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.UNCHECKED_CAST_TO_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.WRONG_BODY_OF_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.WRONG_JS_QUALIFIER

@Suppress("unused")
object FirWebCommonErrorsDefaultMessages : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("FIR").also { map ->
        map.put(WRONG_JS_QUALIFIER, "Qualifier contains illegal characters.")
        map.put(NESTED_EXTERNAL_DECLARATION, "Non-top-level 'external' declaration.")
        map.put(WRONG_EXTERNAL_DECLARATION, "Declaration of such kind ({0}) cannot be external.", CommonRenderers.STRING)
        map.put(NESTED_CLASS_IN_EXTERNAL_INTERFACE, "Interface cannot contain nested classes and objects.")
        map.put(INLINE_EXTERNAL_DECLARATION, "Inline external declaration.")
        map.put(
            NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE,
            "Only nullable properties of external interfaces are allowed to be non-abstract."
        )
        map.put(EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER, "External class constructor cannot have a property parameter.")
        map.put(EXTERNAL_ANONYMOUS_INITIALIZER, "Anonymous initializers in external classes are prohibited.")
        map.put(EXTERNAL_DELEGATION, "Cannot use delegate on external declaration.")
        map.put(EXTERNAL_DELEGATED_CONSTRUCTOR_CALL, "Delegated constructor call in external class is prohibited.")
        map.put(
            WRONG_BODY_OF_EXTERNAL_DECLARATION,
            "Wrong body of external declaration. Must be either ' = definedExternally' or '{ definedExternally }'."
        )
        map.put(WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION, "Wrong initializer of external declaration. Must be ' = definedExternally'.")
        map.put(
            WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER,
            "Wrong default value for parameter of external function. Must be ' = definedExternally'."
        )
        map.put(
            CANNOT_CHECK_FOR_EXTERNAL_INTERFACE,
            "Cannot check for external interface: ''{0}''",
            FirDiagnosticRenderers.RENDER_TYPE,
        )
        map.put(
            UNCHECKED_CAST_TO_EXTERNAL_INTERFACE,
            "Unchecked cast to external interface: ''{0}'' to ''{1}''.",
            FirDiagnosticRenderers.RENDER_TYPE,
            FirDiagnosticRenderers.RENDER_TYPE,
        )
        map.put(EXTERNAL_INTERFACE_AS_CLASS_LITERAL, "Cannot refer to external interface from class literal.")
        map.put(
            EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT,
            "Cannot pass external interface ''{0}'' for reified type parameter.",
            FirDiagnosticRenderers.RENDER_TYPE
        )


        map.put(NESTED_JS_EXPORT, "'@JsExport' is only allowed on files and top-level declarations.")

        map.put(JSCODE_ARGUMENT_NON_CONST_EXPRESSION, "An argument for the 'js()' function must be a constant string expression.")
    }
}