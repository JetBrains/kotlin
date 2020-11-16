/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.getFlag
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.toFlag
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrValueParameterImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrValueParameterSymbol,
    override val name: Name,
    override val index: Int,
    override var type: IrType,
    override var varargElementType: IrType?,
    isCrossinline: Boolean,
    isNoinline: Boolean,
    isHidden: Boolean = false,
    isAssignable: Boolean = false
) : IrValueParameter() {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ParameterDescriptor
        get() = symbol.descriptor

    init {
        symbol.bind(this)
    }

    private val flags = (isCrossinline.toFlag(IS_CROSSINLINE_BIT) or isNoinline.toFlag(IS_NOINLINE_BIT) or
            isHidden.toFlag(IS_HIDDEN_BIT) or isAssignable.toFlag(IS_ASSIGNABLE_BIT)).toByte()

    override val isCrossinline: Boolean
        get() = flags.toInt().getFlag(IS_CROSSINLINE_BIT)

    override val isNoinline: Boolean
        get() = flags.toInt().getFlag(IS_NOINLINE_BIT)

    override val isHidden: Boolean
        get() = flags.toInt().getFlag(IS_HIDDEN_BIT)

    override val isAssignable: Boolean
        get() = flags.toInt().getFlag(IS_ASSIGNABLE_BIT)

    override val factory: IrFactory
        get() = IrFactoryImpl

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    override var defaultValue: IrExpressionBody? = null

    private companion object {
        const val IS_CROSSINLINE_BIT = 1 shl 0
        const val IS_NOINLINE_BIT = 1 shl 1
        const val IS_HIDDEN_BIT = 1 shl 2
        const val IS_ASSIGNABLE_BIT = 1 shl 3
    }
}
