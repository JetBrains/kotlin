/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl

object JvmLoweredDeclarationOrigin {
    val CLASS_STATIC_INITIALIZER by IrDeclarationOriginImpl
    val DEFAULT_IMPLS by IrDeclarationOriginImpl
    val SUPER_INTERFACE_METHOD_BRIDGE by IrDeclarationOriginImpl
    val FIELD_FOR_OUTER_THIS by IrDeclarationOriginImpl
    val LAMBDA_IMPL by IrDeclarationOriginImpl
    val FUNCTION_REFERENCE_IMPL by IrDeclarationOriginImpl.Synthetic
    val SYNTHETIC_ACCESSOR by IrDeclarationOriginImpl.Synthetic
    val SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR by IrDeclarationOriginImpl.Synthetic
    val SYNTHETIC_MARKER_PARAMETER by IrDeclarationOriginImpl.Synthetic
    val TO_ARRAY by IrDeclarationOriginImpl
    val JVM_STATIC_WRAPPER by IrDeclarationOriginImpl
    val JVM_OVERLOADS_WRAPPER by IrDeclarationOriginImpl
    val SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS by IrDeclarationOriginImpl.Synthetic
    val GENERATED_PROPERTY_REFERENCE by IrDeclarationOriginImpl.Synthetic
    val GENERATED_MEMBER_IN_CALLABLE_REFERENCE by IrDeclarationOriginImpl
    val ENUM_MAPPINGS_FOR_WHEN by IrDeclarationOriginImpl.Synthetic
    val ENUM_MAPPINGS_FOR_ENTRIES by IrDeclarationOriginImpl.Synthetic
    val SYNTHETIC_INLINE_CLASS_MEMBER by IrDeclarationOriginImpl.Synthetic
    val SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER by IrDeclarationOriginImpl.Synthetic
    val INLINE_CLASS_GENERATED_IMPL_METHOD by IrDeclarationOriginImpl
    val MULTI_FIELD_VALUE_CLASS_GENERATED_IMPL_METHOD by IrDeclarationOriginImpl
    val STATIC_INLINE_CLASS_REPLACEMENT by IrDeclarationOriginImpl
    val STATIC_MULTI_FIELD_VALUE_CLASS_REPLACEMENT by IrDeclarationOriginImpl
    val STATIC_INLINE_CLASS_CONSTRUCTOR by IrDeclarationOriginImpl
    val STATIC_MULTI_FIELD_VALUE_CLASS_CONSTRUCTOR by IrDeclarationOriginImpl
    val GENERATED_ASSERTION_ENABLED_FIELD by IrDeclarationOriginImpl.Synthetic
    val GENERATED_MULTI_FIELD_VALUE_CLASS_PARAMETER by IrDeclarationOriginImpl
    val TEMPORARY_MULTI_FIELD_VALUE_CLASS_PARAMETER by IrDeclarationOriginImpl
    val TEMPORARY_MULTI_FIELD_VALUE_CLASS_VARIABLE by IrDeclarationOriginImpl
    val MULTI_FIELD_VALUE_CLASS_REPRESENTATION_VARIABLE by IrDeclarationOriginImpl
    val GENERATED_EXTENDED_MAIN by IrDeclarationOriginImpl.Synthetic
    val SUSPEND_IMPL_STATIC_FUNCTION by IrDeclarationOriginImpl.Synthetic
    val INTERFACE_COMPANION_PRIVATE_INSTANCE by IrDeclarationOriginImpl.Synthetic
    val POLYMORPHIC_SIGNATURE_INSTANTIATION by IrDeclarationOriginImpl.Synthetic
    val ENUM_CONSTRUCTOR_SYNTHETIC_PARAMETER by IrDeclarationOriginImpl.Synthetic
    val OBJECT_SUPER_CONSTRUCTOR_PARAMETER by IrDeclarationOriginImpl.Synthetic
    val CONTINUATION_CLASS by IrDeclarationOriginImpl
    val SUSPEND_LAMBDA by IrDeclarationOriginImpl
    val FOR_INLINE_STATE_MACHINE_TEMPLATE by IrDeclarationOriginImpl
    val FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE by IrDeclarationOriginImpl
    val CONTINUATION_CLASS_RESULT_FIELD by IrDeclarationOriginImpl.Synthetic
    val COMPANION_PROPERTY_BACKING_FIELD by IrDeclarationOriginImpl
    val FIELD_FOR_STATIC_CALLABLE_REFERENCE_INSTANCE by IrDeclarationOriginImpl
    val ABSTRACT_BRIDGE_STUB by IrDeclarationOriginImpl
    val INVOKEDYNAMIC_CALL_TARGET by IrDeclarationOriginImpl
    val INLINE_LAMBDA by IrDeclarationOriginImpl
    val PROXY_FUN_FOR_METAFACTORY by IrDeclarationOriginImpl
    val SYNTHETIC_PROXY_FUN_FOR_METAFACTORY by IrDeclarationOriginImpl.Synthetic
    val DESERIALIZE_LAMBDA_FUN by IrDeclarationOriginImpl.Synthetic
}
