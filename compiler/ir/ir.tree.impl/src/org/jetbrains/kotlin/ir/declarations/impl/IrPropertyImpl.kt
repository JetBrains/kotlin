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
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class IrPropertyCommonImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val name: Name,
    override var visibility: DescriptorVisibility,
    override val isVar: Boolean,
    override val isConst: Boolean,
    override val isLateinit: Boolean,
    override val isDelegated: Boolean,
    override val isExternal: Boolean,
    override val isExpect: Boolean,
    override val containerSource: DeserializedContainerSource?,
) : IrProperty() {
    override val factory: IrFactory
        get() = IrFactoryImpl

    override lateinit var parent: IrDeclarationParent
    override var annotations: List<IrConstructorCall> = emptyList()

    override var backingField: IrField? = null

    override var getter: IrSimpleFunction? = null

    override var setter: IrSimpleFunction? = null

    override var overriddenSymbols: List<IrPropertySymbol> = emptyList()

    override var metadata: MetadataSource? = null

    override var attributeOwnerId: IrAttributeContainer = this
}

class IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrPropertySymbol,
    name: Name,
    visibility: DescriptorVisibility,
    override val modality: Modality,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExternal: Boolean,
    isExpect: Boolean = false,
    override val isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
    containerSource: DeserializedContainerSource? = null,
) : IrPropertyCommonImpl(
    startOffset, endOffset, origin, name, visibility, isVar, isConst, isLateinit, isDelegated, isExternal, isExpect,
    containerSource
) {
    init {
        symbol.bind(this)
    }

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
    override var modality: Modality,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExternal: Boolean,
    isExpect: Boolean,
) : IrPropertyCommonImpl(
    startOffset, endOffset, origin, name, visibility, isVar, isConst, isLateinit, isDelegated, isExternal, isExpect,
    containerSource = null,
), IrFakeOverrideProperty {
    override val isFakeOverride: Boolean
        get() = true

    private var _symbol: IrPropertySymbol? = null

    override val symbol: IrPropertySymbol
        get() = _symbol ?: error("$this has not acquired a symbol yet")

    @ObsoleteDescriptorBasedAPI
    override val descriptor
        get() = _symbol?.descriptor ?: this.toIrBasedDescriptor()

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun acquireSymbol(symbol: IrPropertySymbol): IrProperty {
        assert(_symbol == null) { "$this already has symbol _symbol" }
        _symbol = symbol
        symbol.bind(this)
        return this
    }

    override val isBound: Boolean
        get() = _symbol != null
}
