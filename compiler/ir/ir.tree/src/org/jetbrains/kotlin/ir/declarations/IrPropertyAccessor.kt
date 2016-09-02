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

import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrPropertyAccessor : IrGeneralFunction {
    override val descriptor: PropertyAccessorDescriptor
}

interface IrPropertyGetter : IrPropertyAccessor {
    override val descriptor: PropertyGetterDescriptor

    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.PROPERTY_GETTER
}

interface IrPropertySetter : IrPropertyAccessor {
    override val descriptor: PropertySetterDescriptor

    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.PROPERTY_SETTER
}

abstract class IrPropertyAccessorBase(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin
) : IrGeneralFunctionBase(startOffset, endOffset, origin), IrPropertyAccessor

class IrPropertyGetterImpl(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        override val descriptor: PropertyGetterDescriptor
) : IrPropertyAccessorBase(startOffset, endOffset, origin), IrPropertyGetter {
    constructor(
            startOffset: Int,
            endOffset: Int,
            origin: IrDeclarationOrigin,
            descriptor: PropertyGetterDescriptor,
            body: IrBody
    ) : this(startOffset, endOffset, origin, descriptor) {
        this.body = body
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitPropertyGetter(this, data)
}

class IrPropertySetterImpl(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        override val descriptor: PropertySetterDescriptor
) : IrPropertyAccessorBase(startOffset, endOffset, origin), IrPropertySetter {
    constructor(
            startOffset: Int,
            endOffset: Int,
            origin: IrDeclarationOrigin,
            descriptor: PropertySetterDescriptor,
            body: IrBody
    ) : this(startOffset, endOffset, origin, descriptor) {
        this.body = body
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitPropertySetter(this, data)
}