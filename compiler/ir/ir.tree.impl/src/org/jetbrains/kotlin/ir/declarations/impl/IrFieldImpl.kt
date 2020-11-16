/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.getFlag
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.toFlag
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrFieldImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrFieldSymbol,
    override val name: Name,
    override var type: IrType,
    override var visibility: DescriptorVisibility,
    isFinal: Boolean,
    isExternal: Boolean,
    isStatic: Boolean,
) : IrField() {
    init {
        symbol.bind(this)
    }

    private val flags = (isFinal.toFlag(IS_FINAL_BIT) or isExternal.toFlag(IS_EXTERNAL_BIT) or isStatic.toFlag(IS_STATIC_BIT)).toByte()

    override val factory: IrFactory
        get() = IrFactoryImpl

    override val isFinal: Boolean
        get() = flags.toInt().getFlag(IS_FINAL_BIT)

    override val isExternal: Boolean
        get() = flags.toInt().getFlag(IS_EXTERNAL_BIT)

    override val isStatic: Boolean
        get() = flags.toInt().getFlag(IS_STATIC_BIT)

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    @ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor
        get() = symbol.descriptor

    override var initializer: IrExpressionBody? = null

    override var correspondingPropertySymbol: IrPropertySymbol? = null

    override var metadata: MetadataSource? = null

    private companion object {
        const val IS_FINAL_BIT = 1 shl 0
        const val IS_EXTERNAL_BIT = 1 shl 1
        const val IS_STATIC_BIT = 1 shl 2
    }
}
