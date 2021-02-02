/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.LanguageFeatureMessageRenderer
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.AMBIGUOUS_CALLS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.DECLARATION_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.FIR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.FQ_NAMES_IN_TYPES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.NULLABLE_STRING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.RENDER_CLASS_OR_OBJECT
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.RENDER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.SYMBOL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.SYMBOLS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.VARIABLE_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.VISIBILITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.WHEN_MISSING_CASES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_INLINE_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_DELEGATED_PROPERTY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_FUNCTION_WITH_BODY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_PROPERTY_WITH_GETTER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_PROPERTY_WITH_INITIALIZER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_PROPERTY_WITH_SETTER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_SUPER_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ANNOTATION_CLASS_MEMBER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ARGUMENT_PASSED_TWICE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ARGUMENT_TYPE_MISMATCH
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ASSIGNED_VALUE_IS_NEVER_READ
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ASSIGN_OPERATOR_AMBIGUITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.BACKING_FIELD_IN_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.BREAK_OR_CONTINUE_OUTSIDE_A_LOOP
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CALLABLE_REFERENCE_LHS_NOT_A_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CANNOT_CHANGE_ACCESS_PRIVILEGE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CANNOT_INFER_PARAMETER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CAN_BE_VAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CATCH_PARAMETER_WITH_DEFAULT_VALUE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CLASS_IN_SUPERTYPE_FOR_ENUM
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CLASS_LITERAL_LHS_NOT_A_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.COMPONENT_FUNCTION_AMBIGUITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.COMPONENT_FUNCTION_MISSING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.COMPONENT_FUNCTION_ON_NULLABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CONFLICTING_OVERLOADS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CONFLICTING_PROJECTION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CONFLICTING_UPPER_BOUNDS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CONSTRUCTOR_IN_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CONSTRUCTOR_IN_OBJECT
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CONST_VAL_WITHOUT_INITIALIZER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CONST_VAL_WITH_DELEGATE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CONST_VAL_WITH_GETTER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CONST_VAL_WITH_NON_CONST_INITIALIZER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CYCLIC_CONSTRUCTOR_DELEGATION_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.CYCLIC_GENERIC_UPPER_BOUND
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DATA_CLASS_NOT_PROPERTY_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DATA_CLASS_VARARG_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DATA_CLASS_WITHOUT_PARAMETERS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DELEGATED_PROPERTY_INSIDE_INLINE_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DELEGATED_PROPERTY_IN_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DELEGATION_IN_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DEPRECATED_MODIFIER_PAIR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DEPRECATED_TYPE_PARAMETER_SYNTAX
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DESERIALIZATION_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DYNAMIC_UPPER_BOUND
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EMPTY_RANGE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ENUM_AS_SUPERTYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ERROR_FROM_JAVA_RESOLUTION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ERROR_IN_CONTRACT_DESCRIPTION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPECTED_DECLARATION_WITH_BODY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPECTED_DELEGATED_PROPERTY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPECTED_LATEINIT_PROPERTY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPECTED_PRIVATE_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPECTED_PROPERTY_INITIALIZER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPLICIT_DELEGATION_CALL_REQUIRED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPOSED_FUNCTION_RETURN_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPOSED_PARAMETER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPOSED_PROPERTY_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPOSED_RECEIVER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPOSED_SUPER_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPOSED_SUPER_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPOSED_TYPE_PARAMETER_BOUND
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXTENSION_PROPERTY_WITH_BACKING_FIELD
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FINAL_UPPER_BOUND
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FORBIDDEN_VARARG_PARAMETER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUNCTION_DECLARATION_WITH_NO_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_CONSTRUCTOR_REFERENCE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.GENERIC_THROWABLE_SUBCLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.HIDDEN
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ILLEGAL_CONST_EXPRESSION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ILLEGAL_UNDERSCORE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INAPPLICABLE_CANDIDATE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INAPPLICABLE_INFIX_MODIFIER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INAPPLICABLE_LATEINIT_MODIFIER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INCOMPATIBLE_MODIFIERS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INFERENCE_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INITIALIZER_TYPE_MISMATCH
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INLINE_CLASS_CANNOT_BE_RECURSIVE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INLINE_CLASS_CANNOT_EXTEND_CLASSES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INLINE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INLINE_CLASS_NOT_FINAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INLINE_CLASS_NOT_TOP_LEVEL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INNER_CLASS_INSIDE_INLINE_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INSTANCE_ACCESS_BEFORE_SUPER_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INTERFACE_WITH_SUPERCLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INVALID_IF_AS_EXPRESSION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INVALID_TYPE_OF_ANNOTATION_MEMBER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.LEAKED_IN_PLACE_LAMBDA
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.LOCAL_ANNOTATION_CLASS_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.LOCAL_INTERFACE_NOT_ALLOWED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.LOCAL_OBJECT_NOT_ALLOWED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MANY_COMPANION_OBJECTS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MISPLACED_TYPE_PARAMETER_CONSTRAINTS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MISSING_VAL_ON_ANNOTATION_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MULTIPLE_VARARG_PARAMETERS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MUST_BE_INITIALIZED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NAMED_ARGUMENTS_NOT_ALLOWED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NAMED_PARAMETER_NOT_FOUND
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NESTED_CLASS_NOT_ALLOWED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NONE_APPLICABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NON_FINAL_MEMBER_IN_FINAL_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NON_FINAL_MEMBER_IN_OBJECT
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NON_MEMBER_FUNCTION_NO_BODY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NON_PRIVATE_CONSTRUCTOR_IN_ENUM
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NON_VARARG_SPREAD
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NOTHING_TO_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NOT_AN_ANNOTATION_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NOT_A_LOOP_LABEL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NOT_A_SUPERTYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NO_ELSE_IN_WHEN
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NO_GET_METHOD
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NO_SET_METHOD
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NO_THIS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NO_VALUE_FOR_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NULLABLE_TYPE_IN_CLASS_LITERAL_LHS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NULLABLE_TYPE_OF_ANNOTATION_MEMBER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ONLY_ONE_CLASS_BOUND_ALLOWED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.OTHER_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.OVERLOAD_RESOLUTION_AMBIGUITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.OVERRIDING_FINAL_MEMBER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.PRIVATE_FUNCTION_WITH_NO_BODY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.PRIVATE_PROPERTY_IN_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.PRIVATE_SETTER_FOR_OPEN_PROPERTY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.PROPERTY_INITIALIZER_IN_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.PROPERTY_INITIALIZER_NO_BACKING_FIELD
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.PROPERTY_TYPE_MISMATCH_ON_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.PROPERTY_WITH_BACKING_FIELD_INSIDE_INLINE_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.PROPERTY_WITH_NO_TYPE_NO_INITIALIZER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RECURSION_IN_IMPLICIT_TYPES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RECURSION_IN_SUPERTYPES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_CALL_OF_CONVERSION_METHOD
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_EXPLICIT_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_MODALITY_MODIFIER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_MODIFIER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_OPEN_IN_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_RETURN_UNIT_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_SETTER_PARAMETER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_VISIBILITY_MODIFIER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REIFIED_TYPE_IN_CATCH_CLAUSE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REIFIED_TYPE_PARAMETER_NO_INLINE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REPEATED_BOUND
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REPEATED_MODIFIER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RESERVED_MEMBER_INSIDE_INLINE_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RETURN_NOT_ALLOWED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RETURN_TYPE_MISMATCH
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RETURN_TYPE_MISMATCH_ON_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SEALED_CLASS_CONSTRUCTOR_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SEALED_SUPERTYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SEALED_SUPERTYPE_IN_LOCAL_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SUPERTYPES_FOR_ANNOTATION_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SUPERTYPE_INITIALIZED_IN_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SUPERTYPE_NOT_A_CLASS_OR_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SUPERTYPE_NOT_INITIALIZED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SUPER_IS_NOT_AN_EXPRESSION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SUPER_NOT_AVAILABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SYNTAX
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TOO_MANY_ARGUMENTS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TOPLEVEL_TYPEALIASES_ONLY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TYPE_CANT_BE_USED_FOR_CONST_VAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TYPE_MISMATCH
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TYPE_PARAMETERS_IN_ENUM
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TYPE_PARAMETERS_IN_OBJECT
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TYPE_PARAMETERS_NOT_ALLOWED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TYPE_PARAMETER_AS_REIFIED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TYPE_PARAMETER_IN_CATCH_CLAUSE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TYPE_PARAMETER_IS_NOT_AN_EXPRESSION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TYPE_PARAMETER_ON_LHS_OF_DOT
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNEXPECTED_SAFE_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNINITIALIZED_ENUM_COMPANION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNINITIALIZED_ENUM_ENTRY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNINITIALIZED_VARIABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNNECESSARY_LATEINIT
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNNECESSARY_NOT_NULL_ASSERTION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNNECESSARY_SAFE_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNRESOLVED_LABEL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNRESOLVED_REFERENCE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNSAFE_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNSAFE_IMPLICIT_INVOKE_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNSAFE_INFIX_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNSAFE_OPERATOR_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNSUPPORTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNSUPPORTED_FEATURE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNUSED_VARIABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UPPER_BOUND_VIOLATED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.USELESS_VARARG_ON_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VALUE_CLASS_CANNOT_BE_CLONEABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VAL_REASSIGNMENT
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VAL_WITH_SETTER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VARARG_OUTSIDE_PARENTHESES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VARIABLE_EXPECTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VARIABLE_INITIALIZER_IS_REDUNDANT
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VARIABLE_NEVER_READ
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VAR_ANNOTATION_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VAR_OVERRIDDEN_BY_VAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.VAR_TYPE_MISMATCH_ON_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.WRONG_INVOCATION_KIND
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.WRONG_MODIFIER_TARGET
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.WRONG_SETTER_PARAMETER_TYPE

