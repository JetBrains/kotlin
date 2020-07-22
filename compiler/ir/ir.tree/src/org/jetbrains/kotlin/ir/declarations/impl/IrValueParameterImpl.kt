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
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.carriers.ValueParameterCarrier
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrValueParameterImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrValueParameterSymbol,
    override val name: Name,
    override val index: Int,
    override val type: IrType,
    override val varargElementType: IrType?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean
) :
    IrDeclarationBase<ValueParameterCarrier>(startOffset, endOffset, origin),
    IrValueParameter,
    ValueParameterCarrier {

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ParameterDescriptor
        get() = symbol.descriptor

    init {
        symbol.bind(this)
    }

    override var defaultValueField: IrExpressionBody? = null

    override var defaultValue: IrExpressionBody?
        get() = getCarrier().defaultValueField
        set(v) {
            if (defaultValue !== v) {
                if (v is IrBodyBase<*>) {
                    v.container = this
                }
                setCarrier().defaultValueField = v
            }
        }
}
