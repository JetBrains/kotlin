/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.factories.IrDeclarationFactory
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.DeepCopyTypeRemapper
import org.jetbrains.kotlin.ir.util.DescriptorsRemapper
import org.jetbrains.kotlin.ir.visitors.acceptVoid

@Suppress("UNCHECKED_CAST")
fun <T : IrElement> T.deepCopyWithVariables(irDeclarationFactory: IrDeclarationFactory = IrDeclarationFactory.DEFAULT): T {
    val descriptorsRemapper = object : DescriptorsRemapper {
        override fun remapDeclaredVariable(descriptor: VariableDescriptor) = WrappedVariableDescriptor()
    }

    val symbolsRemapper = DeepCopySymbolRemapper(descriptorsRemapper)
    acceptVoid(symbolsRemapper)

    val typesRemapper = DeepCopyTypeRemapper(symbolsRemapper)

    return this.transform(
            object : DeepCopyIrTreeWithSymbols(irDeclarationFactory, symbolsRemapper, typesRemapper) {
                override fun getNonTransformedLoop(irLoop: IrLoop): IrLoop {
                    return irLoop
                }

                override fun visitVariable(declaration: IrVariable): IrVariable {
                    val variable = super.visitVariable(declaration)
                    variable.descriptor.let { if (it is WrappedVariableDescriptor) it.bind(variable) }
                    return variable
                }
            },
            null
    ) as T
}