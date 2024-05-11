/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.lazyMappedPropertyListVar
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class Fir2IrLazyPropertyForPureField(
    private val c: Fir2IrComponents,
    private val field: Fir2IrLazyField,
    override val symbol: IrPropertySymbol,
    parent: IrDeclarationParent
) : IrProperty(), Fir2IrComponents by c {
    init {
        this.parent = parent
        symbol.bind(this)
    }

    override var overriddenSymbols: List<IrPropertySymbol> by symbolsMappingForLazyClasses.lazyMappedPropertyListVar(lock) lazy@{
        val containingClass = field.containingClass ?: return@lazy emptyList()

        val baseFieldsWithDispatchReceiverTag =
            lazyFakeOverrideGenerator.computeFakeOverrideKeys(containingClass, field.fir.symbol)
        baseFieldsWithDispatchReceiverTag.map { (symbol, dispatchReceiverLookupTag) ->
            declarationStorage.getIrSymbolForField(symbol, dispatchReceiverLookupTag) as IrPropertySymbol
        }
    }

    override var annotations: List<IrConstructorCall>
        get() = emptyList()
        set(_) = mutationNotSupported()

    override val startOffset: Int
        get() = this.field.startOffset

    override val endOffset: Int
        get() = this.field.endOffset

    override var origin: IrDeclarationOrigin
        get() = this.field.origin
        set(_) = mutationNotSupported()

    @ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor
        get() = symbol.descriptor

    override var isVar: Boolean
        get() = !this.field.isFinal
        set(_) = mutationNotSupported()

    override var isConst: Boolean
        get() = this.field.isStatic && this.field.isFinal
        set(_) = mutationNotSupported()

    override var isLateinit: Boolean
        get() = false
        set(_) = mutationNotSupported()

    override var isDelegated: Boolean
        get() = false
        set(_) = mutationNotSupported()

    override var isExternal: Boolean
        get() = false
        set(_) = mutationNotSupported()

    override var isExpect: Boolean
        get() = false
        set(_) = mutationNotSupported()

    override var name: Name
        get() = this.field.name
        set(_) = mutationNotSupported()

    override var visibility: DescriptorVisibility
        get() = this.field.visibility
        set(_) = mutationNotSupported()

    override var modality: Modality
        get() = Modality.FINAL
        set(_) = mutationNotSupported()

    override var backingField: IrField?
        get() = this.field
        set(_) = mutationNotSupported()

    override var getter: IrSimpleFunction?
        get() = null
        set(_) = mutationNotSupported()

    override var setter: IrSimpleFunction?
        get() = null
        set(_) = mutationNotSupported()

    override var isFakeOverride: Boolean
        get() = this.field.isFakeOverride
        set(_) = mutationNotSupported()

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override val containerSource: DeserializedContainerSource?
        get() = null

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null

    override val factory: IrFactory
        get() = c.irFactory
}
