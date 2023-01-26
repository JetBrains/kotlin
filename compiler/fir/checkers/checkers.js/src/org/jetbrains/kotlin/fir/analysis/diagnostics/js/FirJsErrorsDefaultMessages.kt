/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.js

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.checkMissingMessages
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.CANNOT_CHECK_FOR_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_ANONYMOUS_INITIALIZER
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_DELEGATED_CONSTRUCTOR_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_DELEGATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_ENUM_ENTRY_WITH_BODY
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.DELEGATION_BY_DYNAMIC
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_INTERFACE_AS_CLASS_LITERAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.IMPLEMENTING_FUNCTION_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.INLINE_CLASS_IN_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.INLINE_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_MODULE_PROHIBITED_ON_NON_NATIVE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_MODULE_PROHIBITED_ON_VAR
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NESTED_JS_MODULE_PROHIBITED
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_NAME_IS_NOT_ON_ALL_ACCESSORS
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_NAME_ON_ACCESSOR_AND_PROPERTY
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_NAME_ON_PRIMARY_CONSTRUCTOR_PROHIBITED
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_NAME_PROHIBITED_FOR_EXTENSION_PROPERTY
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_NAME_PROHIBITED_FOR_NAMED_NATIVE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_NAME_PROHIBITED_FOR_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NATIVE_INDEXER_WRONG_PARAMETER_COUNT
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NATIVE_SETTER_WRONG_RETURN_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NESTED_CLASS_IN_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NESTED_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NESTED_JS_EXPORT
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.RUNTIME_ANNOTATION_NOT_SUPPORTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.UNCHECKED_CAST_TO_EXTERNAL_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_BODY_OF_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_JS_QUALIFIER
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_MULTIPLE_INHERITANCE

