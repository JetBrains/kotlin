/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import kotlin.properties.ReadWriteProperty

interface AbstractFir2IrLazyDeclaration<F> :
    IrDeclaration, IrLazyDeclarationBase, Fir2IrComponents where F : FirAnnotationContainer {

    val fir: F

    override val factory: IrFactory
        get() = irFactory

    override fun createLazyAnnotations(): ReadWriteProperty<Any?, List<IrConstructorCall>> = lazyVar(lock) {
        fir.annotations.mapNotNull {
            callGenerator.convertToIrConstructorCall(it) as? IrConstructorCall
        }
    }

    override fun lazyParent(): IrDeclarationParent {
        return parent
    }

    override val stubGenerator: DeclarationStubGenerator
        get() = shouldNotBeCalled()
    override val typeTranslator: TypeTranslator
        get() = shouldNotBeCalled()

    fun mutationNotSupported(): Nothing =
        error("Mutation of Fir2Ir lazy elements is not possible")
}
