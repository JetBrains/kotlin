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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

@OptIn(DescriptorBasedIr::class)
class Scope(val scopeOwnerSymbol: IrSymbol) {
    @DescriptorBasedIr
    val scopeOwner: DeclarationDescriptor get() = scopeOwnerSymbol.descriptor

    fun getLocalDeclarationParent(): IrDeclarationParent {
        if (!scopeOwnerSymbol.isBound) throw AssertionError("Unbound symbol: $scopeOwner")
        val scopeOwnerElement = scopeOwnerSymbol.owner
        return when (scopeOwnerElement) {
            is IrDeclarationParent -> scopeOwnerElement
            !is IrDeclaration -> throw AssertionError("Not a declaration: $scopeOwnerElement")
            else -> scopeOwnerElement.parent
        }
    }

    @Deprecated("Creates unbound symbol")
    constructor(descriptor: DeclarationDescriptor) : this(createSymbolForScopeOwner(descriptor))

    private var lastTemporaryIndex: Int = 0
    private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

    fun inventNameForTemporary(prefix: String = "tmp", nameHint: String? = null): String {
        val index = nextTemporaryIndex()
        return if (nameHint != null) "$prefix${index}_$nameHint" else "$prefix$index"
    }

    private fun createDescriptorForTemporaryVariable(
        type: KotlinType,
        nameHint: String? = null,
        isMutable: Boolean = false
    ): IrTemporaryVariableDescriptor =
        IrTemporaryVariableDescriptorImpl(scopeOwner, Name.identifier(getNameForTemporary(nameHint)), type, isMutable)

    private fun getNameForTemporary(nameHint: String?): String =
        inventNameForTemporary("tmp", nameHint)

    fun createTemporaryVariableDeclaration(
        irType: IrType,
        nameHint: String? = null,
        isMutable: Boolean = false,
        type: KotlinType? = null,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ): IrVariable {
        val originalKotlinType = type ?: irType.toKotlinType()
        return IrVariableImpl(
            startOffset, endOffset, origin,
            createDescriptorForTemporaryVariable(
                originalKotlinType,
                nameHint, isMutable
            ),
            irType
        ).apply {
            parent = getLocalDeclarationParent()
        }
    }

    fun createTemporaryVariable(
        irExpression: IrExpression,
        nameHint: String? = null,
        isMutable: Boolean = false,
        type: KotlinType? = null,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
        irType: IrType? = null
    ): IrVariable {
        val originalKotlinType = type ?: (irExpression.type.originalKotlinType ?: irExpression.type.toKotlinType())
        return createTemporaryVariableDeclaration(
            irType ?: irExpression.type,
            nameHint, isMutable, originalKotlinType,
            origin, irExpression.startOffset, irExpression.endOffset
        ).apply {
            initializer = irExpression
        }
    }

    fun createTemporaryVariableWithGivenDescriptor(
        irExpression: IrExpression,
        nameHint: String? = null,
        isMutable: Boolean = false,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
        descriptor: VariableDescriptor
    ): IrVariable {
        return IrVariableImpl(
            irExpression.startOffset, irExpression.endOffset, origin,
            IrVariableSymbolImpl(descriptor),
            Name.identifier(getNameForTemporary(nameHint)),
            irExpression.type,
            isVar = isMutable,
            isConst = false,
            isLateinit = false
        ).also {
            it.initializer = irExpression
        }
    }
}

private fun createSymbolForScopeOwner(descriptor: DeclarationDescriptor) =
    when (descriptor) {
        is ClassDescriptor -> IrClassSymbolImpl(descriptor)
        is ClassConstructorDescriptor -> IrConstructorSymbolImpl(descriptor.original)
        is FunctionDescriptor -> IrSimpleFunctionSymbolImpl(descriptor.original)
        is PropertyDescriptor -> IrFieldSymbolImpl(descriptor)
        else -> throw AssertionError("Unexpected scopeOwner descriptor: $descriptor")
    }
