/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.jvm

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers.STRING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.RENDER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.SYMBOL
import org.jetbrains.kotlin.fir.analysis.diagnostics.checkMissingMessages
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.CONCURRENT_HASH_MAP_CONTAINS_OPERATOR
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.CONFLICTING_JVM_DECLARATIONS
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.DELEGATION_BY_IN_JVM_RECORD
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.DEPRECATED_JAVA_ANNOTATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.ENUM_JVM_RECORD
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.EXTERNAL_DECLARATION_CANNOT_BE_INLINED
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.EXTERNAL_DECLARATION_CANNOT_HAVE_BODY
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.EXTERNAL_DECLARATION_IN_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.FIELD_IN_JVM_RECORD
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.FUNCTION_DELEGATE_MEMBER_NAME_CLASH
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.ILLEGAL_JVM_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.INAPPLICABLE_JVM_FIELD
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.INAPPLICABLE_JVM_FIELD_WARNING
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.INAPPLICABLE_JVM_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.INNER_JVM_RECORD
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JAVA_SHADOWED_PROTECTED_FIELD_REFERENCE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JAVA_TYPE_MISMATCH
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_DEFAULT_IN_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_DEFAULT_IN_JVM6_TARGET
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_DEFAULT_NOT_IN_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_DEFAULT_REQUIRED_FOR_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_DEFAULT_WITH_COMPATIBILITY_IN_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_DEFAULT_WITH_COMPATIBILITY_NOT_ON_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_EXPOSE_BOXED_WITHOUT_INLINE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_INLINE_WITHOUT_VALUE_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_PACKAGE_NAME_CANNOT_BE_EMPTY
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_PACKAGE_NAME_MUST_BE_VALID_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_RECORD_EXTENDS_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_RECORD_NOT_LAST_VARARG_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_RECORD_NOT_VAL_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_STATIC_ON_CONST_OR_JVM_FIELD
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_STATIC_ON_EXTERNAL_IN_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_STATIC_ON_NON_PUBLIC_MEMBER
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JVM_SYNTHETIC_ON_DELEGATE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.LOCAL_JVM_RECORD
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.NON_DATA_CLASS_JVM_RECORD
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.NON_FINAL_JVM_RECORD
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.NON_SOURCE_REPEATED_ANNOTATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.OVERLOADS_ABSTRACT
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.OVERLOADS_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.OVERLOADS_LOCAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.OVERLOADS_PRIVATE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.OVERRIDE_CANNOT_BE_STATIC
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.REDUNDANT_REPEATABLE_ANNOTATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.REPEATED_ANNOTATION_TARGET6
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.REPEATED_ANNOTATION_WITH_CONTAINER
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.STRICTFP_ON_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.SUPER_CALL_WITH_DEFAULT_PARAMETERS
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.SUSPENSION_POINT_INSIDE_CRITICAL_SECTION
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.SYNCHRONIZED_IN_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.SYNCHRONIZED_ON_ABSTRACT
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.SYNCHRONIZED_ON_INLINE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.SYNCHRONIZED_ON_SUSPEND
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.UPPER_BOUND_CANNOT_BE_ARRAY
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.VOLATILE_ON_DELEGATE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.VOLATILE_ON_VALUE

object FirJvmErrorsDefaultMessages : BaseDiagnosticRendererFactory() {

