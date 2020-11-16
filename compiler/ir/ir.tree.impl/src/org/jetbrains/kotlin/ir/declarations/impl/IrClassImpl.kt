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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrClassImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrClassSymbol,
    override val name: Name,
    override val kind: ClassKind,
    override var visibility: DescriptorVisibility,
    modality: Modality,
    isCompanion: Boolean = false,
    isInner: Boolean = false,
    isData: Boolean = false,
    isExternal: Boolean = false,
    isInline: Boolean = false,
    isExpect: Boolean = false,
    isFun: Boolean = false,
    override val source: SourceElement = SourceElement.NO_SOURCE
) : IrClass() {
    init {
        symbol.bind(this)
    }

    private var flags = (modality.toFlags() or
            isCompanion.toFlag(IS_COMPANION_BIT) or isInner.toFlag(IS_INNER_BIT) or isData.toFlag(IS_DATA_BIT) or
            isExternal.toFlag(IS_EXTERNAL_BIT) or isInline.toFlag(IS_INLINE_BIT) or isExpect.toFlag(IS_EXPECT_BIT) or
            isFun.toFlag(IS_FUN_BIT)).toShort()

    override val factory: IrFactory
        get() = IrFactoryImpl

    override var modality: Modality
        get() = flags.toInt().toModality()
        set(value) {
            flags = flags.toInt().setModality(value).toShort()
        }

    override val isCompanion: Boolean
        get() = flags.toInt().getFlag(IS_COMPANION_BIT)

    override val isInner: Boolean
        get() = flags.toInt().getFlag(IS_INNER_BIT)

    override val isData: Boolean
        get() = flags.toInt().getFlag(IS_DATA_BIT)

    override val isExternal: Boolean
        get() = flags.toInt().getFlag(IS_EXTERNAL_BIT)

    override val isInline: Boolean
        get() = flags.toInt().getFlag(IS_INLINE_BIT)

    override val isExpect: Boolean
        get() = flags.toInt().getFlag(IS_EXPECT_BIT)

    override val isFun: Boolean
        get() = flags.toInt().getFlag(IS_FUN_BIT)

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor
        get() = symbol.descriptor

    override var thisReceiver: IrValueParameter? = null

    override val declarations: MutableList<IrDeclaration> = ArrayList()

    override var typeParameters: List<IrTypeParameter> = emptyList()

    override var superTypes: List<IrType> = emptyList()

    override var metadata: MetadataSource? = null

    override var attributeOwnerId: IrAttributeContainer = this

    private companion object {
        const val IS_COMPANION_BIT = 1 shl (IrFlags.MODALITY_BITS + 0)
        const val IS_INNER_BIT = 1 shl (IrFlags.MODALITY_BITS + 1)
        const val IS_DATA_BIT = 1 shl (IrFlags.MODALITY_BITS + 2)
        const val IS_EXTERNAL_BIT = 1 shl (IrFlags.MODALITY_BITS + 3)
        const val IS_INLINE_BIT = 1 shl (IrFlags.MODALITY_BITS + 4)
        const val IS_EXPECT_BIT = 1 shl (IrFlags.MODALITY_BITS + 5)
        const val IS_FUN_BIT = 1 shl (IrFlags.MODALITY_BITS + 6)
    }
}
