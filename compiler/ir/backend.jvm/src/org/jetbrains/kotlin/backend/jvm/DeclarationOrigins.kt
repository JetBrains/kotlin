/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
}
