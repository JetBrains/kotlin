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

package org.jetbrains.kotlin.ir.declarations

interface IrDeclarationOrigin {
    object DEFINED : IrDeclarationOriginImpl("DEFINED")
    object FAKE_OVERRIDE : IrDeclarationOriginImpl("FAKE_OVERRIDE")
    object FOR_LOOP_ITERATOR : IrDeclarationOriginImpl("FOR_LOOP_ITERATOR")
    object FOR_LOOP_VARIABLE : IrDeclarationOriginImpl("FOR_LOOP_VARIABLE")
    object FOR_LOOP_IMPLICIT_VARIABLE : IrDeclarationOriginImpl("FOR_LOOP_IMPLICIT_VARIABLE")
    object PROPERTY_BACKING_FIELD : IrDeclarationOriginImpl("PROPERTY_BACKING_FIELD")
    object DEFAULT_PROPERTY_ACCESSOR : IrDeclarationOriginImpl("DEFAULT_PROPERTY_ACCESSOR")
    object DELEGATE : IrDeclarationOriginImpl("DELEGATE")
    object DELEGATED_PROPERTY_ACCESSOR : IrDeclarationOriginImpl("DELEGATED_PROPERTY_ACCESSOR")
    object DELEGATED_MEMBER : IrDeclarationOriginImpl("DELEGATED_MEMBER")
    object ENUM_CLASS_SPECIAL_MEMBER : IrDeclarationOriginImpl("ENUM_CLASS_SPECIAL_MEMBER")
    object GENERATED_DATA_CLASS_MEMBER : IrDeclarationOriginImpl("GENERATED_DATA_CLASS_MEMBER")
    object LOCAL_FUNCTION_FOR_LAMBDA : IrDeclarationOriginImpl("LOCAL_FUNCTION_FOR_LAMBDA")
    object CATCH_PARAMETER : IrDeclarationOriginImpl("CATCH_PARAMETER")
    object INSTANCE_RECEIVER : IrDeclarationOriginImpl("INSTANCE_RECEIVER")
    object PRIMARY_CONSTRUCTOR_PARAMETER : IrDeclarationOriginImpl("PRIMARY_CONSTRUCTOR_PARAMETER")
    object IR_TEMPORARY_VARIABLE : IrDeclarationOriginImpl("IR_TEMPORARY_VARIABLE")
    object IR_EXTERNAL_DECLARATION_STUB : IrDeclarationOriginImpl("IR_EXTERNAL_DECLARATION_STUB")
    object IR_BUILTINS_STUB : IrDeclarationOriginImpl("IR_BUILTINS_STUB")
}

abstract class IrDeclarationOriginImpl(val name: String) : IrDeclarationOrigin {
    override fun toString(): String = name
}