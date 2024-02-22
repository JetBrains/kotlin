/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

open class IrClassImpl @IrImplementationDetail constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val factory: IrFactory,
    override var name: Name,
    override var visibility: DescriptorVisibility,
    override val symbol: IrClassSymbol,
    override var kind: ClassKind,
    override var modality: Modality,
    override val source: SourceElement,
) : IrClass() {
    override var annotations: List<IrConstructorCall> = emptyList()

    override lateinit var parent: IrDeclarationParent

    override var isExternal: Boolean = false

    override var typeParameters: List<IrTypeParameter> = emptyList()

    @UnsafeDuringIrConstructionAPI
    override val declarations: MutableList<IrDeclaration> = ArrayList()

    override var attributeOwnerId: IrAttributeContainer = this

    override var originalBeforeInline: IrAttributeContainer? = null

    override var metadata: MetadataSource? = null

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor
        get() = symbol.descriptor

    override var isCompanion: Boolean = false

    override var isInner: Boolean = false

    override var isData: Boolean = false

    override var isValue: Boolean = false

    override var isExpect: Boolean = false

    override var isFun: Boolean = false

    override var hasEnumEntries: Boolean = false

    override var superTypes: List<IrType> = emptyList()

    override var thisReceiver: IrValueParameter? = null

    override var valueClassRepresentation: ValueClassRepresentation<IrSimpleType>? = null

    override var sealedSubclasses: List<IrClassSymbol> = emptyList()

    init {
        symbol.bind(this)
    }
}
