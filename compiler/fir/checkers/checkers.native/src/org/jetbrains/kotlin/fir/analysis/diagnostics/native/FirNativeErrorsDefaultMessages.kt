/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.native

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.RENDER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.SYMBOL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.SYMBOLS_ON_NEXT_LINES
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.CANNOT_CHECK_FOR_FORWARD_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.CONFLICTING_OBJC_OVERLOADS
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.CONSTRUCTOR_DOES_NOT_OVERRIDE_ANY_SUPER_CONSTRUCTOR
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.CONSTRUCTOR_MATCHES_SEVERAL_SUPER_CONSTRUCTORS
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.CONSTRUCTOR_OVERRIDES_ALREADY_OVERRIDDEN_OBJC_INITIALIZER
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.EMPTY_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.FORWARD_DECLARATION_AS_CLASS_LITERAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.IDENTITY_HASH_CODE_ON_VALUE_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_EXACT_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_OBJC_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_THREAD_LOCAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INCOMPATIBLE_OBJC_NAME_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INCOMPATIBLE_THROWS_INHERITED
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INCOMPATIBLE_THROWS_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_CHARACTERS_NATIVE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_OBJC_HIDES_TARGETS
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_OBJC_NAME_CHARS
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_OBJC_NAME_FIRST_CHAR
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_REFINES_IN_SWIFT_TARGETS
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.MISSING_EXACT_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.MUST_BE_OBJC_OBJECT_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.MUST_BE_UNIT_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.MUST_NOT_HAVE_EXTENSION_RECEIVER
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.NATIVE_SPECIFIC_ATOMIC
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.NON_LITERAL_OBJC_NAME_ARG
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.PROPERTY_MUST_BE_VAR
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.REDUNDANT_SWIFT_REFINEMENT
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.SUBTYPE_OF_HIDDEN_FROM_OBJC
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.CONFLICTING_ESCAPES_AND_ESCAPES_NOTHING
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.ESCAPES_MARKED_ON_NON_ESCAPING_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.ESCAPES_NOT_MARKED_ON_MUST_ESCAPE_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_ESCAPES_VALUE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_POINTS_TO_INDEX
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_POINTS_TO_VALUE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.MISSING_ESCAPE_ANALYSIS_ANNOTATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.MISSING_ESCAPES_FOR_MUST_ESCAPE_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.POINTS_TO_FROM_NON_ESCAPING_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.POINTS_TO_KIND_1_ONLY_FOR_RETURN
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.POINTS_TO_TO_NON_ESCAPING_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.UNUSED_ESCAPES_ANNOTATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.UNUSED_ESCAPES_NOTHING_ANNOTATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.UNUSED_POINTS_TO_ANNOTATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.THROWS_LIST_EMPTY
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.TWO_OR_LESS_PARAMETERS_ARE_SUPPORTED_HERE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.UNCHECKED_CAST_TO_FORWARD_DECLARATION

object FirNativeErrorsDefaultMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("FIR") { map ->
        map.put(THROWS_LIST_EMPTY, "Throws must have a non-empty class list.")
        map.put(INCOMPATIBLE_THROWS_OVERRIDE, "Member overrides different ''@Throws'' filter from ''{0}''.", SYMBOL)
        map.put(INCOMPATIBLE_THROWS_INHERITED, "Member inherits different ''@Throws'' filters from:{0}", SYMBOLS_ON_NEXT_LINES)
        map.put(
            MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND,
            "''@Throws'' on suspend declaration must have ''{0}'' (or any of its superclasses) listed.",
            TO_STRING
        )
        map.put(
            INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY,
            "'@SharedImmutable' is applicable only to 'val' with backing field or to property with delegation."
        )
        map.put(INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL, "'@SharedImmutable' is applicable only to top-level declarations.")
        map.put(
            INAPPLICABLE_THREAD_LOCAL,
            "'@ThreadLocal' is applicable only to property with backing field, to property with delegation, or to objects."
        )
        map.put(INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL, "'@ThreadLocal' is applicable only to top-level declarations.")
        map.put(INVALID_CHARACTERS_NATIVE, "Name {0}.", TO_STRING)
        map.put(REDUNDANT_SWIFT_REFINEMENT, "ObjC refined declarations cannot be refined in Swift.")
        map.put(
            INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE,
            "Refined declaration ''{0}'' overrides declarations with different or no refinement from:{1}",
            SYMBOL,
            SYMBOLS_ON_NEXT_LINES
        )
        map.put(
            INVALID_OBJC_HIDES_TARGETS,
            "'@HidesFromObjC' annotation is only applicable to annotations with targets CLASS, FUNCTION, and/or PROPERTY."
        )
        map.put(
            INVALID_REFINES_IN_SWIFT_TARGETS,
            "'@RefinesInSwift' annotation is only applicable to annotations with targets FUNCTION and/or PROPERTY."
        )
        map.put(INAPPLICABLE_OBJC_NAME, "'@ObjCName' is not applicable to overrides.")
        map.put(INVALID_OBJC_NAME, "'@ObjCName' must have a 'name' and/or 'swiftName'.")
        map.put(EMPTY_OBJC_NAME, "Empty '@ObjCName' names aren't supported.")
        map.put(INVALID_OBJC_NAME_CHARS, "''@ObjCName'' contains illegal characters ''{0}''.", TO_STRING)
        map.put(INVALID_OBJC_NAME_FIRST_CHAR, "''@ObjCName'' contains illegal first characters ''{0}''.", TO_STRING)
        map.put(
            INCOMPATIBLE_OBJC_NAME_OVERRIDE,
            "Member ''{0}'' inherits inconsistent ''@ObjCName'' from:{1}",
            SYMBOL,
            SYMBOLS_ON_NEXT_LINES
        )
        map.put(INAPPLICABLE_EXACT_OBJC_NAME, "Exact '@ObjCName' is only applicable to classes, objects, and interfaces.")
        map.put(MISSING_EXACT_OBJC_NAME, "Exact '@ObjCName' is required to have an ObjC name.")
        map.put(NON_LITERAL_OBJC_NAME_ARG, "'@ObjCName' accepts only literal 'String' and 'Boolean' values.")
        map.put(SUBTYPE_OF_HIDDEN_FROM_OBJC, "Only '@HiddenFromObjC' declaration can be a subtype of '@HiddenFromObjC' declaration.")


        map.put(
            CANNOT_CHECK_FOR_FORWARD_DECLARATION,
            "Cannot check for forward declaration ''{0}''.",
            RENDER_TYPE
        )
        map.put(
            UNCHECKED_CAST_TO_FORWARD_DECLARATION,
            "Unchecked cast to forward declaration from ''{0}'' to ''{1}''.",
            RENDER_TYPE,
            RENDER_TYPE
        )
        map.put(
            FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT,
            "Cannot pass forward declaration ''{0}'' for reified type parameter.",
            RENDER_TYPE
        )
        map.put(
            FORWARD_DECLARATION_AS_CLASS_LITERAL,
            "Cannot refer to forward declaration ''{0}'' from class literal.",
            RENDER_TYPE
        )
        map.put(TWO_OR_LESS_PARAMETERS_ARE_SUPPORTED_HERE, "Only 0, 1 or 2 parameters are supported here.")
        map.put(PROPERTY_MUST_BE_VAR, "''@{0}'' property must be var.", TO_STRING)
        map.put(MUST_NOT_HAVE_EXTENSION_RECEIVER, "''{0}'' cannot have extension receiver.", TO_STRING)
        map.put(
            MUST_BE_OBJC_OBJECT_TYPE,
            "Unexpected {0}: ''{1}''\nOnly Objective-C object types are supported here.",
            TO_STRING,
            RENDER_TYPE
        )
        map.put(
            MUST_BE_UNIT_TYPE,
            "Unexpected {0}: ''{1}''\nOnly ''Unit'' is supported here.",
            TO_STRING,
            RENDER_TYPE
        )
        map.put(
            CONSTRUCTOR_OVERRIDES_ALREADY_OVERRIDDEN_OBJC_INITIALIZER,
            "Constructor with ''@{0}'' overrides initializer that is already overridden explicitly.",
            TO_STRING
        )
        map.put(
            CONSTRUCTOR_DOES_NOT_OVERRIDE_ANY_SUPER_CONSTRUCTOR,
            "Constructor with ''@{0}'' doesn''t override any super class constructor.\nIt must completely match by parameter names and types.",
            TO_STRING
        )
        map.put(
            CONSTRUCTOR_MATCHES_SEVERAL_SUPER_CONSTRUCTORS,
            "Constructor with ''@{0}'' matches more than one of super constructors.",
            TO_STRING
        )
        map.put(
            CONFLICTING_OBJC_OVERLOADS,
            "Conflicting overloads:{0}\nAdd @ObjCSignatureOverride to allow collision for functions inherited from Objective-C.",
            SYMBOLS_ON_NEXT_LINES
        )
        map.put(
            INAPPLICABLE_OBJC_OVERRIDE,
            "@ObjCSignatureOverride is only allowed on methods overriding methods from Objective-C.",
        )
        map.put(
            NATIVE_SPECIFIC_ATOMIC,
            "Native-specific atomic type ''kotlin.concurrent.{0}'' is going to be deprecated soon." +
                    " Consider using ''kotlin.concurrent.atomics.{0}'' instead.",
            TO_STRING
        )
        map.put(
            IDENTITY_HASH_CODE_ON_VALUE_TYPE,
            "Call to ''kotlin.native.identityHashCode'' on an instance of value type ''{0}'' can have unexpected behavior.",
            RENDER_TYPE,
        )
        
