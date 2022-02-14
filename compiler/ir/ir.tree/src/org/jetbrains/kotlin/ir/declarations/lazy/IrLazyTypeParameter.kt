/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

@OptIn(ObsoleteDescriptorBasedAPI::class)
class IrLazyTypeParameter(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrTypeParameterSymbol,
    override val descriptor: TypeParameterDescriptor,
    override var name: Name,
    override val index: Int,
    override val isReified: Boolean,
    override val variance: Variance,
    override val stubGenerator: DeclarationStubGenerator,
    override val typeTranslator: TypeTranslator,
) : IrTypeParameter(), IrLazyDeclarationBase {
    init {
        symbol.bind(this)
    }

    override var parent: IrDeclarationParent by createLazyParent()

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    override var superTypes: List<IrType> by lazyVar(stubGenerator.lock) {
        typeTranslator.buildWithScope(this.parent as IrTypeParametersContainer) {
            descriptor.upperBounds.mapTo(arrayListOf()) { it.toIrType() }
        }
    }
}
