/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl

object JvmLoweredDeclarationOrigin {
    val CLASS_STATIC_INITIALIZER by IrDeclarationOriginImpl.Regular
    val DEFAULT_IMPLS by IrDeclarationOriginImpl.Regular
    val SUPER_INTERFACE_METHOD_BRIDGE by IrDeclarationOriginImpl.Regular
    val FIELD_FOR_OUTER_THIS by IrDeclarationOriginImpl.Regular
    val LAMBDA_IMPL by IrDeclarationOriginImpl.Regular
    val FUNCTION_REFERENCE_IMPL by IrDeclarationOriginImpl.Synthetic
    val SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR by IrDeclarationOriginImpl.Synthetic
    val TO_ARRAY by IrDeclarationOriginImpl.Regular
    val JVM_STATIC_WRAPPER by IrDeclarationOriginImpl.Regular
    val JVM_OVERLOADS_WRAPPER by IrDeclarationOriginImpl.Regular
    val SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS by IrDeclarationOriginImpl.Synthetic
    val GENERATED_PROPERTY_REFERENCE by IrDeclarationOriginImpl.Synthetic
    val GENERATED_MEMBER_IN_CALLABLE_REFERENCE by IrDeclarationOriginImpl.Regular
    val ENUM_MAPPINGS_FOR_WHEN by IrDeclarationOriginImpl.Synthetic
    val ENUM_MAPPINGS_FOR_ENTRIES by IrDeclarationOriginImpl.Synthetic
    val SYNTHETIC_INLINE_CLASS_MEMBER by IrDeclarationOriginImpl.Synthetic
    val SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER by IrDeclarationOriginImpl.Synthetic
    val INLINE_CLASS_GENERATED_IMPL_METHOD by IrDeclarationOriginImpl.Regular
    val MULTI_FIELD_VALUE_CLASS_GENERATED_IMPL_METHOD by IrDeclarationOriginImpl.Regular
    val STATIC_INLINE_CLASS_REPLACEMENT by IrDeclarationOriginImpl.Regular
    val STATIC_MULTI_FIELD_VALUE_CLASS_REPLACEMENT by IrDeclarationOriginImpl.Regular
    val STATIC_INLINE_CLASS_CONSTRUCTOR by IrDeclarationOriginImpl.Regular
    val STATIC_MULTI_FIELD_VALUE_CLASS_CONSTRUCTOR by IrDeclarationOriginImpl.Regular
    val INLINE_CLASS_CONSTRUCTOR_SYNTHETIC_PARAMETER by IrDeclarationOriginImpl.Regular
    val NON_EXPOSED_CONSTRUCTOR_SYNTHETIC_PARAMETER by IrDeclarationOriginImpl.Regular
    val EXPOSED_INLINE_CLASS_CONSTRUCTOR by IrDeclarationOriginImpl.Regular
    val GENERATED_ASSERTION_ENABLED_FIELD by IrDeclarationOriginImpl.Synthetic
    val GENERATED_MULTI_FIELD_VALUE_CLASS_PARAMETER by IrDeclarationOriginImpl.Regular
    val TEMPORARY_MULTI_FIELD_VALUE_CLASS_PARAMETER by IrDeclarationOriginImpl.Regular
    val TEMPORARY_MULTI_FIELD_VALUE_CLASS_VARIABLE by IrDeclarationOriginImpl.Regular
    val MULTI_FIELD_VALUE_CLASS_REPRESENTATION_VARIABLE by IrDeclarationOriginImpl.Regular
    val GENERATED_EXTENDED_MAIN by IrDeclarationOriginImpl.Synthetic
    val SUSPEND_IMPL_STATIC_FUNCTION by IrDeclarationOriginImpl.Synthetic
    val INTERFACE_COMPANION_PRIVATE_INSTANCE by IrDeclarationOriginImpl.Synthetic
    val POLYMORPHIC_SIGNATURE_INSTANTIATION by IrDeclarationOriginImpl.Synthetic
    val ENUM_CONSTRUCTOR_SYNTHETIC_PARAMETER by IrDeclarationOriginImpl.Synthetic
    val OBJECT_SUPER_CONSTRUCTOR_PARAMETER by IrDeclarationOriginImpl.Synthetic
    val CONTINUATION_CLASS by IrDeclarationOriginImpl.Regular
    val SUSPEND_LAMBDA by IrDeclarationOriginImpl.Regular
    val FOR_INLINE_STATE_MACHINE_TEMPLATE by IrDeclarationOriginImpl.Regular
    val FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE by IrDeclarationOriginImpl.Regular
    val INVOKE_OF_INLINE_SUSPEND_PARAM_DEFAULT_VALUE by IrDeclarationOriginImpl.Regular
    val CONTINUATION_CLASS_RESULT_FIELD by IrDeclarationOriginImpl.Synthetic
    val SUSPEND_LAMBDA_PARAMETER by IrDeclarationOriginImpl.Regular
    val COMPANION_PROPERTY_BACKING_FIELD by IrDeclarationOriginImpl.Regular
    val FIELD_FOR_STATIC_CALLABLE_REFERENCE_INSTANCE by IrDeclarationOriginImpl.Regular
    val ABSTRACT_BRIDGE_STUB by IrDeclarationOriginImpl.Regular
    val INVOKEDYNAMIC_CALL_TARGET by IrDeclarationOriginImpl.Regular
    val PROXY_FUN_FOR_METAFACTORY by IrDeclarationOriginImpl.Regular
    val SYNTHETIC_PROXY_FUN_FOR_METAFACTORY by IrDeclarationOriginImpl.Synthetic
    val DESERIALIZE_LAMBDA_FUN by IrDeclarationOriginImpl.Synthetic
}
