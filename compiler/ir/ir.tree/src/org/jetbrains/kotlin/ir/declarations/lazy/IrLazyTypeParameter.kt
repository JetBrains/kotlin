/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.withInitialIr
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

@OptIn(ObsoleteDescriptorBasedAPI::class)
class IrLazyTypeParameter(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrTypeParameterSymbol,
    override val descriptor: TypeParameterDescriptor,
    override val name: Name,
    override val index: Int,
    override val isReified: Boolean,
    override val variance: Variance,
    stubGenerator: DeclarationStubGenerator,
    typeTranslator: TypeTranslator
) :
    IrLazyDeclarationBase(startOffset, endOffset, origin, stubGenerator, typeTranslator),
    IrTypeParameter {

    init {
        symbol.bind(this)
    }

    override val superTypes: MutableList<IrType> by lazy {
        withInitialIr {
            typeTranslator.buildWithScope(this.parent as IrTypeParametersContainer) {
                val descriptor = symbol.descriptor
                descriptor.upperBounds.mapTo(arrayListOf()) { it.toIrType() }
            }
        }
    }
}
