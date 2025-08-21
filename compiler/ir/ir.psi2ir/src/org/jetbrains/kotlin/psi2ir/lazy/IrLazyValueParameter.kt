/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.lazy

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

@OptIn(ObsoleteDescriptorBasedAPI::class)
class IrLazyValueParameter(
    override var startOffset: Int,
    override var endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrValueParameterSymbol,
    override val descriptor: ValueParameterDescriptor,
    override var name: Name,
    kotlinType: KotlinType,
    varargElementKotlinType: KotlinType?,
    override var isCrossinline: Boolean,
    override var isNoinline: Boolean,
    override var isHidden: Boolean,
    override var isAssignable: Boolean,
    override val stubGenerator: DeclarationStubGenerator,
    override val typeTranslator: TypeTranslator,
) : IrValueParameter(), Psi2IrLazyDeclarationBase {
    override var defaultValue: IrExpressionBody? = null

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    override var type: IrType by lazyVar(stubGenerator.lock) {
        kotlinType.toIrType()
    }

    override var varargElementType: IrType? by lazyVar(stubGenerator.lock) {
        varargElementKotlinType?.toIrType()
    }

    override var attributeOwnerId: IrElement = this

    init {
        symbol.bind(this)
    }
}
