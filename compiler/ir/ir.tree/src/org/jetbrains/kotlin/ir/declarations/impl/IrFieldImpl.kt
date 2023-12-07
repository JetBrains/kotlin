/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrFieldImpl @IrImplementationDetail constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrFieldSymbol,
    override var name: Name,
    override var type: IrType,
    override var visibility: DescriptorVisibility,
    override var isFinal: Boolean,
    override var isExternal: Boolean,
    override var isStatic: Boolean,
    override val factory: IrFactory = IrFactoryImpl,
) : IrField() {
    init {
        symbol.bind(this)
    }

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    @ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor
        get() = symbol.descriptor

    override var initializer: IrExpressionBody? = null

    override var correspondingPropertySymbol: IrPropertySymbol? = null

    override var metadata: MetadataSource? = null
}