    override val MAP = KtDiagnosticFactoryToRendererMap("FIR").also { map ->
        map.put(CONFLICTING_JVM_DECLARATIONS, "Platform declaration clash")
        map.put(JAVA_TYPE_MISMATCH, "Java type mismatch expected {0} but found {1}. Use explicit cast", RENDER_TYPE, RENDER_TYPE)
        map.put(UPPER_BOUND_CANNOT_BE_ARRAY, "Upper bound of a type parameter cannot be an array")
        map.put(STRICTFP_ON_CLASS, "'@Strictfp' annotation on classes is unsupported yet")
        map.put(VOLATILE_ON_VALUE, "'@Volatile' annotation cannot be used on immutable properties")
        map.put(VOLATILE_ON_DELEGATE, "'@Volatile' annotation cannot be used on delegated properties")
        map.put(SYNCHRONIZED_ON_ABSTRACT, "'@Synchronized' annotation cannot be used on abstract functions")
        map.put(SYNCHRONIZED_ON_INLINE, "'@Synchronized' annotation has no effect on inline functions")
        map.put(SYNCHRONIZED_ON_SUSPEND, "@Synchronized annotation is not applicable to suspend functions and lambdas")
        map.put(SYNCHRONIZED_IN_INTERFACE, "'@Synchronized' annotation cannot be used on interface members")
        map.put(OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS, "'@JvmOverloads' annotation has no effect for methods without default arguments")
        map.put(OVERLOADS_ABSTRACT, "'@JvmOverloads' annotation cannot be used on abstract methods")
        map.put(OVERLOADS_INTERFACE, "'@JvmOverloads' annotation cannot be used on interface methods")
        map.put(OVERLOADS_PRIVATE, "'@JvmOverloads' annotation has no effect on private declarations")
        map.put(OVERLOADS_LOCAL, "'@JvmOverloads' annotation cannot be used on local declarations")
        map.put(
            OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR,
            "'@JvmOverloads' annotation cannot be used on constructors of annotation classes"
        )
        map.put(DEPRECATED_JAVA_ANNOTATION, "This annotation is deprecated in Kotlin. Use ''@{0}'' instead", TO_STRING)
        map.put(POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION, "Only named arguments are available for Java annotations")
        map.put(JVM_PACKAGE_NAME_CANNOT_BE_EMPTY, "'@JvmPackageName' annotation value cannot be empty")
        map.put(
            JVM_PACKAGE_NAME_MUST_BE_VALID_NAME,
            "'@JvmPackageName' annotation value must be a valid dot-qualified name of a package"
        )
        map.put(
            JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES,
            "'@JvmPackageName' annotation is not supported for files with class declarations"
        )
        map.put(
            SUPER_CALL_WITH_DEFAULT_PARAMETERS,
            "Super-calls with default arguments are not allowed. Please specify all arguments of ''super.{0}'' explicitly",
            TO_STRING
        )

        map.put(LOCAL_JVM_RECORD, "Local @JvmRecord classes are not allowed")
        map.put(NON_FINAL_JVM_RECORD, "@JvmRecord class should be final")
        map.put(ENUM_JVM_RECORD, "@JvmRecord class should not be an enum")
        map.put(
            JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS,
            "Primary constructor with parameters is required for @JvmRecord class"
        )
        map.put(JVM_RECORD_NOT_VAL_PARAMETER, "Constructor parameter of @JvmRecord class should be a val")
        map.put(JVM_RECORD_NOT_LAST_VARARG_PARAMETER, "Only the last constructor parameter of @JvmRecord may be a vararg")
        map.put(JVM_RECORD_EXTENDS_CLASS, "Record cannot inherit a class", RENDER_TYPE)
        map.put(INNER_JVM_RECORD, "@JvmRecord class should not be inner")
        map.put(FIELD_IN_JVM_RECORD, "It's not allowed to have non-constructor properties with backing field in @JvmRecord class")
        map.put(DELEGATION_BY_IN_JVM_RECORD, "Delegation is not allowed for @JvmRecord classes")
        map.put(NON_DATA_CLASS_JVM_RECORD, "Only data classes are allowed to be marked as @JvmRecord")
        map.put(ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE, "Classes cannot have explicit 'java.lang.Record' supertype")

        map.put(OVERRIDE_CANNOT_BE_STATIC, "Override member cannot be '@JvmStatic' in object")
        map.put(
            JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION,
            "Only members in named objects and companion objects of classes can be annotated with '@JvmStatic'"
        )
        map.put(
            JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION,
            "Only members in named objects and companion objects can be annotated with '@JvmStatic'"
        )
        map.put(
            JVM_STATIC_ON_NON_PUBLIC_MEMBER,
            "Only public members in interface companion objects can be annotated with '@JvmStatic'"
        )
        map.put(
            JVM_STATIC_ON_CONST_OR_JVM_FIELD,
            "'@JvmStatic' annotation is useless for const or '@JvmField' properties",
        )
        map.put(
            JVM_STATIC_ON_EXTERNAL_IN_INTERFACE,
            "'@JvmStatic' annotation cannot be used on 'external' members of interface companions"
        )

        map.put(INAPPLICABLE_JVM_NAME, "'@JvmName' annotation is not applicable to this declaration")
        map.put(ILLEGAL_JVM_NAME, "Illegal JVM name")

        map.put(FUNCTION_DELEGATE_MEMBER_NAME_CLASH, "Spread operator is prohibited for arguments to signature-polymorphic calls")

        map.put(VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION, "Value classes without @JvmInline annotation are not supported yet")
        map.put(JVM_INLINE_WITHOUT_VALUE_CLASS, "@JvmInline annotation is only applicable to value classes")
        map.put(JVM_EXPOSE_BOXED_WITHOUT_INLINE, "'@JvmExposeBoxed' annotation is only applicable to '@JvmInline' value classes")

        map.put(JVM_DEFAULT_NOT_IN_INTERFACE, "'@JvmDefault' is only supported on interface members")
        map.put(
            JVM_DEFAULT_IN_JVM6_TARGET,
            "''@{0}'' is only supported since JVM target 1.8. Recompile with ''-jvm-target 1.8''",
            STRING
        )
        map.put(JVM_DEFAULT_REQUIRED_FOR_OVERRIDE, "'@JvmDefault' is required for an override of a '@JvmDefault' member")
        map.put(JVM_DEFAULT_IN_DECLARATION, "Usage of ''@{0}'' is only allowed with -Xjvm-default option", STRING)
        map.put(
            JVM_DEFAULT_WITH_COMPATIBILITY_IN_DECLARATION,
            "Usage of '@JvmDefaultWithCompatibility' is only allowed with '-Xjvm-default=all' option"
        )
        map.put(
            JVM_DEFAULT_WITH_COMPATIBILITY_NOT_ON_INTERFACE,
            "'@JvmDefaultWithCompatibility' annotation is only allowed on interfaces"
        )
        map.put(
            NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT,
            "Non-@JvmDefault interface method cannot override default Java method. Please annotate this method with @JvmDefault or enable `-Xjvm-default=all|all-compatibility`"
        )

        map.put(EXTERNAL_DECLARATION_IN_INTERFACE, "Members of interfaces can not be external")
        map.put(EXTERNAL_DECLARATION_CANNOT_BE_ABSTRACT, "External declaration can not be abstract")
        map.put(EXTERNAL_DECLARATION_CANNOT_HAVE_BODY, "External declaration can not have a body")
        map.put(EXTERNAL_DECLARATION_CANNOT_BE_INLINED, "Inline functions can not be external")

        map.put(INAPPLICABLE_JVM_FIELD, "{0}", STRING)
        map.put(INAPPLICABLE_JVM_FIELD_WARNING, "{0}. This warning will become an error in further releases", STRING)

        map.put(JVM_SYNTHETIC_ON_DELEGATE, "'@JvmSynthetic' annotation cannot be used on delegated properties")

        map.put(
            NON_SOURCE_REPEATED_ANNOTATION,
            "Repeatable annotations with non-SOURCE retention are only supported starting from Kotlin 1.6"
        )
        map.put(
            REPEATED_ANNOTATION_TARGET6,
            "Repeatable annotations with non-SOURCE retention are not supported with JVM target 1.6. Use -jvm-target 1.8"
        )
        map.put(
            REPEATED_ANNOTATION_WITH_CONTAINER,
            "Repeated annotation ''@{0}'' cannot be used on a declaration which is annotated with its container annotation ''@{1}''",
            TO_STRING,
            TO_STRING
        )

        map.put(
            DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET,
            "Super calls to Java default methods are prohibited in JVM target 1.6. Recompile with '-jvm-target 1.8'"
        )
        map.put(
            INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET,
            "Calls to static methods in Java interfaces are prohibited in JVM target 1.6. Recompile with '-jvm-target 1.8'"
        )
        map.put(
            INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER,
            "Interfaces can call JVM-default members via super only within JVM-default members. Please use '-Xjvm-default=all/all-compatibility' modes for such calls"
        )
        map.put(
            SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC,
            "Using protected members which are not @JvmStatic in the superclass companion is unsupported yet"
        )

        map.put(
            REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY,
            "Container annotation ''{0}'' must have a property ''value'' of type ''Array<{1}>''.",
            TO_STRING,
            TO_STRING
        )
        map.put(
            REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER,
            "Container annotation ''{0}'' does not have a default value for ''{1}''.",
            TO_STRING,
            TO_STRING
        )
        map.put(
            REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION,
            "Container annotation ''{0}'' has shorter retention (''{1}'') than the repeatable annotation ''{2}'' (''{3}'').",
            TO_STRING,
            TO_STRING,
            TO_STRING,
            TO_STRING
        )
        map.put(
            REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET,
            "Target set of container annotation ''{0}'' must be a subset of the target set of contained annotation ''{1}''.",
            TO_STRING,
            TO_STRING
        )
        map.put(
            REPEATABLE_ANNOTATION_HAS_NESTED_CLASS_NAMED_CONTAINER,
            "Repeatable annotation cannot have a nested class named 'Container'. This name is reserved for auto-generated container class"
        )
        map.put(
            SUSPENSION_POINT_INSIDE_CRITICAL_SECTION,
            "The ''{0}'' suspension point is inside a critical section",
            SYMBOL
        )
        map.put(
            CONCURRENT_HASH_MAP_CONTAINS_OPERATOR,
            "Method 'contains' from ConcurrentHashMap may have unexpected semantics: it calls 'containsValue' instead of 'containsKey'. " +
                    "Use explicit form of the call to 'containsKey'/'containsValue'/'contains' or cast the value to kotlin.collections.Map instead. " +
                    "See https://youtrack.jetbrains.com/issue/KT-18053 for more details"
        )
        map.put(
            SPREAD_ON_SIGNATURE_POLYMORPHIC_CALL,
            "Spread operator is prohibited for arguments to signature-polymorphic calls"
        )
        map.put(
            JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE,
            "Java SAM interface constructor references are prohibited"
        )
        map.put(
            JAVA_SHADOWED_PROTECTED_FIELD_REFERENCE,
            "It's impossible to refer here a protected Java field from class ''{0}'', " +
                    "because an invisible property from class ''{1}'' shadows it. " +
                    "Rename the property from class ''{1}'' or change visibility of the field from class ''{0}''",
            TO_STRING,
            TO_STRING
        )
        map.put(
            REDUNDANT_REPEATABLE_ANNOTATION,
            "Please, remove the ''{0}'' annotation, as ''{1}'' is already enough",
            TO_STRING,
            TO_STRING,
        )

        map.checkMissingMessages(FirJvmErrors)
    }
}