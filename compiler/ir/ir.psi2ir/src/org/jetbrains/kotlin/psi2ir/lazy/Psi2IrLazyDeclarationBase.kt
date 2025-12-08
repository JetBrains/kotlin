/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.lazy

import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.types.KotlinType
import kotlin.properties.ReadWriteProperty

interface Psi2IrLazyDeclarationBase : IrLazyDeclarationBase {
    val stubGenerator: DeclarationStubGenerator
    val typeTranslator: TypeTranslator

    override val factory: IrFactory
        get() = stubGenerator.symbolTable.irFactory

    fun KotlinType.toIrType(): IrType =
        typeTranslator.translateType(this)

    fun ReceiverParameterDescriptor.generateReceiverParameterStub(kind: IrParameterKind): IrValueParameter =
        factory.createValueParameter(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            kind = kind,
            origin = origin,
            name = name,
            type = type.toIrType(),
            isAssignable = false,
            symbol = IrValueParameterSymbolImpl(this),
            varargElementType = null,
            isCrossinline = false,
            isNoinline = false,
            isHidden = false,
        )

    override fun createLazyAnnotations(): ReadWriteProperty<Any?, List<IrConstructorCall>> = lazyVar(stubGenerator.lock) {
        descriptor.annotations.mapNotNull(typeTranslator.constantValueGenerator::generateAnnotationCall).toMutableList()
    }
}