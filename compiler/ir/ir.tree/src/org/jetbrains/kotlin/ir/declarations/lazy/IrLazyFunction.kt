/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor

class IrLazyFunction(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrSimpleFunctionSymbol,
    name: Name,
    visibility: Visibility,
    override val modality: Modality,
    isInline: Boolean,
    isExternal: Boolean,
    override val isTailrec: Boolean,
    override val isSuspend: Boolean,
    isExpect: Boolean,
    override val isFakeOverride: Boolean,
    override val isOperator: Boolean,
    stubGenerator: DeclarationStubGenerator,
    typeTranslator: TypeTranslator
) :
    IrLazyFunctionBase(startOffset, endOffset, origin, name, visibility, isInline, isExternal, isExpect, stubGenerator, typeTranslator),
    IrSimpleFunction {

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrSimpleFunctionSymbol,
        stubGenerator: DeclarationStubGenerator,
        TypeTranslator: TypeTranslator
    ) : this(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name,
        symbol.descriptor.visibility,
        symbol.descriptor.modality,
        isInline = symbol.descriptor.isInline,
        isExternal = symbol.descriptor.isExternal,
        isTailrec = symbol.descriptor.isTailrec,
        isSuspend = symbol.descriptor.isSuspend,
        isExpect = symbol.descriptor.isExpect,
        isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
        isOperator = symbol.descriptor.isOperator,
        stubGenerator = stubGenerator,
        typeTranslator = TypeTranslator
    )

    override val descriptor: FunctionDescriptor = symbol.descriptor

    override val typeParameters: MutableList<IrTypeParameter> by lazy {
        typeTranslator.buildWithScope(this) {
            stubGenerator.symbolTable.withScope(descriptor) {
                val propertyIfAccessor = descriptor.propertyIfAccessor
                propertyIfAccessor.typeParameters.mapTo(arrayListOf()) { typeParameterDescriptor ->
                    if (descriptor != propertyIfAccessor) {
                        stubGenerator.generateOrGetScopedTypeParameterStub(typeParameterDescriptor).also { irTypeParameter ->
                            irTypeParameter.parent = this@IrLazyFunction
                        }
                    } else {
                        stubGenerator.generateOrGetTypeParameterStub(typeParameterDescriptor)
                    }
                }
            }
        }
    }


    override val overriddenSymbols: MutableList<IrSimpleFunctionSymbol> by lazy {
        descriptor.overriddenDescriptors.mapTo(arrayListOf()) {
            stubGenerator.generateFunctionStub(it.original).symbol
        }
    }

    override var correspondingPropertySymbol: IrPropertySymbol? = null

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitSimpleFunction(this, data)
}