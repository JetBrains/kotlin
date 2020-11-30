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
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class Scope(val scopeOwnerSymbol: IrSymbol) {
    fun getLocalDeclarationParent(): IrDeclarationParent {
        if (!scopeOwnerSymbol.isBound) throw AssertionError("Unbound symbol: $scopeOwnerSymbol")
        return when (val scopeOwnerElement = scopeOwnerSymbol.owner) {
            is IrDeclarationParent -> scopeOwnerElement
            !is IrDeclaration -> throw AssertionError("Not a declaration: $scopeOwnerElement")
            else -> scopeOwnerElement.parent
        }
    }

    private var lastTemporaryIndex: Int = 0
    private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

    fun inventNameForTemporary(prefix: String = "tmp", nameHint: String? = null): String {
        val index = nextTemporaryIndex()
        return if (nameHint != null) "$prefix${index}_$nameHint" else "$prefix$index"
    }

    private fun getNameForTemporary(nameHint: String?): String =
        inventNameForTemporary("tmp", nameHint)

    fun createTemporaryVariableDeclaration(
        irType: IrType,
        nameHint: String? = null,
        isMutable: Boolean = false,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ): IrVariable {
        val name = Name.identifier(getNameForTemporary(nameHint))
        return IrVariableImpl(
            startOffset, endOffset, origin, IrVariableSymbolImpl(), name,
            irType, isMutable, isConst = false, isLateinit = false
        ).apply {
            parent = getLocalDeclarationParent()
        }
    }

    fun createTemporaryVariable(
        irExpression: IrExpression,
        nameHint: String? = null,
        isMutable: Boolean = false,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
        irType: IrType? = null
    ): IrVariable {
        return createTemporaryVariableDeclaration(
            irType ?: irExpression.type,
            nameHint, isMutable,
            origin, irExpression.startOffset, irExpression.endOffset
        ).apply {
            initializer = irExpression
        }
    }
}