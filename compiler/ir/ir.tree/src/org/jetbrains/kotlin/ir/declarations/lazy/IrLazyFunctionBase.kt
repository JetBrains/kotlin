/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.mapOptimized
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

@OptIn(DescriptorBasedIr::class)
abstract class IrLazyFunctionBase(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val name: Name,
    override var visibility: Visibility,
    override val isInline: Boolean,
    override val isExternal: Boolean,
    override val isExpect: Boolean,
    stubGenerator: DeclarationStubGenerator,
    typeTranslator: TypeTranslator
) :
    IrLazyDeclarationBase(startOffset, endOffset, origin, stubGenerator, typeTranslator),
    IrFunction {

    val initialSignatureFunction: IrFunction? by lazyVar {
        descriptor.initialSignatureDescriptor?.takeIf { it != descriptor }?.original?.let(stubGenerator::generateFunctionStub)
    }

    override var dispatchReceiverParameter: IrValueParameter? by lazyVar {
        typeTranslator.buildWithScope(this) {
            descriptor.dispatchReceiverParameter?.generateReceiverParameterStub()?.also { it.parent = this@IrLazyFunctionBase }
        }
    }
    override var extensionReceiverParameter: IrValueParameter? by lazyVar {
        typeTranslator.buildWithScope(this) {
            descriptor.extensionReceiverParameter?.generateReceiverParameterStub()?.also { it.parent = this@IrLazyFunctionBase }
        }
    }

    override var valueParameters: List<IrValueParameter> by lazyVar {
        typeTranslator.buildWithScope(this) {
            descriptor.valueParameters.mapTo(arrayListOf()) {
                stubGenerator.generateValueParameterStub(it).apply { parent = this@IrLazyFunctionBase }
            }
        }
    }

    final override var body: IrBody? = null

    final override var returnType: IrType by lazyVar {
        typeTranslator.buildWithScope(this) {
            descriptor.returnType!!.toIrType()
        }
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }

        dispatchReceiverParameter?.accept(visitor, data)
        extensionReceiverParameter?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }

        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        typeParameters = typeParameters.mapOptimized { it.transform(transformer, data) }

        dispatchReceiverParameter = dispatchReceiverParameter?.transform(transformer, data)
        extensionReceiverParameter = extensionReceiverParameter?.transform(transformer, data)
        valueParameters = valueParameters.mapOptimized { it.transform(transformer, data) }

        body = body?.transform(transformer, data)
    }
}