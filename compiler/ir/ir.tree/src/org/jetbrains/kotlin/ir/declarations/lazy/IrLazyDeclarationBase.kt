/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.types.KotlinType
import kotlin.properties.ReadWriteProperty

@OptIn(ObsoleteDescriptorBasedAPI::class)
interface IrLazyDeclarationBase : IrDeclaration {
    val stubGenerator: DeclarationStubGenerator
    val typeTranslator: TypeTranslator

    override val factory: IrFactory
        get() = stubGenerator.symbolTable.irFactory

    fun KotlinType.toIrType(): IrType =
        typeTranslator.translateType(this)

    fun ReceiverParameterDescriptor.generateReceiverParameterStub(): IrValueParameter =
        factory.createValueParameter(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = origin,
            name = name,
            type = type.toIrType(),
            isAssignable = false,
            symbol = IrValueParameterSymbolImpl(this),
            index = UNDEFINED_PARAMETER_INDEX,
            varargElementType = null,
            isCrossinline = false,
            isNoinline = false,
            isHidden = false,
        )

    fun createLazyAnnotations(): ReadWriteProperty<Any?, List<IrConstructorCall>> = lazyVar(stubGenerator.lock) {
        descriptor.annotations.mapNotNull(typeTranslator.constantValueGenerator::generateAnnotationConstructorCall).toMutableList()
    }
}
