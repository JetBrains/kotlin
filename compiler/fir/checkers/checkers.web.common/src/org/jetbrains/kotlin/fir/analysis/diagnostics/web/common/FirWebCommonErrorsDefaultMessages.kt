/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.web.common

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.CANNOT_CHECK_FOR_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.EXTERNAL_ANONYMOUS_INITIALIZER
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.EXTERNAL_DELEGATED_CONSTRUCTOR_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.EXTERNAL_DELEGATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.EXTERNAL_INTERFACE_AS_CLASS_LITERAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.INLINE_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.WRONG_JS_EXPORT_TARGET_VISIBILITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.JSCODE_ARGUMENT_NON_CONST_EXPRESSION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.JS_MODULE_PROHIBITED_ON_VAR
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.MULTIPLE_JS_EXPORT_DEFAULT_IN_ONE_FILE
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.NAMED_COMPANION_IN_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.NESTED_CLASS_IN_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.NESTED_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.NESTED_JS_EXPORT
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.NESTED_JS_MODULE_PROHIBITED
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.UNCHECKED_CAST_TO_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.UNSUPPORTED_REFLECTION_API
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.WRONG_BODY_OF_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors.WRONG_JS_QUALIFIER

@Suppress("unused")
object FirWebCommonErrorsDefaultMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("FIR") { map ->
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
            "Cannot check for external interface ''{0}''.",
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
        map.put(
            MULTIPLE_JS_EXPORT_DEFAULT_IN_ONE_FILE,
            "Only one declaration with '@JsExport.Default' is allowed per file."
        )
        map.put(
            WRONG_JS_EXPORT_TARGET_VISIBILITY,
            "'@JsExport' is only allowed for public declarations. This declaration will not be exported."
        )

        map.put(JSCODE_ARGUMENT_NON_CONST_EXPRESSION, "An argument for the 'js()' function must be a constant string expression.")
        map.put(NAMED_COMPANION_IN_EXTERNAL_INTERFACE, "Named companions are not allowed inside external interfaces.")
        map.put(UNSUPPORTED_REFLECTION_API, "{0}", TO_STRING)

        map.put(CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION, "This property can only be used from external declarations.")
        map.put(
            EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE,
            "External type extends non-external type ''{0}''.",
            FirDiagnosticRenderers.RENDER_TYPE
        )
        map.put(JS_MODULE_PROHIBITED_ON_VAR, "'@JsModule' annotation is prohibited for 'var' declarations. Use 'val' instead.")
        map.put(
            NESTED_JS_MODULE_PROHIBITED,
            "'@JsModule' and '@JsNonModule' cannot appear here since the file is already marked by either '@JsModule' or '@JsNonModule'."
        )
        map.put(
            NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE,
            "Only external declarations are allowed in files marked with ''{0}'' annotation.",
            FirDiagnosticRenderers.RENDER_TYPE
        )
    }
}
