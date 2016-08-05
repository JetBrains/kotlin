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

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.SourceLocation
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrProperty : IrDeclarationNonRoot {
    override val descriptor: PropertyDescriptor
    val getter: IrPropertyGetter?
    val setter: IrPropertySetter?
}

interface IrSimpleProperty : IrProperty {
    val valueInitializer: IrBody?
}

interface IrDelegatedProperty : IrProperty {
    val delegateInitializer: IrBody
}

abstract class IrPropertyBase(
        sourceLocation: SourceLocation,
        override val descriptor: PropertyDescriptor
) : IrDeclarationNonRootBase(sourceLocation), IrProperty {
    override lateinit var parent: IrDeclaration
    override var getter: IrPropertyGetter? = null
    override var setter: IrPropertySetter? = null

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    fun initialize(getter: IrPropertyGetter?, setter: IrPropertySetter?) {
        this.getter = getter
        this.setter = setter
    }
}

class IrSimplePropertyImpl(
        sourceLocation: SourceLocation,
        descriptor: PropertyDescriptor,
        override val valueInitializer: IrBody?
) : IrPropertyBase(sourceLocation, descriptor), IrSimpleProperty {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitSimpleProperty(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        valueInitializer?.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}

class IrDelegatedPropertyImpl(
        sourceLocation: SourceLocation,
        descriptor: PropertyDescriptor,
        override val delegateInitializer: IrBody
) : IrPropertyBase(sourceLocation, descriptor), IrDelegatedProperty {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitDelegatedProperty(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        delegateInitializer.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}
