/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.error

enum class ErrorTypeKind(val debugMessage: String, val isUnresolved: Boolean = false) {
    /* Unresolved types */
    UNRESOLVED_TYPE("Unresolved type for %s", true),
    UNRESOLVED_TYPE_PARAMETER_TYPE("Unresolved type parameter type", true),
    UNRESOLVED_CLASS_TYPE("Unresolved class %s", true),
    UNRESOLVED_JAVA_CLASS("Unresolved java class %s", true),
    UNRESOLVED_DECLARATION("Unresolved declaration %s", true),
    UNRESOLVED_KCLASS_CONSTANT_VALUE("Unresolved type for %s (arrayDimensions=%s)", true),
    UNRESOLVED_TYPE_ALIAS("Unresolved type alias %s"),

    /* Return types */
    RETURN_TYPE("Return type for %s cannot be resolved"),
    RETURN_TYPE_FOR_FUNCTION("Return type for function cannot be resolved"),
    RETURN_TYPE_FOR_PROPERTY("Return type for property %s cannot be resolved"),
    RETURN_TYPE_FOR_CONSTRUCTOR("Return type for constructor %s cannot be resolved"),
    IMPLICIT_RETURN_TYPE_FOR_FUNCTION("Implicit return type for function %s cannot be resolved"),
    IMPLICIT_RETURN_TYPE_FOR_PROPERTY("Implicit return type for property %s cannot be resolved"),
    IMPLICIT_RETURN_TYPE_FOR_PROPERTY_ACCESSOR("Implicit return type for property accessor %s cannot be resolved"),
    ERROR_TYPE_FOR_DESTRUCTURING_COMPONENT("%s() return type"),

    /* Recursion or cyclic */
    RECURSIVE_TYPE("Recursive type"),
    RECURSIVE_TYPE_ALIAS("Recursive type alias %s"),
    RECURSIVE_ANNOTATION_TYPE("Recursive annotation's type"),
    CYCLIC_UPPER_BOUNDS("Cyclic upper bounds"),
    CYCLIC_SUPERTYPES("Cyclic supertypes"),

    /* Resolution and type inference */
    UNINFERRED_LAMBDA_CONTEXT_RECEIVER_TYPE("Cannot infer a lambda context receiver type"),
    UNINFERRED_LAMBDA_PARAMETER_TYPE("Cannot infer a lambda parameter type"),
    UNINFERRED_TYPE_VARIABLE("Cannot infer a type variable %s"),
    RESOLUTION_ERROR_TYPE("Resolution error type (%s)"),
    ERROR_EXPECTED_TYPE("Error expected type"),
    ERROR_DATA_FLOW_TYPE("Error type for data flow"),
    ERROR_WHILE_RECONSTRUCTING_BARE_TYPE("Failed to reconstruct type %s"),
    UNABLE_TO_SUBSTITUTE_TYPE("Unable to substitute type (%s)"),

    /* Special internal error types */
    DONT_CARE("Special DONT_CARE type"),
    STUB_TYPE("Stub type %s"),
    FUNCTION_PLACEHOLDER_TYPE("Function placeholder type (arguments: %s)"),
    TYPE_FOR_RESULT("Stubbed 'Result' type"),
    TYPE_FOR_COMPILER_EXCEPTION("Error type for a compiler exception while analyzing %s"),

    /* Inconsistent types */
    ERROR_FLEXIBLE_TYPE("Error java flexible type with id %s. (%s..%s)"),
    ERROR_RAW_TYPE("Error raw type %s"),
    TYPE_WITH_MISMATCHED_TYPE_ARGUMENTS_AND_PARAMETERS("Inconsistent type %s (parameters.size = %s, arguments.size = %s)"),
    ILLEGAL_TYPE_RANGE_FOR_DYNAMIC("Illegal type range for dynamic type %s..%s"),

    /* Deserialization */
    CANNOT_LOAD_DESERIALIZE_TYPE_PARAMETER("Unknown type parameter %s. Please try recompiling module containing \"%s\""),
    CANNOT_LOAD_DESERIALIZE_TYPE_PARAMETER_BY_NAME("Couldn't deserialize type parameter %s in %s"),
    INCONSISTENT_SUSPEND_FUNCTION("Inconsistent suspend function type in metadata with constructor %s"),
    UNEXPECTED_FLEXIBLE_TYPE_ID("Unexpected id of a flexible type %s. (%s..%s)"),
    UNKNOWN_TYPE("Unknown type"),

