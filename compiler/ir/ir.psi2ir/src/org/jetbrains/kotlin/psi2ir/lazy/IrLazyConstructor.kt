/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.lazy

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyFunctionBase
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@OptIn(ObsoleteDescriptorBasedAPI::class)
class IrLazyConstructor(
    override var startOffset: Int,
    override var endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrConstructorSymbol,
    override val descriptor: ClassConstructorDescriptor,
    override var name: Name,
    override var visibility: DescriptorVisibility,
    override var isInline: Boolean,
    override var isExternal: Boolean,
    override var isPrimary: Boolean,
    override var isExpect: Boolean,
    override val stubGenerator: DeclarationStubGenerator,
    override val typeTranslator: TypeTranslator,
) : IrConstructor(), IrLazyFunctionBase {
    init {
        this.contextReceiverParametersCount = descriptor.contextReceiverParameters.size
    }

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    override var body: IrBody? = null

    override var returnType: IrType by lazyVar(stubGenerator.lock) { createReturnType() }

    override val initialSignatureFunction: IrFunction? by createInitialSignatureFunction()

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override var typeParameters: List<IrTypeParameter> by lazyVar(stubGenerator.lock) {
        typeTranslator.buildWithScope(this) {
            stubGenerator.symbolTable.withScope(this) {
                val classTypeParametersCount = descriptor.constructedClass.original.declaredTypeParameters.size
                val allConstructorTypeParameters = descriptor.typeParameters
                allConstructorTypeParameters.subList(classTypeParametersCount, allConstructorTypeParameters.size).mapTo(ArrayList()) {
                    stubGenerator.generateOrGetTypeParameterStub(it)
                }
            }
        }
    }

    override var attributeOwnerId: IrElement = this

    override val containerSource: DeserializedContainerSource?
        get() = (descriptor as? DescriptorWithContainerSource)?.containerSource

    init {
        symbol.bind(this)
    }
}