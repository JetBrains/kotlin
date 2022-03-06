/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformIfNeeded
import org.jetbrains.kotlin.ir.util.transformInPlace
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
    abstract val isValue: Boolean
    abstract val isExpect: Boolean
    abstract val isFun: Boolean

    abstract val source: SourceElement

    abstract var superTypes: List<IrType>

    abstract var thisReceiver: IrValueParameter?

    abstract var inlineClassRepresentation: InlineClassRepresentation<IrSimpleType>?

    abstract var multiFieldValueClassRepresentation: MultiFieldValueClassRepresentation<IrSimpleType>?

    abstract var sealedSubclasses: List<IrClassSymbol>

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitClass(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
        thisReceiver?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        typeParameters = typeParameters.transformIfNeeded(transformer, data)
        declarations.transformInPlace(transformer, data)
        thisReceiver = thisReceiver?.transform(transformer, data)
    }
}
