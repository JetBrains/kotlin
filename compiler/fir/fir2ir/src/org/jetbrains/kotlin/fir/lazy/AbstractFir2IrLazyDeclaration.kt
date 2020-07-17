/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.symbols.Fir2IrBindableSymbol
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

abstract class AbstractFir2IrLazyDeclaration<F : FirMemberDeclaration, D : IrSymbolOwner>(
    private val components: Fir2IrComponents,
    startOffset: Int,
    endOffset: Int,
    override var origin: IrDeclarationOrigin,
    val fir: F,
    open val symbol: Fir2IrBindableSymbol<*, D>
) : IrElementBase(startOffset, endOffset), IrDeclaration, IrDeclarationParent, Fir2IrComponents by components {
    internal fun prepareTypeParameters() {
        typeParameters = fir.typeParameters.mapIndexedNotNull { index, typeParameter ->
            if (typeParameter !is FirTypeParameter) return@mapIndexedNotNull null
            classifierStorage.getIrTypeParameter(typeParameter, index).apply {
                parent = this@AbstractFir2IrLazyDeclaration
                if (superTypes.isEmpty()) {
                    typeParameter.bounds.mapTo(superTypes) { it.toIrType(typeConverter) }
                }
            }
        }
    }

    lateinit var typeParameters: List<IrTypeParameter>

    override lateinit var parent: IrDeclarationParent

    @ObsoleteDescriptorBasedAPI
    override val descriptor: DeclarationDescriptor
        get() = symbol.descriptor

    override var annotations: List<IrConstructorCall> by lazyVar {
        fir.annotations.mapNotNull {
            callGenerator.convertToIrConstructorCall(it) as? IrConstructorCall
        }
    }
}
