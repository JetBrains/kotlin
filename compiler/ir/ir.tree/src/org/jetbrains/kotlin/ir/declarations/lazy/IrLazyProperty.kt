/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor

class IrLazyProperty(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrPropertySymbol,
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: PropertyDescriptor,
    override var name: Name,
    override var visibility: DescriptorVisibility,
    override var modality: Modality,
    override var isVar: Boolean,
    override var isConst: Boolean,
    override var isLateinit: Boolean,
    override var isDelegated: Boolean,
    override var isExternal: Boolean,
    override var isExpect: Boolean,
    override var isFakeOverride: Boolean,
    override val stubGenerator: DeclarationStubGenerator,
    override val typeTranslator: TypeTranslator,
) : IrProperty(), IrLazyDeclarationBase {
    init {
        symbol.bind(this)
    }

    override var parent: IrDeclarationParent by createLazyParent()

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    private val hasBackingField: Boolean =
        descriptor.compileTimeInitializer != null || descriptor.getter == null ||
                stubGenerator.extensions.isPropertyWithPlatformField(descriptor)

    override var backingField: IrField? by lazyVar(stubGenerator.lock) {
        if (hasBackingField) {
            stubGenerator.generateFieldStub(descriptor).apply {
                correspondingPropertySymbol = this@IrLazyProperty.symbol
            }
        } else null
    }

    override var getter: IrSimpleFunction? by lazyVar(stubGenerator.lock) {
        descriptor.getter?.let {
            stubGenerator.generateFunctionStub(it, createPropertyIfNeeded = false)
                .apply { correspondingPropertySymbol = this@IrLazyProperty.symbol }
        }
    }

    override var setter: IrSimpleFunction? by lazyVar(stubGenerator.lock) {
        descriptor.setter?.let {
            stubGenerator.generateFunctionStub(it, createPropertyIfNeeded = false)
                .apply { correspondingPropertySymbol = this@IrLazyProperty.symbol }
        }
    }

    override var overriddenSymbols: List<IrPropertySymbol> by lazyVar(stubGenerator.lock) {
        descriptor.overriddenDescriptors.mapTo(ArrayList()) {
            stubGenerator.generatePropertyStub(it.original).symbol
        }
    }

    override val containerSource: DeserializedContainerSource?
        get() = (descriptor as? DeserializedPropertyDescriptor)?.containerSource

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override var attributeOwnerId: IrAttributeContainer
        get() = this
        set(_) = error("We should never need to change attributeOwnerId of external declarations.")

    override var originalBeforeInline: IrAttributeContainer?
        get() = this
        set(_) = error("We should never need to change originalBeforeInline of external declarations.")
}
