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
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class IrProperty :
    IrDeclarationBase(), IrOverridableMember, IrMetadataSourceOwner, IrAttributeContainer, IrMemberWithContainerSource {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: PropertyDescriptor
    abstract override val symbol: IrPropertySymbol

    abstract val isVar: Boolean
    abstract val isConst: Boolean
    abstract val isLateinit: Boolean
    abstract val isDelegated: Boolean
    abstract val isExternal: Boolean
    abstract val isExpect: Boolean
    abstract val isFakeOverride: Boolean

    abstract var backingField: IrField?
    abstract var getter: IrSimpleFunction?
    abstract var setter: IrSimpleFunction?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitProperty(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        backingField?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        backingField = backingField?.transform(transformer, data) as? IrField
        getter = getter?.run { transform(transformer, data) as IrSimpleFunction }
        setter = setter?.run { transform(transformer, data) as IrSimpleFunction }
    }
}