@Suppress("unused")
object FirJsErrorsDefaultMessages : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("FIR").also { map ->
        map.put(WRONG_JS_QUALIFIER, "Qualifier contains illegal characters")
        map.put(JS_MODULE_PROHIBITED_ON_VAR, "@JsModule and @JsNonModule annotations prohibited for 'var' declarations. Use 'val' instead.")
        map.put(JS_MODULE_PROHIBITED_ON_NON_NATIVE, "@JsModule and @JsNonModule annotations prohibited for non-external declarations.")
        map.put(
            NESTED_JS_MODULE_PROHIBITED,
            "@JsModule and @JsNonModule can't appear on here since the file is already marked by either @JsModule or @JsNonModule"
        )
        map.put(
            WRONG_MULTIPLE_INHERITANCE,
            "Can''t apply multiple inheritance here, since it''s impossible to generate bridge for system function {0}",
            FirDiagnosticRenderers.SYMBOL
        )
        map.put(DELEGATION_BY_DYNAMIC, "Can't delegate to dynamic value")
        map.put(IMPLEMENTING_FUNCTION_INTERFACE, "Implementing function interface is prohibited in JavaScript")
        map.put(OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS, "Overriding `external` function with optional parameters")
        map.put(
            OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE,
            "Overriding `external` function with optional parameters by declaration from superclass: {0}",
            FirDiagnosticRenderers.SYMBOL
        )
        map.put(CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION, "This property can only be used from external declarations")
        map.put(RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION, "Runtime annotation can't be put on external declaration")
        map.put(
            RUNTIME_ANNOTATION_NOT_SUPPORTED,
            "Reflection is not supported in JavaScript target, therefore you won't be able to read this annotation in run-time"
        )
        map.put(EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER, "External class constructor cannot have a property parameter")
        map.put(EXTERNAL_ENUM_ENTRY_WITH_BODY, "Entry of external enum class can't have body")
        map.put(EXTERNAL_ANONYMOUS_INITIALIZER, "Anonymous initializer is not allowed in external classes")
        map.put(EXTERNAL_DELEGATION, "Can't use delegate on external declaration")
        map.put(EXTERNAL_DELEGATED_CONSTRUCTOR_CALL, "Delegated constructor call in external class is not allowed")
        map.put(
            WRONG_BODY_OF_EXTERNAL_DECLARATION,
            "Wrong body of external declaration. Must be either ' = definedExternally' or { definedExternally }"
        )
        map.put(WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION, "Wrong initializer of external declaration. Must be ' = definedExternally'")
        map.put(
            WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER,
            "Wrong default value for parameter of external function. Must be ' = definedExternally'"
        )
        map.put(NESTED_EXTERNAL_DECLARATION, "Non top-level `external` declaration")
        map.put(WRONG_EXTERNAL_DECLARATION, "Declaration of such kind ({0}) can''t be external", CommonRenderers.STRING)
        map.put(NESTED_CLASS_IN_EXTERNAL_INTERFACE, "Interface can't contain nested classes and objects")
        map.put(EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE, "External type extends non-external type")
        map.put(INLINE_EXTERNAL_DECLARATION, "Inline external declaration")
        map.put(
            INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING,
            "Using value classes as parameter type or return type of external declarations is experimental"
        )
        map.put(
            INLINE_CLASS_IN_EXTERNAL_DECLARATION,
            "Using value classes as parameter type or return type of external declarations is not supported"
        )
        map.put(EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION, "Function types with receiver are prohibited in external declarations")
        map.put(
            NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE,
            "Only nullable properties of external interfaces are allowed to be non-abstract"
        )
        map.put(
            NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN,
            "Annotation ''{0}'' is allowed only on member functions of declaration annotated as ''kotlin.js.native'' or on toplevel extension functions",
            FirDiagnosticRenderers.RENDER_TYPE
        )
        map.put(
            NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER,
            "Native {0}''s first parameter type should be ''kotlin.String'' or subtype of ''kotlin.Number''",
            CommonRenderers.STRING
        )
        map.put(NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS, "Native {0}''s parameter can not have default value", CommonRenderers.STRING)
        map.put(NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE, "Native getter's return type should be nullable")
        map.put(
            NATIVE_SETTER_WRONG_RETURN_TYPE,
            "Native setter's return type should be 'Unit' or a supertype of the second parameter's type"
        )
        map.put(
            NATIVE_INDEXER_WRONG_PARAMETER_COUNT, "Expected {0} parameters for native {1}",
            KtDiagnosticRenderers.TO_STRING,
            CommonRenderers.STRING
        )
        map.put(
            NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE,
            "Can''t put non-external declarations in file marked with {0} annotation",
            FirDiagnosticRenderers.RENDER_TYPE
        )
        map.put(JS_NAME_PROHIBITED_FOR_EXTENSION_PROPERTY, "@JsName is prohibited for extension properties")
        map.put(JS_NAME_IS_NOT_ON_ALL_ACCESSORS, "@JsName should be on all of the property accessors")
        map.put(JS_NAME_PROHIBITED_FOR_NAMED_NATIVE, "@JsName is prohibited for external declaration with explicit name")
        map.put(JS_NAME_PROHIBITED_FOR_OVERRIDE, "@JsName is prohibited for overridden members")
        map.put(JS_NAME_ON_PRIMARY_CONSTRUCTOR_PROHIBITED, "@JsName annotation is prohibited for primary constructors")
        map.put(JS_NAME_ON_ACCESSOR_AND_PROPERTY, "@JsName can be either on a property or its accessors, not both of them")
        map.put(
            CANNOT_CHECK_FOR_EXTERNAL_INTERFACE,
            "Cannot check for external interface: {0}",
            FirDiagnosticRenderers.RENDER_TYPE,
        )
        map.put(
            UNCHECKED_CAST_TO_EXTERNAL_INTERFACE,
            "Unchecked cast to external interface: {0} to {1}",
            FirDiagnosticRenderers.RENDER_TYPE,
            FirDiagnosticRenderers.RENDER_TYPE,
        )
        map.put(EXTERNAL_INTERFACE_AS_CLASS_LITERAL, "Can't refer to external interface from class literal")
        map.put(NESTED_JS_EXPORT, "@JsExport is only allowed on files and top-level declarations")

        map.checkMissingMessages(FirJsErrors)
    }
}