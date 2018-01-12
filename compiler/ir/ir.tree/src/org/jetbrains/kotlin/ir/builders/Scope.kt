/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.createFunctionSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

class Scope(val scopeOwnerSymbol: IrSymbol) {
    val scopeOwner: DeclarationDescriptor get() = scopeOwnerSymbol.descriptor

    @Deprecated("Creates unbound symbol")
    constructor(descriptor: DeclarationDescriptor) : this(createSymbolForScopeOwner(descriptor))

    private var lastTemporaryIndex: Int = 0
    private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

    fun createDescriptorForTemporaryVariable(
        type: KotlinType,
        nameHint: String? = null,
        isMutable: Boolean = false
    ): IrTemporaryVariableDescriptor =
        IrTemporaryVariableDescriptorImpl(scopeOwner, Name.identifier(getNameForTemporary(nameHint)), type, isMutable)

    private fun getNameForTemporary(nameHint: String?): String {
        val index = nextTemporaryIndex()
        return if (nameHint != null) "tmp${index}_$nameHint" else "tmp$index"
    }

    fun createTemporaryVariable(
        irExpression: IrExpression,
        nameHint: String? = null,
        isMutable: Boolean = false,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
    ): IrVariable =
        IrVariableImpl(
            irExpression.startOffset, irExpression.endOffset, origin,
            createDescriptorForTemporaryVariable(irExpression.type, nameHint, isMutable),
            irExpression
        )
}

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Creates unbound symbol")
fun createSymbolForScopeOwner(descriptor: DeclarationDescriptor) =
    when (descriptor) {
        is ClassDescriptor -> IrClassSymbolImpl(descriptor)
        is FunctionDescriptor -> createFunctionSymbol(descriptor)
        is PropertyDescriptor -> IrFieldSymbolImpl(descriptor)
        else -> throw AssertionError("Unexpected scopeOwner descriptor: $descriptor")
    }