@Suppress("unused")
class FirDefaultErrorMessages : DefaultErrorMessages.Extension {
    override fun getMap(): DiagnosticFactoryToRendererMap {
        return MAP.psiDiagnosticMap
    }

    companion object {
        fun getRendererForDiagnostic(diagnostic: FirDiagnostic<*>): FirDiagnosticRenderer<*> {
            val factory = diagnostic.factory
            return MAP[factory] ?: factory.firRenderer
        }

        // * - The old FE reports these diagnostics with additional parameters
        // & - New diagnostic that has no analogues in the old FE
        // + - Better message required
        // # - The new diagnostic differs from the old FE's one
        val MAP = FirDiagnosticFactoryToRendererMap("FIR").also { map ->
            // Meta-errors
            map.put(UNSUPPORTED, "Unsupported [{0}]", TO_STRING)
            map.put(UNSUPPORTED_FEATURE, "{0}", LanguageFeatureMessageRenderer(LanguageFeatureMessageRenderer.Type.UNSUPPORTED))

            // Miscellaneous
            map.put(SYNTAX, "Syntax error")
            map.put(OTHER_ERROR, "Unknown (other) error")

            // General syntax
            map.put(ILLEGAL_CONST_EXPRESSION, "Illegal const expression")
            map.put(ILLEGAL_UNDERSCORE, "Illegal underscore")
//            map.put(EXPRESSION_REQUIRED, ...) // &
            map.put(BREAK_OR_CONTINUE_OUTSIDE_A_LOOP, "'break' and 'continue' are only allowed inside a loop")
            map.put(NOT_A_LOOP_LABEL, "The label does not denote a loop") // *
            map.put(VARIABLE_EXPECTED, "Variable expected")
            map.put(DELEGATION_IN_INTERFACE, "Interfaces cannot use delegation")
            map.put(NESTED_CLASS_NOT_ALLOWED, "{0} is not allowed here", TO_STRING)

            // Unresolved
            map.put(HIDDEN, "Symbol {0} is invisible", SYMBOL)
            map.put(UNRESOLVED_REFERENCE, "Unresolved reference: {0}", NULLABLE_STRING)
            map.put(UNRESOLVED_LABEL, "Unresolved label")
            map.put(DESERIALIZATION_ERROR, "Deserialization error")
            map.put(ERROR_FROM_JAVA_RESOLUTION, "Java resolution error")
//            map.put(UNKNOWN_CALLABLE_KIND, ...) // &
//            map.put(MISSING_STDLIB_CLASS, ...) // &
            map.put(NO_THIS, "'this' is not defined in this context")

            // Super
            map.put(SUPER_IS_NOT_AN_EXPRESSION, "Super cannot be a callee")
            map.put(SUPER_NOT_AVAILABLE, "No supertypes are accessible in this context")
            map.put(ABSTRACT_SUPER_CALL, "Abstract member cannot be accessed directly")
            map.put(
                INSTANCE_ACCESS_BEFORE_SUPER_CALL,
                "Cannot access ''{0}'' before superclass constructor has been called",
                TO_STRING
            )

            // Supertypes
            map.put(ENUM_AS_SUPERTYPE, "Enum as supertype")
            map.put(RECURSION_IN_SUPERTYPES, "Recursion in supertypes")
            map.put(NOT_A_SUPERTYPE, "Not an immediate supertype")
            map.put(SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE, "Superclass is not accessible from interface")
            map.put(
                QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE,
                "Explicitly qualified supertype is extended by another supertype ''{0}''",
                TO_STRING
            )
            map.put(SUPERTYPE_INITIALIZED_IN_INTERFACE, "Interfaces cannot initialize supertypes")
            map.put(INTERFACE_WITH_SUPERCLASS, "An interface cannot inherit from a class")
            map.put(CLASS_IN_SUPERTYPE_FOR_ENUM, "Enum class cannot inherit from classes")
            map.put(SEALED_SUPERTYPE, "This type is sealed, so it can be inherited by only its own nested classes or objects")
            map.put(SEALED_SUPERTYPE_IN_LOCAL_CLASS, "Local class cannot extend a sealed class")
            map.put(SUPERTYPE_NOT_A_CLASS_OR_INTERFACE, "Supertype is not a class or interface", TO_STRING)


            // Constructor problems
            map.put(CONSTRUCTOR_IN_OBJECT, "Constructors are not allowed for objects")
            map.put(CONSTRUCTOR_IN_INTERFACE, "An interface may not have a constructor")
            map.put(NON_PRIVATE_CONSTRUCTOR_IN_ENUM, "Constructor must be private in enum class")
            map.put(NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED, "Constructor must be private or protected in sealed class")
            map.put(CYCLIC_CONSTRUCTOR_DELEGATION_CALL, "There's a cycle in the delegation calls chain")
            map.put(PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED, "Primary constructor call expected")
            map.put(SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR, "Supertype initialization is impossible without primary constructor")
            map.put(DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR, "Call to super is not allowed in enum constructor")
            map.put(PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS, "Primary constructor required for data class")
            map.put(
                EXPLICIT_DELEGATION_CALL_REQUIRED,
                "Explicit 'this' or 'super' call is required. There is no constructor in superclass that can be called without arguments"
            )
            map.put(SEALED_CLASS_CONSTRUCTOR_CALL, "Sealed types cannot be instantiated")
            map.put(DATA_CLASS_WITHOUT_PARAMETERS, "Data class must have at least one primary constructor parameter")
            map.put(DATA_CLASS_VARARG_PARAMETER, "Primary constructor vararg parameters are forbidden for data classes")
            map.put(DATA_CLASS_NOT_PROPERTY_PARAMETER, "Data class primary constructor must have only property (val / var) parameters")

            // Annotations
            map.put(ANNOTATION_CLASS_MEMBER, "Members are not allowed in annotation class")
            map.put(ANNOTATION_ARGUMENT_MUST_BE_CONST, "An annotation argument must be a compile-time constant")
            map.put(
                ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT,
                "Default value of annotation parameter must be a compile-time constant"
            )
            map.put(LOCAL_ANNOTATION_CLASS_ERROR, "Annotation class cannot be local")
            map.put(MISSING_VAL_ON_ANNOTATION_PARAMETER, "'val' keyword is missing on annotation parameter")
            map.put(NULLABLE_TYPE_OF_ANNOTATION_MEMBER, "An annotation parameter cannot be nullable")
            map.put(INVALID_TYPE_OF_ANNOTATION_MEMBER, "Invalid type of annotation member")
            map.put(VAR_ANNOTATION_PARAMETER, "An annotation parameter cannot be 'var'")
            map.put(NOT_AN_ANNOTATION_CLASS, "Illegal annotation class: {0}", NULLABLE_STRING)
            map.put(SUPERTYPES_FOR_ANNOTATION_CLASS, "Annotation class cannot have supertypes")

            // Exposed visibility group // #
            map.put(
                EXPOSED_TYPEALIAS_EXPANDED_TYPE,
                "{0} typealias exposes {2} in expanded type ''{1}''",
                TO_STRING,
                DECLARATION_NAME,
                TO_STRING
            )
            map.put(
                EXPOSED_FUNCTION_RETURN_TYPE,
                "{0} function exposes its {2} return type ''{1}''",
                TO_STRING,
                DECLARATION_NAME,
                TO_STRING
            )
            map.put(EXPOSED_RECEIVER_TYPE, "{0} member exposes its {2} receiver type ''{1}''", TO_STRING, DECLARATION_NAME, TO_STRING)
            map.put(EXPOSED_PROPERTY_TYPE, "{0} property exposes its {2} type ''{1}''", TO_STRING, DECLARATION_NAME, TO_STRING)
            map.put(EXPOSED_PARAMETER_TYPE, "{0} function exposes its {2} parameter type ''{1}''", TO_STRING, DECLARATION_NAME, TO_STRING)
            map.put(EXPOSED_SUPER_INTERFACE, "{0} sub-interface exposes its {2} supertype ''{1}''", TO_STRING, DECLARATION_NAME, TO_STRING)
            map.put(EXPOSED_SUPER_CLASS, "{0} subclass exposes its {2} supertype ''{1}''", TO_STRING, DECLARATION_NAME, TO_STRING)
            map.put(
                EXPOSED_TYPE_PARAMETER_BOUND,
                "{0} generic exposes its {2} parameter bound type ''{1}''",
                TO_STRING,
                DECLARATION_NAME,
                TO_STRING
            )

            // Modifiers
            map.put(INAPPLICABLE_INFIX_MODIFIER, "''infix'' modifier is inapplicable on this function")
            map.put(REPEATED_MODIFIER, "Repeated ''{0}''", TO_STRING)
            map.put(REDUNDANT_MODIFIER, "Modifier ''{0}'' is redundant because ''{1}'' is present", TO_STRING, TO_STRING)
            map.put(DEPRECATED_MODIFIER_PAIR, "Modifier ''{0}'' is deprecated in presence of ''{1}''", TO_STRING, TO_STRING)
            map.put(INCOMPATIBLE_MODIFIERS, "Modifier ''{0}'' is incompatible with ''{1}''", TO_STRING, TO_STRING)
            map.put(REDUNDANT_OPEN_IN_INTERFACE, "Modifier 'open' is redundant for abstract interface members")
            map.put(WRONG_MODIFIER_TARGET, "Modifier ''{0}'' is not applicable to ''{1}''", TO_STRING, TO_STRING)

            // Classes and interfaces
            map.put(SUPERTYPE_NOT_INITIALIZED, "This type has a constructor, and thus must be initialized here")

            // Applicability
            map.put(NONE_APPLICABLE, "None of the following functions are applicable: {0}", SYMBOLS)
            map.put(INAPPLICABLE_CANDIDATE, "Inapplicable candidate(s): {0}", SYMBOL)
            map.put(INAPPLICABLE_LATEINIT_MODIFIER, "''lateinit'' modifier {0}", TO_STRING)
            map.put(VARARG_OUTSIDE_PARENTHESES, "Passing value as a vararg is only allowed inside a parenthesized argument list")
            map.put(NAMED_ARGUMENTS_NOT_ALLOWED, "Named arguments are not allowed for {0}", TO_STRING)
            map.put(NON_VARARG_SPREAD, "The spread operator (*foo) may only be applied in a vararg position")
            map.put(TOO_MANY_ARGUMENTS, "Too many arguments for {0}", FQ_NAMES_IN_TYPES)
            map.put(ARGUMENT_PASSED_TWICE, "An argument is already passed for this parameter")
            map.put(NO_VALUE_FOR_PARAMETER, "No value passed for parameter ''{0}''", NAME)
            map.put(NAMED_PARAMETER_NOT_FOUND, "Cannot find a parameter with this name: {0}", TO_STRING)

            map.put(ARGUMENT_TYPE_MISMATCH, "Argument type mismatch: actual type is {1} but {0} was expected", TO_STRING, TO_STRING)

            // Ambiguity
            map.put(OVERLOAD_RESOLUTION_AMBIGUITY, "Overload resolution ambiguity between candidates: {0}", SYMBOLS)
            map.put(ASSIGN_OPERATOR_AMBIGUITY, "Ambiguity between assign operator candidates: {0}", SYMBOLS)

            // Types & type parameters
            map.put(TYPE_MISMATCH, "Type mismatch: inferred type is {1} but {0} was expected", TO_STRING, TO_STRING)
            map.put(RECURSION_IN_IMPLICIT_TYPES, "Recursion in implicit types")
            map.put(INFERENCE_ERROR, "Inference error")
            map.put(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT, "Projections are not allowed on type arguments of functions and properties")
            map.put(UPPER_BOUND_VIOLATED, "Type argument is not within its bounds: should be subtype of ''{0}''", RENDER_TYPE)
            map.put(TYPE_ARGUMENTS_NOT_ALLOWED, "Type arguments are not allowed for type parameters") // *
            map.put(
                WRONG_NUMBER_OF_TYPE_ARGUMENTS,
                "{0,choice,0#No type arguments|1#One type argument|1<{0,number,integer} type arguments} expected for {1}",
                null,
                SYMBOL
            )
            map.put(TYPE_PARAMETERS_IN_OBJECT, "Type parameters are not allowed for objects")
//            map.put(ILLEGAL_PROJECTION_USAGE, ...) // &
            map.put(TYPE_PARAMETERS_IN_ENUM, "Enum class cannot have type parameters")
            map.put(
                CONFLICTING_PROJECTION,
                "Projection is conflicting with variance of the corresponding type parameter of {0}. Remove the projection or replace it with ''*''",
                TO_STRING
            )
            map.put(
                VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED,
                "Variance annotations are only allowed for type parameters of classes and interfaces"
            )
            map.put(CATCH_PARAMETER_WITH_DEFAULT_VALUE, "Catch clause parameter may not have a default value")
            map.put(REIFIED_TYPE_IN_CATCH_CLAUSE, "Reified type is forbidden for catch parameter")
            map.put(TYPE_PARAMETER_IN_CATCH_CLAUSE, "Type parameter is forbidden for catch parameter")

            map.put(
                KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE,
                "Declaration has an inconsistent return type. " +
                        "Please add upper bound Any for type parameter ''{0}'' or specify return type explicitly",
                SYMBOL
            )

            map.put(TYPE_PARAMETER_AS_REIFIED, "Cannot use ''{0}'' as reified type parameter. Use a class instead", SYMBOL)
            map.put(
                FINAL_UPPER_BOUND,
                "''{0}'' is a final type, and thus a value of the type parameter is predetermined",
                RENDER_TYPE
            )
            map.put(UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE, "Extension function type can not be used as an upper bound")

            map.put(
                BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER,
                "Type parameter cannot have any other bounds if it's bounded by another type parameter"
            )

            map.put(ONLY_ONE_CLASS_BOUND_ALLOWED, "Only one of the upper bounds can be a class")
            map.put(REPEATED_BOUND, "Type parameter already has this bound")

            map.put(
                CONFLICTING_UPPER_BOUNDS,
                "Upper bounds of {0} have empty intersection",
                SYMBOL
            )

            map.put(
                NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER,
                "{0} does not refer to a type parameter of {1}",
                TO_STRING,
                NAME
            )

            map.put(BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED, "Bounds are not allowed on type alias parameters")

            map.put(REIFIED_TYPE_PARAMETER_NO_INLINE, "Only type parameters of inline functions can be reified")

            map.put(TYPE_PARAMETERS_NOT_ALLOWED, "Type parameters are not allowed here")

            map.put(TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER, "Type parameter of a property must be used in its receiver type")

            map.put(RETURN_TYPE_MISMATCH, "Return type mismatch: expected {0}, actual {1}", RENDER_TYPE, RENDER_TYPE)

            map.put(CYCLIC_GENERIC_UPPER_BOUND, "Type parameter has cyclic upper bounds")

            map.put(DEPRECATED_TYPE_PARAMETER_SYNTAX, "Type parameters must be placed before the name of the function")

            map.put(
                MISPLACED_TYPE_PARAMETER_CONSTRAINTS,
                "If a type parameter has multiple constraints, they all need to be placed in the 'where' clause"
            )

            map.put(DYNAMIC_UPPER_BOUND, "Dynamic type can not be used as an upper bound")

            // Reflection
            map.put(
                EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED,
                "''{0}'' is a member and an extension at the same time. References to such elements are not allowed",
                NAME
            )
            map.put(CALLABLE_REFERENCE_LHS_NOT_A_CLASS, "Left-hand side of a callable reference cannot be a type parameter")
            map.put(CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR, "Annotation class cannot be instantiated")

            map.put(CLASS_LITERAL_LHS_NOT_A_CLASS, "Only classes are allowed on the left hand side of a class literal")
            map.put(NULLABLE_TYPE_IN_CLASS_LITERAL_LHS, "Type in a class literal must not be nullable")
            map.put(
                EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS,
                "Expression in a class literal has a nullable type ''{0}'', use !! to make the type non-nullable",
                RENDER_TYPE
            )

            // Inline and value classes
            map.put(INLINE_CLASS_NOT_TOP_LEVEL, "Inline classes cannot be local or inner")
            map.put(INLINE_CLASS_NOT_FINAL, "Inline classes can be only final")
            map.put(ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_INLINE_CLASS, "Primary constructor is required for inline class")
            map.put(INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE, "Inline class must have exactly one primary constructor parameter")
            map.put(
                INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER,
                "Value class primary constructor must have only final read-only (val) property parameter"
            )
            map.put(PROPERTY_WITH_BACKING_FIELD_INSIDE_INLINE_CLASS, "Inline class cannot have properties with backing fields")
            map.put(DELEGATED_PROPERTY_INSIDE_INLINE_CLASS, "Inline class cannot have delegated properties")
            map.put(INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE, "Inline class cannot have value parameter of type ''{0}''", TO_STRING)
            map.put(INLINE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION, "Inline class cannot implement an interface by delegation")
            map.put(INLINE_CLASS_CANNOT_EXTEND_CLASSES, "Inline class cannot extend classes")
            map.put(INLINE_CLASS_CANNOT_BE_RECURSIVE, "Inline class cannot be recursive")
            map.put(RESERVED_MEMBER_INSIDE_INLINE_CLASS, "Member with the name ''{0}'' is reserved for future releases", TO_STRING)
            map.put(
                SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS,
                "Secondary constructors with bodies are reserved for for future releases"
            )
            map.put(INNER_CLASS_INSIDE_INLINE_CLASS, "Inline class cannot have inner classes")
            map.put(VALUE_CLASS_CANNOT_BE_CLONEABLE, "Value class cannot be Cloneable")

            // Overrides
            map.put(NOTHING_TO_OVERRIDE, "''{0}'' overrides nothing", DECLARATION_NAME)
            map.put(OVERRIDING_FINAL_MEMBER, "''{0}'' in ''{1}'' is final and cannot be overridden", NAME, TO_STRING)

            map.put(
                CANNOT_WEAKEN_ACCESS_PRIVILEGE,
                "Cannot weaken access privilege ''{0}'' for ''{1}'' in ''{2}''",
                VISIBILITY,
                NAME,
                TO_STRING
            )
            map.put(
                CANNOT_CHANGE_ACCESS_PRIVILEGE,
                "Cannot change access privilege ''{0}'' for ''{1}'' in ''{2}''",
                VISIBILITY,
                NAME,
                TO_STRING
            )

            map.put(
                ABSTRACT_MEMBER_NOT_IMPLEMENTED,
                "{0} is not abstract and does not implement abstract member {1}",
                RENDER_CLASS_OR_OBJECT,
                NAME
            )
            map.put(
                ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED,
                "{0} is not abstract and does not implement abstract base class member {1}",
                RENDER_CLASS_OR_OBJECT,
                NAME
            )
            map.put(
                MANY_IMPL_MEMBER_NOT_IMPLEMENTED,
                "{0} must override {1} because it inherits many implementations of it",
                RENDER_CLASS_OR_OBJECT,
                NAME
            )
            map.put(
                MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED,
                "{0} must override {1} because it inherits multiple interface methods of it",
                RENDER_CLASS_OR_OBJECT,
                NAME
            )

            map.put(
                RETURN_TYPE_MISMATCH_ON_OVERRIDE,
                "Return type of ''{0}'' is not a subtype of the return type of the overridden member ''{1}''",
                DECLARATION_NAME,
                DECLARATION_NAME
            )
            map.put(
                PROPERTY_TYPE_MISMATCH_ON_OVERRIDE,
                "Type of ''{0}'' is not a subtype of the overridden property ''{1}''",
                DECLARATION_NAME,
                DECLARATION_NAME
            )
            map.put(
                VAR_TYPE_MISMATCH_ON_OVERRIDE,
                "Type of ''{0}'' doesn''t match the type of the overridden var-property ''{1}''",
                DECLARATION_NAME,
                DECLARATION_NAME
            )

            map.put(
                VAR_OVERRIDDEN_BY_VAL,
                "Var-property {0} cannot be overridden by val-property {1}",
                FQ_NAMES_IN_TYPES,
                FQ_NAMES_IN_TYPES
            )
            map.put(NON_FINAL_MEMBER_IN_FINAL_CLASS, "'open' has no effect in a final class")
            map.put(NON_FINAL_MEMBER_IN_OBJECT, "'open' has no effect in an object")
            map.put(
                GENERIC_THROWABLE_SUBCLASS,
                "Subclass of 'Throwable' may not have type parameters"
            )
            map.put(
                INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS,
                "Inner class of generic class extending 'Throwable' is prohibited"
            )

            // Redeclarations
            map.put(MANY_COMPANION_OBJECTS, "Only one companion object is allowed per class")
            map.put(CONFLICTING_OVERLOADS, "Conflicting overloads: {0}", SYMBOLS) // *
            map.put(REDECLARATION, "Conflicting declarations: {0}", SYMBOLS) // *
            map.put(METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE, "An interface may not implement a method of 'Any'") // &

            // Invalid local declarations
            map.put(
                LOCAL_OBJECT_NOT_ALLOWED,
                "Named object ''{0}'' is a singleton and cannot be local. Try to use anonymous object instead",
                TO_STRING
            ) // +
            map.put(
                LOCAL_INTERFACE_NOT_ALLOWED,
                "''{0}'' is an interface so it cannot be local. Try to use anonymous object or abstract class instead",
                TO_STRING
            )

            // Functions
            map.put(
                ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS,
                "Abstract function ''{0}'' in non-abstract class ''{1}''",
                DECLARATION_NAME,
                DECLARATION_NAME
            )
            map.put(ABSTRACT_FUNCTION_WITH_BODY, "A function ''{0}'' with body cannot be abstract", DECLARATION_NAME)
            map.put(NON_ABSTRACT_FUNCTION_WITH_NO_BODY, "Function ''{0}'' without a body must be abstract", DECLARATION_NAME)
            map.put(PRIVATE_FUNCTION_WITH_NO_BODY, "Function ''{0}'' without body cannot be private", DECLARATION_NAME)
            map.put(NON_MEMBER_FUNCTION_NO_BODY, "Function ''{0}'' must have a body", DECLARATION_NAME)

            map.put(FUNCTION_DECLARATION_WITH_NO_NAME, "Function declaration must have a name")

            map.put(
                ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE,
                "An anonymous function is not allowed to specify default values for its parameters"
            )
            map.put(USELESS_VARARG_ON_PARAMETER, "Vararg on this parameter is useless")
            map.put(MULTIPLE_VARARG_PARAMETERS, "Multiple vararg-parameters are prohibited")
            map.put(FORBIDDEN_VARARG_PARAMETER_TYPE, "Forbidden vararg parameter type: {0}", RENDER_TYPE)
            map.put(VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION, "A type annotation is required on a value parameter")
            map.put(CANNOT_INFER_PARAMETER_TYPE, "cannot infer a type for this parameter. Please specify it explicitly.")

            // Fun interfaces
            map.put(FUN_INTERFACE_CONSTRUCTOR_REFERENCE, "Functional interface constructor references are prohibited")

            // Properties & accessors
            map.put(
                ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS,
                "Abstract property ''{0}'' in non-abstract class ''{1}''",
                DECLARATION_NAME,
                DECLARATION_NAME
            )
            map.put(PRIVATE_PROPERTY_IN_INTERFACE, "Abstract property in an interface cannot be private")

            map.put(ABSTRACT_PROPERTY_WITH_INITIALIZER, "Property with initializer cannot be abstract")
            map.put(PROPERTY_INITIALIZER_IN_INTERFACE, "Property initializers are not allowed in interfaces")
            map.put(
                PROPERTY_WITH_NO_TYPE_NO_INITIALIZER,
                "This property must either have a type annotation, be initialized or be delegated"
            )

            map.put(MUST_BE_INITIALIZED, "Property must be initialized")
            map.put(MUST_BE_INITIALIZED_OR_BE_ABSTRACT, "Property must be initialized or be abstract")
            map.put(EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT, "Extension property must have accessors or be abstract")
            map.put(UNNECESSARY_LATEINIT, "Lateinit is unnecessary: definitely initialized in constructors")

            map.put(BACKING_FIELD_IN_INTERFACE, "Property in an interface cannot have a backing field")
            map.put(EXTENSION_PROPERTY_WITH_BACKING_FIELD, "Extension property cannot be initialized because it has no backing field")
            map.put(PROPERTY_INITIALIZER_NO_BACKING_FIELD, "Initializer is not allowed here because this property has no backing field")

            map.put(ABSTRACT_DELEGATED_PROPERTY, "Delegated property cannot be abstract")
            map.put(DELEGATED_PROPERTY_IN_INTERFACE, "Delegated properties are not allowed in interfaces")

            map.put(ABSTRACT_PROPERTY_WITH_GETTER, "Property with getter implementation cannot be abstract")
            map.put(ABSTRACT_PROPERTY_WITH_SETTER, "Property with setter implementation cannot be abstract")
            map.put(PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY, "Private setters are not allowed for abstract properties")
            map.put(PRIVATE_SETTER_FOR_OPEN_PROPERTY, "Private setters are not allowed for open properties")
            map.put(VAL_WITH_SETTER, "A 'val'-property cannot have a setter")
            map.put(
                WRONG_SETTER_PARAMETER_TYPE,
                "Setter parameter type must be equal to the type of the property, i.e. ''{0}''",
                RENDER_TYPE,
                RENDER_TYPE
            )
            map.put(INITIALIZER_TYPE_MISMATCH, "Initializer type mismatch: expected {0}, actual {1}", RENDER_TYPE, RENDER_TYPE)

            map.put(CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT, "Const 'val' are only allowed on top level or in objects")
            map.put(CONST_VAL_WITH_GETTER, "Const 'val' should not have a getter")
            map.put(CONST_VAL_WITH_DELEGATE, "Const 'val' should not have a delegate")
            map.put(TYPE_CANT_BE_USED_FOR_CONST_VAL, "Const ''val'' has type ''{0}''. Only primitives and String are allowed", RENDER_TYPE)
            map.put(CONST_VAL_WITHOUT_INITIALIZER, "Const 'val' should have an initializer")
            map.put(CONST_VAL_WITH_NON_CONST_INITIALIZER, "Const 'val' initializer should be a constant value")

            // Multi-platform projects
            map.put(EXPECTED_DECLARATION_WITH_BODY, "Expected declaration must not have a body")
            map.put(EXPECTED_PROPERTY_INITIALIZER, "Expected property cannot have an initializer")
            map.put(EXPECTED_DELEGATED_PROPERTY, "Expected property cannot be delegated")
            map.put(EXPECTED_PRIVATE_DECLARATION, "Expected declaration cannot be private")
            map.put(EXPECTED_LATEINIT_PROPERTY, "Expected property cannot be lateinit")

            // Destructuring declaration
            map.put(INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION, "Initializer required for destructuring declaration")
            map.put(
                COMPONENT_FUNCTION_MISSING,
                "Destructuring declaration initializer of type {1} must have a ''{0}()'' function",
                TO_STRING,
                RENDER_TYPE
            )
            map.put(
                COMPONENT_FUNCTION_AMBIGUITY,
                "Function ''{0}''() is ambiguous for this expression: {1}",
                TO_STRING,
                AMBIGUOUS_CALLS
            )
            map.put(
                COMPONENT_FUNCTION_ON_NULLABLE,
                "Not nullable value required to call ''{0}()'' function of destructuring declaration initializer",
                TO_STRING
            )
            map.put(
                COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH,
                "''{0}()'' function returns ''{1}'', but ''{2}'' is expected",
                TO_STRING,
                RENDER_TYPE,
                RENDER_TYPE
            )

            // Control flow diagnostics
            map.put(UNINITIALIZED_VARIABLE, "{0} must be initialized before access", VARIABLE_NAME)
            map.put(UNINITIALIZED_ENUM_ENTRY, "Enum entry ''{0}'' is uninitialized here", VARIABLE_NAME)
            map.put(UNINITIALIZED_ENUM_COMPANION, "Companion object of enum class ''{0}'' is uninitialized here", SYMBOL)
            map.put(VAL_REASSIGNMENT, "Val cannot be reassigned", VARIABLE_NAME)
            map.put(VAL_REASSIGNMENT_VIA_BACKING_FIELD, "Reassignment of read-only property via backing field is deprecated", VARIABLE_NAME)
            map.put(VAL_REASSIGNMENT_VIA_BACKING_FIELD_ERROR, "Reassignment of read-only property via backing field", VARIABLE_NAME)
            map.put(
                WRONG_INVOCATION_KIND,
                "{2} wrong invocation kind: given {3} case, but {4} case is possible",
                SYMBOL,
                TO_STRING,
                TO_STRING
            )
            map.put(LEAKED_IN_PLACE_LAMBDA, "Leaked in-place lambda: {2}", SYMBOL)
            map.put(FirErrors.WRONG_IMPLIES_CONDITION, "Wrong implies condition")

            // Nullability
            map.put(
                UNSAFE_CALL,
                "Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type {0}",
                RENDER_TYPE
            )
            map.put(
                UNSAFE_IMPLICIT_INVOKE_CALL,
                "Reference has a nullable type ''{0}'', use explicit \"?.invoke\" to make a function-like call instead.",
                RENDER_TYPE
            )
            map.put(
                UNSAFE_INFIX_CALL,
                "Infix call corresponds to a dot-qualified call ''{0}.{1}({2})'' which is not allowed on a nullable receiver ''{0}''. " +
                        "Use ''?.''-qualified call instead",
                FIR,
                TO_STRING,
                FIR
            )
            map.put(
                UNSAFE_OPERATOR_CALL,
                "Operator call corresponds to a dot-qualified call ''{0}.{1}({2})'' which is not allowed on a nullable receiver ''{0}''. ",
                FIR,
                TO_STRING,
                FIR
            )
            map.put(UNNECESSARY_NOT_NULL_ASSERTION, "Unnecessary non-null assertion (!!) on a non-null receiver of type {0}", RENDER_TYPE)
            map.put(NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION, "Non-null assertion (!!) is called on a lambda expression")
            map.put(NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE, "Non-null assertion (!!) is called on a callable reference expression")
            map.put(UNNECESSARY_SAFE_CALL, "Unnecessary safe call on a non-null receiver of type {0}", RENDER_TYPE)
            map.put(UNEXPECTED_SAFE_CALL, "Safe-call is not allowed here")

            // When expressions
            map.put(NO_ELSE_IN_WHEN, "''when'' expression must be exhaustive, add necessary {0}", WHEN_MISSING_CASES)
            map.put(INVALID_IF_AS_EXPRESSION, "'if' must have both main and 'else' branches if used as an expression")

            // Context tracking
            map.put(TYPE_PARAMETER_IS_NOT_AN_EXPRESSION, "Type parameter ''{0}'' is not an expression", SYMBOL)
            map.put(
                TYPE_PARAMETER_ON_LHS_OF_DOT,
                "Type parameter ''{0}'' cannot have or inherit a companion object, so it cannot be on the left hand side of dot",
                SYMBOL
            )

            // Function contracts
            map.put(ERROR_IN_CONTRACT_DESCRIPTION, "Error in contract description", TO_STRING)

            // Conventions
            map.put(NO_GET_METHOD, "No get method providing array access")
            map.put(NO_SET_METHOD, "No set method providing array access")

            // Type alias
            map.put(TOPLEVEL_TYPEALIASES_ONLY, "Nested and local type aliases are not supported")

            // Returns
            map.put(RETURN_NOT_ALLOWED, "'return' is not allowed here")
            map.put(RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY, "Returns are not allowed for functions with expression body. Use block body in '{...}'")

            // Extended checkers group
            map.put(REDUNDANT_VISIBILITY_MODIFIER, "Redundant visibility modifier")
            map.put(REDUNDANT_MODALITY_MODIFIER, "Redundant modality modifier")
            map.put(REDUNDANT_RETURN_UNIT_TYPE, "Redundant return 'unit' type")
            map.put(REDUNDANT_EXPLICIT_TYPE, "Redundant explicit type")
            map.put(REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE, "Redundant string template")
            map.put(CAN_BE_VAL, "'var' can be 'val'")
            map.put(CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT, "Assignment can be replaced with operator assignment")
            map.put(REDUNDANT_CALL_OF_CONVERSION_METHOD, "Redundant call of conversion method")
            map.put(ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS, "Replace '==' with 'Arrays.equals'")
            map.put(EMPTY_RANGE, "Range is empty")
            map.put(REDUNDANT_SETTER_PARAMETER_TYPE, "Redundant setter parameter type")
            map.put(UNUSED_VARIABLE, "Variable is unused")
            map.put(ASSIGNED_VALUE_IS_NEVER_READ, "Assigned value is never read")
            map.put(VARIABLE_INITIALIZER_IS_REDUNDANT, "Initializer is redundant")
            map.put(VARIABLE_NEVER_READ, "Variable is never read")
        }
    }
}
