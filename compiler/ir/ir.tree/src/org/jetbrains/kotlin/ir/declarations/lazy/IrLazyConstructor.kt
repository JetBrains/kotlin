/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

@OptIn(DescriptorBasedIr::class)
class IrLazyConstructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrConstructorSymbol,
    override val descriptor: ClassConstructorDescriptor,
    name: Name,
    visibility: Visibility,
    isInline: Boolean,
    isExternal: Boolean,
    override val isPrimary: Boolean,
    isExpect: Boolean,
    stubGenerator: DeclarationStubGenerator,
    typeTranslator: TypeTranslator
) :
    IrLazyFunctionBase(
        startOffset, endOffset, origin, name,
        visibility, isInline, isExternal, isExpect,
        stubGenerator, typeTranslator
    ),
    IrConstructor {

    override var typeParameters: List<IrTypeParameter> by lazyVar {
        typeTranslator.buildWithScope(this) {
            stubGenerator.symbolTable.withScope(descriptor) {
                val classTypeParametersCount = descriptor.constructedClass.original.declaredTypeParameters.size
                val allConstructorTypeParameters = descriptor.typeParameters
                allConstructorTypeParameters.subList(classTypeParametersCount, allConstructorTypeParameters.size).mapTo(ArrayList()) {
                    stubGenerator.generateOrGetTypeParameterStub(it)
                }
            }
        }
    }

    init {
        symbol.bind(this)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitConstructor(this, data)
}