/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer

@PrettyIrDsl
interface IrDeclarationContainerBuilder {

    val declarationBuilders: MutableList<IrDeclarationBuilder<*>>

    val symbolContext: SymbolContext

    @IrNodeBuilderDsl
    fun irClass(name: String, block: IrElementBuilderClosure<IrClassBuilder>) {
        declarationBuilders.add(IrClassBuilder(symbolContext, name).apply(block))
    }

    @IrNodeBuilderDsl
    fun irSimpleFunction(name: String, block: IrElementBuilderClosure<IrSimpleFunctionBuilder>) {
        declarationBuilders.add(IrSimpleFunctionBuilder(name).apply(block))
    }
}

internal fun IrDeclarationContainerBuilder.addDeclarationsTo(declarationContainer: IrDeclarationContainer) {
    for (declarationBuilder in declarationBuilders) {
        val declaration = declarationBuilder.build()
        declaration.parent = declarationContainer
        declarationContainer.declarations.add(declaration)
    }
}
