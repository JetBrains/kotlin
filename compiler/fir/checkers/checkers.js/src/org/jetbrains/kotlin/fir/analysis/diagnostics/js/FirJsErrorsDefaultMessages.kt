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
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.DELEGATION_BY_DYNAMIC
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_ENUM_ENTRY_WITH_BODY
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.IMPLEMENTING_FUNCTION_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.INLINE_CLASS_IN_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_BUILTIN_NAME_CLASH
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_EXTERNAL_ARGUMENT
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_EXTERNAL_INHERITORS_ONLY
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_FAKE_NAME_CLASH
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_MODULE_PROHIBITED_ON_NON_NATIVE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_MODULE_PROHIBITED_ON_VAR
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_NAME_CLASH
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_NAME_IS_NOT_ON_ALL_ACCESSORS
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_NAME_ON_ACCESSOR_AND_PROPERTY
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_NAME_ON_PRIMARY_CONSTRUCTOR_PROHIBITED
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_NAME_PROHIBITED_FOR_EXTENSION_PROPERTY
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_NAME_PROHIBITED_FOR_NAMED_NATIVE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_NAME_PROHIBITED_FOR_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_STATIC_NOT_IN_CLASS_COMPANION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NAMED_COMPANION_IN_EXPORTED_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_STATIC_ON_CONST
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_STATIC_ON_NON_PUBLIC_MEMBER
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NAME_CONTAINS_ILLEGAL_CHARS
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NATIVE_INDEXER_WRONG_PARAMETER_COUNT
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NATIVE_SETTER_WRONG_RETURN_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NESTED_JS_MODULE_PROHIBITED
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NON_CONSUMABLE_EXPORTED_IDENTIFIER
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NON_EXPORTABLE_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_STATIC_ON_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.PROPERTY_DELEGATION_BY_DYNAMIC
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.RUNTIME_ANNOTATION_NOT_SUPPORTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.SPREAD_OPERATOR_IN_DYNAMIC_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_EXPORTED_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_MULTIPLE_INHERITANCE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_OPERATION_WITH_DYNAMIC

