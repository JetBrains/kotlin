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
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl

interface JvmLoweredDeclarationOrigin : IrDeclarationOrigin {
    object CLASS_STATIC_INITIALIZER : IrDeclarationOriginImpl("CLASS_STATIC_INITIALIZER")
    object DEFAULT_IMPLS : IrDeclarationOriginImpl("DEFAULT_IMPL")
    object FIELD_FOR_OUTER_THIS : IrDeclarationOriginImpl("FIELD_FOR_OUTER_THIS")
    object FUNCTION_REFERENCE_IMPL : IrDeclarationOriginImpl("FUNCTION_REFERENCE_IMPL")
    object SYNTHETIC_ACCESSOR : IrDeclarationOriginImpl("SYNTHETIC_ACCESSOR", isSynthetic = true)
    object TO_ARRAY : IrDeclarationOriginImpl("TO_ARRAY")
    object JVM_STATIC_WRAPPER : IrDeclarationOriginImpl("JVM_STATIC_WRAPPER")
    object JVM_STATIC_WRAPPER_SYNTHETIC : IrDeclarationOriginImpl("JVM_STATIC_WRAPPER_SYNTHETIC", isSynthetic = true)
    object JVM_OVERLOADS_WRAPPER : IrDeclarationOriginImpl("JVM_OVERLOADS_WRAPPER")
    object SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS :
        IrDeclarationOriginImpl("SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS", isSynthetic = true)
    object GENERATED_PROPERTY_REFERENCE : IrDeclarationOriginImpl("GENERATED_PROPERTY_REFERENCE", isSynthetic = true)
    object GENERATED_SAM_IMPLEMENTATION : IrDeclarationOriginImpl("GENERATED_SAM_IMPLEMENTATION", isSynthetic = true)
}

interface JvmLoweredStatementOrigin : IrStatementOrigin {
    object DEFAULT_IMPLS_DELEGATION : IrStatementOriginImpl("DEFAULT_IMPL_DELEGATION")
    object TO_ARRAY : IrDeclarationOriginImpl("TO_ARRAY")
}
