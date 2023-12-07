/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrTypeAliasImpl @IrImplementationDetail constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override val symbol: IrTypeAliasSymbol,
    override var name: Name,
    override var visibility: DescriptorVisibility,
    override var expandedType: IrType,
    override var isActual: Boolean,
    override var origin: IrDeclarationOrigin,
    override val factory: IrFactory = IrFactoryImpl,
) : IrTypeAlias() {
    init {
        symbol.bind(this)
    }

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    @ObsoleteDescriptorBasedAPI
    override val descriptor: TypeAliasDescriptor
        get() = symbol.descriptor

    override var typeParameters: List<IrTypeParameter> = emptyList()
}
