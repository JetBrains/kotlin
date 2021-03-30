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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.util.transformIfNeeded
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrClass :
    IrDeclarationBase(), IrPossiblyExternalDeclaration, IrDeclarationWithVisibility,
    IrDeclarationContainer, IrTypeParametersContainer, IrAttributeContainer, IrMetadataSourceOwner {

    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: ClassDescriptor
    abstract override val symbol: IrClassSymbol

    abstract val kind: ClassKind
    abstract var modality: Modality
    abstract val isCompanion: Boolean
    abstract val isInner: Boolean
    abstract val isData: Boolean
    abstract val isInline: Boolean
    abstract val isExpect: Boolean
    abstract val isFun: Boolean

    abstract val source: SourceElement

    abstract var superTypes: List<IrType>

    abstract var thisReceiver: IrValueParameter?

    abstract var inlineClassRepresentation: InlineClassRepresentation<IrSimpleType>?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitClass(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        thisReceiver?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        thisReceiver = thisReceiver?.transform(transformer, data)
        typeParameters = typeParameters.transformIfNeeded(transformer, data)
        declarations.transformInPlace(transformer, data)
    }
}

fun IrClass.addMember(member: IrDeclaration) {
    declarations.add(member)
}

fun IrClass.addAll(members: List<IrDeclaration>) {
    declarations.addAll(members)
}
