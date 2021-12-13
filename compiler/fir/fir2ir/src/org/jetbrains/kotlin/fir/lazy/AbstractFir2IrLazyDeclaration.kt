/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.symbols.Fir2IrBindableSymbol
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import kotlin.properties.ReadWriteProperty

interface AbstractFir2IrLazyDeclaration<F, D : IrDeclaration> :
    IrDeclaration, IrDeclarationParent, IrLazyDeclarationBase, Fir2IrComponents where F : FirMemberDeclaration, F : FirAnnotationContainer {

    val fir: F
    override val symbol: Fir2IrBindableSymbol<*, D>

    override val factory: IrFactory
        get() = irFactory

    var typeParameters: List<IrTypeParameter>

    fun prepareTypeParameters() {
        typeParameters = fir.typeParameters.mapIndexedNotNull { index, typeParameter ->
            if (typeParameter !is FirTypeParameter) return@mapIndexedNotNull null
            classifierStorage.getIrTypeParameter(typeParameter, index).apply {
                parent = this@AbstractFir2IrLazyDeclaration
                if (superTypes.isEmpty()) {
                    superTypes = typeParameter.bounds.map { it.toIrType(typeConverter) }
                }
            }
        }
    }

    override fun createLazyAnnotations(): ReadWriteProperty<Any?, List<IrConstructorCall>> = lazyVar(lock) {
        fir.annotations.mapNotNull {
            callGenerator.convertToIrConstructorCall(it) as? IrConstructorCall
        }
    }

    override fun lazyParent(): IrDeclarationParent {
        return parent
    }

    override val stubGenerator: DeclarationStubGenerator
        get() = error("Should not be called")
    override val typeTranslator: TypeTranslator
        get() = error("Should not be called")
}
