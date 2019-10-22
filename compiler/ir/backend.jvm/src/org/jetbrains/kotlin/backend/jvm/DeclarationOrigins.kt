/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl

interface JvmLoweredDeclarationOrigin : IrDeclarationOrigin {
    object CLASS_STATIC_INITIALIZER : IrDeclarationOriginImpl("CLASS_STATIC_INITIALIZER")
    object DEFAULT_IMPLS : IrDeclarationOriginImpl("DEFAULT_IMPLS")
    object DEFAULT_IMPLS_BRIDGE : IrDeclarationOriginImpl("DEFAULT_IMPLS_BRIDGE")
    object DEFAULT_IMPLS_BRIDGE_TO_SYNTHETIC : IrDeclarationOriginImpl("DEFAULT_IMPLS_BRIDGE_TO_SYNTHETIC", isSynthetic = true)
    object MULTIFILE_BRIDGE : IrDeclarationOriginImpl("MULTIFILE_BRIDGE")
    object FIELD_FOR_OUTER_THIS : IrDeclarationOriginImpl("FIELD_FOR_OUTER_THIS")
    object LAMBDA_IMPL : IrDeclarationOriginImpl("LAMBDA_IMPL")
    object FUNCTION_REFERENCE_IMPL : IrDeclarationOriginImpl("FUNCTION_REFERENCE_IMPL", isSynthetic = true)
    object SYNTHETIC_ACCESSOR : IrDeclarationOriginImpl("SYNTHETIC_ACCESSOR", isSynthetic = true)
    object TO_ARRAY : IrDeclarationOriginImpl("TO_ARRAY")
    object JVM_STATIC_WRAPPER : IrDeclarationOriginImpl("JVM_STATIC_WRAPPER")
    object JVM_STATIC_WRAPPER_SYNTHETIC : IrDeclarationOriginImpl("JVM_STATIC_WRAPPER_SYNTHETIC", isSynthetic = true)
    object JVM_OVERLOADS_WRAPPER : IrDeclarationOriginImpl("JVM_OVERLOADS_WRAPPER")
    object SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS :
        IrDeclarationOriginImpl("SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS", isSynthetic = true)
    object SYNTHETIC_METHOD_FOR_TYPEALIAS_ANNOTATIONS :
        IrDeclarationOriginImpl("SYNTHETIC_METHOD_FOR_TYPEALIAS_ANNOTATIONS", isSynthetic = true)
    object GENERATED_PROPERTY_REFERENCE : IrDeclarationOriginImpl("GENERATED_PROPERTY_REFERENCE", isSynthetic = true)
    object GENERATED_SAM_IMPLEMENTATION : IrDeclarationOriginImpl("GENERATED_SAM_IMPLEMENTATION", isSynthetic = true)
    object GENERATED_MEMBER_IN_CALLABLE_REFERENCE : IrDeclarationOriginImpl("GENERATED_MEMBER_IN_CALLABLE_REFERENCE", isSynthetic = false)
    object ENUM_MAPPINGS_FOR_WHEN : IrDeclarationOriginImpl("ENUM_MAPPINGS_FOR_WHEN", isSynthetic = true)
    object SYNTHETIC_INLINE_CLASS_MEMBER : IrDeclarationOriginImpl("SYNTHETIC_INLINE_CLASS_MEMBER", isSynthetic = true)
    object INLINE_CLASS_GENERATED_IMPL_METHOD : IrDeclarationOriginImpl("INLINE_CLASS_GENERATED_IMPL_METHOD")
    object GENERATED_ASSERTION_ENABLED_FIELD : IrDeclarationOriginImpl("GENERATED_ASSERTION_ENABLED_FIELD", isSynthetic = true)
    object GENERATED_MAIN_FOR_PARAMETERLESS_MAIN_METHOD : IrDeclarationOriginImpl("GENERATED_MAIN_FOR_PARAMETERLESS_MAIN_METHOD", isSynthetic = true)
    object SUSPEND_FUNCTION_VIEW : IrDeclarationOriginImpl("SUSPEND_FUNCTION_VIEW")
    object SUSPEND_IMPL_STATIC_FUNCTION : IrDeclarationOriginImpl("SUSPEND_IMPL_STATIC_FUNCTION", isSynthetic = true)
    object INTERFACE_COMPANION_PRIVATE_INSTANCE : IrDeclarationOriginImpl("INTERFACE_COMPANION_PRIVATE_INSTANCE", isSynthetic = true)
    object POLYMORPHIC_SIGNATURE_INSTANTIATION : IrDeclarationOriginImpl("POLYMORPHIC_SIGNATURE_INSTANTIATION", isSynthetic = true)
    object ENUM_CONSTRUCTOR_SYNTHETIC_PARAMETER : IrDeclarationOriginImpl("ENUM_CONSTRUCTOR_SYNTHETIC_PARAMETER", isSynthetic = true)
    object OBJECT_SUPER_CONSTRUCTOR_PARAMETER : IrDeclarationOriginImpl("OBJECT_SUPER_CONSTURCTOR_PARAMETER", isSynthetic = true)
    object CONTINUATION_CLASS : IrDeclarationOriginImpl("CONTINUATION_CLASS")
    object SUSPEND_LAMBDA : IrDeclarationOriginImpl("SUSPEND_LAMBDA")
    object FOR_INLINE_STATE_MACHINE_TEMPLATE : IrDeclarationOriginImpl("FOR_INLINE_TEMPLATE")
    object FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE : IrDeclarationOriginImpl("FOR_INLINE_TEMPLATE_CROSSINLINE")
    object FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE_VIEW : IrDeclarationOriginImpl("FOR_INLINE_TEMPLATE_CROSSINLINE_VIEW")
}
