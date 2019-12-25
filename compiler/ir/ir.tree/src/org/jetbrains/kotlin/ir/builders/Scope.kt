/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.factories.IrDeclarationFactory
import org.jetbrains.kotlin.ir.factories.createVariable
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

class Scope(val scopeOwnerSymbol: IrSymbol, val irDeclarationFactory: IrDeclarationFactory) {
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
    constructor(descriptor: DeclarationDescriptor, irDeclarationFactory: IrDeclarationFactory) : this(
        createSymbolForScopeOwner(descriptor),
        irDeclarationFactory
    )

    private var lastTemporaryIndex: Int = 0
    private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

    private fun createDescriptorForTemporaryVariable(
        type: KotlinType,
        nameHint: String? = null,
        isMutable: Boolean = false
    ): IrTemporaryVariableDescriptor =
        IrTemporaryVariableDescriptorImpl(scopeOwner, Name.identifier(getNameForTemporary(nameHint)), type, isMutable)

    private fun getNameForTemporary(nameHint: String?): String {
        val index = nextTemporaryIndex()
        return if (nameHint != null) "tmp${index}_$nameHint" else "tmp$index"
    }

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
        return irDeclarationFactory.createVariable(
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
        return irDeclarationFactory.createVariable(
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