@Suppress("unused")
object FirJsErrorsDefaultMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap = KtDiagnosticFactoryToRendererMap("FIR").also { map ->
        map.put(
            JS_MODULE_PROHIBITED_ON_VAR,
            "'@JsModule' and '@JsNonModule' annotations are prohibited for 'var' declarations. Use 'val' instead."
        )
        map.put(
            JS_MODULE_PROHIBITED_ON_NON_NATIVE,
            "'@JsModule' and '@JsNonModule' annotations are prohibited for non-external declarations."
        )
        map.put(
            NESTED_JS_MODULE_PROHIBITED,
            "'@JsModule' and '@JsNonModule' cannot appear here since the file is already marked by either '@JsModule' or '@JsNonModule'."
        )
        map.put(
            CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE,
            "When accessing module declarations from UMD, they must be marked with both @JsModule and @JsNonModule."
        )
        map.put(
            CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM,
            "Cannot access ''{0}'' marked with @JsModule annotation from non-modular project.", FirDiagnosticRenderers.SYMBOL
        )
        map.put(
            CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM,
            "Cannot access ''{0}'' marked with @JsNonModule annotation from modular project.", FirDiagnosticRenderers.SYMBOL
        )
        map.put(
            WRONG_MULTIPLE_INHERITANCE,
            "Multiple inheritance cannot be used here, since it''s impossible to generate a bridge for system function {0}.",
            FirDiagnosticRenderers.SYMBOL
        )
        map.put(DELEGATION_BY_DYNAMIC, "Cannot delegate to dynamic value.")
        map.put(PROPERTY_DELEGATION_BY_DYNAMIC, "Cannot apply property delegation by dynamic handler.")
        map.put(SPREAD_OPERATOR_IN_DYNAMIC_CALL, "Cannot apply spread operator in dynamic call.")
        map.put(WRONG_OPERATION_WITH_DYNAMIC, "Wrong operation with dynamic value: {0}.", CommonRenderers.STRING)
        map.put(IMPLEMENTING_FUNCTION_INTERFACE, "Implementing a function interface is prohibited in JavaScript.")
        map.put(OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS, "Overriding 'external' function with optional parameters.")
        map.put(
            OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE,
            "Overriding ''external'' function with optional parameters by declaration from superclass: {0}.",
            FirDiagnosticRenderers.SYMBOL
        )
        map.put(CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION, "This property can only be used from external declarations.")
        map.put(RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION, "Runtime annotation cannot be put on external declaration.")
        map.put(
            RUNTIME_ANNOTATION_NOT_SUPPORTED,
            "Reflection is not supported in JavaScript target; therefore, annotations cannot be read at runtime."
        )
        map.put(EXTERNAL_ENUM_ENTRY_WITH_BODY, "Entry of external enum class cannot have a body.")
        map.put(EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE, "External type extends non-external type.")
        map.put(
            INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING,
            "Using value classes as parameter type or return type of external declarations is experimental."
        )
        map.put(
            ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING,
            "Using enum classes with an 'external' qualifier becomes deprecated and will be an error in future releases."
        )
        map.put(
            INLINE_CLASS_IN_EXTERNAL_DECLARATION,
            "Using value classes as parameter type or return type of external declarations is not supported."
        )
        map.put(EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION, "Function types with receivers are prohibited in external declarations.")
        map.put(
            NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN,
            "Annotation ''{0}'' is only allowed on member functions of declarations annotated with ''kotlin.js.native'' or on top-level extension functions.",
            FirDiagnosticRenderers.RENDER_TYPE
        )
        map.put(
            NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER,
            "Native {0}''s first parameter type must be ''kotlin.String'' or a subtype of ''kotlin.Number''.",
            CommonRenderers.STRING
        )
        map.put(NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS, "Native {0}''s parameter cannot have default value.", CommonRenderers.STRING)
        map.put(NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE, "Native getter's return type must be nullable.")
        map.put(
            NATIVE_SETTER_WRONG_RETURN_TYPE,
            "Native setter's return type must be 'Unit' or a supertype of the second parameter's type."
        )
        map.put(
            NATIVE_INDEXER_WRONG_PARAMETER_COUNT, "Expected {0} parameters for native {1}.",
            KtDiagnosticRenderers.TO_STRING,
            CommonRenderers.STRING
        )
        map.put(
            NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE,
            "Only external declarations are allowed in files marked with ''{0}'' annotation.",
            FirDiagnosticRenderers.RENDER_TYPE
        )
        map.put(
            JS_EXTERNAL_INHERITORS_ONLY,
            "External {0} cannot be a parent of non-external {1}.",
            FirDiagnosticRenderers.RENDER_CLASS_OR_OBJECT_NAME_QUOTED,
            FirDiagnosticRenderers.RENDER_CLASS_OR_OBJECT_NAME_QUOTED
        )
        map.put(
            JS_EXTERNAL_ARGUMENT,
            "Expected argument with external type, but type ''{0}'' is non-external.",
            FirDiagnosticRenderers.RENDER_TYPE
        )
        map.put(JS_NAME_PROHIBITED_FOR_EXTENSION_PROPERTY, "'@JsName' is prohibited for extension properties.")
        map.put(
            JS_BUILTIN_NAME_CLASH,
            "JavaScript name generated for this declaration clashes with built-in declaration ''{0}''.",
            CommonRenderers.STRING
        )
        map.put(NAME_CONTAINS_ILLEGAL_CHARS, "Name contains illegal chars that cannot appear in JavaScript identifier.")
        map.put(
            JS_NAME_CLASH,
            "JavaScript name ''{0}'' generated for this declaration clashes with other declarations:{1}",
            CommonRenderers.STRING,
            FirDiagnosticRenderers.SYMBOLS_ON_NEXT_LINES
        )
        map.put(
            JS_FAKE_NAME_CLASH,
            "JavaScript name ''{0}'' is generated for different inherited members:\n{1}{2}",
            CommonRenderers.STRING,
            FirDiagnosticRenderers.SYMBOL,
            FirDiagnosticRenderers.SYMBOLS_ON_NEXT_LINES
        )

        map.put(JS_NAME_IS_NOT_ON_ALL_ACCESSORS, "All property accessors must be annotated with '@JsName'.")
        map.put(JS_NAME_PROHIBITED_FOR_NAMED_NATIVE, "'@JsName' is prohibited for external declaration with explicit name.")
        map.put(JS_NAME_PROHIBITED_FOR_OVERRIDE, "'@JsName' is prohibited for overridden members.")
        map.put(JS_NAME_ON_PRIMARY_CONSTRUCTOR_PROHIBITED, "'@JsName' annotation is prohibited for primary constructors.")
        map.put(JS_NAME_ON_ACCESSOR_AND_PROPERTY, "'@JsName' can be either on a property or its accessors, not both of them.")
        map.put(WRONG_EXPORTED_DECLARATION, "Declaration of such kind ({0}) cannot be exported to JavaScript.", CommonRenderers.STRING)
        map.put(
            NON_EXPORTABLE_TYPE,
            "Exported declaration uses non-exportable {0} type ''{1}''.",
            CommonRenderers.STRING,
            FirDiagnosticRenderers.RENDER_TYPE,
        )
        map.put(
            NON_CONSUMABLE_EXPORTED_IDENTIFIER,
            "Exported declaration contains non-consumable identifier ''{0}'', which cannot be represented inside TS definitions and ESM.",
            CommonRenderers.STRING,
        )
        map.put(NAMED_COMPANION_IN_EXPORTED_INTERFACE, "Named companions are not allowed inside exported interfaces.")
        map.put(JS_STATIC_NOT_IN_CLASS_COMPANION, "Only members of class companion objects can be annotated with '@JsStatic'.")
        map.put(JS_STATIC_ON_NON_PUBLIC_MEMBER, "Only public members of class companion objects can be annotated with '@JsStatic'.")
        map.put(JS_STATIC_ON_CONST, "'@JsStatic' annotation is useless for const.")
        map.put(JS_STATIC_ON_OVERRIDE, "Override member of a companion object cannot be '@JsStatic'.")
    }
}
