/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
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
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrValueParameterSymbol,
    override val descriptor: ValueParameterDescriptor,
    override var name: Name,
    override val index: Int,
    kotlinType: KotlinType,
    varargElementKotlinType: KotlinType?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean,
    override val isHidden: Boolean,
    override val isAssignable: Boolean,
    override val stubGenerator: DeclarationStubGenerator,
    override val typeTranslator: TypeTranslator,
) : IrValueParameter(), IrLazyDeclarationBase {
    override lateinit var parent: IrDeclarationParent

    override var defaultValue: IrExpressionBody? = null

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    override var type: IrType by lazyVar(stubGenerator.lock) {
        kotlinType.toIrType()
    }

    override var varargElementType: IrType? by lazyVar(stubGenerator.lock) {
        varargElementKotlinType?.toIrType()
    }

    init {
        symbol.bind(this)
    }
}
