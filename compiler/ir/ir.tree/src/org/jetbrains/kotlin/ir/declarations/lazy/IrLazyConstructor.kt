/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@OptIn(ObsoleteDescriptorBasedAPI::class)
class IrLazyValueParameter(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrValueParameterSymbol,
    override val descriptor: ValueParameterDescriptor,
    override val name: Name,
    override val index: Int,
    override var type: IrType,
    override var varargElementType: IrType?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean,
    override val isHidden: Boolean,
    override val isAssignable: Boolean,
    override val stubGenerator: DeclarationStubGenerator,
    override val typeTranslator: TypeTranslator,
) : IrValueParameter(), IrLazyDeclarationBase {
    override lateinit var parent: IrDeclarationParent

    override var defaultValue: IrExpressionBody? = null

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    init {
        symbol.bind(this)
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
class IrLazyConstructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrConstructorSymbol,
    override val descriptor: ClassConstructorDescriptor,
    override val name: Name,
    override var visibility: DescriptorVisibility,
    override val isInline: Boolean,
    override val isExternal: Boolean,
    override val isPrimary: Boolean,
    override val isExpect: Boolean,
    override val stubGenerator: DeclarationStubGenerator,
    override val typeTranslator: TypeTranslator,
) : IrConstructor(), IrLazyFunctionBase {
    override var parent: IrDeclarationParent by createLazyParent()

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    override var body: IrBody? = null

    override var returnType: IrType by lazyVar(stubGenerator.lock) { createReturnType() }

    override val initialSignatureFunction: IrFunction? by createInitialSignatureFunction()

    override var dispatchReceiverParameter: IrValueParameter? by lazyVar(stubGenerator.lock) {
        createReceiverParameter(descriptor.dispatchReceiverParameter)
    }

    override var extensionReceiverParameter: IrValueParameter? by lazyVar(stubGenerator.lock) {
        createReceiverParameter(descriptor.extensionReceiverParameter)
    }

    override var valueParameters: List<IrValueParameter> by lazyVar(stubGenerator.lock) { createValueParameters() }

    override var contextReceiverParametersCount: Int = descriptor.contextReceiverParameters.size

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

    override val containerSource: DeserializedContainerSource?
        get() = (descriptor as? DescriptorWithContainerSource)?.containerSource

    init {
        symbol.bind(this)
    }
}
