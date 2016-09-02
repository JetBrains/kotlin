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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.IrStatement

interface IrDeclaration : IrStatement {
    val descriptor: DeclarationDescriptor
    val declarationKind: IrDeclarationKind
    val origin: IrDeclarationOrigin
}

enum class IrDeclarationKind {
    MODULE,
    FILE,
    CLASS,
    ENUM_ENTRY,
    FUNCTION,
    CONSTRUCTOR,
    PROPERTY,
    PROPERTY_ACCESSOR,
    VARIABLE,
    LOCAL_PROPERTY,
    LOCAL_PROPERTY_ACCESSOR,
    TYPEALIAS,
    ANONYMOUS_INITIALIZER,
    DUMMY;
}

enum class IrDeclarationOrigin {
    DEFINED,
    DELEGATE,
    DELEGATED_PROPERTY_ACCESSOR,
    DELEGATED_MEMBER,
    CLASS_FOR_ENUM_ENTRY,
    ENUM_CLASS_SPECIAL_MEMBER,
    GENERATED_DATA_CLASS_MEMBER,
    LOCAL_FUNCTION_FOR_LAMBDA,
    IR_TEMPORARY_VARIABLE,
}

abstract class IrDeclarationBase(
        startOffset: Int,
        endOffset: Int,
        override val origin: IrDeclarationOrigin
) : IrElementBase(startOffset, endOffset), IrDeclaration
