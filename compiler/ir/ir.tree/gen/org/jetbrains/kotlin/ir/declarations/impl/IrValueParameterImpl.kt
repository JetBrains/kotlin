/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

// IMPORTANT: This class is used in the Compose IDE plugin (platform 233).
// Don't rename it or change its constructor's signature so as not to break binary compatibility when an older version of
// the Compose IDE plugin is used with a newer version of the Kotlin IDE plugin that vendors the updated compiler version.
class IrValueParameterImpl @IrImplementationDetail constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrValueParameterSymbol,
    override var name: Name,
    override var index: Int,
    override var type: IrType,
    override var varargElementType: IrType?,
    override var isCrossinline: Boolean,
    override var isNoinline: Boolean,
    override var isHidden: Boolean,
    override val isAssignable: Boolean,
    override val factory: IrFactory = IrFactoryImpl,
) : IrValueParameter() {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ParameterDescriptor
        get() = symbol.descriptor

    init {
        symbol.bind(this)
    }

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    override var defaultValue: IrExpressionBody? = null
}
