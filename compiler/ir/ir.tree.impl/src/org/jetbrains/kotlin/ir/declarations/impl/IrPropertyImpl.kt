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

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertyDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class IrPropertyCommonImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val name: Name,
    override var visibility: DescriptorVisibility,
    protected var flags: Short,
    override val containerSource: DeserializedContainerSource?,
) : IrProperty() {
    override val factory: IrFactory
        get() = IrFactoryImpl

    override var modality: Modality
        get() = flags.toInt().toModality()
        set(value) {
            flags = flags.toInt().setModality(value).toShort()
        }

    override val isVar: Boolean
        get() = flags.toInt().getFlag(IS_VAR_BIT)

    override val isConst: Boolean
        get() = flags.toInt().getFlag(IS_CONST_BIT)

    override val isLateinit: Boolean
        get() = flags.toInt().getFlag(IS_LATEINIT_BIT)

    override val isDelegated: Boolean
        get() = flags.toInt().getFlag(IS_DELEGATED_BIT)

    override val isExternal: Boolean
        get() = flags.toInt().getFlag(IS_EXTERNAL_BIT)

    override val isExpect: Boolean
        get() = flags.toInt().getFlag(IS_EXPECT_BIT)

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    override var backingField: IrField? = null

    override var getter: IrSimpleFunction? = null

    override var setter: IrSimpleFunction? = null

    override var metadata: MetadataSource? = null

    override var attributeOwnerId: IrAttributeContainer = this

    protected companion object {
        const val IS_VAR_BIT = 1 shl (IrFlags.MODALITY_BITS + 0)
        const val IS_CONST_BIT = 1 shl (IrFlags.MODALITY_BITS + 1)
        const val IS_LATEINIT_BIT = 1 shl (IrFlags.MODALITY_BITS + 2)
        const val IS_DELEGATED_BIT = 1 shl (IrFlags.MODALITY_BITS + 3)
        const val IS_EXTERNAL_BIT = 1 shl (IrFlags.MODALITY_BITS + 4)
        const val IS_EXPECT_BIT = 1 shl (IrFlags.MODALITY_BITS + 5)
        const val IS_FAKE_OVERRIDE_BIT = 1 shl (IrFlags.MODALITY_BITS + 6)
    }
}

class IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrPropertySymbol,
    name: Name,
    visibility: DescriptorVisibility,
    modality: Modality,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExternal: Boolean,
    isExpect: Boolean = false,
    isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
    containerSource: DeserializedContainerSource? = null,
) : IrPropertyCommonImpl(
    startOffset, endOffset, origin, name, visibility,
    (modality.toFlags() or
            isVar.toFlag(IS_VAR_BIT) or isConst.toFlag(IS_CONST_BIT) or isLateinit.toFlag(IS_LATEINIT_BIT) or
            isDelegated.toFlag(IS_DELEGATED_BIT) or isExternal.toFlag(IS_EXTERNAL_BIT) or isExpect.toFlag(IS_EXPECT_BIT) or
            isFakeOverride.toFlag(IS_FAKE_OVERRIDE_BIT)).toShort(),
    containerSource
) {
    init {
        symbol.bind(this)
    }

    override val isFakeOverride: Boolean
        get() = flags.toInt().getFlag(IS_FAKE_OVERRIDE_BIT)

    @ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor
        get() = symbol.descriptor
}

class IrFakeOverridePropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    name: Name,
    visibility: DescriptorVisibility,
    modality: Modality,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExternal: Boolean,
    isExpect: Boolean,
) : IrPropertyCommonImpl(
    startOffset, endOffset, origin, name, visibility,
    (modality.toFlags() or
            isVar.toFlag(IS_VAR_BIT) or isConst.toFlag(IS_CONST_BIT) or isLateinit.toFlag(IS_LATEINIT_BIT) or
            isDelegated.toFlag(IS_DELEGATED_BIT) or isExternal.toFlag(IS_EXTERNAL_BIT) or isExpect.toFlag(IS_EXPECT_BIT)).toShort(),
    containerSource = null,
), IrFakeOverrideProperty {
    override val isFakeOverride: Boolean
        get() = true

    private var _symbol: IrPropertySymbol? = null

    override val symbol: IrPropertySymbol
        get() = _symbol ?: error("$this has not acquired a symbol yet")

    @ObsoleteDescriptorBasedAPI
    override val descriptor
        get() = _symbol?.descriptor ?: WrappedPropertyDescriptor()

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun acquireSymbol(symbol: IrPropertySymbol): IrProperty {
        assert(_symbol == null) { "$this already has symbol _symbol" }
        _symbol = symbol
        symbol.bind(this)
        (symbol.descriptor as? WrappedPropertyDescriptor)?.bind(this)
        return this
    }
}
