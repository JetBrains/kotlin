/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.name.SpecialNames

@PrettyIrDsl
interface IrDeclarationContainerBuilder {

    @Suppress("PropertyName") // TODO: Make @RequiresOptIn?
    val __internal_declarationBuilders: MutableList<IrDeclarationBuilder<*>>

    val buildingContext: IrBuildingContext

    @Suppress("FunctionName") // TODO: Make @RequiresOptIn?
    fun __internal_addDeclarationBuilder(declarationBuilder: IrDeclarationBuilder<*>) {
        __internal_declarationBuilders.add(declarationBuilder)
    }
}

internal fun IrDeclarationContainerBuilder.addDeclarationsTo(declarationContainer: IrDeclarationContainer) {
    for (declarationBuilder in __internal_declarationBuilders) {
        val declaration = declarationBuilder.build()
        declaration.parent = declarationContainer
        declarationContainer.declarations.add(declaration)
    }
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irConstructor(block: IrElementBuilderClosure<IrConstructorBuilder>) {
    irConstructor(SpecialNames.INIT, block)
}
