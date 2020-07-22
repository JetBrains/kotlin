/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.hasBackingField

class IrLazyProperty(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrPropertySymbol,
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: PropertyDescriptor,
    override val name: Name,
    override var visibility: Visibility,
    override val modality: Modality,
    override val isVar: Boolean,
    override val isConst: Boolean,
    override val isLateinit: Boolean,
    override val isDelegated: Boolean,
    override val isExternal: Boolean,
    override val isExpect: Boolean,
    override val isFakeOverride: Boolean,
    override val stubGenerator: DeclarationStubGenerator,
    override val typeTranslator: TypeTranslator,
    bindingContext: BindingContext? = null
) : IrProperty(), IrLazyDeclarationBase {
    init {
        symbol.bind(this)
    }

    override var parent: IrDeclarationParent by createLazyParent()

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    private val hasBackingField: Boolean =
        descriptor.hasBackingField(bindingContext) || stubGenerator.extensions.isPropertyWithPlatformField(descriptor)

    override var backingField: IrField? by lazyVar {
        if (hasBackingField) {
            stubGenerator.generateFieldStub(descriptor).apply {
                correspondingPropertySymbol = this@IrLazyProperty.symbol
            }
        } else null
    }

    override var getter: IrSimpleFunction? by lazyVar {
        descriptor.getter?.let { stubGenerator.generateFunctionStub(it, createPropertyIfNeeded = false) }?.apply {
            correspondingPropertySymbol = this@IrLazyProperty.symbol
        }
    }

    override var setter: IrSimpleFunction? by lazyVar {
        descriptor.setter?.let { stubGenerator.generateFunctionStub(it, createPropertyIfNeeded = false) }?.apply {
            correspondingPropertySymbol = this@IrLazyProperty.symbol
        }
    }

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")
}