    /* Stubs for not specified types */
    NO_TYPE_SPECIFIED("No type specified for %s"),
    NO_TYPE_FOR_LOOP_RANGE("Loop range has no type"),
    NO_TYPE_FOR_LOOP_PARAMETER("Loop parameter has no type"),
    MISSED_TYPE_FOR_PARAMETER("Missed a type for a value parameter %s"),
    MISSED_TYPE_ARGUMENT_FOR_TYPE_PARAMETER("Missed a type argument for a type parameter %s"),

    /* Illegal type usages */
    PARSE_ERROR_ARGUMENT("Error type for parse error argument %s"),
    STAR_PROJECTION_IN_CALL("Error type for star projection directly passing as a call type argument"),
    PROHIBITED_DYNAMIC_TYPE("Dynamic type in a not allowed context"),
    NOT_ANNOTATION_TYPE_IN_ANNOTATION_CONTEXT("Not an annotation type %s in the annotation context"),
    UNIT_RETURN_TYPE_FOR_INC_DEC("Unit type returned by inc or dec"),
    RETURN_NOT_ALLOWED("Return not allowed"),

    /* Plugin related */
    UNRESOLVED_PARCEL_TYPE("Unresolved 'Parcel' type", true),
    KAPT_ERROR_TYPE("Kapt error type"),
    SYNTHETIC_ELEMENT_ERROR_TYPE("Error type for synthetic element"),
    AD_HOC_ERROR_TYPE_FOR_LIGHTER_CLASSES_RESOLVE("Error type in ad hoc resolve for lighter classes"),

    /* Expressions related types */
    ERROR_EXPRESSION_TYPE("Error expression type"),
    ERROR_RECEIVER_TYPE("Error receiver type for %s"),
    ERROR_CONSTANT_VALUE("Error constant value %s"),
    EMPTY_CALLABLE_REFERENCE("Empty callable reference"),
    UNSUPPORTED_CALLABLE_REFERENCE_TYPE("Unsupported callable reference type %s"),
    TYPE_FOR_DELEGATION("Error delegation type for %s"),

    /* Declaration related types */
    UNAVAILABLE_TYPE_FOR_DECLARATION("Type is unavailable for declaration %s"),
    ERROR_TYPE_PARAMETER("Error type parameter"),
    ERROR_TYPE_PROJECTION("Error type projection"),
    ERROR_SUPER_TYPE("Error super type"),
    SUPER_TYPE_FOR_ERROR_TYPE("Supertype of error type %s"),
    ERROR_PROPERTY_TYPE("Error property type"),
    ERROR_CLASS("Error class"),
    TYPE_FOR_ERROR_TYPE_CONSTRUCTOR("Type for error type constructor (%s)"),
    INTERSECTION_OF_ERROR_TYPES("Intersection of error types %s"),
    CANNOT_COMPUTE_ERASED_BOUND("Cannot compute erased upper bound of a type parameter %s"),

    /* Couldn't load a type */
    NOT_FOUND_UNSIGNED_TYPE("Unsigned type %s not found"),
    ERROR_ENUM_TYPE("Not found the corresponding enum class for given enum entry %s.%s"),
    NO_RECORDED_TYPE("Not found recorded type for %s"),
    NOT_FOUND_DESCRIPTOR_FOR_FUNCTION("Descriptor not found for function %s"),
    NOT_FOUND_DESCRIPTOR_FOR_CLASS("Cannot build class type, descriptor not found for builder %s"),
    NOT_FOUND_DESCRIPTOR_FOR_TYPE_PARAMETER("Cannot build type parameter type, descriptor not found for builder %s"),
    UNMAPPED_ANNOTATION_TARGET_TYPE("Type for unmapped Java annotation target to Kotlin one"), // java.lang.annotation.Target -> kotlin.annotation.Target
    UNKNOWN_ARRAY_ELEMENT_TYPE_OF_ANNOTATION_ARGUMENT("Unknown type for an array element of a java annotation argument"),
    NOT_FOUND_FQNAME_FOR_JAVA_ANNOTATION("No fqName for annotation %s"),
    NOT_FOUND_FQNAME("No fqName for %s"),

    /* Other error types */
    TYPE_FOR_GENERATED_ERROR_EXPRESSION("Type for generated error expression"),
    ;
}
