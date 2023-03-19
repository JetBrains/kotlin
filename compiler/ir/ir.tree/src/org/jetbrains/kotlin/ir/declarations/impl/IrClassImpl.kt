/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

open class IrClassImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    final override val symbol: IrClassSymbol,
    override var name: Name,
    override var kind: ClassKind,
    override var visibility: DescriptorVisibility,
    override var modality: Modality,
    override var isCompanion: Boolean = false,
    override var isInner: Boolean = false,
    override var isData: Boolean = false,
    override var isExternal: Boolean = false,
    override var isValue: Boolean = false,
    override var isExpect: Boolean = false,
    override var isFun: Boolean = false,
    override val source: SourceElement = SourceElement.NO_SOURCE,
    override val factory: IrFactory = IrFactoryImpl
) : IrClass() {
    init {
        symbol.bind(this)
    }

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor
        get() = symbol.descriptor

    override var thisReceiver: IrValueParameter? = null

    override val declarations: MutableList<IrDeclaration> = ArrayList()

    override var typeParameters: List<IrTypeParameter> = emptyList()

    override var superTypes: List<IrType> = emptyList()

    override var valueClassRepresentation: ValueClassRepresentation<IrSimpleType>? = null

    override var metadata: MetadataSource? = null

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null

    override var sealedSubclasses: List<IrClassSymbol> = emptyList()
}
