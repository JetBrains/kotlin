/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrLocalDelegatedPropertyImpl @IrImplementationDetail constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrLocalDelegatedPropertySymbol,
    override var name: Name,
    override var type: IrType,
    override var isVar: Boolean,
    override val factory: IrFactory = IrFactoryImpl,
) : IrLocalDelegatedProperty() {
    init {
        symbol.bind(this)
    }

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    @ObsoleteDescriptorBasedAPI
    override val descriptor: VariableDescriptorWithAccessors
        get() = symbol.descriptor

    override lateinit var delegate: IrVariable

    override lateinit var getter: IrSimpleFunction

    override var setter: IrSimpleFunction? = null

    override var metadata: MetadataSource? = null
}
