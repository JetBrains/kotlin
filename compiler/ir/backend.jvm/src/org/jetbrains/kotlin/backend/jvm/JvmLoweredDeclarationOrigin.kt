/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl

interface JvmLoweredDeclarationOrigin : IrDeclarationOrigin {
    object CLASS_STATIC_INITIALIZER : IrDeclarationOriginImpl("CLASS_STATIC_INITIALIZER")
    object DEFAULT_IMPLS : IrDeclarationOriginImpl("DEFAULT_IMPLS")
    object SUPER_INTERFACE_METHOD_BRIDGE : IrDeclarationOriginImpl("SUPER_INTERFACE_METHOD_BRIDGE")
    object FIELD_FOR_OUTER_THIS : IrDeclarationOriginImpl("FIELD_FOR_OUTER_THIS")
    object LAMBDA_IMPL : IrDeclarationOriginImpl("LAMBDA_IMPL")
    object FUNCTION_REFERENCE_IMPL : IrDeclarationOriginImpl("FUNCTION_REFERENCE_IMPL", isSynthetic = true)
    object SYNTHETIC_ACCESSOR : IrDeclarationOriginImpl("SYNTHETIC_ACCESSOR", isSynthetic = true)
    object SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR :
        IrDeclarationOriginImpl("SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR", isSynthetic = true)
    object SYNTHETIC_MARKER_PARAMETER : IrDeclarationOriginImpl("SYNTHETIC_MARKER_PARAMETER", isSynthetic = true)
    object TO_ARRAY : IrDeclarationOriginImpl("TO_ARRAY")
    object JVM_STATIC_WRAPPER : IrDeclarationOriginImpl("JVM_STATIC_WRAPPER")
    object JVM_OVERLOADS_WRAPPER : IrDeclarationOriginImpl("JVM_OVERLOADS_WRAPPER")
    object SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS :
        IrDeclarationOriginImpl("SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS", isSynthetic = true)
    object GENERATED_PROPERTY_REFERENCE : IrDeclarationOriginImpl("GENERATED_PROPERTY_REFERENCE", isSynthetic = true)
    object GENERATED_MEMBER_IN_CALLABLE_REFERENCE : IrDeclarationOriginImpl("GENERATED_MEMBER_IN_CALLABLE_REFERENCE", isSynthetic = false)
    object ENUM_MAPPINGS_FOR_WHEN : IrDeclarationOriginImpl("ENUM_MAPPINGS_FOR_WHEN", isSynthetic = true)
    object ENUM_MAPPINGS_FOR_ENTRIES : IrDeclarationOriginImpl("ENUM_MAPPINGS_FOR_ENTRIES", isSynthetic = true)
    object SYNTHETIC_INLINE_CLASS_MEMBER : IrDeclarationOriginImpl("SYNTHETIC_INLINE_CLASS_MEMBER", isSynthetic = true)
    object SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER :
        IrDeclarationOriginImpl("SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER", isSynthetic = true)
    object INLINE_CLASS_GENERATED_IMPL_METHOD : IrDeclarationOriginImpl("INLINE_CLASS_GENERATED_IMPL_METHOD")
    object FUNCTION_WITH_EXPOSED_INLINE_CLASS : IrDeclarationOriginImpl("FUNCTION_WITH_EXPOSED_INLINE_CLASS")
    object MULTI_FIELD_VALUE_CLASS_GENERATED_IMPL_METHOD : IrDeclarationOriginImpl("MULTI_FIELD_VALUE_CLASS_GENERATED_IMPL_METHOD")
    object STATIC_INLINE_CLASS_REPLACEMENT : IrDeclarationOriginImpl("STATIC_INLINE_CLASS_REPLACEMENT")
    object STATIC_MULTI_FIELD_VALUE_CLASS_REPLACEMENT : IrDeclarationOriginImpl("STATIC_MULTI_FIELD_VALUE_CLASS_REPLACEMENT")
    object STATIC_INLINE_CLASS_CONSTRUCTOR : IrDeclarationOriginImpl("STATIC_INLINE_CLASS_CONSTRUCTOR")
    object STATIC_MULTI_FIELD_VALUE_CLASS_CONSTRUCTOR : IrDeclarationOriginImpl("STATIC_MULTI_FIELD_VALUE_CLASS_CONSTRUCTOR")
    object GENERATED_ASSERTION_ENABLED_FIELD : IrDeclarationOriginImpl("GENERATED_ASSERTION_ENABLED_FIELD", isSynthetic = true)
    object GENERATED_MULTI_FIELD_VALUE_CLASS_PARAMETER : IrDeclarationOriginImpl("GENERATED_MULTI_FIELD_VALUE_CLASS_PARAMETER")
    object TEMPORARY_MULTI_FIELD_VALUE_CLASS_PARAMETER : IrDeclarationOriginImpl("TEMPORARY_MULTI_FIELD_VALUE_CLASS_PARAMETER")
    object TEMPORARY_MULTI_FIELD_VALUE_CLASS_VARIABLE : IrDeclarationOriginImpl("TEMPORARY_MULTI_FIELD_VALUE_CLASS_VARIABLE")
    object MULTI_FIELD_VALUE_CLASS_REPRESENTATION_VARIABLE : IrDeclarationOriginImpl("MULTI_FIELD_VALUE_CLASS_REPRESENTATION_VARIABLE")
    object GENERATED_EXTENDED_MAIN : IrDeclarationOriginImpl("GENERATED_EXTENDED_MAIN", isSynthetic = true)
    object SUSPEND_IMPL_STATIC_FUNCTION : IrDeclarationOriginImpl("SUSPEND_IMPL_STATIC_FUNCTION", isSynthetic = true)
    object INTERFACE_COMPANION_PRIVATE_INSTANCE : IrDeclarationOriginImpl("INTERFACE_COMPANION_PRIVATE_INSTANCE", isSynthetic = true)
    object POLYMORPHIC_SIGNATURE_INSTANTIATION : IrDeclarationOriginImpl("POLYMORPHIC_SIGNATURE_INSTANTIATION", isSynthetic = true)
    object ENUM_CONSTRUCTOR_SYNTHETIC_PARAMETER : IrDeclarationOriginImpl("ENUM_CONSTRUCTOR_SYNTHETIC_PARAMETER", isSynthetic = true)
    object OBJECT_SUPER_CONSTRUCTOR_PARAMETER : IrDeclarationOriginImpl("OBJECT_SUPER_CONSTRUCTOR_PARAMETER", isSynthetic = true)
    object CONTINUATION_CLASS : IrDeclarationOriginImpl("CONTINUATION_CLASS")
    object SUSPEND_LAMBDA : IrDeclarationOriginImpl("SUSPEND_LAMBDA")
    object FOR_INLINE_STATE_MACHINE_TEMPLATE : IrDeclarationOriginImpl("FOR_INLINE_TEMPLATE")
    object FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE : IrDeclarationOriginImpl("FOR_INLINE_TEMPLATE_CROSSINLINE")
    object CONTINUATION_CLASS_RESULT_FIELD: IrDeclarationOriginImpl("CONTINUATION_CLASS_RESULT_FIELD", isSynthetic = true)
    object COMPANION_PROPERTY_BACKING_FIELD : IrDeclarationOriginImpl("COMPANION_PROPERTY_BACKING_FIELD")
    object FIELD_FOR_STATIC_CALLABLE_REFERENCE_INSTANCE : IrDeclarationOriginImpl("FIELD_FOR_STATIC_CALLABLE_REFERENCE_INSTANCE")
    object ABSTRACT_BRIDGE_STUB : IrDeclarationOriginImpl("ABSTRACT_BRIDGE_STUB")
    object INVOKEDYNAMIC_CALL_TARGET : IrDeclarationOriginImpl("INVOKEDYNAMIC_CALL_TARGET")
    object INLINE_LAMBDA : IrDeclarationOriginImpl("INLINE_LAMBDA")
    object PROXY_FUN_FOR_METAFACTORY : IrDeclarationOriginImpl("PROXY_FUN_FOR_METAFACTORY")
    object SYNTHETIC_PROXY_FUN_FOR_METAFACTORY : IrDeclarationOriginImpl("SYNTHETIC_PROXY_FUN_FOR_METAFACTORY", isSynthetic = true)
    object DESERIALIZE_LAMBDA_FUN : IrDeclarationOriginImpl("DESERIALIZE_LAMBDA_FUN", isSynthetic = true)
}
