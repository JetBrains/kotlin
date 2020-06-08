/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.DeepCopyTypeRemapper
import org.jetbrains.kotlin.ir.util.DescriptorsRemapper
import org.jetbrains.kotlin.ir.visitors.acceptVoid

@Suppress("UNCHECKED_CAST")
@OptIn(DescriptorBasedIr::class)
fun <T : IrElement> T.deepCopyWithVariables(): T {
    val descriptorsRemapper = object : DescriptorsRemapper {
        override fun remapDeclaredVariable(descriptor: VariableDescriptor) = WrappedVariableDescriptor()
    }

    val symbolsRemapper = DeepCopySymbolRemapper(descriptorsRemapper)
    acceptVoid(symbolsRemapper)

    val typesRemapper = DeepCopyTypeRemapper(symbolsRemapper)

    return this.transform(
            object : DeepCopyIrTreeWithSymbols(symbolsRemapper, typesRemapper) {
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