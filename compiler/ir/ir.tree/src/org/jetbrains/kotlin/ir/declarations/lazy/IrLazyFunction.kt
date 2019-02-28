/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
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
    stubGenerator: DeclarationStubGenerator,
    typeTranslator: TypeTranslator
) :
    IrLazyFunctionBase(startOffset, endOffset, origin, name, visibility, isInline, isExternal, stubGenerator, typeTranslator),
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
        symbol.descriptor.isInline,
        symbol.descriptor.isExternal,
        symbol.descriptor.isTailrec,
        symbol.descriptor.isSuspend,
        stubGenerator,
        TypeTranslator
    )

    override val descriptor: FunctionDescriptor = symbol.descriptor

    override val typeParameters: MutableList<IrTypeParameter> by lazy {
        typeTranslator.buildWithScope(this) {
            stubGenerator.symbolTable.enterScope(descriptor)
            val propertyIfAccessor = descriptor.propertyIfAccessor
            propertyIfAccessor.typeParameters.mapTo(arrayListOf()) {
                if (descriptor != propertyIfAccessor) {
                    stubGenerator.generateOrGetScopedTypeParameterStub(it).also {
                        it.parent = this@IrLazyFunction
                    }
                } else {
                    stubGenerator.generateOrGetTypeParameterStub(it)
                }
            }.also {
                stubGenerator.symbolTable.leaveScope(descriptor)
            }
        }
    }


    override val overriddenSymbols: MutableList<IrSimpleFunctionSymbol> by lazy {
        descriptor.overriddenDescriptors.mapTo(arrayListOf()) {
            stubGenerator.generateFunctionStub(it.original).symbol
        }
    }

    @Suppress("OverridingDeprecatedMember")
    override var correspondingProperty: IrProperty?
        get() = correspondingPropertySymbol?.owner
        set(value) {
            correspondingPropertySymbol = value?.symbol
        }

    override var correspondingPropertySymbol: IrPropertySymbol? = null

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitSimpleFunction(this, data)
}