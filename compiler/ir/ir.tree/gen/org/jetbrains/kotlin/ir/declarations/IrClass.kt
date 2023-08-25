/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformIfNeeded
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.class]
 */
abstract class IrClass : IrDeclarationBase(), IrPossiblyExternalDeclaration,
        IrDeclarationWithVisibility, IrTypeParametersContainer, IrDeclarationContainer,
        IrAttributeContainer, IrMetadataSourceOwner {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: ClassDescriptor

    abstract override val symbol: IrClassSymbol

    abstract var kind: ClassKind

    abstract var modality: Modality

    abstract var isCompanion: Boolean

    abstract var isInner: Boolean

    abstract var isData: Boolean

    abstract var isValue: Boolean

    abstract var isExpect: Boolean

    abstract var isFun: Boolean

    abstract val source: SourceElement

    abstract var superTypes: List<IrType>

    abstract var thisReceiver: IrValueParameter?

    abstract var valueClassRepresentation: ValueClassRepresentation<IrSimpleType>?

    /**
     * If this is a sealed class or interface, this list contains symbols of all its immediate
     * subclasses.
     * Otherwise, this is an empty list.
     *
     * NOTE: If this [IrClass] was deserialized from a klib, this list will always be empty!
     * See [KT-54028](https://youtrack.jetbrains.com/issue/KT-54028).
     */
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