        // Escape analysis annotation messages
        map.put(
            UNUSED_ESCAPES_ANNOTATION,
            "Unused '@Escapes': {0}",
            TO_STRING
        )
        map.put(
            UNUSED_ESCAPES_NOTHING_ANNOTATION,
            "Unused '@Escapes.Nothing': {0}",
            TO_STRING
        )
        map.put(
            UNUSED_POINTS_TO_ANNOTATION,
            "Unused '@PointsTo': {0}",
            TO_STRING
        )
        map.put(
            CONFLICTING_ESCAPES_AND_ESCAPES_NOTHING,
            "Conflicting '@Escapes' and '@Escapes.Nothing'"
        )
        map.put(
            MISSING_ESCAPE_ANALYSIS_ANNOTATION,
            "External function with parameters that may escape requires '@Escapes' or '@Escapes.Nothing' or '@PointsTo'"
        )
        map.put(
            MISSING_ESCAPES_FOR_MUST_ESCAPE_TYPE,
            "External function with parameters that must always escape is not marked by '@Escapes'"
        )
        map.put(
            INVALID_ESCAPES_VALUE,
            "@Escapes value is invalid: {0}",
            TO_STRING
        )
        map.put(
            ESCAPES_MARKED_ON_NON_ESCAPING_TYPE,
            "{0} is marked as escaping by '@Escapes', but the type cannot escape to the heap",
            TO_STRING
        )
        map.put(
            ESCAPES_NOT_MARKED_ON_MUST_ESCAPE_TYPE,
            "{0} is not marked as escaping by '@Escapes', but the type must always escape to the heap",
            TO_STRING
        )
        map.put(
            INVALID_POINTS_TO_VALUE,
            "@PointsTo value is invalid: {0}",
            TO_STRING
        )
        map.put(
            INVALID_POINTS_TO_INDEX,
            "@PointsTo value is invalid at index {0} nibble {1}: {2}",
            TO_STRING,
            TO_STRING,
            TO_STRING
        )
        map.put(
            POINTS_TO_KIND_1_ONLY_FOR_RETURN,
            "{0} is marked as pointing to {1} by '@PointsTo' with kind 1, but kind 1 is only allowed for the return value",
            TO_STRING,
            TO_STRING
        )
        map.put(
            POINTS_TO_FROM_NON_ESCAPING_TYPE,
            "{0} is marked as pointing to {1} by '@PointsTo', but {0}''s type cannot escape to the heap",
            TO_STRING,
            TO_STRING
        )
        map.put(
            POINTS_TO_TO_NON_ESCAPING_TYPE,
            "{0} is marked as pointing to {1} by '@PointsTo', but {1}''s type cannot escape to the heap",
            TO_STRING,
            TO_STRING
        )
    }
}
